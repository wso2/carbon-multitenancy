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

import org.wso2.carbon.multitenancy.deployment.service.models.Deployment;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Deployment service client interface.
 */
public interface DeploymentServiceClient {

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    void deploy(Deployment deployment);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/")
    List<Deployment> getDeployments();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    Deployment getDeployment(@PathParam("id") String id);

    @DELETE
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    void undeploy(Deployment deployment);
}
