package org.oxLogoServlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(urlPatterns = "/servlet/logo")
public class IdpLogoServlet extends HttpServlet {

	private static final long serialVersionUID = 5445488800130871634L;

	private static final Logger log = LoggerFactory.getLogger(IdpLogoServlet.class);

	public static final String BASE_IDP_LOGO_PATH = "/opt/gluu/jetty/idp/custom/static/logo/";

	// @Inject
	// private OrganizationService organizationService;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		response.setContentType("image/jpg");
		response.setDateHeader("Expires", new Date().getTime() + 1000L * 1800);
		// GluuOrganization organization = organizationService.getOrganization();
		boolean hasSucceed = readCustomLogo(response, null);
		if (!hasSucceed) {
			readDefaultLogo(response);
		}
	}

	private boolean readDefaultLogo(HttpServletResponse response) {
		String defaultLogoFileName = "/WEB-INF/static/images/logo.png";
		InputStream in = null;
		OutputStream out = null;
		try {
			in = getServletContext().getResourceAsStream(defaultLogoFileName);
			out = response.getOutputStream();
			IOUtils.copy(in, out);
			return true;
		} catch (IOException e) {
			log.debug("Error loading default logo: " + e.getMessage());
			return false;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private boolean readCustomLogo(HttpServletResponse response, Object organization) {
		if (organization == null) {
			return false;
		}
		File directory = new File(BASE_IDP_LOGO_PATH);
		if (!directory.exists()) {
			directory.mkdir();
		}
		File logoPath = new File(BASE_IDP_LOGO_PATH + "");
		if (!logoPath.exists()) {
			return false;
		}
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new FileInputStream(logoPath);
			out = response.getOutputStream();
			IOUtils.copy(in, out);
			return true;
		} catch (IOException e) {
			log.debug("Error loading custom logo: " + e.getMessage());
			return false;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
