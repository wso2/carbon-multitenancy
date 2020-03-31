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

import org.wso2.carbon.stratos.common.exception.TenantMgtException;
import org.wso2.carbon.user.core.common.User;
import org.wso2.carbon.user.core.tenant.Tenant;

import java.util.List;

/**
 * This interface used to expose tenant management functionality as an OSGi Service.
 */
public interface TenantMgtService {

    /**
     * super admin adds a tenant.
     *
     * @param tenant tenant information.
     * @return UUID uuid used to represent the tenant.
     * @throws TenantMgtException if error in adding new tenant.
     */
    String addTenant(Tenant tenant) throws TenantMgtException;

    /**
     * Retrieve all the tenants.
     *
     * @param limit        limit per page.
     * @param offset       offset value.
     * @param filter       filter value for IdP search.
     * @param sortOrder    order of IdP ASC/DESC.
     * @param sortBy       the column value need to sort.
     * @return List<Tenant>
     * @throws TenantMgtException if failed to get the tenants.
     */
    List<Tenant> listTenants(Integer limit, Integer offset, String filter, String sortOrder, String sortBy)
            throws TenantMgtException;

    /**
     * Get a specific tenant.
     *
     * @param tenantDomain tenant domain.
     * @return Tenant
     * @throws TenantMgtException if getting the tenant fails.
     */
    Tenant getTenant(String tenantDomain) throws TenantMgtException;

    /**
     * Get owner of the tenant.
     * @param tenantDomain tenant domain.
     * @return User user details.
     * @throws TenantMgtException if owner retrieval fails.
     */
    User getOwner(String tenantDomain) throws TenantMgtException;

    /**
     * Activate a deactivated tenant, by the super tenant.
     *
     * @param tenantDomain tenant domain
     * @throws TenantMgtException if the tenant activation fails.
     */
    void activateTenant(String tenantDomain) throws TenantMgtException;

    /**
     * Deactivate the given tenant.
     *
     * @param tenantDomain tenant domain
     * @throws TenantMgtException if tenant deactivation fails.
     */
    void deactivateTenant(String tenantDomain) throws TenantMgtException;
}
