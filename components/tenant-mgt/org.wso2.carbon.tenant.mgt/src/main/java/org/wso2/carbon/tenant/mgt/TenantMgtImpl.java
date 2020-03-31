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
import org.wso2.carbon.stratos.common.util.CommonUtil;
import org.wso2.carbon.tenant.mgt.core.TenantPersistor;
import org.wso2.carbon.tenant.mgt.core.exception.TenantManagementClientException;
import org.wso2.carbon.tenant.mgt.core.exception.TenantManagementServerException;
import org.wso2.carbon.tenant.mgt.core.exception.TenantMgtException;
import org.wso2.carbon.tenant.mgt.core.internal.TenantMgtCoreServiceComponent;
import org.wso2.carbon.tenant.mgt.internal.TenantMgtServiceComponent;
import org.wso2.carbon.tenant.mgt.util.TenantMgtUtil;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.TenantMgtConfiguration;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.config.multitenancy.MultiTenantRealmConfigBuilder;
import org.wso2.carbon.user.core.constants.UserCoreClaimConstants;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.wso2.carbon.tenant.mgt.util.TenantMgtUtil.initializeTenantInfoBean;

/**
 * Default implementation of {@link org.wso2.carbon.tenant.mgt.TenantMgtService} interface.
 */
public class TenantMgtImpl implements TenantMgtService {

    private static final Log log = LogFactory.getLog(TenantMgtImpl.class);
    private static final String ADD_ADMIN_TRUE = "true";

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

            tenant = addRealmConfigToTenant(tenant);

            tenantId = persistor.persistTenant(tenant);
            tenant.setId(tenantId);

            addTenantAdminUser(tenant);

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

    private Tenant addRealmConfigToTenant(Tenant tenant) throws TenantMgtException {

        RealmService realmService = TenantMgtCoreServiceComponent.getRealmService();
        RealmConfiguration realmConfig = realmService.getBootstrapRealmConfiguration();
        TenantMgtConfiguration tenantMgtConfiguration = realmService.getTenantMgtConfiguration();
        try {
            MultiTenantRealmConfigBuilder builder = TenantMgtCoreServiceComponent.
                    getRealmService().getMultiTenantRealmConfigBuilder();
            RealmConfiguration realmConfigToPersist =
                    builder.getRealmConfigForTenantToPersist(realmConfig, tenantMgtConfiguration,
                            tenant, -1);
            tenant.setRealmConfig(realmConfigToPersist);
            // Make AddAdmin true since user creation should happen even AddAdmin false
            realmService.getBootstrapRealm().getRealmConfiguration().setAddAdmin(ADD_ADMIN_TRUE);
            return tenant;

        } catch (UserStoreException e) {
            throw new TenantMgtException("Error while getting the realm config.", e);
        }
    }

    /**
     * Add tenant admin user. When get the User Realm from Realm Service it create the admin user.
     *
     * @param tenant tenant information.
     * @throws TenantManagementServerException throws when tenant admin user addition fails.
     */
    private void addTenantAdminUser(Tenant tenant) throws TenantManagementServerException {

        RealmService realmService = TenantMgtCoreServiceComponent.getRealmService();
        try {
            realmService.getTenantManager().getTenant(tenant.getId()).getRealmConfig()
                    .setAdminPassword(tenant.getAdminPassword());
            // Here when get the user realm it create admin user and group.
            realmService.getTenantUserRealm(tenant.getId());
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new TenantManagementServerException("Error while adding tenant admin user.", e);
        }
    }

    private Date createDate() {

        long createdDate = System.currentTimeMillis();
        return new Date(createdDate);
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
     * Get the list of the tenants.
     *
     * @return List<Tenant>
     * @throws TenantManagementServerException if getting tenant failed.
     */
    private List<Tenant> getAllTenants() throws TenantManagementServerException {

        TenantManager tenantManager = TenantMgtServiceComponent.getTenantManager();
        Tenant[] tenants;
        try {
            tenants = (Tenant[]) tenantManager.getAllTenants();
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            String msg = "Error in retrieving the tenant information.";
            throw new TenantManagementServerException(msg, e);
        }

        List<Tenant> tenantList = Arrays.asList(tenants);
        List<Tenant> tenantListWithUserId = new ArrayList<>();
        for (Tenant tenant : tenantList) {
            String userId = getClaimValue(tenant.getAdminName(), UserCoreClaimConstants.USER_ID_CLAIM_URI,
                    tenant.getId());
            tenant.setAdminUserId(userId);
            tenantListWithUserId.add(tenant);
        }

        return tenantListWithUserId;
    }

    /**
     * Retrieve all the tenants.
     *
     * @return List<Tenant>
     * @throws TenantMgtException if failed to get the tenants.
     */
    public List<Tenant> retrieveTenants() throws TenantMgtException {

        return getAllTenants();
    }

    /**
     * Get a specific tenant.
     *
     * @param tenantDomain tenant domain.
     * @return Tenant
     * @throws TenantMgtException if getting the tenant fails.
     */
    public Tenant getTenant(String tenantDomain) throws TenantMgtException {

        TenantManager tenantManager = TenantMgtServiceComponent.getTenantManager();
        int tenantId;
        Tenant tenant;
        try {
            tenantId = tenantManager.getTenantId(tenantDomain);
            tenant = (Tenant) tenantManager.getTenant(tenantId);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new TenantManagementServerException("Error while getting the tenant: " + tenantDomain + " .", e);
        }

        String userId = getClaimValue(tenant.getAdminName(), UserCoreClaimConstants.USER_ID_CLAIM_URI, tenantId);
        tenant.setAdminUserId(userId);

        return tenant;
    }

    private String getClaimValue(String userName, String claim, int tenantId) throws TenantManagementServerException {

        String claimValue = null;
        RealmService realmService = TenantMgtServiceComponent.getRealmService();
        try {
            UserRealm tenantUserRealm = realmService.getTenantUserRealm(tenantId);
            if (tenantUserRealm != null) {
                UserStoreManager userStoreManager = (UserStoreManager) tenantUserRealm.getUserStoreManager();
                if (userStoreManager != null) {
                    claimValue = userStoreManager.getUserClaimValue(userName, claim,
                            UserCoreConstants.DEFAULT_PROFILE);
                }
            }
        } catch (org.wso2.carbon.user.api.UserStoreException ex) {
            throw new TenantManagementServerException("Error while getting claim value for the claim: " + claim, ex);
        }
        return claimValue;
    }

    /**
     * Activate a deactivated tenant, by the super tenant.
     *
     * @param tenantDomain tenant domain
     * @throws TenantMgtException if the tenant activation fails.
     */
    public void activateTenant(String tenantDomain) throws TenantMgtException {

        TenantManager tenantManager = TenantMgtServiceComponent.getTenantManager();
        int tenantId;
        try {
            tenantId = tenantManager.getTenantId(tenantDomain);
            TenantMgtUtil.activateTenant(tenantDomain, tenantManager, tenantId);

            // Notify tenant activation all listeners.
            TenantMgtUtil.triggerTenantActivation(tenantId);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new TenantManagementServerException("Error in retrieving the tenant id for the tenant domain: " +
                    tenantDomain + " .", e);
        } catch (StratosException e) {
            throw new TenantManagementServerException("Error in notifying tenant activation of tenant: " +
                    tenantDomain + " .", e);
        } catch (Exception e) {
            throw new TenantManagementServerException("Error while activating the tenant: " + tenantDomain + " .", e);
        }

        log.info("Activated the tenant '" + tenantDomain + " [" + tenantId + "]' by '" +
                PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername() + "'");
    }

    /**
     * Deactivate the given tenant.
     *
     * @param tenantDomain tenant domain
     * @throws TenantMgtException if tenant deactivation fails.
     */
    public void deactivateTenant(String tenantDomain) throws TenantMgtException {

        TenantManager tenantManager = TenantMgtServiceComponent.getTenantManager();
        int tenantId;
        try {
            tenantId = tenantManager.getTenantId(tenantDomain);
            // Notify tenant deactivation to all listeners.
            TenantMgtUtil.triggerTenantDeactivation(tenantId);
            TenantMgtUtil.deactivateTenant(tenantDomain, tenantManager, tenantId);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new TenantManagementServerException("Error in retrieving the tenant id for the tenant domain: " +
                    tenantDomain + " .", e);
        } catch (StratosException e) {
            throw new TenantManagementServerException("Error while triggering tenant deactivation for the tenant: " +
                    tenantDomain + " .", e);
        } catch (Exception e) {
            throw new TenantManagementServerException("Error while deactivating the tenant: " + tenantDomain + " .", e);
        }
        log.info("Deactivated the tenant '" + tenantDomain + " [" + tenantId +
                "]' by '" + PrivilegedCarbonContext.getThreadLocalCarbonContext().
                getUsername() + "'");
    }


}
