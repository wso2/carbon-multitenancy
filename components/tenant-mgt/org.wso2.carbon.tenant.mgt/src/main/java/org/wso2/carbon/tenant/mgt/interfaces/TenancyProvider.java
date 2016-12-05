/*
 * Copyright (c) 2016, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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
package org.wso2.carbon.tenant.mgt.interfaces;

import org.wso2.carbon.tenant.mgt.exceptions.TenantManagementException;
import org.wso2.carbon.tenant.mgt.models.Tenant;

/**
 * Interface of a tenant provider.
 */
public interface TenancyProvider {

    /**
     * Get all the tenants.
     *
     * @return Tenant[]
     */
    Tenant[] getTenants();

    /**
     * Get details of a tenant.
     *
     * @param name Tenant name
     * @return Tenant
     * @throws TenantManagementException
     */
    Tenant getTenant(String name) throws TenantManagementException;

    /**
     * Create a tenant using a given name.
     *
     * @param tenant Tenant
     * @throws TenantManagementException
     */
    void createTenant(Tenant tenant) throws TenantManagementException;

    /**
     * Delete a tenant.
     *
     * @param name Tenant name
     * @return Status
     * @throws TenantManagementException
     */
    boolean deleteTenant(String name) throws TenantManagementException;
}
