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

import feign.Feign;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.jaxrs.JAXRSContract;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.multitenancy.tenant.service.TenantService;
import org.wso2.carbon.multitenancy.tenant.service.models.Tenant;
import org.wso2.msf4j.MicroservicesRunner;

import java.util.List;

public class TenantServiceTest {

    private static final int SERVER_PORT = 8282;

    private MicroservicesRunner microservicesRunner;
    private TenantServiceClient tenantServiceClient;

    @BeforeClass()
    public void setup() {
        TenantService tenantService = new TenantService(new MockTenancyProvider());
        microservicesRunner = new MicroservicesRunner(SERVER_PORT);
        microservicesRunner.deploy(tenantService).start();

        tenantServiceClient = Feign.builder()
                .contract(new JAXRSContract())
                .encoder(new GsonEncoder())
                .decoder(new GsonDecoder())
                .target(TenantServiceClient.class, "http://localhost:" + SERVER_PORT + "/tenants");
    }

    @Test(description = "Verify POST /tenants and GET /tenants/{name}")
    public void testCreateTenant() {
        String tenantFooName = "CreateFoo";
        Tenant tenantFoo = new Tenant(tenantFooName);
        tenantServiceClient.addTenant(tenantFoo);

        Tenant result = tenantServiceClient.getTenant(tenantFooName);
        Assert.assertNotNull(result);
    }

    @Test(description = "Verify GET /tenants")
    public void testGetAllTenants() {
        String tenantFooName = "GetAllFoo";
        Tenant tenantFoo = new Tenant(tenantFooName);
        tenantServiceClient.addTenant(tenantFoo);

        String tenantBarName = "GetAllBar";
        Tenant tenantBar = new Tenant(tenantBarName);
        tenantServiceClient.addTenant(tenantBar);

        List<Tenant> result = tenantServiceClient.getAllTenants();
        Assert.assertNotNull(result);

        Tenant resultFoo = findTenant(result, tenantFooName);
        Assert.assertNotNull(resultFoo);
        Assert.assertEquals(resultFoo.getName(), tenantFooName);

        Tenant resultBar = findTenant(result, tenantBarName);
        Assert.assertNotNull(resultBar);
        Assert.assertEquals(resultBar.getName(), tenantBarName);
    }

    @Test(description = "Verify DELETE /tenants")
    public void testDeleteTenant() {
        String tenantFooName = "DeleteFoo";
        Tenant tenantFoo = new Tenant(tenantFooName);
        tenantServiceClient.addTenant(tenantFoo);

        Tenant tenantBar = new Tenant("TenantBar");
        tenantServiceClient.addTenant(tenantBar);

        tenantServiceClient.delete(tenantFooName);

        List<Tenant> result = tenantServiceClient.getAllTenants();
        Assert.assertNotNull(result);

        Tenant resultFoo = findTenant(result, tenantFooName);
        Assert.assertNull(resultFoo);
    }

    private Tenant findTenant(List<Tenant> tenants, String tenantName) {
        for (Tenant tenant : tenants) {
            if (tenant.getName().equals(tenantName)) {
                return tenant;
            }
        }
        return null;
    }

    @AfterClass()
    public void tearDown() {
        if (microservicesRunner != null) {
            microservicesRunner.stop();
        }
    }
}
