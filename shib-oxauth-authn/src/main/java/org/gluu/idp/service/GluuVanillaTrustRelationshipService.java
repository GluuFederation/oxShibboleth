package org.gluu.idp.service;

import java.util.List;

import org.gluu.idp.externalauth.openid.conf.IdpConfigurationFactory;
import org.gluu.search.filter.Filter;

import org.gluu.idp.model.GluuVanillaTrustRelationship;
import org.gluu.persist.PersistenceEntryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GluuVanillaTrustRelationshipService {
    
    private static final String ORG_DN = "o=gluu";
    private static final Logger log = LoggerFactory.getLogger(GluuVanillaTrustRelationshipService.class);
    
    private PersistenceEntryManager persistenceEntryManager;

    public GluuVanillaTrustRelationshipService(final IdpConfigurationFactory configurationFactory) {
        
        persistenceEntryManager = configurationFactory.getPersistenceEntryManager();
    }

    public GluuVanillaTrustRelationship findTrustRelationshipByInum(String inum) {

        try {
            log.debug("findTrustRelationshipByInum() -- inum {}",inum);
            String [] attributes=  new String[] {"dn","inum","oxAuthPostLogoutRedirectURI","spLogoutRedirectUrl"};
            return persistenceEntryManager.find(getDnForTrustRelationship(inum),GluuVanillaTrustRelationship.class,attributes);
        }catch(Exception e) {
            log.error(String.format("Error fetching TrustRelationship with inum -- %s",inum),e);
            return null;
        }
    }

    public GluuVanillaTrustRelationship findTrustRelationshipByRelyingParty(String relyingPartyId) {

        try {
            log.debug("findTrustRelationshipByRelyingParty() -- relyingPartyId {}",relyingPartyId);
            final String [] attributes = new String [] {"dn","inum","oxAuthPostLogoutRedirectURI","spLogoutRedirectUrl"}; 
            String dn = getDnForTrustRelationship(null);
            Filter filter =  Filter.createEqualityFilter("gluuEntityId",relyingPartyId).multiValued();
            List<GluuVanillaTrustRelationship> trlist = persistenceEntryManager.findEntries(dn,GluuVanillaTrustRelationship.class,filter,attributes,1);
            if(trlist.isEmpty()) {
                log.debug("findTrustRelationshipByRelyingParty() -- no entries found for relyingParty {}",relyingPartyId);
                return null;
            }
            return trlist.get(0);
        }catch(Exception e) {
            log.error(String.format("Error fetching TrustRelationship with relyingPartyId -- %s",relyingPartyId),e);
            return null;
        }
    }

    public String getDnForTrustRelationship(final String inum) {

        if(inum == null || inum.isEmpty()) {

            return String.format("ou=trustRelationships,%s",ORG_DN);
        }else {
            return String.format("inum=%s,ou=trustRelationships,%s",inum,ORG_DN);
        }
    }
}
