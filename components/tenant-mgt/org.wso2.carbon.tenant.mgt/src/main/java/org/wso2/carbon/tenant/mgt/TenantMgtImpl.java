/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.stratos.common.exception.TenantClientException;
import org.wso2.carbon.stratos.common.exception.TenantServerException;
import org.wso2.carbon.stratos.common.util.ClaimsMgtUtil;
import org.wso2.carbon.stratos.common.util.CommonUtil;
import org.wso2.carbon.tenant.mgt.core.TenantPersistor;
import org.wso2.carbon.tenant.mgt.core.exception.TenantManagementClientException;
import org.wso2.carbon.tenant.mgt.core.exception.TenantManagementServerException;
import org.wso2.carbon.tenant.mgt.core.exception.TenantMgtException;
import org.wso2.carbon.tenant.mgt.internal.TenantMgtServiceComponent;
import org.wso2.carbon.tenant.mgt.util.TenantMgtUtil;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.wso2.carbon.tenant.mgt.util.TenantMgtUtil.initializeTenantInfoBean;

/**
 * Default implementation of {@link org.wso2.carbon.tenant.mgt.TenantMgtService} interface.
 */
public class TenantMgtImpl implements TenantMgtService {

    private static final Log log = LogFactory.getLog(TenantMgtImpl.class);

    /**
     * super admin adds a tenant.
     *
     * @param tenant tenant information.
     * @return UUID uuid used to represent the tenant.
     * @throws TenantMgtException if error in adding new tenant.
     */
    public String addTenant(Tenant tenant) throws TenantMgtException {

        try {
            CommonUtil.validateEmail(tenant.getEmail());
        } catch (Exception e) {
            if (e instanceof TenantClientException) {
                throw new TenantManagementClientException(((TenantClientException) e).getErrorCode(), e.getMessage());
            } else if (e instanceof TenantServerException) {
                throw new TenantManagementServerException(e.getMessage(), e);
            }
        }

        String tenantDomain = tenant.getDomain();
        String resourceId;
        int tenantId;
        TenantPersistor persistor = new TenantPersistor();
        try {
            TenantMgtUtil.validateDomain(tenantDomain);
            checkIsSuperTenantInvoking();

            // Set a thread local variable to identify the operations triggered for a tenant admin user.
            TenantMgtUtil.setTenantAdminCreationOperation(true);

            tenant.setCreatedDate(createDate());
            resourceId = UUIDGenerator.generateUUID();
            tenant.setResourceId(resourceId);

            // Not validating the domain ownership, since created by super tenant.
            tenantId = persistor.persistTenant(tenant, false, null, null,
                    false);
            tenant.setId(tenantId);

            try {
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
                carbonContext.setTenantDomain(tenantDomain);
                carbonContext.setTenantId(tenantId);

                TenantMgtUtil.addClaimsToUserStoreManager(tenant);
            } finally {
                // Remove thread local variable set to identify operation triggered for a tenant admin user.
                TenantMgtUtil.clearTenantAdminCreationOperation();
                PrivilegedCarbonContext.endTenantFlow();
            }

            TenantInfoBean tenantInfoBean = initializeTenantInfoBean(tenantId, tenant);
            notifyTenantAddition(tenantInfoBean);

            // For the super tenant tenant creation, tenants are always activated as they are created.
            TenantMgtUtil.activateTenantInitially(tenantInfoBean, tenantId);
        } catch (Exception e) {
            if (e instanceof TenantClientException) {
                throw new TenantManagementClientException(((TenantClientException) e).getErrorCode(), e.getMessage());
            } else {
                throw new TenantManagementServerException(e.getMessage(), e);
            }
        }
        log.info("Added the tenant '" + tenantDomain + " [" + tenantId +
                "]' by '" + PrivilegedCarbonContext.getThreadLocalCarbonContext().
                getUsername() + "'");
        TenantMgtUtil.prepareStringToShowThemeMgtPage(tenant.getId(), resourceId);

        return resourceId;
    }

    private Date createDate() {

        long createdDate = System.currentTimeMillis();
        return new Date(createdDate);
    }

    /**
     * Check if the selected domain is available to register.
     *
     * @param domainName Domain name.
     * @return true, if the domain is available to register.
     * @throws Exception, If unable to get the tenant manager, or get the tenant id from manager.
     */
    public boolean checkDomainAvailability(String domainName) throws Exception {

        if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(domainName)) {
            return false;
        }
        TenantMgtUtil.validateDomain(domainName);
        TenantManager tenantManager = TenantMgtServiceComponent.getTenantManager();
        int tenantId = tenantManager.getTenantId(domainName);
        if (tenantId == -1) {
            if (log.isDebugEnabled()) {
                log.debug("Tenant Domain " + domainName + " is available to register.");
            }
            return true;
        }
        return false;
    }

    private void notifyTenantAddition(TenantInfoBean tenantInfoBean) throws TenantMgtException {

        //Notify tenant addition
        try {
            TenantMgtUtil.triggerAddTenant(tenantInfoBean);
        } catch (StratosException e) {
            String msg = "Error in notifying tenant addition.";
            throw new TenantMgtException(msg, e);
        }
    }

    private void checkIsSuperTenantInvoking() throws TenantManagementServerException {

        UserRegistry userRegistry = (UserRegistry) (CarbonContext.getThreadLocalCarbonContext().getRegistry(
                RegistryType.USER_GOVERNANCE));
        if (userRegistry == null) {
            throw new TenantManagementServerException("Invalid data. Security Alert! User registry is null. A user " +
                    "is trying create a tenant without an authenticated session.");
        }

        if (userRegistry.getTenantId() != MultitenantConstants.SUPER_TENANT_ID) {
            throw new TenantManagementServerException("Invalid data. Security Alert! Non super tenant trying to " +
                    "create a tenant.");
        }
    }

    /**
     * Get the list of the tenants
     *
     * @return List<TenantInfoBean>
     * @throws Exception UserStoreException
     */
    private List<Tenant> getAllTenants() throws Exception {

        TenantManager tenantManager = TenantMgtServiceComponent.getTenantManager();
        Tenant[] tenants;
        try {
            tenants = (Tenant[]) tenantManager.getAllTenants();
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant information.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
        return Arrays.asList(tenants);
    }

    /**
     * Retrieve all the tenants
     *
     * @return tenantInfoBean[]
     * @throws Exception if failed to get Tenant Manager
     */
    public List<Tenant> retrieveTenants() throws Exception {

        return getAllTenants();
    }

    /**
     * Get a specific tenant
     *
     * @param tenantDomain tenant domain
     * @return tenantInfoBean
     * @throws Exception UserStoreException
     */
    public Tenant getTenant(String tenantDomain) throws Exception {

        TenantManager tenantManager = TenantMgtServiceComponent.getTenantManager();
        int tenantId;
        try {
            tenantId = tenantManager.getTenantId(tenantDomain);
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant id for the tenant domain: " +
                    tenantDomain + ".";
            log.error(msg);
            throw new Exception(msg, e);
        }
        Tenant tenant;
        try {
            tenant = (Tenant) tenantManager.getTenant(tenantId);
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant from the tenant manager.";
            log.error(msg);
            throw new Exception(msg, e);
        }

        // retrieve first and last names from the UserStoreManager
        tenant.setAdminFirstName(ClaimsMgtUtil.getFirstNamefromUserStoreManager(
                TenantMgtServiceComponent.getRealmService(), tenantId));
        tenant.setAdminLastName(ClaimsMgtUtil.getLastNamefromUserStoreManager(
                TenantMgtServiceComponent.getRealmService(), tenantId));

        return tenant;
    }

    /**
     * Activate a deactivated tenant, by the super tenant.
     *
     * @param tenantDomain tenant domain
     * @throws Exception UserStoreException.
     */
    public void activateTenant(String tenantDomain) throws Exception {

        TenantManager tenantManager = TenantMgtServiceComponent.getTenantManager();
        int tenantId;
        try {
            tenantId = tenantManager.getTenantId(tenantDomain);
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant id for the tenant domain: " + tenantDomain
                    + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        TenantMgtUtil.activateTenant(tenantDomain, tenantManager, tenantId);

        //Notify tenant activation all listeners
        try {
            TenantMgtUtil.triggerTenantActivation(tenantId);
        } catch (StratosException e) {
            String msg = "Error in notifying tenant activate.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        log.info("Activated the tenant '" + tenantDomain + " [" + tenantId +
                "]' by '" + PrivilegedCarbonContext.getThreadLocalCarbonContext().
                getUsername() + "'");
    }

    /**
     * Deactivate the given tenant
     *
     * @param tenantDomain tenant domain
     * @throws Exception UserStoreException
     */
    public void deactivateTenant(String tenantDomain) throws Exception {

        TenantManager tenantManager = TenantMgtServiceComponent.getTenantManager();
        int tenantId;
        try {
            tenantId = tenantManager.getTenantId(tenantDomain);
        } catch (UserStoreException e) {
            String msg =
                    "Error in retrieving the tenant id for the tenant domain: " +
                            tenantDomain + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        TenantMgtUtil.deactivateTenant(tenantDomain, tenantManager, tenantId);

        //Notify tenant deactivation all listeners
        try {
            TenantMgtUtil.triggerTenantDeactivation(tenantId);
        } catch (StratosException e) {
            String msg = "Error in notifying tenant deactivate.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        log.info("Deactivated the tenant '" + tenantDomain + " [" + tenantId +
                "]' by '" + PrivilegedCarbonContext.getThreadLocalCarbonContext().
                getUsername() + "'");
    }
}
