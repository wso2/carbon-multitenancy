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

package org.wso2.carbon.multitenancy.application;

import org.wso2.carbon.multitenancy.deployment.service.DeploymentService;
import org.wso2.carbon.multitenancy.deployment.service.exceptions.DeploymentNotFoundMapper;
import org.wso2.carbon.multitenancy.tenant.service.TenantService;
import org.wso2.carbon.multitenancy.tenant.service.exceptions.BadRequestMapper;
import org.wso2.carbon.multitenancy.tenant.service.exceptions.DeploymentEnvironmentMapper;
import org.wso2.carbon.multitenancy.tenant.service.exceptions.TenantCreationFailedMapper;
import org.wso2.carbon.multitenancy.tenant.service.exceptions.TenantNotFoundMapper;
import org.wso2.msf4j.MicroservicesRunner;

/**
 * Multitenancy services application class.
 */
public class Application {

    public static void main(String[] args) {
        new MicroservicesRunner()
                .deploy(new DeploymentService())
                .deploy(new TenantService())
                .addExceptionMapper(new BadRequestMapper())
                .addExceptionMapper(new TenantCreationFailedMapper())
                .addExceptionMapper(new TenantNotFoundMapper())
                .addExceptionMapper(new DeploymentEnvironmentMapper())
                .addExceptionMapper(new org.wso2.carbon.multitenancy.deployment.service.exceptions.BadRequestMapper())
                .addExceptionMapper(
                        new org.wso2.carbon.multitenancy.deployment.service.exceptions.DeploymentEnvironmentMapper())
                .addExceptionMapper(new DeploymentNotFoundMapper())
                .start();
    }
}
