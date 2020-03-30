/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.tenant.mgt;

import org.wso2.carbon.user.core.tenant.Tenant;

import java.util.List;

/**
 * This interface used to expose tenant management functionality as an OSGi Service.
 */
public interface TenantMgtService {

    /**
     * super admin adds a tenant
     *
     * @param tenant
     * @return UUID
     * @throws Exception if error in adding new tenant.
     */
    String addTenant(Tenant tenant) throws Exception;

    /**
     * Check if the selected domain is available to register.
     *
     * @param domainName Domain name.
     * @return true, if the domain is available to register.
     * @throws Exception, If unable to get the tenant manager, or get the tenant id from manager.
     */
    boolean checkDomainAvailability(String domainName) throws Exception;

    /**
     * Retrieve all the tenants
     *
     * @return tenantInfoBean[]
     * @throws Exception if failed to get Tenant Manager
     */
    List<Tenant> retrieveTenants() throws Exception;

    /**
     * Get a specific tenant
     *
     * @param tenantDomain tenant domain
     * @return tenantInfoBean
     * @throws Exception UserStoreException
     */
    Tenant getTenant(String tenantDomain) throws Exception;

    /**
     * Activate a deactivated tenant, by the super tenant.
     *
     * @param tenantDomain tenant domain
     * @throws Exception UserStoreException.
     */
    void activateTenant(String tenantDomain) throws Exception;

    /**
     * Deactivate the given tenant
     *
     * @param tenantDomain tenant domain
     * @throws Exception UserStoreException
     */
    void deactivateTenant(String tenantDomain) throws Exception;
}
