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

/**
 * Interface for a deployment provider.
 */
public interface DeploymentProvider {
    /**
     * List deployments.
     */
//    List<Deployment> listDeployments();

    /**
     * Deploy a product in a respective environment.
     *
     * @param definition YAML definition
     * @return Status
     */
    boolean deploy(String definition);

    /**
     * Remove a product deployment.
     *
     * @param definition YAML definition
     * @return Status
     */
    boolean undeploy(String definition);

    /**
     * Add a load balancer.
     *
     * @param definition YAML definition
     * @return Status
     */
    boolean addLoadBalancer(String definition);
}
