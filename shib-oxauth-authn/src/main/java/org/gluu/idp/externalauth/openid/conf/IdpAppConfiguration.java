package org.gluu.idp.externalauth.openid.conf;

import org.gluu.conf.model.AppConfiguration;

/**
 * Idp application configuration
 * 
 * @author Yuriy Movchan
 * @version 0.1, 09/13/2018
 */
public class IdpAppConfiguration extends AppConfiguration {

	private static final long serialVersionUID = 5450226508968717097L;

	private String scriptDn;

	public String getScriptDn() {
		return scriptDn;
	}

	public void setScriptDn(String scriptDn) {
		this.scriptDn = scriptDn;
	}

}
