package org.gluu.idp.externalauth.openid.conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.conf.service.ConfigurationFactory;

/**
 * IDP configuration factory
 * 
 * @author Yuriy Movchan
 * @version 0.1, 09/13/2018
 */
public final class IdpConfigurationFactory extends ConfigurationFactory<IdpAppConfiguration, IdpAppConfigurationEntry> {

	@SuppressWarnings("unused")
	private final Logger LOG = LoggerFactory.getLogger(IdpConfigurationFactory.class);

	private static class ConfigurationSingleton {
		static IdpConfigurationFactory INSTANCE = new IdpConfigurationFactory();
	}

	public static IdpConfigurationFactory instance() {
		return ConfigurationSingleton.INSTANCE;
	}

	@Override
	protected String getDefaultConfigurationFileName() {
		return "gluu.properties";
	}

	@Override
	protected Class<IdpAppConfigurationEntry> getAppConfigurationType() {
		return IdpAppConfigurationEntry.class;
	}

	@Override
	protected String getApplicationConfigurationPropertyName() {
		return "oxidp_ConfigurationEntryDN";
	}

}
