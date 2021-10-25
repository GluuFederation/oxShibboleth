package org.gluu.idp.externalauth.openid.client;

import org.gluu.idp.externalauth.openid.conf.IdpAppConfiguration;
import org.gluu.idp.externalauth.openid.conf.IdpConfigurationFactory;
import org.gluu.idp.externalauth.openid.conf.IdpAppConfigurationEntry;
import org.gluu.oxauth.client.OpenIdClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the oxAuth client which prepares AutZ requests and retrieve user profile
 * 
 * @author Yuriy Movchan
 * @version 0.1, 09/13/2018
 */
public class IdpAuthClient extends OpenIdClient<IdpAppConfiguration, IdpAppConfigurationEntry> {

	@SuppressWarnings("unused")
	private final Logger LOG = LoggerFactory.getLogger(IdpAuthClient.class);

	public IdpAuthClient(final IdpConfigurationFactory configurationFactory) {
		super(configurationFactory);
	}

}
