package org.gluu.idp.externalauth;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.gluu.util.StringHelper;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.attribute.filter.AttributeFilter;
import net.shibboleth.idp.authn.impl.ValidateExternalAuthentication;

import net.shibboleth.utilities.java.support.service.ReloadableService;

/**
 * @author Yuriy Movchan on 09/13/2021
 */
public class OxAuthValidateExternalAuthentication extends ValidateExternalAuthentication {


	public OxAuthValidateExternalAuthentication() {
		super(null);
	}

	public OxAuthValidateExternalAuthentication(@Nullable final ReloadableService<AttributeFilter> filterService) {
		super(filterService);
	}

	/** {@inheritDoc} */
	@Override
	protected void buildAuthenticationResult(@Nonnull final ProfileRequestContext profileRequestContext,
			@Nonnull final AuthenticationContext authenticationContext) {
		super.buildAuthenticationResult(profileRequestContext, authenticationContext);

		Object usedAcr = authenticationContext.getAuthenticationStateMap().get(ShibOxAuthAuthServlet.OXAUTH_ACR_USED);
		if (usedAcr != null) {
			final AuthenticationResult result = authenticationContext.getAuthenticationResult();
			result.getAdditionalData().put(ShibOxAuthAuthServlet.OXAUTH_ACR_USED, StringHelper.toString(usedAcr));
		}

		Object requestedAcr = authenticationContext.getAuthenticationStateMap().get(ShibOxAuthAuthServlet.OXAUTH_ACR_REQUESTED);
		if (requestedAcr != null) {
			final AuthenticationResult result = authenticationContext.getAuthenticationResult();
			result.getAdditionalData().put(ShibOxAuthAuthServlet.OXAUTH_ACR_REQUESTED, StringHelper.toString(requestedAcr));
		}
	}

}