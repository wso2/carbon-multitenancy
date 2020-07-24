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
package org.wso2.carbon.tenant.mgt.services;

import org.wso2.carbon.stratos.common.exception.TenantMgtException;
import org.wso2.carbon.user.core.common.User;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.tenant.TenantSearchResult;

/**
 * This interface used to expose tenant management functionality as an OSGi Service.
 */
public interface TenantMgtService {

    /**
     * Adds a tenant.
     *
     * @param tenant tenant information.
     * @return tenantUniqueID.
     * @throws TenantMgtException if error in adding new tenant.
     */
    String addTenant(Tenant tenant) throws TenantMgtException;

    /**
     * Retrieve all the tenants.
     *
     * @param limit        limit per page.
     * @param offset       offset value.
     * @param filter       filter value for tenant search.
     * @param sortOrder    order of Tenant ASC/DESC.
     * @param sortBy       the column value need to sort.
     * @return List<Tenant>
     * @throws TenantMgtException if tenant listing failed.
     */
    TenantSearchResult listTenants(Integer limit, Integer offset, String sortOrder, String sortBy, String filter)
            throws TenantMgtException;

    /**
     * Get a specific tenant using tenant uuid.
     *
     * @param tenantUniqueIdentifier tenant uuid.
     * @return Tenant
     * @throws TenantMgtException if getting the tenant fails.
     */
    Tenant getTenant(String tenantUniqueIdentifier) throws TenantMgtException;

    /**
     * Get owner of the tenant using tenant uuid.
     * @param tenantUniqueIdentifier tenant uuid.
     * @return User.
     * @throws TenantMgtException if owner retrieval fails.
     */
    User getOwner(String tenantUniqueIdentifier) throws TenantMgtException;

    /**
     * Activate a deactivated tenant, by the super tenant.
     *
     * @param tenantUniqueIdentifier tenant uuid.
     * @throws TenantMgtException if the tenant activation fails.
     */
    void activateTenant(String tenantUniqueIdentifier) throws TenantMgtException;

    /**
     * Deactivate the given tenant.
     *
     * @param tenantUniqueIdentifier tenant uuid.
     * @throws TenantMgtException if tenant deactivation fails.
     */
    void deactivateTenant(String tenantUniqueIdentifier) throws TenantMgtException;

    /**
     * Delete the given tenant.
     *
     * @param tenantUniqueIdentifier tenant uuid.
     * @throws TenantMgtException if tenant deletion fails.
     */
    void deleteTenant(String tenantUniqueIdentifier) throws TenantMgtException;
}
