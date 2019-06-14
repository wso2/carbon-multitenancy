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
package org.wso2.carbon.tenant.mgt.core.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.service.TenantRegistryLoader;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(
        name = "org.wso2.carbon.tenant.mgt.core",
        immediate = true)
public class TenantMgtCoreServiceComponent {

    private static Log log = LogFactory.getLog(TenantMgtCoreServiceComponent.class);

    private static BundleContext bundleContext;

    private static RealmService realmService;

    private static RegistryService registryService;

    private static TenantRegistryLoader registryLoader;

    @Activate
    protected void activate(ComponentContext context) {

        try {
            bundleContext = context.getBundleContext();
            log.debug("******* Tenant Core bundle is activated ******* ");
        } catch (Exception e) {
            log.error("Error occurred while activating tenant.mgt.core bundle. " + e);
        }
    }

    @Reference(
            name = "tenant.registryloader",
            service = org.wso2.carbon.registry.core.service.TenantRegistryLoader.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetTenantRegistryLoader")
    protected void setTenantRegistryLoader(TenantRegistryLoader tenantRegLoader) {

        TenantMgtCoreServiceComponent.registryLoader = tenantRegLoader;
    }

    protected void unsetTenantRegistryLoader(TenantRegistryLoader tenantRegLoader) {

        TenantMgtCoreServiceComponent.registryLoader = null;
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        log.debug("******* Tenant Core bundle is deactivated ******* ");
    }

    @Reference(
            name = "registry.service",
            service = org.wso2.carbon.registry.core.service.RegistryService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRegistryService")
    protected void setRegistryService(RegistryService registryService) {

        TenantMgtCoreServiceComponent.registryService = registryService;
    }

    protected void unsetRegistryService(RegistryService registryService) {

        setRegistryService(null);
    }

    @Reference(
            name = "user.realmservice.default",
            service = org.wso2.carbon.user.core.service.RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        TenantMgtCoreServiceComponent.realmService = realmService;
    }

    protected void unsetRealmService(RealmService realmService) {

        setRealmService(null);
    }

    public static BundleContext getBundleContext() {

        return bundleContext;
    }

    public static RegistryService getRegistryService() {

        return registryService;
    }

    public static RealmService getRealmService() {

        return realmService;
    }

    public static TenantManager getTenantManager() {

        return realmService.getTenantManager();
    }

    public static RealmConfiguration getBootstrapRealmConfiguration() {

        return realmService.getBootstrapRealmConfiguration();
    }

    public static UserRegistry getGovernanceSystemRegistry(int tenantId) throws RegistryException {

        return registryService.getGovernanceSystemRegistry(tenantId);
    }

    public static UserRegistry getConfigSystemRegistry(int tenantId) throws RegistryException {

        return registryService.getConfigSystemRegistry(tenantId);
    }

    public static TenantRegistryLoader getRegistryLoader() {

        return registryLoader;
    }
}
