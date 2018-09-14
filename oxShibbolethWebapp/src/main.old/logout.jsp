<%@ page import="edu.internet2.middleware.shibboleth.idp.authn.LoginContext" %>
<%@ page import="edu.internet2.middleware.shibboleth.idp.authn.Saml2LoginContext" %>
<%@ page import="edu.internet2.middleware.shibboleth.idp.authn.LoginHandler" %>
<%@ page import="edu.internet2.middleware.shibboleth.idp.session.*" %>
<%@ page import="edu.internet2.middleware.shibboleth.idp.util.HttpServletHelper" %>
<%@ page import="org.opensaml.saml2.metadata.*" %>
<%@ page import="org.opensaml.util.storage.StorageService" %>
<%@ page import="org.gluu.oxauth.client.session.SignOutHandler" %>

<%
Cookie[] ck = request.getCookies();
   if (ck != null) {
      for (int i = 0; i < ck.length; i++)
      {
            Cookie cc = ck[i];
                cc.setMaxAge(0);
                cc.setPath("/idp");
                response.addCookie(cc);
        }
   }


String actionURL = SignOutHandler.instance().getOAuthLogoutUrl(request);
if (actionURL == null) {
    actionURL = request.getScheme() + "://" + request.getServerName() + "/identity/authentication/finishlogout";
}
response.sendRedirect(actionURL);
%>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">

<body onload="document.forms[0].submit()">
        <noscript>
            <p>
                <strong>Note:</strong> Since your browser does not support JavaScript,
                you must press the Continue button once to proceed.
            </p>
        </noscript>
    </body>
</html>