package org.gluu.idp.service;

import java.io.Serializable;
import java.util.List;

import org.gluu.idp.externalauth.openid.conf.IdpConfigurationFactory;
import org.gluu.idp.model.GluuAttribute;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.search.filter.Filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to map openid claim names  
 * to SAML2 Attribute names 
 */
public class GluuAttributeMappingService implements Serializable {

    
    private final Logger LOG = LoggerFactory.getLogger(GluuAttributeMappingService.class);

    private PersistenceEntryManager persistenceEntryManager;

    public GluuAttributeMappingService(final IdpConfigurationFactory configurationFactory) {

        persistenceEntryManager = configurationFactory.getPersistenceEntryManager();
    }


    public String getSaml2AttributeNameFromClaimName(String claimName) {
        
        try {
            LOG.debug("Attempting to map claim `"+ claimName + "` to gluu attribute name");
            if(claimName == null || claimName.isEmpty()) {
                LOG.debug("Specified claim is null or empty");
                return null;
            }
            String dn = getDnForAttribute(null);

            if(claimName.equalsIgnoreCase("username")) {
                claimName = "user_name";
            }

            Filter searchFilter = Filter.createEqualityFilter("oxAuthClaimName",claimName);
            List<GluuAttribute> attributes = persistenceEntryManager.findEntries(dn,GluuAttribute.class,searchFilter);
            if(attributes.isEmpty()) {
                LOG.debug("No attribute found for claim `" + claimName + "`");
                return null;
            }
            return attributes.get(0).getName();
        }catch(Exception e) {
            LOG.debug("An error occured during claim <--> attribute mapping",e);
            return null;
        }
    }


    private final String getDnForAttribute(String inum) {

        String organizationDn = "o=gluu";

        if(inum == null || (inum != null && inum.isEmpty())) {

            return String.format("ou=attributes,%s",organizationDn);
        }

        return String.format("inum=%s,ou=attributes,%s", inum, organizationDn);
    }
}
