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
import org.osgi.service.component.annotations.Component;
import org.wso2.carbon.deployment.automation.exceptions.DeploymentAutomationException;
import org.wso2.carbon.deployment.automation.interfaces.DeploymentProvider;
import org.wso2.carbon.deployment.automation.kubernetes.KubernetesDeploymentProvider;
import org.wso2.carbon.deployment.automation.models.Deployment;
import org.wso2.msf4j.Microservice;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

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

    /**
     * Get list of deployments.
     * http://localhost:9090/deployments?platform=kubernetes
     *
     * @param platform Deployment platform
     * @return Response
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/")
    @ApiOperation(
            value = "Get all deployments",
            notes = "Returns all deployment",
            response = Deployment.class,
            responseContainer = "List")
    public Response getDeployments(@QueryParam("platform") String platform) {
        try {
            List<Deployment> deployments = getDeploymentProvider(platform).listDeployments();
            return Response.ok()
                    .entity(deployments)
                    .build();
        } catch (DeploymentAutomationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        }
    }

    /**
     * Get a specific deployment by ID.
     * * http://localhost:9090/deployments/wso2esb?platform=kubernetes
     *
     * @param id       Deployment ID
     * @param platform Deployment platform
     * @return Response
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @ApiOperation(
            value = "Get a deployment",
            notes = "Find and return a deployment by ID")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deployment found"),
            @ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 404, message = "Deployment not found")
    })
    public Response getDeployment(@ApiParam(value = "Deployment ID", required = true) @PathParam("id") String id,
                                  @ApiParam(value = "Platform", required = true) @QueryParam("platform") String platform) {
        try {
            Deployment deployment = getDeploymentProvider(platform).getDeployment(id);
            if (deployment == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .build();
            }
            return Response.ok()
                    .entity(deployment)
                    .build();
        } catch (DeploymentAutomationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        }
    }

    /**
     * Deploy a product in the environment.
     * curl -X POST -H "Content-Type: application/json"
     * -d '{"product":"esb","version":"4.9.0","pattern":1,"platform":"kubernetes"}' http://localhost:9090/deployments
     *
     * @param deployment Deployment details
     * @return Response
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Deploy a specific product.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deployment successful"),
            @ApiResponse(code = 400, message = "Deployment failed")
    })
    public Response deploy(@ApiParam(value = "Deployment object", required = true) Deployment deployment) {
        try {
            getDeploymentProvider(deployment.getPlatform()).deploy(deployment);
            return Response.ok()
                    .build();
        } catch (DeploymentAutomationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        }
    }

    /**
     * Undeploy a product from the environment.
     * curl -X DELETE -H "Content-Type: application/json"
     * -d '{"product":"esb","version":"4.9.0","pattern":1,"platform":"kubernetes"}' http://localhost:9090/deployments
     *
     * @param deployment Product details
     * @return Response
     */
    @DELETE
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Remove a specific product deployment.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Undeployment successful"),
            @ApiResponse(code = 400, message = "Undeployment failed")
    })
    public Response undeploy(@ApiParam(value = "Deployment object", required = true) Deployment deployment) {
        try {
            getDeploymentProvider(deployment.getPlatform()).undeploy(deployment);
            return Response.ok()
                    .build();
        } catch (DeploymentAutomationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        }
    }

    /**
     * Get deployment provider.
     *
     * @param platform Name of the deployment platform
     * @return DeploymentProvider
     * @throws DeploymentAutomationException
     */
    private DeploymentProvider getDeploymentProvider(String platform) throws DeploymentAutomationException {
        if (platform.toLowerCase().equals(DEPLOYMENT_KUBERNETES)) {
            return new KubernetesDeploymentProvider();
        }
        throw new DeploymentAutomationException("Deployment platform '" + platform +
                "' is not supported by the service.");
    }
}

