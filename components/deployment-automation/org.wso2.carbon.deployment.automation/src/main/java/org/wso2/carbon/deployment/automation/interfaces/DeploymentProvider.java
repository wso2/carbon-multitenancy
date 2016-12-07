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

package org.wso2.carbon.deployment.automation.interfaces;

import org.wso2.carbon.deployment.automation.exceptions.DeploymentAutomationException;
import org.wso2.carbon.deployment.automation.models.Deployment;

import java.util.List;

/**
 * Interface for a deployment provider.
 */
public interface DeploymentProvider {
    /**
     * Get all the deployments.
     *
     * @return List of deployments
     */
    List<Deployment> listDeployments();

    /**
     * Get a deployment by ID.
     *
     * @param id Deployment ID
     * @return Deployment
     */
    Deployment getDeployment(String id);

    /**
     * Deploy a product in a respective environment.
     *
     * @param deployment Deployment details
     */
    void deploy(Deployment deployment) throws DeploymentAutomationException;

    /**
     * Remove a product deployment.
     *
     * @param deployment Deployment details
     */
    void undeploy(Deployment deployment) throws DeploymentAutomationException;
}
