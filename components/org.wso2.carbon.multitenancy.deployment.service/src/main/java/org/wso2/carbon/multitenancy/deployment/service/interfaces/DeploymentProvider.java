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

package org.wso2.carbon.multitenancy.deployment.service.interfaces;

import org.wso2.carbon.multitenancy.deployment.service.exceptions.BadRequestException;
import org.wso2.carbon.multitenancy.deployment.service.exceptions.DeploymentEnvironmentException;
import org.wso2.carbon.multitenancy.deployment.service.exceptions.DeploymentNotFoundException;
import org.wso2.carbon.multitenancy.deployment.service.models.Deployment;

import java.util.List;

/**
 * Interface for a deployment provider.
 */
public interface DeploymentProvider {

    /**
     * Get all the deployments.
     *
     * @param namespace Namespace of the tenant
     * @return List of deployments
     */
    List<Deployment> listDeployments(String namespace);

    /**
     * Get a deployment by ID.
     *
     * @param namespace Namespace of the tenant
     * @param id Deployment ID
     * @return Deployment
     */
    Deployment getDeployment(String namespace, String id) throws DeploymentNotFoundException;

    /**
     * Deploy a product in a respective environment.
     *
     * @param namespace Namespace of the tenant
     * @param deployment Deployment details
     */
    void deploy(String namespace, Deployment deployment) throws DeploymentEnvironmentException, BadRequestException;

    /**
     * Remove a product deployment.
     *
     * @param namespace Namespace of the tenant
     * @param deployment Deployment details
     */
    void undeploy(String namespace, Deployment deployment) throws DeploymentEnvironmentException, BadRequestException;
}
