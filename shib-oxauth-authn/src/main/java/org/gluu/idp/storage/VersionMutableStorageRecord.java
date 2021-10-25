package org.gluu.idp.storage;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;

import org.opensaml.storage.MutableStorageRecord;

public class VersionMutableStorageRecord extends MutableStorageRecord implements Serializable, Externalizable {

	private static final long serialVersionUID = 8516163557055487227L;

	public VersionMutableStorageRecord() {
		super(null, null);
	}

	public VersionMutableStorageRecord(@Nonnull @NotEmpty final String value, @Nullable Long expiration, Long version) {
        super(value, expiration);
        super.setVersion(version);
    }

	/**
     * Converts a {@link org.opensaml.storage.StorageRecord#getExpiration()} value in milliseconds to the corresponding
     * value in seconds.
     * 
     * @param exp the expiration value
     *
     * @return 0 if given expiration is null, otherwise <code>exp/1000</code>.
     */
    public static int expiry(final Long exp) {
        return exp == null ? 0 : (int) (exp / 1000);
    }

    /**
     * Gets the expiration date as an integer representing seconds since the Unix epoch, 1970-01-01T00:00:00.
     * The value provided by this method is suitable for representing the memcached entry expiration.
     *
     * @return 0 if expiration is null, otherwise <code>getExpiration()/1000</code>.
     */
    public int getExpiry() {
        return expiry(getExpiration());
    }

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF(getValue());
		if (getExpiration() == null) {
			out.writeBoolean(false);
		} else {
			out.writeBoolean(true);
			out.writeLong(getExpiration().longValue());
		}
		out.writeLong(getVersion());
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		setValue(in.readUTF());
		if (in.readBoolean()) {
			setExpiration(in.readLong());
		}
		setVersion(in.readLong());
	}

}