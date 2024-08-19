package org.gluu.idp.externalauth;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;

import javax.security.auth.Subject;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.gluu.context.J2EContext;
import org.gluu.context.WebContext;
import org.gluu.idp.context.GluuScratchContext;
import org.gluu.idp.externalauth.openid.client.IdpAuthClient;
import org.gluu.idp.script.service.IdpCustomScriptManager;
import org.gluu.idp.script.service.external.IdpExternalScriptService;
import org.gluu.idp.service.GluuAttributeMappingService;
import org.gluu.oxauth.client.auth.principal.OpenIdCredentials;
import org.gluu.oxauth.client.auth.user.UserProfile;
import org.gluu.oxauth.model.exception.InvalidJwtException;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.util.StringHelper;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.webflow.context.ExternalContextHolder;
import org.springframework.webflow.context.servlet.ServletExternalContext;
import org.springframework.webflow.engine.impl.FlowExecutionImpl;
import org.springframework.webflow.execution.FlowExecutionFactory;
import org.springframework.webflow.execution.FlowExecutionKey;
import org.springframework.webflow.execution.repository.FlowExecutionRepository;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.springframework.webflow.executor.FlowExecutorImpl;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.ExternalAuthentication;
import net.shibboleth.idp.authn.ExternalAuthenticationException;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.saml.saml2.profile.config.BrowserSSOProfileConfiguration;

/**
 * A Servlet that validates the oxAuth code and then pushes the authenticated
 * principal name into the correct location before handing back control to Shib
 *
 * @author Yuriy Movchan
 * @version 0.1, 09/13/2018
 */
@WebServlet(name = "ShibOxAuthAuthServlet", urlPatterns = { "/Authn/oxAuth/*" })
public class ShibOxAuthAuthServlet extends HttpServlet {

    private static final long serialVersionUID = -4864851392327422662L;

    private final Logger LOG = LoggerFactory.getLogger(ShibOxAuthAuthServlet.class);

    private final String OXAUTH_PARAM_ENTITY_ID = "entityId";
    private final String OXAUTH_PARAM_ISSUER_ID = "issuerId";
    private final String OXAUTH_PARAM_EXTRA_PARAMS = "extraParameters";
    private final String OXAUTH_ATTRIBIUTE_SEND_END_SESSION_REQUEST = "sendEndSession";

    public final static String OXAUTH_ACR_USED = "acr_used";
    public final static String OXAUTH_ACR_REQUESTED = "acr_requested";

    private IdpAuthClient authClient;

    private final Set<OxAuthToShibTranslator> translators = new HashSet<OxAuthToShibTranslator>();

	private IdpCustomScriptManager customScriptManager;
	private IdpExternalScriptService externalScriptService;
    private GluuAttributeMappingService gluuAttributeMappingService;

    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);

        ServletContext context = getServletContext();

        WebApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(context);
    
        this.authClient = (IdpAuthClient) applicationContext.getBean("idpAuthClient");
        this.customScriptManager = (IdpCustomScriptManager) applicationContext.getBean("idpCustomScriptManager");
        this.gluuAttributeMappingService = (GluuAttributeMappingService) applicationContext.getBean("gluuAttributeMappingService");
       
        // Call custom script manager init to make sure that it initialized
    	this.customScriptManager.init();
    	this.externalScriptService = this.customScriptManager.getIdpExternalScriptService();

		final ApplicationContext ac = (ApplicationContext) context
				.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

        buildTranslators(ac.getEnvironment());
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException {
    	if (!checkRequest(request, response)) {
    		return;
    	}

    	try {
            ExternalContextHolder.setExternalContext(new ServletExternalContext(request.getServletContext(), request, response));

            final String requestUrl = request.getRequestURL().toString();
            LOG.trace("Get request to: '{}'", requestUrl);

            boolean logoutEndpoint = requestUrl.endsWith("/logout");
            if (logoutEndpoint ) {
                processLogoutRequest(request, response);
                return;
            }

            boolean ssoLogoutEndpoint = requestUrl.endsWith("/ssologout");
            if (ssoLogoutEndpoint ) {
                processSsoLogoutRequest(request, response);
                return;
            }

            // Web context
            final WebContext context = new J2EContext(request, response);
            final boolean authorizationResponse = authClient.isAuthorizationResponse(context);

            HttpServletRequest externalRequest = request;
            if (authorizationResponse) {
                try {
                    final Jwt jwt = Jwt.parse(authClient.getRequestState(context));

                    externalRequest = new HttpServletRequestWrapper(request) {
                        @Override
                        public String getParameter(String name) {
                            if (jwt.getClaims().hasClaim(name)) {
                                return jwt.getClaims().getClaimAsString(name);
                            }

                            return super.getParameter(name);
                        }
                    };
                } catch (InvalidJwtException ex) {
                    LOG.debug("State is not in JWT format", ex);
                }
            }

            // Get authentication key from request 
            final String flowExecutionKey = ExternalAuthentication.startExternalAuthentication(externalRequest);

            // Get external authentication properties
            final boolean force = Boolean.parseBoolean(request.getAttribute(ExternalAuthentication.FORCE_AUTHN_PARAM).toString());

            // It's an authentication
            if (!authorizationResponse) {
                LOG.debug("Initiating oxAuth login redirect");
                startLoginRequest(request, response, flowExecutionKey, force);
                return;
            }

            LOG.info("Processing authorization response");

            // Check if oxAuth request state is correct
            if (!authClient.isValidRequestState(context)) {
                LOG.error("The state in session and in request are not equals");

                // Re-init login page
                startLoginRequest(request, response, flowExecutionKey, force);
                return;
            }

            processAuthorizationResponse(request, response, flowExecutionKey);

        } catch (final ExternalAuthenticationException ex) {
            LOG.error("Error processing oxAuth authentication request", ex);
            loadErrorPage(request, response);
        } catch (final Exception ex) {
            LOG.error("Something unexpected happened", ex);
            request.setAttribute(ExternalAuthentication.AUTHENTICATION_ERROR_KEY, AuthnEventIds.AUTHN_EXCEPTION);
        } finally {
            ExternalContextHolder.setExternalContext(null);
        }
    }

    private final boolean checkRequest(final HttpServletRequest request, final HttpServletResponse response) throws ServletException {
		// Check whether a session is required.
		if (request.getSession(false) == null) {
            LOG.error("Pre-existing session required but none found");
            loadErrorPage(request, response);
            
            return false;
		}
		
		return true;
	}

    private void processAuthorizationResponse(final HttpServletRequest request, final HttpServletResponse response, final String authenticationKey)
            throws ExternalAuthenticationException, IOException {
        try {
            // Web context
            final WebContext context = new J2EContext(request, response);

            final OpenIdCredentials openIdCredentials = authClient.getCredentials(context);
            LOG.debug("Client name : '{}'", openIdCredentials.getClientName());

            final UserProfile userProfile = authClient.getUserProfile(openIdCredentials, context);
            LOG.debug("User profile : {}", userProfile);
            

            if (userProfile == null) {
                LOG.error("Token validation failed, returning InvalidToken");
                request.setAttribute(ExternalAuthentication.AUTHENTICATION_ERROR_KEY, "InvalidToken");
            } else {
            	// Get IDP contexts
                ProfileRequestContext profileRequestContext = ExternalAuthentication.getProfileRequestContext(authenticationKey, request);
                Function<ProfileRequestContext, AuthenticationContext> authenticationContextLookupStrategy = new ChildContextLookup<>(AuthenticationContext.class);
                final AuthenticationContext authenticationContext = authenticationContextLookupStrategy.apply(profileRequestContext);

                boolean postAuthResult = true;
        		if (this.externalScriptService.isEnabled()) {
        			PostAuthenticationContext postAuthenticationContext = buildContext(request, response, profileRequestContext, authenticationContext, userProfile, authenticationKey);
        			postAuthResult =  this.externalScriptService.executePostAuthenticationMethod(postAuthenticationContext);
            		if (!postAuthResult) {
            			LOG.error("Post authentication scripts returns false");
                        request.setAttribute(ExternalAuthentication.AUTHENTICATION_ERROR_KEY, "InvalidToken");
            		}
        		}

        		if (postAuthResult) {
	                List<IdPAttribute> idpAttributes = new ArrayList<IdPAttribute>();
	                boolean result = false;
	                TranslateAttributesContext translateAttributesContext = buildContext(request, response, userProfile, authenticationKey,idpAttributes);
	                if(this.externalScriptService.isEnabled()) {
	
	                    result = this.externalScriptService.executeExternalTranslateAttributesMethod(translateAttributesContext);
	                }
	
	                if(!result) {
	                    LOG.debug("Using default translate attributes method");
	                    for(final OxAuthToShibTranslator translator : translators) {
	                        translator.doTranslation(translateAttributesContext);
	                    }
	                }
	
	                if(!idpAttributes.isEmpty()) {
	                    LOG.debug("Storing generated idp attributes");
	                    ProfileRequestContext prContext = ExternalAuthentication.getProfileRequestContext(authenticationKey, request);
	                    GluuScratchContext gluuScratchContext = prContext.getSubcontext(GluuScratchContext.class, true);
	                    gluuScratchContext.setIdpAttributes(idpAttributes);
	                }
	
	                LOG.debug("Created an IdP subject instance with principals for {} ", userProfile.getId());
	                final Set<Principal> userPrincipals = new HashSet<Principal>();
	                userPrincipals.add(new UsernamePrincipal(userProfile.getId()));
	                request.setAttribute(ExternalAuthentication.SUBJECT_KEY, new Subject(false, userPrincipals, Collections.emptySet(),Collections.emptySet()));
	
	                if (authenticationContext != null) {
	                	String usedAcr = userProfile.getUsedAcr();
	                	if (StringHelper.isEmpty(usedAcr)) {
		                    LOG.debug("ACR method is undefined");
	                	} else {
	                		authenticationContext.getAuthenticationStateMap().put(OXAUTH_ACR_USED, usedAcr);
		                    LOG.debug("Used ACR method: {}", usedAcr);
	                	}
	                }
        		}
            }
        } catch (final Exception ex) {
            LOG.error("Token validation failed, returning InvalidToken", ex);
            request.setAttribute(ExternalAuthentication.AUTHENTICATION_ERROR_KEY, "InvalidToken");
        } finally {
            ExternalAuthentication.finishExternalAuthentication(authenticationKey, request, response);
        }
    }

    protected void startLoginRequest(final HttpServletRequest request, final HttpServletResponse response, final String authenticationKey, final Boolean force) {
        try {
            // Web context
            final WebContext context = new J2EContext(request, response);

            final Map<String, String> customResponseHeaders = new HashMap<String, String>();
            final String convId = request.getParameter(ExternalAuthentication.CONVERSATION_KEY);
            customResponseHeaders.put(ExternalAuthentication.CONVERSATION_KEY, convId);
            
            final Map<String, String> customParameters = new HashMap<String, String>();
            final String relayingPartyId = request.getAttribute(ExternalAuthentication.RELYING_PARTY_PARAM).toString();
            customParameters.put(OXAUTH_PARAM_ENTITY_ID, relayingPartyId);
             
            try {

                ProfileRequestContext prContext = ExternalAuthentication.getProfileRequestContext(authenticationKey, request);
                GluuScratchContext gluuScratchContext = prContext.getSubcontext(GluuScratchContext.class);
                if(gluuScratchContext != null) {
                    String extra_http_params = gluuScratchContext.getExtraHttpParameters();
                    if(extra_http_params != null &&  !extra_http_params.isEmpty()) {
                        customParameters.put(OXAUTH_PARAM_EXTRA_PARAMS,URLEncoder.encode(extra_http_params,"UTF-8"));
                    }
                }
            }catch(ExternalAuthenticationException e) {
                LOG.info("Could not set extra parameters for the request. Extra request parameters will not be available to oxAuth",e);
            }

            
            try {
                ProfileRequestContext profileRequestContext = ExternalAuthentication.getProfileRequestContext(convId, request);
                AuthnRequest authnRequest = (AuthnRequest) profileRequestContext.getInboundMessageContext().getMessage();
                if (authnRequest != null) {
                    RequestedAuthnContext authnContext = authnRequest.getRequestedAuthnContext();
                    Issuer issuer = authnRequest.getIssuer();
                    if (issuer != null) {
                    	customParameters.put(OXAUTH_PARAM_ISSUER_ID, issuer.getValue());
                    }
                    String acrs = null;
                    if (authnContext == null) {
                        Function<ProfileRequestContext, RelyingPartyContext> authenticationContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
                        final RelyingPartyContext relyingPartyContext = authenticationContextLookupStrategy.apply(profileRequestContext);
                        if (relyingPartyContext != null) {
                        	 ProfileConfiguration profileConfiguration = relyingPartyContext.getProfileConfig();
                        	if (profileConfiguration instanceof BrowserSSOProfileConfiguration) {
                        		List<Principal> principals = ((BrowserSSOProfileConfiguration) profileConfiguration).getDefaultAuthenticationMethods(profileRequestContext);
                                acrs = principals.stream()
                                        .map(Principal::getName).collect(Collectors.joining(" "));
                        	}
                        }
                    } else {
                        acrs = authnContext.getAuthnContextClassRefs().stream()
                                .map(AuthnContextClassRef::getAuthnContextClassRef).collect(Collectors.joining(" "));
                    }
                    
                    if (StringHelper.isNotEmpty(acrs)) {
                        customParameters.put("acr_values", acrs);

                        Function<ProfileRequestContext, AuthenticationContext> authenticationContextLookupStrategy = new ChildContextLookup<>(AuthenticationContext.class);
                        final AuthenticationContext authenticationContext = authenticationContextLookupStrategy.apply(profileRequestContext);
                        if (authenticationContext != null) {
                        	authenticationContext.getAuthenticationStateMap().put(OXAUTH_ACR_REQUESTED, acrs);
    	                    LOG.debug("Requested ACR method: {}", acrs);
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error("Unable to process to AuthnContextClassRef", e);
            }           

            final String loginUrl = authClient.getRedirectionUrl(context, customResponseHeaders, customParameters, force);
            LOG.debug("Generated redirection Url", loginUrl);

            LOG.debug("loginUrl: {}", loginUrl);
            response.sendRedirect(loginUrl);
        } catch (final IOException ex) {
            LOG.error("Unable to redirect to oxAuth from ShibOxAuth", ex);
        }
    }

    protected void processLogoutRequest(final HttpServletRequest request, final HttpServletResponse response) {
        try {
            // Web context
            final WebContext context = new J2EContext(request, response);

            final String logoutUrl = authClient.getLogoutRedirectionUrl(context);
            LOG.debug("Generated logout redirection Url", logoutUrl);
            

            LOG.debug("logoutUrl: {}", logoutUrl);
            response.sendRedirect(logoutUrl);

            authClient.clearAuthorized(context);
            authClient.setAttribute(context, OXAUTH_ATTRIBIUTE_SEND_END_SESSION_REQUEST, Boolean.TRUE);
            LOG.debug("Client authorization is removed (set null id_token in session)");
        } catch (final IOException ex) {
            LOG.error("Unable to redirect to oxAuth from ShibOxAuth", ex);
        }
    }

    protected void processSsoLogoutRequest(final HttpServletRequest request, final HttpServletResponse response) {
        try {
            // Web context
            final WebContext context = new J2EContext(request, response);
            final Object sendEndSession = authClient.getAttribute(context, OXAUTH_ATTRIBIUTE_SEND_END_SESSION_REQUEST);
            if (Boolean.TRUE.equals(sendEndSession)) {
                authClient.setAttribute(context, OXAUTH_ATTRIBIUTE_SEND_END_SESSION_REQUEST, null);
                LOG.debug("Client send end_session request. Ignoring OP initiated logout request");
                return;
            }

            final String logoutUrl = "/idp/profile/Logout";
            LOG.debug("logoutUrl: {}", logoutUrl);
            response.sendRedirect(logoutUrl);

            authClient.clearAuthorized(context);
            LOG.debug("Client authorization is removed (set null id_token in session)");
        } catch (final IOException ex) {
            LOG.error("Unable to redirect to oxAuth from ShibOxAuth", ex);
        }
    }

    /**
     * Attempt to build the set of translators from the fully qualified class names
     * set in the properties. If nothing has been set then default to the
     * AuthenticatedNameTranslator only.
     */
    private void buildTranslators(final Environment environment) {
        translators.add(new AuthenticatedNameTranslator());

        final String oxAuthToShibTranslators = StringUtils.defaultString(environment.getProperty("shib.oxauth.oxAuthToShibTranslator", ""));
        for (final String classname : StringUtils.split(oxAuthToShibTranslators, ';')) {
            try {
                LOG.debug("Loading translator class {}", classname);
                final Class<?> c = Class.forName(classname);
                final OxAuthToShibTranslator e = (OxAuthToShibTranslator) c.newInstance();
                if (e instanceof EnvironmentAware) {
                    ((EnvironmentAware) e).setEnvironment(environment);
                }
                translators.add(e);
                LOG.debug("Added translator class {}", classname);
            } catch (final Exception ex) {
                LOG.error("Error building oxAuth to Shib translator with name: " + classname, ex);
            }
        }
    }

    private void loadErrorPage(final HttpServletRequest request, final HttpServletResponse response) {
        final RequestDispatcher requestDispatcher = request.getRequestDispatcher("/no-conversation-state.jsp");
        try {
            requestDispatcher.forward(request, response);
        } catch (final Exception ex) {
            LOG.error("Error rendering the empty conversation state (shib-oxauth-authn3) error view.");
            response.resetBuffer();
            response.setStatus(404);
        }
    }

	private TranslateAttributesContext buildContext(HttpServletRequest request, HttpServletResponse response, UserProfile userProfile, 
                                                    String authenticationKey, List<IdPAttribute> idpAttributes) {

		TranslateAttributesContext translateAttributesContext = new TranslateAttributesContext(request, response, 
                userProfile, authenticationKey, idpAttributes);
        translateAttributesContext.setGluuAttributeMappingService(gluuAttributeMappingService);
		return translateAttributesContext;
	}

    private PostAuthenticationContext buildContext(HttpServletRequest request, HttpServletResponse response,
			ProfileRequestContext profileRequestContext, AuthenticationContext authenticationContext,
			UserProfile userProfile, String authenticationKey) {

    	PostAuthenticationContext postAuthenticationContext = new PostAuthenticationContext(request, response,
    			profileRequestContext, authenticationContext, userProfile, authenticationKey);

    	return postAuthenticationContext;
	}

}
