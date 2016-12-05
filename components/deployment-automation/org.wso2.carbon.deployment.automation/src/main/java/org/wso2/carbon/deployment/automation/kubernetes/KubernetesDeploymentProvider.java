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

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.deployment.automation.exceptions.DeploymentAutomationException;
import org.wso2.carbon.deployment.automation.interfaces.DeploymentProvider;
import org.wso2.carbon.deployment.automation.models.Product;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deployment provider for Kubernetes cluster manager.
 */
public class KubernetesDeploymentProvider extends KubernetesBase implements DeploymentProvider {
    private static final String KIND_DEPLOYMENT = "deployment";
    private static final String KIND_SERVICE = "service";
    private static final String KIND_INGRESS = "ingress";
    private static final String KIND_LIST = "list";
    private static final String ENV_KUBERNETES_HOME = "KUBERNETES_HOME";

    /**
     * Deploy a product in a Kubernetes environment.
     *
     * @param product Product details
     * @return Status
     */
    @Override
    public void deploy(Product product) throws DeploymentAutomationException {
        // TODO: set the KUBERNETES_NAMESPACE=tenant-1 environment variable
        Map<String, String> artifacts = getKubernetesArtifacts(product);
        // Add deployment
        if (artifacts.containsKey(KIND_DEPLOYMENT)) {
            Deployment deployment = new Yaml().loadAs(artifacts.get(KIND_DEPLOYMENT), Deployment.class);
            client.extensions().deployments().create(deployment);
        }
        // Add service
        if (artifacts.containsKey(KIND_SERVICE)) {
            Service service = new Yaml().loadAs(artifacts.get(KIND_SERVICE), Service.class);
            client.services().create(service);
        }
        // Add ingress
        if (artifacts.containsKey(KIND_INGRESS)) {
            Ingress ingress = new Yaml().loadAs(artifacts.get(KIND_INGRESS), Ingress.class);
            client.extensions().ingresses().create(ingress);
        }
    }

    /**
     * Remove a product deployment from a Kubernetes environment.
     *
     * @param product Product details
     * @return Status
     */
    @Override
    public void undeploy(Product product) throws DeploymentAutomationException {
        Map<String, String> artifacts = getKubernetesArtifacts(product);
        //Remove ingress
        if (artifacts.containsKey(KIND_INGRESS)){
            Ingress ingress = new Yaml().loadAs(artifacts.get(KIND_INGRESS), Ingress.class);
            client.extensions()
                    .ingresses()
                    .withName(ingress.getMetadata().getName())
                    .delete();
        }
        // Remove service
        if (artifacts.containsKey(KIND_SERVICE)) {
            Service service = new Yaml().loadAs(artifacts.get(KIND_SERVICE), Service.class);
            client.services()
                    .withName(service.getMetadata().getName())
                    .delete();
        }
        // Remove deployment
        if (artifacts.containsKey(KIND_DEPLOYMENT)) {
            Deployment deployment = new Yaml().loadAs(artifacts.get(KIND_DEPLOYMENT), Deployment.class);
            client.extensions()
                    .deployments()
                    .withName(deployment.getMetadata().getName())
                    .delete();
        }
    }

    /**
     * Get the kubernetes artifacts from the file system.
     * In order to identify the directory which resides respective artifacts, KUBERNETES_HOME environment variable
     * should be set.
     *
     * @param product Product details
     * @return Hashmap of Kubernetes artifacts against kind
     * @throws DeploymentAutomationException
     */
    private Map<String, String> getKubernetesArtifacts(Product product) throws DeploymentAutomationException {
        String kubernetesHome = System.getenv(ENV_KUBERNETES_HOME);
        if (kubernetesHome == null || kubernetesHome.equals("")) {
            throw new DeploymentAutomationException(ENV_KUBERNETES_HOME + " environment variable is not set.");
        }
        String path = (kubernetesHome.endsWith("/") ? kubernetesHome : kubernetesHome + "/") + product.getProduct() +
                "/" + product.getVersion() + "/pattern-" + product.getPattern() + "/profile.yaml";

        Map<String, String> results = new HashMap<String, String>();
        Map<String, Object> map = null;
        try {
            map = (Map<String, Object>) new Yaml().load(new FileInputStream(new File(path)));
        } catch (FileNotFoundException e) {
            throw new DeploymentAutomationException("Unable to find the Kubernetes artifact for the given product" +
                    path, e);
        }
        if (map.get("kind").toString().toLowerCase().equals(KIND_LIST)) {
            for (LinkedHashMap item : (List<LinkedHashMap>) map.get("items")) {
                results.put(item.get("kind").toString().toLowerCase(), new Yaml().dump(item));
            }
        } else {
            // todo
            results.put(map.get("kind").toString().toLowerCase(), new Yaml().dump(map));
        }
        return results;
    }
}
