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

package org.wso2.carbon.deployment.automation.kubernetes;

import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import org.wso2.carbon.deployment.automation.interfaces.DeploymentProvider;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deployment provider for Kubernetes cluster manager.
 */
public class KubernetesDeploymentProvider extends KubernetesBase implements DeploymentProvider {

    public static final String KIND_DEPLOYMENT = "deployment";
    public static final String KIND_SERVICE = "service";
    public static final String KIND_INGRESS = "ingress";

    /**
     * Deploy a product in a Kubernetes environment.
     *
     * @param definition YAML definition of the entire deployment
     * @return Status
     */
    @Override
    public boolean deploy(String definition) {
        // TODO: set the KUBERNETES_NAMESPACE=tenant-1 environment variable
        Map<String, String> definitions = extractDefinitionsByKind(definition);

        // Add deployment
        Deployment deployment = new Yaml().loadAs(definitions.get(KIND_DEPLOYMENT), Deployment.class);
        client.extensions().deployments().create(deployment);

        // Add service
        Service service = new Yaml().loadAs(definitions.get(KIND_SERVICE), Service.class);
        client.services().create(service);

        // Add ingress
        Ingress ingress = new Yaml().loadAs(definitions.get(KIND_INGRESS), Ingress.class);
        client.extensions().ingresses().create(ingress);

        return true;
    }

    /**
     * Remove a product deployment from a Kubernetes environment.
     *
     * @param definition YAML definition of the entire deployment
     * @return Status
     */
    @Override
    public boolean undeploy(String definition) {
        boolean result = true;
        Map<String, String> definitions = extractDefinitionsByKind(definition);

        //Remove ingress
        Ingress ingress = new Yaml().loadAs(definitions.get(KIND_INGRESS), Ingress.class);
        result &= client.extensions()
                .ingresses()
                .withName(ingress.getMetadata().getName())
                .delete();

        // Remove service
        Service service = new Yaml().loadAs(definitions.get(KIND_SERVICE), Service.class);
        result &= client.services()
                .withName(service.getMetadata().getName())
                .delete();

        // Remove deployment
        Deployment deployment = new Yaml().loadAs(definitions.get(KIND_DEPLOYMENT), Deployment.class);
        result &= client.extensions()
                .deployments()
                .withName(deployment.getMetadata().getName())
                .delete();

        return result;
    }

    /**
     * Add a replication controller.
     *
     * @param definition YAML definition of the replication controller.
     * @return Status
     */
    @Override
    public boolean addLoadBalancer(String definition) {
        ReplicationController controller = new Yaml().loadAs(definition, ReplicationController.class);
        client.replicationControllers().create(controller);
        return true;
    }

    /**
     * Extract definitions by kind.
     *
     * @param definition YAML Definition
     * @return Definition map by kind
     */
    private Map<String, String> extractDefinitionsByKind(String definition) {
        Map<String, String> definitions = new HashMap<String, String>();
        // Read the given definition, split it into sub items and deploy each item i.e. deployment, service and ingress
        Map<String, Object> map = (Map<String, Object>) new Yaml().load(definition);
        List<LinkedHashMap> items = (List<LinkedHashMap>) map.get("items");

        for (LinkedHashMap item : items) {
            definitions.put(item.get("kind").toString().toLowerCase(), new Yaml().dump(item));
        }
        return definitions;
    }
}
