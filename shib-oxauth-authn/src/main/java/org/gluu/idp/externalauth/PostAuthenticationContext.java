package org.gluu.idp.externalauth;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gluu.oxauth.client.auth.user.UserProfile;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.authn.context.AuthenticationContext;

/**
 * Translate attributes context
 *
 * @author Yuriy Movchan
 * @version 0.1, 10/20/2021
 */
public class PostAuthenticationContext implements Serializable {

	private static final long serialVersionUID = 1922377169827670256L;

	private final HttpServletRequest request;
	private final HttpServletResponse response;

	private final ProfileRequestContext profileRequestContext;
	private final AuthenticationContext authenticationContext;

	private final UserProfile userProfile;
	private final String authenticationKey;

	public PostAuthenticationContext(HttpServletRequest request, HttpServletResponse response,
			ProfileRequestContext profileRequestContext, AuthenticationContext authenticationContext,
			UserProfile userProfile, String authenticationKey) {
		this.request = request;
		this.response = response;
		this.profileRequestContext = profileRequestContext;
		this.authenticationContext = authenticationContext;
		this.userProfile = userProfile;
		this.authenticationKey = authenticationKey;
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	public HttpServletResponse getResponse() {
		return response;
	}

	public ProfileRequestContext getProfileRequestContext() {
		return profileRequestContext;
	}

	public AuthenticationContext getAuthenticationContext() {
		return authenticationContext;
	}

	public UserProfile getUserProfile() {
		return userProfile;
	}

	public String getAuthenticationKey() {
		return authenticationKey;
	}

}
