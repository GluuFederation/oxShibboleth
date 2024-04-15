package org.gluu.idp.service;

import java.util.function.Function;

import net.shibboleth.idp.profile.context.RelyingPartyContext;
import org.gluu.idp.model.GluuVanillaTrustRelationship;
import org.gluu.idp.service.GluuVanillaTrustRelationshipService;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class GluuCustomViewService {
    
    private final Logger log = LoggerFactory.getLogger(GluuCustomViewService.class);
    private GluuVanillaTrustRelationshipService trService;

    public GluuCustomViewService(final GluuVanillaTrustRelationshipService trService) {

        log.info("GluuCustomViewService() constructor");
        this.trService = trService;
    }

    public String getRelyingPartyLogoutRedirectUrl(final ProfileRequestContext prContext) {

        try {
            log.debug("Getting logout url for the currently active relying party");
            final Function<ProfileRequestContext,RelyingPartyContext> rpCtxLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
            final RelyingPartyContext rpCtx =  rpCtxLookupStrategy.apply(prContext);
            if(rpCtx == null) {
                log.debug("Could not obtain the relying party context from the profile request context");
                return null;
            }
            GluuVanillaTrustRelationship tr = trService.findTrustRelationshipByRelyingParty(rpCtx.getRelyingPartyId());
            if(tr == null) {
                log.debug("No trust relationship found associated to the relying party {}",rpCtx.getRelyingPartyId());
                return null;
            }
            return tr.getSpLogoutRedirectUrl();
        }catch(Exception e) {
            log.debug("Error while fetching logout url for currently active relying party",e);
            return null;
        }
    }
}
