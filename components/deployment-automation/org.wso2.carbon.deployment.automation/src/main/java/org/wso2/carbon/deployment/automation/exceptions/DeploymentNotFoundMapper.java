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

package org.wso2.carbon.deployment.automation.exceptions;

import org.osgi.service.component.annotations.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Exception mapper for DeploymentNotFoundException.
 */
@Component(
        name = "org.wso2.carbon.deployment.automation.exceptions.DeploymentNotFoundMapper",
        service = ExceptionMapper.class,
        immediate = true
)
public class DeploymentNotFoundMapper implements ExceptionMapper<DeploymentNotFoundException> {
    @Override
    public Response toResponse(DeploymentNotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .build();
    }
}