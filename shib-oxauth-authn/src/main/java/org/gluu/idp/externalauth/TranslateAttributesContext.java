package org.gluu.idp.externalauth;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;

import org.gluu.idp.service.GluuAttributeMappingService;
import org.gluu.oxauth.client.auth.user.UserProfile;

/**
 * Translate attributes context
 *
 * @author Yuriy Movchan
 * @version 0.1, 06/22/2020
 */
public class TranslateAttributesContext implements Serializable {

	private static final long serialVersionUID = 1922377169827670256L;

	private final HttpServletRequest request;
	private final HttpServletResponse response;
	private final UserProfile userProfile;
	private final String authenticationKey;
	private final List<IdPAttribute> idpAttributes;
	private GluuAttributeMappingService gluuAttributeMappingService;

	public TranslateAttributesContext(HttpServletRequest request, HttpServletResponse response, UserProfile userProfile,
			String authenticationKey, List<IdPAttribute> idpAttributes) {
		this.request = request;
		this.response = response;
		this.userProfile = userProfile;
		this.authenticationKey = authenticationKey;
		this.idpAttributes =  idpAttributes;
		this.gluuAttributeMappingService = null;
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	public HttpServletResponse getResponse() {
		return response;
	}

	public UserProfile getUserProfile() {
		return userProfile;
	}

	public String getAuthenticationKey() {
		return authenticationKey;
	}

	public GluuAttributeMappingService getGluuAttributeMappingService() {

		return this.gluuAttributeMappingService;
	}

	public void setGluuAttributeMappingService(GluuAttributeMappingService gluuAttributeMappingService) {

		this.gluuAttributeMappingService = gluuAttributeMappingService;
	}
	
	public void addIdpAttribute(IdPAttribute idpAttribute) {

		this.idpAttributes.add(idpAttribute);
	}

	public void addIdpAttribute(String id, List<IdPAttributeValue> values) {

		IdPAttribute newAttribute = new IdPAttribute(id);
		newAttribute.setValues(values);
		this.idpAttributes.add(newAttribute);
	}

	public void addIdpStringAttribute(String id, String value) {

		List<IdPAttributeValue> values = new ArrayList<IdPAttributeValue>();
		values.add(new StringAttributeValue(value));
		addIdpAttribute(id,values);
	}

	public List<IdPAttribute> getIdpAttributes() {

		return this.idpAttributes;
	}
}
