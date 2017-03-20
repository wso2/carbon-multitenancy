/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 */

package org.wso2.carbon.multitenancy.tenant.service.tests;

import org.wso2.carbon.multitenancy.tenant.service.exceptions.BadRequestException;
import org.wso2.carbon.multitenancy.tenant.service.exceptions.TenantCreationFailedException;
import org.wso2.carbon.multitenancy.tenant.service.exceptions.TenantNotFoundException;
import org.wso2.carbon.multitenancy.tenant.service.interfaces.TenancyProvider;
import org.wso2.carbon.multitenancy.tenant.service.models.Tenant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock tenancy provider simulates tenancy provider functionality for implementing API tests.
 */
public class MockTenancyProvider implements TenancyProvider {

    private Map<String, Tenant> tenantsMap = new HashMap<>();

    @Override
    public List<Tenant> getTenants() {
        return new ArrayList<>(tenantsMap.values());
    }

    @Override
    public Tenant getTenant(String name) throws TenantNotFoundException {
        return tenantsMap.get(name);
    }

    @Override
    public void createTenant(Tenant tenant) throws TenantCreationFailedException, BadRequestException {
        tenantsMap.put(tenant.getName(), tenant);
    }

    @Override
    public boolean deleteTenant(String name) {
        if (tenantsMap.get(name) == null) {
            return false;
        }
        tenantsMap.remove(name);
        return true;
    }
}
