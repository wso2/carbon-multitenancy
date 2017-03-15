/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

import org.wso2.carbon.tenant.mgt.exceptions.BadRequestException;
import org.wso2.carbon.tenant.mgt.exceptions.TenantCreationFailedException;
import org.wso2.carbon.tenant.mgt.exceptions.TenantNotFoundException;
import org.wso2.carbon.tenant.mgt.models.Tenant;

import java.util.List;

/**
 * Interface of a tenant provider.
 */
public interface TenancyProvider {

    /**
     * Get all the tenants.
     *
     * @return List of tenants
     */
    List<Tenant> getTenants();

    /**
     * Get details of a tenant.
     *
     * @param name Tenant name
     * @return Tenant
     * @throws TenantNotFoundException
     */
    Tenant getTenant(String name) throws TenantNotFoundException;

    /**
     * Create a tenant.
     *
     * @param tenant Tenant
     * @throws TenantCreationFailedException
     */
    void createTenant(Tenant tenant) throws TenantCreationFailedException, BadRequestException;

    /**
     * Delete a tenant by name.
     *
     * @param name Tenant name
     * @return Status
     */
    boolean deleteTenant(String name);
}
