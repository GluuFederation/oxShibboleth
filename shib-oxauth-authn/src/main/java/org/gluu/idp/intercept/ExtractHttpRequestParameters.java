package org.gluu.idp.intercept;

import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;

import org.gluu.idp.context.GluuScratchContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.profile.AbstractProfileAction;


public class ExtractHttpRequestParameters extends AbstractProfileAction {
    
    private static final String [] parameterBlackList = {"SAMLRequest", "Signature","RelayState","SigAlg","execution"};

    private final Logger LOG = LoggerFactory.getLogger(ExtractHttpRequestParameters.class);

    private HttpServletRequest httpRequest;

    
    public void setHttpRequest(HttpServletRequest httpRequest) {

        this.httpRequest = httpRequest;
    }

    public HttpServletRequest getHttpRequest() {

        return this.httpRequest;
    }

    @Override
    public void doExecute(final ProfileRequestContext profileRequestContext) {

        super.doExecute(profileRequestContext);
        if(httpRequest == null) {
            LOG.debug("Http Request Parameter Extraction Failed. HttpRequest is null");
            return;
        }

        LOG.debug("Begin http request extraction");
        GluuScratchContext scratchcontext = profileRequestContext.getSubcontext(GluuScratchContext.class,true);
        if(scratchcontext == null) {
            LOG.debug("GluuScratchContext is null. Http parameter extraction not possible");
            return;
        }
        Enumeration<String> paramnames = httpRequest.getParameterNames();
        for(;paramnames.hasMoreElements();) {
            String paramname = paramnames.nextElement();
            LOG.debug("Found http parameter {}",paramname);
            if(!isBlacklistedParameter(paramname)) {
                scratchcontext.addExtraHttpParameter(paramname,httpRequest.getParameter(paramname));
                LOG.debug("Http parameter added {}:{}",paramname,httpRequest.getParameter(paramname));
            }
        }
        LOG.debug("End http request extraction");
    }

    private boolean isBlacklistedParameter(String paramname) {

        if(paramname == null)
            return false;
        
        for(String blacklistItem : parameterBlackList) {
            if(blacklistItem.equalsIgnoreCase(paramname)) {
                return true;
            }
        }

        return false;
    }
}
