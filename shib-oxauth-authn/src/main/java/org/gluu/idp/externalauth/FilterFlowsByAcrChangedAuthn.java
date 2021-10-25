package org.gluu.idp.externalauth;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.gluu.util.StringHelper;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.saml.saml2.profile.config.BrowserSSOProfileConfiguration;

/**
 * @author Yuriy Movchan on 09/13/2021
 */
public class FilterFlowsByAcrChangedAuthn extends AbstractAuthenticationAction {

	private static final String OX_AUTH_FLOW_ID = "authn/oxAuth";
	private final Logger LOG = LoggerFactory.getLogger(FilterFlowsByAcrChangedAuthn.class);
	
	private boolean disabledAcrCheck = false;

	public FilterFlowsByAcrChangedAuthn() {
	}

	protected boolean doPreExecute(ProfileRequestContext profileRequestContext,
			AuthenticationContext authenticationContext) {
		if (!super.doPreExecute(profileRequestContext, authenticationContext)) {
			return false;
		}
		
		if (disabledAcrCheck) {
			return false;
		}
		
		Map<String, AuthenticationResult> activeResultsMap = authenticationContext.getActiveResults();
		if (!activeResultsMap.containsKey(OX_AUTH_FLOW_ID)) {
			LOG.debug("{} Session does not have authn/oxAuth results, nothing to do", getLogPrefix());
			return false;
		}

		AuthenticationResult authenticationResult = authenticationContext.getActiveResults().get(OX_AUTH_FLOW_ID);
		String usedAcr = authenticationResult.getAdditionalData().get(ShibOxAuthAuthServlet.OXAUTH_ACR_USED);
		String previousRequestedAcr = authenticationResult.getAdditionalData().get(ShibOxAuthAuthServlet.OXAUTH_ACR_REQUESTED);

		List<String> requestedAcrs = determineAcrs(profileRequestContext);
		LOG.debug("{} Used ACR: {}:{}, requested ACRs: {}", getLogPrefix(), usedAcr, previousRequestedAcr, requestedAcrs);
		
		if ((requestedAcrs == null) || (requestedAcrs.size() == 0)) {
			LOG.debug("{} There is no requested ACRs , nothing to do", getLogPrefix());
			return false;
		}
		
		for (String requestedAcr : requestedAcrs) {
			if (StringHelper.equals(usedAcr, requestedAcr) || StringHelper.equals(previousRequestedAcr, requestedAcr)) {
				LOG.debug("{} Used and requested ACR are the same: {}, nothing to do", getLogPrefix(), usedAcr);
				return false;
			}
		}

		LOG.debug("{} Force to create new AuthZ request with new ACRs: {}, nothing to do", getLogPrefix(), requestedAcrs);
		return true;
	}
	
	protected List<String> determineAcrs(ProfileRequestContext profileRequestContext) {
        AuthnRequest authnRequest = (AuthnRequest) profileRequestContext.getInboundMessageContext().getMessage();
        if (authnRequest == null) {
        	return null;
        }
        
        List<String> acrs = null;
        RequestedAuthnContext authnContext = authnRequest.getRequestedAuthnContext();
        if (authnContext == null) {
            Function<ProfileRequestContext, RelyingPartyContext> authenticationContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
            final RelyingPartyContext relyingPartyContext = authenticationContextLookupStrategy.apply(profileRequestContext);
            if (relyingPartyContext != null) {
            	ProfileConfiguration profileConfiguration = relyingPartyContext.getProfileConfig();
            	if (profileConfiguration instanceof BrowserSSOProfileConfiguration) {
            		List<Principal> principals = ((BrowserSSOProfileConfiguration) profileConfiguration).getDefaultAuthenticationMethods(profileRequestContext);
                    acrs = principals.stream()
                            .map(Principal::getName).collect(Collectors.toList());
            	}
            }
        } else {
            acrs = authnContext.getAuthnContextClassRefs().stream()
                .map(AuthnContextClassRef::getAuthnContextClassRef).collect(Collectors.toList());
        }
        
        return acrs;
	}

	protected void doExecute(ProfileRequestContext profileRequestContext, AuthenticationContext authenticationContext) {
		if (!disabledAcrCheck) {
			authenticationContext.getActiveResults().clear();
			LOG.info("{} Removed all active results to force authentication", getLogPrefix());
		}
	}

	public boolean isDisabledAcrCheck() {
		return disabledAcrCheck;
	}

	public void setDisabledAcrCheck(boolean disabledAcrCheck) {
		this.disabledAcrCheck = disabledAcrCheck;
	}

}
