package org.gluu.idp.utilities.java.support.security;

import javax.annotation.Nonnull;

import org.gluu.util.security.SecurityProviderUtility;

/**
 * Init BC/BCFIPS before invoking tool
 *
 * @author Yuriy Movchan Date: 02/05/2022
 */
public class BasicKeystoreKeyStrategyTool
		extends net.shibboleth.utilities.java.support.security.BasicKeystoreKeyStrategyTool {

	public static void main(@Nonnull final String[] args) throws Exception {
		SecurityProviderUtility.installBCProvider(true);

		net.shibboleth.utilities.java.support.security.BasicKeystoreKeyStrategyTool.main(args);
	}

}
