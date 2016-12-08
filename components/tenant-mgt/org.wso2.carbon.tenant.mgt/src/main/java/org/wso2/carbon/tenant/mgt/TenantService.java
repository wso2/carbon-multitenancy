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

package org.wso2.carbon.tenant.mgt;

import io.swagger.annotations.*;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.wso2.carbon.tenant.mgt.exceptions.DeploymentEnvironmentException;
import org.wso2.carbon.tenant.mgt.exceptions.TenantCreationFailedException;
import org.wso2.carbon.tenant.mgt.exceptions.TenantNotFoundException;
import org.wso2.carbon.tenant.mgt.exceptions.BadRequestException;
import org.wso2.carbon.tenant.mgt.interfaces.TenancyProvider;
import org.wso2.carbon.tenant.mgt.kubernetes.KubernetesTenancyProvider;
import org.wso2.carbon.tenant.mgt.models.Tenant;
import org.wso2.msf4j.Microservice;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Handles tenants in a containerized environment. The service is available at.
 * http://localhost:9090/tenants
 */
@Api(value = "service", description = "Manage tenants in a containerized environment")
@SwaggerDefinition(
        info = @Info(
                title = "Tenants Swagger Definition",
                version = "1.0",
                description = "Tenants service. Manages tenants in a containerized environment.",
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
@Path("/tenants")
@Component(
        name = "org.wso2.carbon.tenant.mgt.TenantService",
        service = Microservice.class,
        immediate = true
)
public class TenantService implements Microservice {
    private static final String DEPLOYMENT_KUBERNETES = "kubernetes";
    private static final String ENV_DEPLOYMENT_PLATFORM = "WSO2_DEPLOYMENT_PLATFORM";

    /**
     * Get list of all the available tenants.
     * http://localhost:9090/tenants
     *
     * @return Tenants
     * @throws DeploymentEnvironmentException
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get all tenants",
            response = Tenant.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 412, message = "Invalid deployment platform"),
    })
    public Response getAllTenants() throws DeploymentEnvironmentException {
        return Response.ok()
                .entity(getTenancyProvider().getTenants())
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    /**
     * Get details of a particular tenant.
     * http://localhost:9090/tenants/tenant-a
     *
     * @param name Tenant name
     * @return Tenant
     * @throws TenantNotFoundException
     * @throws DeploymentEnvironmentException
     */
    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a tenant by name")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Tenant found"),
            @ApiResponse(code = 404, message = "Tenant not found"),
            @ApiResponse(code = 412, message = "Invalid deployment platform")
    })
    public Response getTenant(@ApiParam(value = "Tenant name", required = true) @PathParam("name") String name)
            throws TenantNotFoundException, DeploymentEnvironmentException {
        return Response.ok()
                .entity(getTenancyProvider().getTenant(name))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    /**
     * Add a new tenant.
     * curl -X POST -H "Content-Type: application/json" -d '{ name: "tenant-a" }' http://localhost:9090/tenants
     *
     * @param tenant Tenant
     * @return Status
     * @throws TenantCreationFailedException
     * @throws DeploymentEnvironmentException
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Add new tenant")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Tenant created"),
            @ApiResponse(code = 400, message = "Invalid request body"),
            @ApiResponse(code = 409, message = "Tenant creation failed"),
            @ApiResponse(code = 412, message = "Invalid deployment platform")
    })
    public Response addTenant(@ApiParam(value = "Tenant object", required = true) Tenant tenant)
            throws TenantCreationFailedException, DeploymentEnvironmentException, BadRequestException {
        getTenancyProvider().createTenant(tenant);
        return Response.status(Response.Status.CREATED)
                .build();
    }

    /**
     * Delete a tenant
     * curl -X DELETE http://localhost:9090/tenants/tenant-a
     *
     * @param name Tenant name
     * @return Status
     * @throws DeploymentEnvironmentException
     */
    @DELETE
    @Path("/{name}")
    @ApiOperation(value = "Delete tenant by name")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Tenant deleted"),
            @ApiResponse(code = 412, message = "Invalid deployment platform")
    })
    public Response delete(@ApiParam(value = "Tenant name", required = true) @PathParam("name") String name)
            throws DeploymentEnvironmentException {
        getTenancyProvider().deleteTenant(name);
        return Response.ok()
                .build();
    }

    /**
     * Get tenancy provider.
     *
     * @return Deployment platform
     * @throws DeploymentEnvironmentException
     */
    private TenancyProvider getTenancyProvider() throws DeploymentEnvironmentException {
        String platform = System.getenv(ENV_DEPLOYMENT_PLATFORM);
        if (platform == null || platform.equals("")) {
            throw new DeploymentEnvironmentException("Unable to identify the deployment platform.");
        }

        if (platform.toLowerCase().equals(DEPLOYMENT_KUBERNETES)) {
            return new KubernetesTenancyProvider();
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