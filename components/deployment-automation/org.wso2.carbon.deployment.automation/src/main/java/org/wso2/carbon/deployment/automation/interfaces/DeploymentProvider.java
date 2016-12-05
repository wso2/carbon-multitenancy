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
import org.wso2.carbon.deployment.automation.models.Product;

/**
 * Interface for a deployment provider.
 */
public interface DeploymentProvider {
    
    /**
     * Deploy a product in a respective environment.
     *
     * @param product Product details
     * @return Status
     */
    void deploy(Product product) throws DeploymentAutomationException;

    /**
     * Remove a product deployment.
     *
     * @param product Product details
     * @return Status
     */
    void undeploy(Product product) throws DeploymentAutomationException;
}
