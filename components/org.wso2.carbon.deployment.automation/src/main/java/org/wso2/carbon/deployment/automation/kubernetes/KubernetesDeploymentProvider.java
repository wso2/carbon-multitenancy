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

package org.wso2.carbon.deployment.automation.kubernetes;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import org.wso2.carbon.deployment.automation.exceptions.BadRequestException;
import org.wso2.carbon.deployment.automation.exceptions.DeploymentEnvironmentException;
import org.wso2.carbon.deployment.automation.exceptions.DeploymentNotFoundException;
import org.wso2.carbon.deployment.automation.interfaces.DeploymentProvider;
import org.wso2.carbon.deployment.automation.models.Deployment;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Deployment provider for Kubernetes cluster manager.
 */
public class KubernetesDeploymentProvider extends KubernetesBase implements DeploymentProvider {

    private static final String KIND_DEPLOYMENT = "deployment";
    private static final String KIND_SERVICE = "service";
    private static final String KIND_INGRESS = "ingress";
    private static final String KIND_LIST = "list";
    private static final String YAML_EXTENSION = ".yaml";
    private static final String KIND = "kind";
    private static final String ITEMS = "items";

    /**
     * Get list of deployments.
     *
     * @return Array of deployments
     */
    @Override
    public List<Deployment> listDeployments() {
        return client.extensions().deployments().list().getItems().stream()
                .map(deployment -> {
                    Map<String, String> labels = deployment.getSpec().getTemplate().getMetadata().getLabels();
                    return new Deployment(
                            deployment.getMetadata().getUid(),
                            labels.get("product"),
                            labels.get("version"),
                            Integer.parseInt(labels.get("pattern")));
                })
                .collect(Collectors.toList());
    }

    /**
     * Get a deployment by ID.
     *
     * @param id Deployment ID
     * @return Deployment
     */
    @Override
    public Deployment getDeployment(String id) throws DeploymentNotFoundException {
        Optional<Deployment> filteredDeployments = client.extensions().deployments().list().getItems().stream()
                .filter(deployment -> deployment.getMetadata().getUid().equals(id))
                .map(deployment -> {
                    Map<String, String> labels = deployment.getSpec().getTemplate().getMetadata().getLabels();
                    return new Deployment(
                            deployment.getMetadata().getUid(),
                            labels.get("product"),
                            labels.get("version"),
                            Integer.parseInt(labels.get("pattern")));
                }).findFirst();

        if (!filteredDeployments.isPresent()) {
            throw new DeploymentNotFoundException("Deployment '" + id + "' not found.");
        }
        return filteredDeployments.get();
    }

    /**
     * Deploy a product in a Kubernetes environment.
     *
     * @param deployment Deployment details
     * @throws DeploymentEnvironmentException
     */
    @Override
    public void deploy(Deployment deployment) throws DeploymentEnvironmentException, BadRequestException {
        for (String profilePath : getProductProfiles(deployment)) {
            Map<String, String> resources = fetchResources(profilePath);
            // Add deployment
            if (resources.containsKey(KIND_DEPLOYMENT)) {
                io.fabric8.kubernetes.api.model.extensions.Deployment kubernetesDeployment = new Yaml().loadAs(
                        resources.get(KIND_DEPLOYMENT), io.fabric8.kubernetes.api.model.extensions.Deployment.class);
                client.extensions().deployments().create(kubernetesDeployment);
            }
            // Add service
            if (resources.containsKey(KIND_SERVICE)) {
                Service service = new Yaml().loadAs(resources.get(KIND_SERVICE), Service.class);
                client.services().create(service);
            }
            // Add ingress
            if (resources.containsKey(KIND_INGRESS)) {
                Ingress ingress = new Yaml().loadAs(resources.get(KIND_INGRESS), Ingress.class);
                client.extensions().ingresses().create(ingress);
            }
        }
    }

    /**
     * Undeploy a product from a Kubernetes environment.
     *
     * @param deployment Deployment details
     * @throws DeploymentEnvironmentException
     */
    @Override
    public void undeploy(Deployment deployment) throws DeploymentEnvironmentException, BadRequestException {
        for (String profilePath : getProductProfiles(deployment)) {
            Map<String, String> resources = fetchResources(profilePath);
            //Remove ingress
            if (resources.containsKey(KIND_INGRESS)) {
                Ingress ingress = new Yaml().loadAs(resources.get(KIND_INGRESS), Ingress.class);
                client.extensions()
                        .ingresses()
                        .withName(ingress.getMetadata().getName())
                        .delete();
            }
            // Remove service
            if (resources.containsKey(KIND_SERVICE)) {
                Service service = new Yaml().loadAs(resources.get(KIND_SERVICE), Service.class);
                client.services()
                        .withName(service.getMetadata().getName())
                        .delete();
            }
            // Remove deployment
            if (resources.containsKey(KIND_DEPLOYMENT)) {
                io.fabric8.kubernetes.api.model.extensions.Deployment kubernetesDeployment = new Yaml().loadAs(
                        resources.get(KIND_DEPLOYMENT), io.fabric8.kubernetes.api.model.extensions.Deployment.class);
                client.extensions()
                        .deployments()
                        .withName(kubernetesDeployment.getMetadata().getName())
                        .delete();
            }
        }
    }

    /**
     * Get available product profile paths for the given product pattern.
     *
     * @param deployment Deployment details
     * @return List of paths to profiles
     */
    private List<String> getProductProfiles(Deployment deployment) throws BadRequestException {
        String path = Paths.get(System.getProperty("carbon.home"), "deployment", "kubernetes-artifacts",
                deployment.getProduct(), deployment.getVersion(), "pattern-" + deployment.getPattern()).toString();

        File patternDir = new File(path);
        if (!patternDir.exists()) {
            throw new BadRequestException("Unable to find the deployment artifacts for given details.");
        }
        return Arrays.stream(patternDir.listFiles())
                .filter(file -> file.getName().endsWith(YAML_EXTENSION))
                .map(file -> Paths.get(path, file.getName()).toString())
                .collect(Collectors.toList());
    }

    /**
     * Parse product profile to fetch Kubernetes resources.
     *
     * @param path Path to profile
     * @return Map contains resources
     * @throws DeploymentEnvironmentException
     */
    private Map<String, String> fetchResources(String path) throws DeploymentEnvironmentException {
        Map<String, String> resources = new HashMap<String, String>();
        Map<String, Object> map;
        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(new File(path));
            map = (Map<String, Object>) new Yaml().load(fileInputStream);
        } catch (FileNotFoundException e) {
            throw new DeploymentEnvironmentException("Unable to find the profile for the given product", e);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException ignore) {
                }
            }
        }

        if (map.get(KIND).toString().equalsIgnoreCase(KIND_LIST)) {
            for (LinkedHashMap item : (List<LinkedHashMap>) map.get(ITEMS)) {
                resources.put(item.get(KIND).toString(), new Yaml().dump(item));
            }
        } else {
            resources.put(map.get(KIND).toString(), new Yaml().dump(map));
        }
        return resources;
    }
}
