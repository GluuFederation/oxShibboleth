package org.gluu.idp.externalauth;

/**
 * This interface defines the public interface for a class that will translate the information from oxAuth to Shib. The translator
 * should only push details into the request and should NOT attempt to call
 * AuthenticationEngine.returnToAuthenticationEngine(request, response);
 * <p>
 * Instance of this type should implement hashcode and equals.
 *
 * @author Yuriy Movchan
 * @version 0.1, 09/13/2018
 * @version 0.2 , 06/09/2021
 */
public interface OxAuthToShibTranslator {
  
    /**
     * Perform attribute translation 
     * @param context            The TranslateAttributesContext object 
     * @throws Exception the exception
     */
    void doTranslation(TranslateAttributesContext context) throws Exception;
}
