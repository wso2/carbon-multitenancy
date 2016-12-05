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

import com.sun.corba.se.impl.protocol.MinimalServantCacheLocalCRDImpl;
import org.osgi.service.component.annotations.Component;
import org.wso2.carbon.deployment.automation.interfaces.DeploymentProvider;
import org.wso2.carbon.deployment.automation.kubernetes.KubernetesDeploymentProvider;
import org.wso2.msf4j.Microservice;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/deployments")
@Component(
        name = "org.wso2.carbon.deployment.automation.DeploymentService",
        service = Microservice.class,
        immediate = true
)
public class DeploymentService implements Microservice {

    private DeploymentProvider deploymentProvider;

    /**
     * Initializes the concrete deployment provider.
     */
    public DeploymentService() {
        this.deploymentProvider = new KubernetesDeploymentProvider();
    }

    /**
     * Deploy a product in a containerized environment.
     *
     * @param definition
     * @return
     */
    @POST
    @Path("/")
    public Response deploy(String definition) {
        if (deploymentProvider.deploy(definition)) {
            return Response.ok()
                    .build();
        }
        return Response.status(Response.Status.BAD_REQUEST)
                .build();
    }

    /**
     * Un-deploy a product from a containerized environment.
     *
     * @return
     */
    @DELETE
    @Path("/")
    public Response undeploy(String definition) {
        if (deploymentProvider.undeploy(definition)) {
            return Response.ok()
                    .build();
        }
        return Response.status(Response.Status.BAD_REQUEST)
                .build();
    }

    /**
     * Deploy a load balancer.
     *
     * @param definition
     * @return
     */
    @POST
    @Path("/lb")
    public Response deployLoadBalancer(String definition) {
        if (deploymentProvider.addLoadBalancer(definition)) {
            return Response.ok()
                    .build();
        }
        return Response.status(Response.Status.BAD_REQUEST)
                .build();
    }
}

