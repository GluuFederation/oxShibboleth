package org.gluu.idp.externalauth.openid.conf;

import org.gluu.conf.model.AppConfigurationEntry;
import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.JsonObject;

/**
 * Ldap application configuration model
 * 
 * @author Yuriy Movchan
 * @version 0.1, 09/13/2018
 */
public class IdpAppConfigurationEntry extends AppConfigurationEntry {

    private static final long serialVersionUID = -7301311833970330177L;

    @JsonObject
    @AttributeName(name = "oxConfApplication")
    private IdpAppConfiguration application;

    public IdpAppConfiguration getApplication() {
        return application;
    }

    public void setApplication(IdpAppConfiguration application) {
        this.application = application;
    }

}
