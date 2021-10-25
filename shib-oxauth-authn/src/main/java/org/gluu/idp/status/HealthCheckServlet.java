package org.gluu.idp.status;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gluu.idp.externalauth.openid.conf.IdpConfigurationFactory;
import org.gluu.persist.PersistenceEntryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * A Servlet that validates the oxAuth code and then pushes the authenticated
 * principal name into the correct location before handing back control to Shib
 *
 * @author Yuriy Movchan
 * @version 0.1, 09/13/2018
 */
@WebServlet(name = "ShibOxAuthHealthCheck", urlPatterns = { "/health-check" })
public class HealthCheckServlet extends HttpServlet {

    private static final long serialVersionUID = -1264851392327422662L;

    private final Logger LOG = LoggerFactory.getLogger(HealthCheckServlet.class);

	private PersistenceEntryManager persistenceEntryManager;

    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);

        IdpConfigurationFactory configurationFactory = IdpConfigurationFactory.instance();
		this.persistenceEntryManager = configurationFactory.getPersistenceEntryManager();
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException {
    	boolean isConnected = persistenceEntryManager.getOperationService().isConnected();
    	String dbStatus = isConnected ? "online" : "offline"; 
        String resultResponse = "{\"status\": \"running\", \"db_status\":\"" + dbStatus + "\"}";

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
		try (PrintWriter out = response.getWriter()) {
		     out.print(resultResponse);
	         response.setStatus(HttpServletResponse.SC_OK);
		     out.flush();
		} catch (IOException e) {
            LOG.error("Error sending health-check response");
            response.resetBuffer();
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
    }

}
