package org.gluu.idp.externalauth;

import java.security.Principal;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.saml.saml2.profile.config.BrowserSSOProfileConfiguration;

import org.gluu.orm.util.StringHelper;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OxAuthReuseResultByAcr implements Predicate<ProfileRequestContext> { 
    
    private final Logger LOG = LoggerFactory.getLogger(OxAuthReuseResultByAcr.class);

    private Function<ProfileRequestContext,AuthenticationContext> authnContextLookupStrategy;

    private static final String OX_AUTH_FLOW_ID  = "authn/oxAuth";

    public OxAuthReuseResultByAcr() {

        authnContextLookupStrategy = new ChildContextLookup<ProfileRequestContext,AuthenticationContext>(AuthenticationContext.class);
    }

    @Override
    public boolean test(ProfileRequestContext profileRequestContext) {
        
        final AuthenticationContext authnContext = authnContextLookupStrategy.apply(profileRequestContext);
        if(authnContext == null) {
            //In principle this should not happen 
            LOG.debug("No Authentication context found. Re-using result");
            return true;
        }

        Map<String,AuthenticationResult> activeResultsMap = authnContext.getActiveResults();
        if(!activeResultsMap.containsKey(OX_AUTH_FLOW_ID)) {
            LOG.debug("Session does not have any authn/oxAuth results. Re-using authentication result");
            return true;
        }

        AuthenticationResult authnResult  = authnContext.getActiveResults().get(OX_AUTH_FLOW_ID);
        String usedAcr = authnResult.getAdditionalData().get(ShibOxAuthAuthServlet.OXAUTH_ACR_USED);
        String previouslyRequestedAcr = authnResult.getAdditionalData().get(ShibOxAuthAuthServlet.OXAUTH_ACR_REQUESTED);

        List<String> requestedAcrs = determineAcrs(profileRequestContext, authnContext);
        LOG.info("Used ACR {}:{}, requested ACRs: {}",usedAcr,previouslyRequestedAcr,requestedAcrs);

        if((requestedAcrs == null) || (requestedAcrs.size() == 0) ) {
            LOG.debug("There are no requested acrs. Re-using authentication result");
            return true;
        }

        for(String requestedAcr : requestedAcrs) {
            if(StringHelper.equals(usedAcr,requestedAcr) || StringHelper.equals(previouslyRequestedAcr,requestedAcr)) {
                LOG.debug("Used and requested ACR are the same: {}, Re-using authentication result",usedAcr);
                return true;
            }
        }

        LOG.debug("Will not be re-using authentication result. Auth request will be created with ACRs: {}",requestedAcrs);
        return false;
    }

    private final List<String> determineAcrs(ProfileRequestContext profileRequestContext, AuthenticationContext authnContext) {

        if(profileRequestContext == null || profileRequestContext.getInboundMessageContext() == null) {
            return null;
        }

        AuthnRequest authnRequest = (AuthnRequest) profileRequestContext.getInboundMessageContext().getMessage();
        if(authnRequest == null) {
            return null;
        }
        List<String> acrs = null;
        RequestedAuthnContext requestedAuthnContext = authnRequest.getRequestedAuthnContext();
        if(requestedAuthnContext == null) {
            Function<ProfileRequestContext,RelyingPartyContext> rpContextLookupStrategy = 
                    new ChildContextLookup<ProfileRequestContext,RelyingPartyContext>(RelyingPartyContext.class);
            final RelyingPartyContext relyingPartyContext = rpContextLookupStrategy.apply(profileRequestContext);
            
            if(relyingPartyContext != null) {
                ProfileConfiguration profileConfiguration = relyingPartyContext.getProfileConfig();
                if(profileConfiguration instanceof BrowserSSOProfileConfiguration) {
                    List<Principal> principals = ((BrowserSSOProfileConfiguration) profileConfiguration).getDefaultAuthenticationMethods(profileRequestContext);
                    acrs = principals.stream().map(Principal::getName).collect(Collectors.toList());
                }
            }
        }else {
        
            acrs = requestedAuthnContext.getAuthnContextClassRefs().stream()
                               .map(AuthnContextClassRef::getAuthnContextClassRef).collect(Collectors.toList());
        }

        return acrs;
    }
    
}
