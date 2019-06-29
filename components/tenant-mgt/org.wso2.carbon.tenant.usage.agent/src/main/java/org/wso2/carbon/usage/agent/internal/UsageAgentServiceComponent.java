/*
 *  Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.usage.agent.internal;

import org.apache.axis2.AxisFault;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.event.core.EventBroker;
import org.wso2.carbon.statistics.services.SystemStatisticsUtil;
import org.wso2.carbon.usage.agent.listeners.UsageStatsAxis2ConfigurationContextObserver;
import org.wso2.carbon.usage.agent.util.PublisherUtils;
import org.wso2.carbon.usage.agent.util.Util;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.Axis2ConfigurationContextObserver;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * this class is used to activate and deactivate the UsageAgentServiceComponent, set and unset
 * Serverconfigurarion, set and unset EventBrokerService.
 */
@Component(
        name = "org.wso2.carbon.usage.agent",
        immediate = true)
public class UsageAgentServiceComponent {

    private static Log log = LogFactory.getLog(UsageAgentServiceComponent.class);

    /**
     * method to activate UsageAgentServiceComponent
     *
     * @param context componentContext
     */
    @Activate
    protected void activate(ComponentContext context) {

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain(true);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(CarbonConstants.REGISTRY_SYSTEM_USERNAME);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setUserRealm(Util.getRealmService()
                    .getBootstrapRealm());
            // initialize listeners
            // currently we have RegistryUsagePersistingListener only
            Util.initializeAllListeners();
            // initialize persistenceManager for persisting BandwidthUsage
            Util.initializePersistenceManager();
            // create statistic event subscription
            Util.createStaticEventSubscription();
            if ("true".equals(ServerConfiguration.getInstance().getFirstProperty("EnableMetering"))) {
                // PublisherUtils.defineUsageEventStream();
                UsageStatsAxis2ConfigurationContextObserver statObserver = new
                        UsageStatsAxis2ConfigurationContextObserver();
                context.getBundleContext().registerService(Axis2ConfigurationContextObserver.class.getName(),
                        statObserver, null);
                log.info("Observer to register the module for request statistics publishing was registered");
            }
            // register the request data persistor osgi service so that we can
            // store service request bandwidths
            // TODO: Remove this and ServiceDataPersistor after fixing ESB metering
            // context.getBundleContext().registerService(
            // RequestDataPersister.class.getName(), new ServiceDataPersistor(), null);
            log.debug("******* Multitenancy Usage Agent bundle is activated ******* ");
        } catch (Throwable e) {
            log.error("******* Failed to activate Multitenancy Usage Agent bundle ****", e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    /**
     * method to deactivate bundle
     *
     * @param context ComponentContext
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {

        log.debug("******* Multitenancy Metering Usage Agent bundle is deactivated ******* ");
    }

    /**
     * method to set RealmService
     *
     * @param realmService RealmService
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
     * method to unsetRealmService
     *
     * @param realmService RealmService
     */
    protected void unsetRealmService(RealmService realmService) {

        Util.setRealmService(null);
    }

    /**
     * method to set ConfigurationContextService
     *
     * @param contextService ConfigurationContextService
     */
    @Reference(
            name = "config.context.service",
            service = org.wso2.carbon.utils.ConfigurationContextService.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetConfigurationContextService")
    protected void setConfigurationContextService(ConfigurationContextService contextService) {

        Util.setConfigurationContextService(contextService);
        PublisherUtils.setConfigurationContextService(contextService);
    }

    /**
     * method to unset ConfigurationContextService
     *
     * @param contextService ConfigurationContextService
     */
    protected void unsetConfigurationContextService(ConfigurationContextService contextService) {

        Util.setConfigurationContextService(null);
    }

    /**
     * method to set server configurations
     *
     * @param serverConfiguration ServerConfiguration
     */
    @Reference(
            name = "server.configuration",
            service = org.wso2.carbon.base.api.ServerConfigurationService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetServerConfiguration")
    protected void setServerConfiguration(ServerConfigurationService serverConfiguration) {

        Util.setServerConfiguration(serverConfiguration);
    }

    /**
     * method to unset server configurations
     *
     * @param serverConfiguration ServerConfiguration
     */
    protected void unsetServerConfiguration(ServerConfigurationService serverConfiguration) {

        Util.setServerConfiguration(null);
    }

    /**
     * method to set EventBrokerService
     *
     * @param registryEventBrokerService EventBroker
     */
    @Reference(
            name = "eventbroker.service",
            service = org.wso2.carbon.event.core.EventBroker.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetEventBrokerService")
    protected void setEventBrokerService(EventBroker registryEventBrokerService) {

        Util.setEventBrokerService(registryEventBrokerService);
    }

    /**
     * method to unset EventBrokerService
     *
     * @param registryEventBrokerService EventBroker
     */
    protected void unsetEventBrokerService(EventBroker registryEventBrokerService) {

        Util.setEventBrokerService(null);
    }

    @Reference(
            name = "org.wso2.carbon.statistics.services",
            service = org.wso2.carbon.statistics.services.SystemStatisticsUtil.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetSystemStatisticsUtil")
    protected void setSystemStatisticsUtil(SystemStatisticsUtil systemStatisticsUtil) {

        Util.setSystemStatisticsUtil(systemStatisticsUtil);
    }

    protected void unsetSystemStatisticsUtil(SystemStatisticsUtil systemStatisticsUtil) {

        Util.setSystemStatisticsUtil(null);
    }
}
