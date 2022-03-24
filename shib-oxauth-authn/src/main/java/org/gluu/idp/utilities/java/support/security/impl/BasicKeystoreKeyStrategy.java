package org.gluu.idp.utilities.java.support.security.impl;

import org.gluu.util.security.SecurityProviderUtility;

/**
 * Init BC/BCFIPS before invoking key storage strategy
 *
 * @author Yuriy Movchan Date: 02/05/2022
 */
public class BasicKeystoreKeyStrategy extends net.shibboleth.utilities.java.support.security.impl.BasicKeystoreKeyStrategy {
	public BasicKeystoreKeyStrategy() {
		SecurityProviderUtility.installBCProvider(true);
	}

}
