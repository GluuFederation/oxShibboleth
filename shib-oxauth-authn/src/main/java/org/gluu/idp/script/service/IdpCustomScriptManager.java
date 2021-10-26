package org.gluu.idp.script.service;

import java.util.Timer;
import java.util.TimerTask;

import org.gluu.idp.externalauth.ShibOxAuthAuthServlet;
import org.gluu.idp.externalauth.openid.conf.IdpConfigurationFactory;
import org.gluu.idp.script.service.external.IdpExternalScriptService;
import org.gluu.service.custom.script.CustomScriptManager;
import org.gluu.service.custom.script.StandaloneCustomScriptManager;
import org.gluu.util.init.Initializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.primitive.TimerSupport;

/**
 * IDP Custom Script Manager
 * 
 * @author Yuriy Movchan
 * @version 0.1, 06/18/2020
 */
public class IdpCustomScriptManager extends Initializable {

    private final Logger LOG = LoggerFactory.getLogger(ShibOxAuthAuthServlet.class);

	private StandaloneCustomScriptManager standaloneCustomScriptManager;
	private IdpExternalScriptService idpExternalScriptService;
	private Timer internalTaskTimer;

	public IdpCustomScriptManager(final IdpConfigurationFactory configurationFactory, boolean init) {
		standaloneCustomScriptManager = new StandaloneCustomScriptManager(
				configurationFactory.getPersistenceEntryManager(),
				configurationFactory.getAppConfiguration().getScriptDn(),
				configurationFactory.getBaseConfiguration().getString("pythonModulesDir"));
		if (init) {
			init();
		}
	}
	
	public IdpExternalScriptService getIdpExternalScriptService() {
		return idpExternalScriptService;
	}

	@Override
	protected void initInternal() {
		// Create external script
		this.idpExternalScriptService = new IdpExternalScriptService();

		// Register required external scripts
		standaloneCustomScriptManager.registerExternalScriptService(idpExternalScriptService);

		// Init script manager and load scripts
		standaloneCustomScriptManager.init();

		// Prepare time to update scripts
        internalTaskTimer = new Timer(TimerSupport.getTimerName(this), true);
        internalTaskTimer.schedule(getUpdateTask(), CustomScriptManager.DEFAULT_INTERVAL * 1000L, CustomScriptManager.DEFAULT_INTERVAL * 1000L);
	}

	public TimerTask getUpdateTask() {
        return new TimerTask() {

            @Override
            public void run() {
                final Long now = System.currentTimeMillis();
                LOG.debug("Running udate task at {}", now);
                try {
                	standaloneCustomScriptManager.reload();
                } catch (final Exception e) {
                    LOG.error("Error running udate task for {}", now, e);
                }
                LOG.debug("Finished udate task for {}", now);
            }
        };
	}

}
