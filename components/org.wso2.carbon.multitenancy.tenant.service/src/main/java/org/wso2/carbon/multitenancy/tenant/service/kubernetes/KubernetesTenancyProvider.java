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
public class KubernetesTenancyProvider extends KubernetesBase implements TenancyProvider {

    private static final String RESERVED_NAMESPACE_DEFAULT = "default";
    private static final String RESERVED_NAMESPACE_KUBE_SYSTEM = "kube-system";

    /**
     * Initializes the Kubernetes client by providing the master node endpoint. Initially it looks for the master node
     * IP address and the port from the KUBERNETES_MASTER_IP and KUBERNETES_MASTER_PORT environment variables and if not
     * available it falls back to the default endpoint URL.
     */
    public KubernetesTenancyProvider() throws DeploymentEnvironmentException {
        super();
    }

    /**
     * Get list of tenants i.e. namespaces other than the default and kube-system.
     *
     * @return Array of tenants
     */
    @Override
    public List<Tenant> getTenants() {
        return client.namespaces().list().getItems().stream()
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
        Namespace namespace = client.namespaces().withName(sanitizeTenantName(name)).get();
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
        client.namespaces().create(new NamespaceBuilder()
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
        return client.namespaces().withName(name).delete();
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
        return (client.namespaces().withName(namespace).get() != null);
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
