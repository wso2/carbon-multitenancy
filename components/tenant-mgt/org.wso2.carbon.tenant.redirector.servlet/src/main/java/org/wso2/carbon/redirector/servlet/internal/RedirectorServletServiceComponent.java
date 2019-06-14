package org.wso2.carbon.redirector.servlet.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.redirector.servlet.util.Util;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(
        name = "org.wso2.carbon.redirector.servlet",
        immediate = true)
public class RedirectorServletServiceComponent {

    private static Log log = LogFactory.getLog(RedirectorServletServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) {

        try {
            log.debug("******* Multitenancy Redirector Servlet admin service bundle is activated ******* ");
        } catch (Exception e) {
            log.error("******* Multitenancy Redirector Servlet admin service bundle failed activating ****", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        log.debug("******* Multitenancy Redirector Servlet admin service bundle is deactivated ******* ");
    }

    @Reference(
            name = "registry.service",
            service = org.wso2.carbon.registry.core.service.RegistryService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRegistryService")
    protected void setRegistryService(RegistryService registryService) {

        Util.setRegistryService(registryService);
    }

    protected void unsetRegistryService(RegistryService registryService) {

        Util.setRegistryService(null);
    }

    @Reference(
            name = "user.realmservice.default",
            service = org.wso2.carbon.user.core.service.RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        Util.setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {

        Util.setRealmService(null);
    }
}
