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

package org.wso2.carbon.multitenancy.deployment.service;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import org.wso2.carbon.multitenancy.deployment.service.exceptions.BadRequestException;
import org.wso2.carbon.multitenancy.deployment.service.exceptions.DeploymentEnvironmentException;
import org.wso2.carbon.multitenancy.deployment.service.exceptions.DeploymentNotFoundException;
import org.wso2.carbon.multitenancy.deployment.service.interfaces.DeploymentProvider;
import org.wso2.carbon.multitenancy.deployment.service.kubernetes.KubernetesDeploymentProvider;
import org.wso2.carbon.multitenancy.deployment.service.models.Deployment;
import org.wso2.msf4j.Microservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Handles deployment automation in a containerized environment. The service is available at,
 * http://<hostname>:<port>/deployments
 */
@Api(value = "service", description = "Manage deployment automation in a containerized environment")
@SwaggerDefinition(
        info = @Info(
                title = "Deployment Automation Swagger Definition",
                version = "1.0",
                description = "Deployment automation service. Manages deployments in a containerized environment.",
                license = @License(
                        name = "Apache 2.0",
                        url = "http://www.apache.org/licenses/LICENSE-2.0"
                ),
                contact = @Contact(
                        name = "WSO2 Inc.",
                        email = "dev@wso2.org",
                        url = "http://wso2.com"
                )
        )
)
@Path("/deployments")
public class DeploymentService implements Microservice {

    private final DeploymentProvider deploymentProvider;

    /**
     * Default deployment service constructor.
     */
    public DeploymentService() {
        deploymentProvider = new KubernetesDeploymentProvider();
    }

    /**
     * Constructor for implementing tests.
     *
     * @param deploymentProvider
     */
    public DeploymentService(DeploymentProvider deploymentProvider) {
        this.deploymentProvider = deploymentProvider;
    }

    /**
     * Get list of deployments.
     * http://localhost:9090/deployments
     *
     * @return List of deployments
     * @throws DeploymentEnvironmentException
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/")
    @ApiOperation(
            value = "Get all deployments",
            response = Deployment.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 412, message = "Invalid deployment platform"),
    })
    public Response getDeployments() throws DeploymentEnvironmentException {
        // TODO: Find namespace of the user
        String namespace = "default";
        return Response.ok()
                .entity(deploymentProvider.listDeployments(namespace))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    /**
     * Get a specific deployment by ID.
     * http://localhost:9090/deployments/wso2esb?platform=kubernetes
     *
     * @param id Deployment ID
     * @return Deployment
     * @throws DeploymentEnvironmentException
     * @throws DeploymentNotFoundException
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @ApiOperation(value = "Get a deployment by ID")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deployment found"),
            @ApiResponse(code = 404, message = "Deployment not found"),
            @ApiResponse(code = 412, message = "Invalid deployment platform")
    })
    public Response getDeployment(@ApiParam(value = "Deployment ID", required = true) @PathParam("id") String id)
            throws DeploymentEnvironmentException, DeploymentNotFoundException {
        // TODO: Find namespace of the user
        String namespace = "default";
        return Response.ok()
                .entity(deploymentProvider.getDeployment(namespace, id))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    /**
     * Deploy a product in the environment.
     * curl -X POST -H "Content-Type: application/json"
     * -d '{"product":"esb","version":"4.9.0","pattern":1,"platform":"kubernetes"}' http://localhost:9090/deployments
     *
     * @param deployment Deployment
     * @return Response
     * @throws DeploymentEnvironmentException
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Deploy a product.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deployment success"),
            @ApiResponse(code = 400, message = "Invalid request body"),
            @ApiResponse(code = 412, message = "Invalid deployment platform")
    })
    public Response deploy(@ApiParam(value = "Deployment object", required = true) Deployment deployment)
            throws DeploymentEnvironmentException, BadRequestException {
        // TODO: Find namespace of the user
        String namespace = "default";
        deploymentProvider.deploy(namespace, deployment);
        return Response.ok()
                .build();
    }

    /**
     * Undeploy a product from the environment.
     * curl -X DELETE -H "Content-Type: application/json"
     * -d '{"product":"esb","version":"4.9.0","pattern":1,"platform":"kubernetes"}' http://localhost:9090/deployments
     *
     * @param deployment Deployment
     * @return Response
     * @throws DeploymentEnvironmentException
     */
    @DELETE
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Undeploy a product")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Undeployment success"),
            @ApiResponse(code = 400, message = "Invalid request body"),
            @ApiResponse(code = 412, message = "Identifying the platform failed")
    })
    public Response undeploy(@ApiParam(value = "Deployment object", required = true) Deployment deployment)
            throws DeploymentEnvironmentException, BadRequestException {
        // TODO: Find namespace of the user
        String namespace = "default";
        deploymentProvider.undeploy(namespace, deployment);
        return Response.ok().build();
    }
}

