package org.gluu.idp.storage;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.TimerTask;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.cryptacular.util.ByteUtil;
import org.cryptacular.util.CodecUtil;
import org.cryptacular.util.HashUtil;
import org.gluu.idp.externalauth.openid.conf.IdpConfigurationFactory;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.service.cache.CacheConfiguration;
import org.gluu.service.cache.CacheProvider;
import org.gluu.service.cache.StandaloneCacheProviderFactory;
import org.gluu.util.security.StringEncrypter;
import org.opensaml.storage.AbstractStorageService;
import org.opensaml.storage.StorageCapabilitiesEx;
import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.VersionMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.annotation.constraint.NonNegative;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Gluu Storage Service for IDP
 * 
 * @author Yuriy Movchan
 * @version 0.1, 01/21/2020
 */
public class GluuStorageService extends AbstractStorageService implements StorageCapabilitiesEx {

	@SuppressWarnings("unused")
	private final Logger LOG = LoggerFactory.getLogger(GluuStorageService.class);

    @NonNegative private Duration contextExpiration;

	/** Maximum length in bytes of memcached keys. */
    private static final int MAX_KEY_LENGTH = 250;

	private final IdpConfigurationFactory configurationFactory;
	private CacheProvider<?> cacheProvider;

    public GluuStorageService(final IdpConfigurationFactory configurationFactory) {
    	LOG.debug("GluuStorage: create");
        Constraint.isNotNull(configurationFactory, "Configuration factory cannot be null");
        this.configurationFactory = configurationFactory;

        this.cacheProvider = createCacheProvider();
        Constraint.isNotNull(cacheProvider, "Cache Provider factory cannot be null");
        
        this.contextExpiration = Duration.ZERO;

        initCacheCapabilities();
    }

	private CacheProvider<?> createCacheProvider() {
    	LOG.debug("GluuStorage: createCacheProvider");
		StringEncrypter stringEncrypter = configurationFactory.getStringEncrypter();
		PersistenceEntryManager persistenceEntryManager = configurationFactory.getPersistenceEntryManager();
		CacheConfiguration cacheConfiguration = configurationFactory.getCacheConfiguration();

		StandaloneCacheProviderFactory cacheProviderFactory = new StandaloneCacheProviderFactory(persistenceEntryManager, stringEncrypter);

		return cacheProviderFactory.getCacheProvider(cacheConfiguration);
	}

	/*
	 * ========================================================================
	 * Storage Service part  
	 * ========================================================================
	 */

	private void initCacheCapabilities() {
    	LOG.debug("GluuStorage: initCacheCapabilities");
		this.setContextSize(Integer.MAX_VALUE);
        this.setKeySize(Integer.MAX_VALUE);
        this.setValueSize(Integer.MAX_VALUE);
	}

    @Override
    public boolean create(@Nonnull String context, @Nonnull String key, @Nonnull String value, @Nullable Long expiration) throws IOException {
    	LOG.debug("GluuStorage: create");
        Constraint.isNotNull(StringSupport.trimOrNull(context), "Context cannot be null or empty");
        Constraint.isNotNull(StringSupport.trimOrNull(key), "Key cannot be null or empty");
        Constraint.isNotNull(StringSupport.trimOrNull(value), "Value cannot be null or empty");

        VersionMutableStorageRecord record = new VersionMutableStorageRecord(value, expiration, 1L);

        final int expiry = record.getExpiry();
        Constraint.isGreaterThan(-1, expiry, "Expiration must be null or positive");
        String namespace = lookupNamespace(context);
        if (namespace == null) {
            namespace = createNamespace(context);
        }
        final String cacheKey = memcachedKey(namespace, key);

        LOG.debug("Creating new entry at {} for context={}, key={}, exp={}", cacheKey, context, key, expiry);
        try {
        	int ttl = getSystemExpiration(record.getExpiration());
            LOG.debug("Create cache record for key={}, ttl={}", key, ttl);
        	cacheProvider.put(ttl, cacheKey, record);
        } catch (final Exception ex) {
            LOG.error("Failed to put object into cache, key: '{}'", cacheKey, ex);
            
            return false;
        }
        
        return true;
    }

    @Nullable
    @Override
    public StorageRecord read(@Nonnull String context, @Nonnull String key) throws IOException {
    	LOG.debug("GluuStorage: read (key)");
        Constraint.isNotNull(StringSupport.trimOrNull(context), "Context cannot be null or empty");
        Constraint.isNotNull(StringSupport.trimOrNull(key), "Key cannot be null or empty");

        final String namespace = lookupNamespace(context);
        if (namespace == null) {
            LOG.debug("Namespace for context {} does not exist", context);
            return null;
        }
        final String cacheKey = memcachedKey(namespace, key);

        LOG.debug("Reading entry at {} for context={}, key={}", cacheKey, context, key);
        final VersionMutableStorageRecord record;
        try {
            record = (VersionMutableStorageRecord) cacheProvider.get(cacheKey);
        } catch (final Exception ex) {
            LOG.error("Failed to get object from cache, key: '{}'", cacheKey, ex);
            throw new IOException("Cache Provider operation failed", ex);
        }
        LOG.debug("Read entry at {} for context={}, key={}, value={}", cacheKey, context, key, record);

        if (record == null) {
            return null;
        }

        return record;
    }

    @Nonnull
    @Override
    public Pair<Long, StorageRecord> read(@Nonnull String context, @Nonnull String key, long version) throws IOException {
    	LOG.debug("GluuStorage: read (key and version)");
        Constraint.isGreaterThan(0, version, "Version must be positive");
        final StorageRecord record = read(context, key);
        if (record == null) {
            return new Pair<>();
        }
        final Pair<Long, StorageRecord> result = new Pair<>(record.getVersion(), null);
        if (version != record.getVersion()) {
            // Only set the record if it's not the same as the version requested
            result.setSecond(record);
        } else {
            LOG.debug("Record has invalid version context={}, key={}, version={}", context, key, version);
        }

        return result;
    }

    private Long doUpdate(final Long version, final String context, final String key, final String value, final Long expiration) throws IOException {
    	LOG.debug("GluuStorage: doUpdate");
        Constraint.isNotNull(StringSupport.trimOrNull(context), "Context cannot be null or empty");
        Constraint.isNotNull(StringSupport.trimOrNull(key), "Key cannot be null or empty");

        final String namespace = lookupNamespace(context);
        if (namespace == null) {
            LOG.debug("Namespace for context {} does not exist", context);
            return null;
        }
        final String cacheKey = memcachedKey(namespace, key);

        LOG.debug("Updating entry at {} for context={}, key={}, exp={}, version={}", cacheKey, context, key, expiration, version);
        // Load record to make sure that we increase version in any case even if version not specified as parameter 
        VersionMutableStorageRecord record = (VersionMutableStorageRecord) this.read(context, key);
        if (record == null) {
            return null;
        }

        if ((version != null) && (version != record.getVersion())) {
            throw new VersionMismatchWrapperException(new VersionMismatchException());
        }

        if (value != null) {
            record.setValue(value);
            record.incrementVersion();
        }

        record.setExpiration(expiration);

        final int expiry = record.getExpiry();
        Constraint.isGreaterThan(-1, expiry, "Expiration must be null or positive");

        try {
        	int ttl = getSystemExpiration(record.getExpiration());
            LOG.debug("Update cache record for key={}, ttl={}", key, ttl);
        	cacheProvider.put(ttl, cacheKey, record);
        } catch (final Exception ex) {
            LOG.error("Failed to update object in cache, key: '{}'", cacheKey, ex);
            
            return null;
        }

        return record.getVersion();
    }

    @Override
    public boolean update(@Nonnull String context, @Nonnull String key, @Nonnull String value, @Nullable Long expiration) throws IOException {
    	LOG.debug("GluuStorage: update");
        Constraint.isNotNull(StringSupport.trimOrNull(value), "Value cannot be null or empty");
        return doUpdate(null, context, key, value, expiration) != null;
    }

    @Nullable
    @Override
    public Long updateWithVersion(long version, @Nonnull String context, @Nonnull String key, @Nonnull String value, @Nullable Long expiration) throws IOException, VersionMismatchException {
    	LOG.debug("GluuStorage: updateWithVersion");
        try {
            Constraint.isNotNull(StringSupport.trimOrNull(value), "Value cannot be null or empty");
            return doUpdate(version, context, key, value, expiration);
        } catch (VersionMismatchWrapperException ex) {
            throw (VersionMismatchException)ex.getCause();
        }
    }

    @Override
    public boolean updateExpiration(@Nonnull String context, @Nonnull String key, @Nullable Long expiration) throws IOException {
    	LOG.debug("GluuStorage: updateExpiration");
        Constraint.isGreaterThan(-1, expiration, "Expiration must be null or positive");
        return doUpdate(null, context, key, null, expiration) != null;
    }

    private boolean doDelete(String context, String key, Long version) throws IOException {
    	LOG.debug("GluuStorage: doDelete");
        Constraint.isNotNull(StringSupport.trimOrNull(context), "Context cannot be null or empty");
        Constraint.isNotNull(StringSupport.trimOrNull(key), "Key cannot be null or empty");

        final String namespace = lookupNamespace(context);
        if (namespace == null) {
            LOG.debug("Namespace for context {} does not exist", context);
            return false;
        }
        final String cacheKey = memcachedKey(namespace, key);

        LOG.debug("Deleting entry at {} for context={}, key={}, version={}", cacheKey, context, key, version);
        if (version != null) {
            VersionMutableStorageRecord record = (VersionMutableStorageRecord) this.read(context, key);
            if (record == null) {
                return false;
            }

            if (version != null && (record.getVersion() != version)) {
                throw new VersionMismatchWrapperException(new VersionMismatchException());
            }
        }

        try {
        	cacheProvider.remove(cacheKey);
        } catch (final Exception ex) {
            LOG.error("Failed to remove object from cache, key: '{}'", cacheKey, ex);
            
            return false;
        }

        return true;
    }

    @Override
    public boolean delete(@Nonnull String context, @Nonnull String key) throws IOException {
    	LOG.debug("GluuStorage: delete");
        return doDelete(context, key, null);
    }

    @Override
    public boolean deleteWithVersion(long version, @Nonnull String context, @Nonnull String key) throws IOException, VersionMismatchException {
    	LOG.debug("GluuStorage: deleteWithVersion");
        try {
            return doDelete(context, key, version);
        } catch (VersionMismatchWrapperException e) {
            throw (VersionMismatchException)e.getCause();
        }
    }

    @Override
    public void reap(@Nonnull String context) throws IOException {
    	LOG.debug("GluuStorage: reap");
        // handled internally
    }

    @Override
    public void updateContextExpiration(@Nonnull String context, @Nullable Long expiration) throws IOException {
    	LOG.debug("GluuStorage: updateContextExpiration");
        Constraint.isNotNull(StringSupport.trimOrNull(context), "Context cannot be null or empty");
        final int expiry = VersionMutableStorageRecord.expiry(expiration);
        Constraint.isGreaterThan(-1, expiry, "Expiration must be null or positive");

        throw new UnsupportedOperationException(
                "updateContextExpiration is not supported");
    }

    @Override
    public void deleteContext(@Nonnull String context) throws IOException {
    	LOG.debug("GluuStorage: deleteContext");
        Constraint.isNotNull(StringSupport.trimOrNull(context), "Context cannot be null or empty");

        final String namespace = lookupNamespace(context);
        if (namespace == null) {
            LOG.debug("Namespace for context {} does not exist. Context values effectively deleted.", context);
            return;
        }

        try {
        	cacheProvider.remove(namespace);
        	cacheProvider.remove(context);
        } catch (final Exception ex) {
            LOG.error("Failed to delete context {} from cache ", context, ex);
        }
    }

	@Override
	public boolean isServerSide() {
    	LOG.debug("GluuStorage: isServerSide");
		return true;
	}

	@Override
	public boolean isClustered() {
    	LOG.debug("GluuStorage: isClustered");
		return true;
	}

	/**
     * Looks up the namespace for the given context name in the cache.
     *
     * @param context Context name.
     *
     * @return Corresponding namespace for given context or null if no namespace exists for context.
     *
     * @throws java.io.IOException On memcached operation errors.
     */
    protected String lookupNamespace(final String context) throws IOException {
    	LOG.debug("GluuStorage: lookupNamespace");
        
    	final String cacheKey = memcachedKey(context);

        LOG.trace("Lookup namespace with context={}, key={}", context, cacheKey);

    	try {
            return (String) cacheProvider.get(cacheKey);
        } catch (final Exception ex) {
            throw new IOException("Memcached operation failed", ex);
        }
    }

    /**
     * Creates a cache-wide unique namespace for the given context name. The context-namespace mapping is stored
     * in the cache.
     *
     * @param context Context name.
     *
     * @return Namespace name for given context.
     *
     * @throws java.io.IOException On memcached operation errors.
     */
    protected String createNamespace(final String context) throws IOException {
    	LOG.debug("GluuStorage: createNamespace");

        LOG.trace("Create namespace with context={}", context);

        int maxIterations = 10;
        String namespace =  null;

        boolean success = false;
        // Perform successive add operations until success to ensure unique namespace
        while (!success && (maxIterations-- >= 0)) {
            namespace = UUID.randomUUID().toString() + "_" + CodecUtil.hex(ByteUtil.toBytes(System.currentTimeMillis()));
            LOG.trace("Create namespace with context={}, namespace={}", context, namespace);
            // Namespace values are safe for memcached keys
            try {
            	boolean foundNamespace = cacheProvider.hasKey(namespace);
            	if (foundNamespace) {
                    LOG.trace("Namespace={} already exists", namespace);
            		// Find another one due to conflict
            		continue;
            	}
                LOG.trace("Storing namespace={}, expiration={}", namespace, contextExpiration.getSeconds());
                cacheProvider.put((int) contextExpiration.getSeconds(), namespace, context);
                success = true;
            } catch (Exception ex) {}
        }
        if (!success) {
            throw new IllegalStateException("Failed to create namespace for context " + context);
        }

        // Create the reverse mapping to support looking up namespace by context name
        try {
        	final String cacheKey = memcachedKey(context);
            LOG.trace("Storing reverse mapping for namespace={}, context={}, key={}, expiration={}", namespace, context, cacheKey, contextExpiration.getSeconds());
            cacheProvider.put((int) contextExpiration.getSeconds(), cacheKey, namespace);
        } catch (Exception ex) {
            throw new IllegalStateException(context + " already exists");
        }

        return namespace;
    }
    /**
     * Creates a memcached key from one or more parts.
     *
     * @param parts Key parts (i.e. namespace, local name)
     *
     * @return Key comprised of 250 characters or less.
     */
    private String memcachedKey(final String ... parts) {
        final String key;
        if (parts.length > 0) {
            final StringBuilder sb = new StringBuilder();
            int i = 0;
            for (final String part : parts) {
                if (i++ > 0) {
                    sb.append(':');
                }
                sb.append(part);
            }
            key = sb.toString();
        } else {
            key = parts[0];
        }
        if (key.length() > MAX_KEY_LENGTH) {
            return CodecUtil.hex(HashUtil.sha512(key));
        }
        return key;
    }

    @Override
	public TimerTask getCleanupTask() {
        return new TimerTask() {

            @Override
            public void run() {
                final Long now = System.currentTimeMillis();
                LOG.debug("Running cleanup task at {}", now);
                try {
                    cacheProvider.cleanup(new Date(now));
                } catch (final Exception e) {
                    LOG.error("Error running cleanup task for {}", now, e);
                }
                LOG.debug("Finished cleanup task for {}", now);
            }
        };
	}

    public void setContextExpiration(@NonNegative final Duration contextExpiration) {
    	LOG.debug("GluuStorage: setContextExpiration");
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        Constraint.isNotNull(contextExpiration, "Context expiration interval cannot be null");
        Constraint.isFalse(contextExpiration.isNegative(), "Context expiration interval must be greater than or equal to zero");
        
        this.contextExpiration = contextExpiration;
    }

    private int getSystemExpiration(Long expiration) {
        return (int) ((expiration == null || expiration == 0) ? 0 : (expiration - System.currentTimeMillis()) / 1000);
    }

	public static class VersionMismatchWrapperException extends RuntimeException {
        public VersionMismatchWrapperException(Throwable cause) {
            super(cause);
        }
    }

}
