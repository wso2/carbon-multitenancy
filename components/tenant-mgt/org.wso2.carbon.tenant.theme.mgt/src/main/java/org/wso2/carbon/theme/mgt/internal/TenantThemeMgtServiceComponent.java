/*
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.theme.mgt.internal;

import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.theme.mgt.util.ThemeLoadingListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.framework.BundleContext;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.theme.mgt.util.ThemeUtil;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;

import java.util.Hashtable;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(
        name = "org.wso2.carbon.governance.theme.tenant",
        immediate = true)
public class TenantThemeMgtServiceComponent {

    private static final String ENABLE_TENANT_THEME_MANAGEMENT = "Tenant.EnableTenantThemeManagement";
    private static Log log = LogFactory.getLog(TenantThemeMgtServiceComponent.class);

    private static ConfigurationContextService configContextService = null;

    @Activate
    protected void activate(ComponentContext context) {

        try {
            ServerConfigurationService serverConfiguration = ServerConfiguration.getInstance();
            String enableTenantTheme = serverConfiguration.getFirstProperty(ENABLE_TENANT_THEME_MANAGEMENT);
            boolean enableTenantThemeMgt = enableTenantTheme == null || Boolean.parseBoolean(enableTenantTheme);
            if (!enableTenantThemeMgt) {
                log.debug("******* Multitenancy Theme bundle is activated. But tenant theme management" +
                        " configuration is disabled *******  ");
                return;
            }
            ThemeUtil.loadResourceThemes();
            // registering the Theme Logding Listener
            BundleContext bundleContext = context.getBundleContext();
            bundleContext.registerService(TenantMgtListener.class.getName(), new ThemeLoadingListener(), new
                    Hashtable());
            log.debug("******* Multitenancy Theme Config bundle is activated ******* ");
        } catch (Exception e) {
            log.error("******* Multitenancy Theme Config bundle failed activating ****", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        log.debug("******* Multitenancy Theme Config bundle is deactivated ******* ");
    }

    @Reference(
            name = "registry.service",
            service = org.wso2.carbon.registry.core.service.RegistryService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRegistryService")
    protected void setRegistryService(RegistryService registryService) {

        ThemeUtil.setRegistryService(registryService);
    }

    protected void unsetRegistryService(RegistryService registryService) {

        ThemeUtil.setRegistryService(null);
    }

    @Reference(
            name = "user.realmservice.default",
            service = org.wso2.carbon.user.core.service.RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        ThemeUtil.setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {

        ThemeUtil.setRealmService(null);
    }

    @Reference(
            name = "config.context.service",
            service = org.wso2.carbon.utils.ConfigurationContextService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetConfigurationContextService")
    protected void setConfigurationContextService(ConfigurationContextService contextService) {

        if (log.isDebugEnabled()) {
            log.debug("Setting the ConfigurationContext");
        }
        configContextService = contextService;
    }

    protected void unsetConfigurationContextService(ConfigurationContextService contextService) {

        if (log.isDebugEnabled()) {
            log.debug("Unsetting the ConfigurationContext");
        }
    }
}
