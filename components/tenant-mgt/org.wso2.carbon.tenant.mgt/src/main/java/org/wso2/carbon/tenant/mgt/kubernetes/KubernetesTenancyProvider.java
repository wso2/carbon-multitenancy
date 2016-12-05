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

package org.wso2.carbon.tenant.mgt.kubernetes;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.wso2.carbon.tenant.mgt.exceptions.TenantManagementException;
import org.wso2.carbon.tenant.mgt.interfaces.TenancyProvider;
import org.wso2.carbon.tenant.mgt.models.Tenant;

import java.util.ArrayList;
import java.util.List;

/**
 * Tenant provider for Kubernetes cluster manager.
 */
public class KubernetesTenancyProvider extends KubernetesBase implements TenancyProvider {

    public static final String RESERVED_NAMESPACE_DEFAULT = "default";
    public static final String RESERVED_NAMESPACE_KUBE_SYSTEM = "kube-system";

    /**
     * Get list of tenants i.e. namespaces other than the default and kube-system.
     *
     * @return Array of tenants
     */
    @Override
    public Tenant[] getTenants() {
        List<Tenant> tenants = new ArrayList<Tenant>();
        for (Namespace ns : client.namespaces().list().getItems()) {
            String name = ns.getMetadata().getName();
            // Do not return reserved namespaces.
            if (!isReservedNamespace(name)) {
                tenants.add(new Tenant(name));
            }
        }
        return tenants.toArray(new Tenant[tenants.size()]);
    }

    /**
     * Get a specific tenant by tenant name.
     *
     * @param name Tenant name
     * @return Tenant
     * @throws TenantManagementException
     */
    @Override
    public Tenant getTenant(String name) throws TenantManagementException {
        Namespace namespace = client.namespaces()
                .withName(sanitizeTenantName(name))
                .get();

        if (namespace == null) {
            throw new TenantManagementException("Tenant '" + name + "' not found.");
        }
        return new Tenant(namespace.getMetadata().getName());
    }

    /**
     * Create a new tenant by creating a new namespace.
     *
     * @param tenant Tenant
     * @return Status
     * @throws TenantManagementException
     */
    @Override
    public void createTenant(Tenant tenant) throws TenantManagementException {
        String name = sanitizeTenantName(tenant.getName());
        // Unable to create a tenant with the system namespace name
        if (isReservedNamespace(name)) {
            throw new TenantManagementException("Tenant name '" + tenant.getName() + "' is unavailable.");
        }

        // Check whether the namespace already exists
        if (isNamespaceExists(name)) {
            throw new TenantManagementException("Tenant '" + tenant.getName() + "' already exists.");
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
     * @throws TenantManagementException
     */
    @Override
    public boolean deleteTenant(String name) throws TenantManagementException {
        name = sanitizeTenantName(name);
        if (isReservedNamespace(name)) {
            throw new TenantManagementException("Tenant '" + name + "' cannot be deleted.");
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
        return name.replaceAll("\\.", "-").toLowerCase();
    }
}
