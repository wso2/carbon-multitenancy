/*
 *  Copyright (c) 2005-2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.activation.internal;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.activation.service.ActivationService;
import org.wso2.carbon.activation.utils.ActivationManager;
import org.wso2.carbon.activation.utils.Util;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * The Declarative Service Component for the Service Activation Module for Tenants.
 */
@SuppressWarnings({"JavaDoc", "unused"})
@Component(
        name = "org.wso2.carbon.metering",
        immediate = true)
public class ActivationServiceComponent {

    private static final Log log = LogFactory.getLog(ActivationServiceComponent.class);

    private static ServiceRegistration registration = null;

    /**
     * Activates the Registry Kernel bundle.
     *
     * @param context the OSGi component context.
     */
    @Activate
    protected void activate(ComponentContext context) {

        try {
            ActivationManager.startCacheCleaner();
            if (registration == null) {
                registration = context.getBundleContext().registerService(ActivationService.class.getName(), new
                        ActivationService(), null);
            }
            log.debug("******* Stratos Activation bundle is activated ******* ");
        } catch (Exception e) {
            log.error("******* Stratos Activation bundle failed activating ****", e);
        }
    }

    /**
     * Deactivates the Registry Kernel bundle.
     *
     * @param context the OSGi component context.
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {

        registration.unregister();
        registration = null;
        ActivationManager.stopCacheCleaner();
        log.debug("******* Stratos Activation bundle is deactivated ******* ");
    }

    /**
     * Method to set the realm service used. This will be used when accessing the user realm. This
     * method is called when the OSGi Realm Service is available.
     *
     * @param realmService the realm service.
     */
    @Reference(
            name = "user.realmservice.default",
            service = org.wso2.carbon.user.core.service.RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        Util.setRealmService(realmService);
    }

    /**
     * This method is called when the current realm service becomes un-available.
     *
     * @param realmService the current realm service instance, to be used for any cleaning-up.
     */
    protected void unsetRealmService(RealmService realmService) {

        Util.setRealmService(null);
    }

    /**
     * Method to set the configuration context service used. This method is called when the OSGi
     * ConfigurationContext Service is available.
     *
     * @param contextService the configuration context service.
     */
    @Reference(
            name = "config.context.service",
            service = org.wso2.carbon.utils.ConfigurationContextService.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetConfigurationContextService")
    protected void setConfigurationContextService(ConfigurationContextService contextService) {

        try {
            if (contextService.getServerConfigContext() != null && contextService.getServerConfigContext()
                    .getAxisConfiguration() != null) {
                contextService.getServerConfigContext().getAxisConfiguration().engageModule("activation");
            } else {
                log.error("Failed to engage Activation Module.");
            }
        } catch (AxisFault e) {
            log.error("Failed to engage Activation Module", e);
        }
    }

    /**
     * This method is called when the current configuration context service becomes un-available.
     *
     * @param contextService the current configuration context service instance, to be used for any
     *                       cleaning-up.
     */
    protected void unsetConfigurationContextService(ConfigurationContextService contextService) {

    }
}
