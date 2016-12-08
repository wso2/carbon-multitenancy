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

package org.wso2.carbon.deployment.automation;

import io.swagger.annotations.*;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.wso2.carbon.deployment.automation.exceptions.*;
import org.wso2.carbon.deployment.automation.exceptions.BadRequestException;
import org.wso2.carbon.deployment.automation.interfaces.DeploymentProvider;
import org.wso2.carbon.deployment.automation.kubernetes.KubernetesDeploymentProvider;
import org.wso2.carbon.deployment.automation.models.Deployment;
import org.wso2.msf4j.Microservice;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Handles deployment automation in a containerized environment. The service is available at,
 * http://localhost:9090/deployments
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
@Component(
        name = "org.wso2.carbon.deployment.automation.DeploymentService",
        service = Microservice.class,
        immediate = true
)
public class DeploymentService implements Microservice {
    private static final String DEPLOYMENT_KUBERNETES = "kubernetes";
    private static final String ENV_DEPLOYMENT_PLATFORM = "WSO2_DEPLOYMENT_PLATFORM";

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
        return Response.ok()
                .entity(getDeploymentProvider().listDeployments())
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
        return Response.ok()
                .entity(getDeploymentProvider().getDeployment(id))
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
        getDeploymentProvider().deploy(deployment);
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
        getDeploymentProvider().undeploy(deployment);
        return Response.ok()
                .build();
    }

    /**
     * Get deployment provider.
     *
     * @return Deployment provider
     * @throws DeploymentEnvironmentException
     */
    private DeploymentProvider getDeploymentProvider() throws DeploymentEnvironmentException {
        String platform = System.getenv(ENV_DEPLOYMENT_PLATFORM);
        if (platform == null || platform.equals("")) {
            throw new DeploymentEnvironmentException("Unable to identify the deployment platform.");
        }

        if (platform.toLowerCase().equals(DEPLOYMENT_KUBERNETES)) {
            return new KubernetesDeploymentProvider();
        } else {
            throw new DeploymentEnvironmentException("Unsupported deployment platform: " + platform);
        }
    }

    @Activate
    protected void activate(BundleContext bundleContext) {
    }

    @Deactivate
    protected void deactivate(BundleContext bundleContext) {
    }
}

