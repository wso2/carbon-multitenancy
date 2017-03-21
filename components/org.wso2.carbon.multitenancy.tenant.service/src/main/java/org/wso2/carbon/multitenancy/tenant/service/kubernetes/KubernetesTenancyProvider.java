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

package org.wso2.carbon.multitenancy.tenant.service.kubernetes;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.multitenancy.tenant.service.exceptions.BadRequestException;
import org.wso2.carbon.multitenancy.tenant.service.exceptions.DeploymentEnvironmentException;
import org.wso2.carbon.multitenancy.tenant.service.exceptions.TenantCreationFailedException;
import org.wso2.carbon.multitenancy.tenant.service.exceptions.TenantNotFoundException;
import org.wso2.carbon.multitenancy.tenant.service.interfaces.TenancyProvider;
import org.wso2.carbon.multitenancy.tenant.service.models.Tenant;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Tenant provider for Kubernetes cluster manager.
 */
public class KubernetesTenancyProvider implements TenancyProvider {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesTenancyProvider.class);

    private static final String KUBERNETES_MASTER_ENV_VAR_NAME = "KUBERNETES_MASTER";
    private static final String KUBERNETES_MASTER_SYS_PROPERTY_NAME = "kubernetes.master";
    private static final String RESERVED_NAMESPACE_DEFAULT = "default";
    private static final String RESERVED_NAMESPACE_KUBE_SYSTEM = "kube-system";

    private final DefaultKubernetesClient kubernetesClient;

    /**
     * Initializes the Kubernetes kubernetesClient by providing the master node endpoint.
     */
    public KubernetesTenancyProvider() {
        String kubernetesMasterUrl = System.getenv(KUBERNETES_MASTER_ENV_VAR_NAME);
        if (kubernetesMasterUrl == null || kubernetesMasterUrl.isEmpty()) {
            kubernetesMasterUrl = System.getProperty(KUBERNETES_MASTER_SYS_PROPERTY_NAME);
        }
        if (kubernetesMasterUrl == null || kubernetesMasterUrl.isEmpty()) {
            throw new DeploymentEnvironmentException("Kubernetes master URL not found, set environment variable "
                    + KUBERNETES_MASTER_ENV_VAR_NAME + " or system property " + KUBERNETES_MASTER_SYS_PROPERTY_NAME
                    + ".");
        }
        kubernetesClient = new DefaultKubernetesClient(kubernetesMasterUrl);

        logger.info("Kubernetes tenancy provider initialized");
        logger.info("Kubernetes master URL: {}", kubernetesMasterUrl);
    }

    /**
     * Get list of tenants i.e. namespaces other than the default and kube-system.
     *
     * @return Array of tenants
     */
    @Override
    public List<Tenant> getTenants() {
        return kubernetesClient.namespaces().list().getItems().stream()
                .filter(namespace -> !isReservedNamespace(namespace.getMetadata().getName()))
                .map(namespace -> {
                    return new Tenant(namespace.getMetadata().getName());
                })
                .collect(Collectors.toList());
    }

    /**
     * Get a specific tenant by tenant name.
     *
     * @param name Tenant name
     * @return Tenant
     * @throws TenantNotFoundException
     */
    @Override
    public Tenant getTenant(String name) throws TenantNotFoundException {
        Namespace namespace = kubernetesClient.namespaces().withName(sanitizeTenantName(name)).get();
        if (namespace == null) {
            throw new TenantNotFoundException("Tenant '" + name + "' not found.");
        }
        return new Tenant(namespace.getMetadata().getName());
    }

    /**
     * Create a new tenant by creating a new namespace.
     *
     * @param tenant Tenant
     * @return Status
     * @throws TenantCreationFailedException
     * @throws BadRequestException
     */
    @Override
    public void createTenant(Tenant tenant) throws TenantCreationFailedException, BadRequestException {
        String name = sanitizeTenantName(tenant.getName());
        // Unable to create a tenant with the system namespace name
        if (isReservedNamespace(name)) {
            throw new BadRequestException("Tenant name '" + tenant.getName() + "' is unavailable.");
        }
        // Check whether the namespace already exists
        if (isNamespaceExists(name)) {
            throw new TenantCreationFailedException("Tenant '" + tenant.getName() + "' already exists.");
        }
        kubernetesClient.namespaces().create(new NamespaceBuilder()
                .withMetadata(
                        new ObjectMetaBuilder()
                                .withName(name)
                                .build())
                .build());
    }

    /**
     * Delete a tenant by deleting the namespace.
     *
     * @param name Tenant name
     * @return Status
     */
    @Override
    public boolean deleteTenant(String name) {
        name = sanitizeTenantName(name);
        // DELETE operation on non-existing resource does not return 404, but 200.
        if (isReservedNamespace(name)) {
            return true;
        }
        return kubernetesClient.namespaces().withName(name).delete();
    }

    /**
     * Check if the namespace is a Kubernetes predefined namespace.
     *
     * @param namespace Namespace
     * @return Status
     */
    private boolean isReservedNamespace(String namespace) {
        return RESERVED_NAMESPACE_DEFAULT.equals(namespace) || RESERVED_NAMESPACE_KUBE_SYSTEM.equals(namespace);
    }

    /**
     * Check whether the namespace already exists.
     *
     * @param namespace Namespace
     * @return Status
     */
    private boolean isNamespaceExists(String namespace) {
        return (kubernetesClient.namespaces().withName(namespace).get() != null);
    }

    /**
     * Sanitize the namespace by replacing any periods with the hyphen.
     *
     * @param name Namespace
     * @return Sanitized namespace
     */
    private String sanitizeTenantName(String name) {
        return name.replaceAll("\\.", "-");
    }
}
