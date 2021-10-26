package org.gluu.idp.consent.processor;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Function;

import javax.annotation.Nonnull;

import org.gluu.idp.context.GluuScratchContext;
import org.gluu.idp.externalauth.openid.conf.IdpConfigurationFactory;
import org.gluu.idp.script.service.IdpCustomScriptManager;
import org.gluu.idp.script.service.external.IdpExternalScriptService;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.utilities.java.support.logic.Constraint;

public class GluuReleaseAttributesPostProcessor extends AbstractProfileAction {

	
	private final Logger LOG = LoggerFactory.getLogger(GluuReleaseAttributesPostProcessor.class);

	private IdpConfigurationFactory configurationFactory;
	private IdpCustomScriptManager customScriptManager;
	private IdpExternalScriptService externalScriptService;

	public GluuReleaseAttributesPostProcessor() {

		configurationFactory = IdpConfigurationFactory.instance();
		customScriptManager = new IdpCustomScriptManager(configurationFactory, true);

		LOG.debug("ReleaseAttributesPostProcessor: create");
		Constraint.isNotNull(configurationFactory, "Configuration factory cannot be null");
		Constraint.isNotNull(customScriptManager, "Custom script manager cannot be null");

		init();
	}

	private void init() {
		// Call custom script manager init to make sure that it initialized
		this.customScriptManager.init();
		this.externalScriptService = this.customScriptManager.getIdpExternalScriptService();
	}

	/**
	 * Performs this profile interceptor action. Default implementation does
	 * nothing.
	 * 
	 * @param profileRequestContext the current profile request context
	 * @param interceptorContext    the current profile interceptor context
	 */
	@Override
	protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
		// Execute default flow first
		LOG.info("Executing external IDP script");
		super.doExecute(profileRequestContext);
		
		AttributeContext attrCtx = getAttributeContext(profileRequestContext);
		if(attrCtx == null) {
			LOG.debug("No attribute context found. No attribute to release");
			return;
		}
		
		Map<String,IdPAttribute> idpAttributeMap = new HashMap<String,IdPAttribute>(attrCtx.getIdPAttributes());

		GluuScratchContext gluuScratchContext = profileRequestContext.getSubcontext(GluuScratchContext.class);
		List<IdPAttribute> translatedIdpAttributes = null;
		if(gluuScratchContext != null) {
			translatedIdpAttributes = gluuScratchContext.getIdpAttributes();
		}

		if(translatedIdpAttributes != null && !translatedIdpAttributes.isEmpty()) {
			for(IdPAttribute idpAttribute : translatedIdpAttributes) {
				idpAttributeMap.put(idpAttribute.getId(),idpAttribute);
			}
		}

		PostProcessAttributesContext context = buildContext(idpAttributeMap);

		for (String attr : idpAttributeMap.keySet()) {
			LOG.info("------------------------attr: {}", attr);
		}

		// Return if script(s) not exists or invalid
		if (!this.externalScriptService.isEnabled()) {
			LOG.info("Using default release attributes post processor");
			attrCtx.setIdPAttributes(idpAttributeMap.values());
			return;
		}

		boolean result = this.externalScriptService.executeExternalUpdateAttributesMethod(context);
		attrCtx.setIdPAttributes(idpAttributeMap.values());
		
		LOG.debug("Executed script method 'updateAttributes' with result {}", result);
	}

	private PostProcessAttributesContext buildContext(final Map<String,IdPAttribute> idpAttributeMap) {

		PostProcessAttributesContext context = new PostProcessAttributesContext();

		context.setAttributeReleaseAction(this);
		context.setIdpAttributeMap(idpAttributeMap);

		return context;
	}

	private AttributeContext getAttributeContext(final ProfileRequestContext profileRequestContext) {

		Function<ProfileRequestContext,AttributeContext> attributeCtxLookupStrategy = null;
		attributeCtxLookupStrategy = new ChildContextLookup<RelyingPartyContext,AttributeContext>(AttributeContext.class)
		                                 .compose(new ChildContextLookup<ProfileRequestContext,RelyingPartyContext>(RelyingPartyContext.class));

		return attributeCtxLookupStrategy.apply(profileRequestContext);
	}

}