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

package org.wso2.carbon.multitenancy.deployment.service.tests;

import feign.Feign;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.jaxrs.JAXRSContract;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.multitenancy.deployment.service.DeploymentService;
import org.wso2.carbon.multitenancy.deployment.service.models.Deployment;
import org.wso2.msf4j.MicroservicesRunner;

import java.util.List;

/**
 * Deployment service test class.
 */
public class DeploymentServiceTest {

    private static final int SERVER_PORT = 8282;

    private MicroservicesRunner microservicesRunner;
    private DeploymentServiceClient deploymentServiceClient;

    @BeforeClass()
    public void setup() {
        DeploymentService tenantService = new DeploymentService(new MockDeploymentProvider());
        microservicesRunner = new MicroservicesRunner(SERVER_PORT);
        microservicesRunner.deploy(tenantService).start();

        deploymentServiceClient = Feign.builder()
                .contract(new JAXRSContract())
                .encoder(new GsonEncoder())
                .decoder(new GsonDecoder())
                .target(DeploymentServiceClient.class, "http://localhost:" + SERVER_PORT + "/deployments");
    }

    @Test(description = "Verify POST /deployments")
    public void testDeployment() {
        Deployment foo = new Deployment("Foo", "apim", "2.1.0", 1);
        deploymentServiceClient.deploy(foo);

        Assert.assertNotNull(deploymentServiceClient.getDeployment("Foo"));
    }

    @Test(description = "Verify GET /deployments")
    public void testGetAllDeployments() {
        Deployment foo = new Deployment("Foo", "apim", "2.1.0", 1);
        deploymentServiceClient.deploy(foo);

        Deployment bar = new Deployment("Bar", "esb", "5.0.0", 1);
        deploymentServiceClient.deploy(bar);

        List<Deployment> deployments = deploymentServiceClient.getDeployments();
        Deployment fooResult = findDeployment(deployments, "Foo");
        Assert.assertNotNull(fooResult);
        Assert.assertEquals(fooResult.getProduct(), "apim");
        Assert.assertEquals(fooResult.getVersion(), "2.1.0");
        Assert.assertEquals(fooResult.getPattern(), 1);

        Deployment barResult = findDeployment(deployments, "Bar");
        Assert.assertNotNull(barResult);
        Assert.assertEquals(barResult.getProduct(), "esb");
        Assert.assertEquals(barResult.getVersion(), "5.0.0");
        Assert.assertEquals(barResult.getPattern(), 1);
    }

    @Test(description = "Verify DELETE /deployments/{id}")
    public void testUndeployment() {
        Deployment foo = new Deployment("Foo", "apim", "2.1.0", 1);
        deploymentServiceClient.deploy(foo);

        Deployment bar = new Deployment("Bar", "esb", "5.0.0", 1);
        deploymentServiceClient.deploy(bar);

        deploymentServiceClient.undeploy(foo);
        Assert.assertNull(deploymentServiceClient.getDeployment("Foo"));
    }

    private Deployment findDeployment(List<Deployment> deployments, String id) {
        for (Deployment deployment : deployments) {
            if (deployment.getId().equals(id)) {
                return deployment;
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
