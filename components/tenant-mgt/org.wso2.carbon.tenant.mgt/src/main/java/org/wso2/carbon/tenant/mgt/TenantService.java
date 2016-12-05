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
import org.wso2.carbon.tenant.mgt.exceptions.TenantManagementException;
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

    private TenancyProvider tenancyProvider;

    public TenantService() {
        this.tenancyProvider = new KubernetesTenancyProvider();
    }

    @Activate
    protected void activate(BundleContext bundleContext) {
    }

    @Deactivate
    protected void deactivate(BundleContext bundleContext) {
    }

    /**
     * Get list of all the available tenants.
     * http://localhost:9090/tenants
     *
     * @return Response
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get all tenants",
            notes = "Returns all the available tenants",
            response = Tenant.class,
            responseContainer = "List")
    public Response getAllTenants() {
        return Response.ok()
                .entity(tenancyProvider.getTenants())
                .build();
    }

    /**
     * Get details of a particular tenant.
     * http://localhost:9090/tenants/tenant-a
     *
     * @param name Tenant name
     * @return Response
     */
    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get a tenant",
            notes = "Find and return a tenant by name")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Tenant found"),
            @ApiResponse(code = 404, message = "Tenant not found")
    })
    public Response getTenant(@ApiParam(value = "Tenant name", required = true) @PathParam("name") String name) {
        try {
            Tenant tenant = tenancyProvider.getTenant(name);
            return Response.ok()
                    .entity(tenant)
                    .build();
        } catch (TenantManagementException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .build();
        }
    }

    /**
     * Add a new tenant.
     * curl -X POST -H "Content-Type: application/json" -d '{ name: "tenant-a" }' http://localhost:9090/tenants
     *
     * @param tenant Tenant object
     * @return Response
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Add a new tenant")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Tenant created"),
            @ApiResponse(code = 400, message = "Tenant creation failed")
    })
    public Response addTenant(@ApiParam(value = "Tenant object", required = true) Tenant tenant) {
        try {
            tenancyProvider.createTenant(tenant);
            return Response.status(Response.Status.CREATED)
                    .build();
        } catch (TenantManagementException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{ \"error\": \"" + e.getMessage() + "\" }")
                    .build();
        }
    }

    /**
     * Delete a tenant
     * curl -X DELETE http://localhost:9090/tenants/tenant-a
     *
     * @param name Tenant name
     * @return Response
     */
    @DELETE
    @Path("/{name}")
    @ApiOperation(
            value = "Delete a tenant",
            notes = "Delete a tenant identified by name")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Tenant deleted"),
            @ApiResponse(code = 404, message = "Tenant not found")
    })
    public Response delete(@ApiParam(value = "Tenant name", required = true) @PathParam("name") String name) {
        try {
            tenancyProvider.deleteTenant(name);
            return Response.ok()
                    .build();
        } catch (TenantManagementException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .build();
        }
    }
}