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

import org.wso2.carbon.multitenancy.deployment.service.interfaces.DeploymentProvider;
import org.wso2.carbon.multitenancy.deployment.service.models.Deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock deployment provider simulates deployment provider functionality for implementing tests.
 */
public class MockDeploymentProvider implements DeploymentProvider {

    private Map<String, Deployment> deploymentMap = new HashMap<>();

    @Override
    public List<Deployment> listDeployments() {
        return new ArrayList<>(deploymentMap.values());
    }

    @Override
    public Deployment getDeployment(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException();
        }

        return deploymentMap.get(id);
    }

    @Override
    public void deploy(Deployment deployment) {
        if (deployment == null) {
            throw new IllegalArgumentException();
        }

        deploymentMap.put(deployment.getId(), deployment);
    }

    @Override
    public void undeploy(Deployment deployment) {
        if (deployment == null) {
            throw new IllegalArgumentException();
        }

        if (deploymentMap.get(deployment.getId()) != null) {
            deploymentMap.remove(deployment.getId());
        }
    }
}
