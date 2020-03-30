/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.tenant.mgt.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.stratos.common.config.CloudServiceConfigParser;
import org.wso2.carbon.stratos.common.config.CloudServicesDescConfig;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.stratos.common.exception.TenantClientException;
import org.wso2.carbon.stratos.common.exception.TenantServerException;
import org.wso2.carbon.stratos.common.util.CloudServicesUtil;
import org.wso2.carbon.stratos.common.util.CommonUtil;
import org.wso2.carbon.tenant.mgt.core.constants.TenantMgtConstants;
import org.wso2.carbon.tenant.mgt.core.exception.TenantManagementClientException;
import org.wso2.carbon.tenant.mgt.core.exception.TenantManagementServerException;
import org.wso2.carbon.tenant.mgt.core.exception.TenantMgtException;
import org.wso2.carbon.tenant.mgt.core.internal.TenantMgtCoreServiceComponent;
import org.wso2.carbon.tenant.mgt.core.util.TenantCoreUtil;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.TenantMgtConfiguration;
import org.wso2.carbon.user.core.*;
import org.wso2.carbon.user.core.AuthorizationManager;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.config.multitenancy.MultiTenantRealmConfigBuilder;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.user.mgt.UserMgtConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import static org.wso2.carbon.tenant.mgt.core.constants.TenantMgtConstants.ErrorMessage.ERROR_CODE_EXISTING_USER_NAME;

/**
 * TenantPersistenceManager - Methods related to persisting the tenant.
 */
public class TenantPersistor {

    private static final Log log = LogFactory.getLog(TenantPersistor.class);

    private static CloudServicesDescConfig cloudServicesDesc = null;
    private static final String ADD_ADMIN_TRUE = "true";

    /**
     * Persists the given tenant.
     *
     * @param tenant                tenant to be persisted.
     * @param checkDomainValidation True, if domain is validated.
     * @param successKey            SuccessKey.
     * @param originatedService     The Service that the tenant registration was originated.
     * @return tenant Id - the tenant id
     * @throws Exception If persisting tenant failed.
     */
    public int persistTenant(Tenant tenant, boolean checkDomainValidation, String successKey,
                             String originatedService, boolean isSkeleton) throws Exception {

        int tenantId;
        if (!isSkeleton) {
            tenantId = persistTenantInUserStore(tenant, checkDomainValidation, successKey);
        } else {
            tenantId = tenant.getId();
        }
        doPostTenantCreationActions(tenant, originatedService);
        return tenantId;
    }

    private int persistTenantInUserStore(Tenant tenant, boolean checkDomainValidation, String successKey)
            throws TenantMgtException {

        int tenantId;
        validateAdminUserName(tenant);
        String tenantDomain = tenant.getDomain();

        boolean isDomainAvailable;
        try {
            isDomainAvailable = CommonUtil.isDomainNameAvailable(tenantDomain);
        } catch (Exception e) {
            if (e instanceof TenantClientException) {
                throw new TenantManagementClientException(((TenantClientException) e).getErrorCode(), e.getMessage());
            } else if (e instanceof TenantServerException) {
                throw new TenantManagementServerException(e.getMessage(), e);
            }
            throw new TenantMgtException(e.getMessage(), e);
        }
        if (!isDomainAvailable) {
            throw new TenantManagementClientException(TenantMgtConstants.ErrorMessage.ERROR_CODE_EXISTING_DOMAIN);
        }

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
            tenantId = addTenant(tenant);
            tenant.setId(tenantId);

            if (checkDomainValidation) {
                if (successKey != null) {
                    if (CommonUtil.validateDomainFromSuccessKey(TenantMgtCoreServiceComponent.
                                    getGovernanceSystemRegistry(MultitenantConstants.SUPER_TENANT_ID),
                            tenant.getDomain(), successKey)) {
                        storeDomainValidationFlagToRegistry(tenant);
                    } else {
                        String msg = "Failed to validate domain";
                        throw new TenantManagementServerException(msg);
                    }
                }
            } else {
                storeDomainValidationFlagToRegistry(tenant);
            }

            addTenantAdminUser(tenant);
        } catch (UserStoreException e) {
            throw new TenantMgtException("Error while getting the realm config.", e);
        } catch (RegistryException e) {
            throw new TenantMgtException("Error while storing validation flag to registry.", e);
        }
        return tenantId;
    }

    private void doPostTenantCreationActions(Tenant tenant,
                                             String originatedService) throws TenantManagementServerException {

        try {
            TenantMgtCoreServiceComponent.getRegistryLoader().loadTenantRegistry(tenant.getId());
            copyUIPermissions(tenant.getId());

            TenantCoreUtil.setOriginatedService(tenant.getId(), originatedService);
            setActivationFlags(tenant.getId(), originatedService);
        } catch (Exception ex) {
            throw new TenantManagementServerException(ex.getMessage(), ex);
        }

        TenantCoreUtil.initializeRegistry(tenant.getId());

    }

    /**
     * Store the domain validation flag in the registry if the domain has been
     * validated.
     * 
     * @param tenant - the tenant
     * @throws RegistryException, if storing the domain validation flag failed.
     */
    protected void storeDomainValidationFlagToRegistry(Tenant tenant) throws RegistryException {

        try {
            String domainValidationPath = StratosConstants.TENANT_DOMAIN_VERIFICATION_FLAG_PATH +
                                                  RegistryConstants.PATH_SEPARATOR + tenant.getId();
            UserRegistry superTenantRegistry = TenantMgtCoreServiceComponent.
                    getGovernanceSystemRegistry(MultitenantConstants.SUPER_TENANT_ID);
            Resource validationFlagR = superTenantRegistry.newResource();
            validationFlagR.setProperty(tenant.getDomain(), "true");
            superTenantRegistry.put(domainValidationPath, validationFlagR);

        } catch (RegistryException e) {
            String msg = "Error in storing the domain validation flag to the registry";
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }
        if (log.isDebugEnabled()) {
            log.debug("Domain Validation Flag is stored to the registry.");
        }
    }

    /**
     * Adds a tenant to the tenant manager
     * 
     * @param tenant - the tenant
     * @return tenantId - the tenant id
     * @throws Exception - UserStoreException
     */
    private int addTenant(Tenant tenant) throws TenantManagementServerException {
        int tenantId;
        TenantManager tenantManager = TenantMgtCoreServiceComponent.getTenantManager();
        try {
            tenantId = tenantManager.addTenant(tenant);
            if (log.isDebugEnabled()) {
                log.debug("Tenant is successfully added: " + tenant.getDomain());
            }
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            String msg = "Error in adding tenant with domain: " + tenant.getDomain();
            log.error(msg, e);
            throw new TenantManagementServerException(msg, e);
        }
        return tenantId;
    }

    /**
     * Add tenant admin user. When get the User Realm from Realm Service it create the admin user
     * 
     * @param tenant - the tenant
     * @throws Exception - UserStoreException
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

    /**
     * Authorize the role
     *
     * @param tenantId - tenant id
     * @throws Exception - UserStoreException
     */
    protected void copyUIPermissions(int tenantId) throws Exception {
        try {
            UserRealm realm = (UserRealm) TenantMgtCoreServiceComponent.
                    getRealmService().getTenantUserRealm(tenantId);
            String adminRole = realm.getRealmConfiguration().getAdminRoleName();
            AuthorizationManager authMan = realm.getAuthorizationManager();
            // Authorize the admin role, if not authorized yet.
            if (!authMan.isRoleAuthorized(adminRole,
                                          CarbonConstants.UI_ADMIN_PERMISSION_COLLECTION,
                                          UserMgtConstants.EXECUTE_ACTION)) {
                authMan.authorizeRole(adminRole, CarbonConstants.UI_ADMIN_PERMISSION_COLLECTION,
                                      UserMgtConstants.EXECUTE_ACTION);
            }
        } catch (UserStoreException e) {
            String msg = "Error in authorizing the admin role.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
        if (log.isDebugEnabled()) {
            log.debug("Role has successfully been authorized.");
        }
    }
    
    protected void setActivationFlags(int tenantId, String originalService) throws Exception {

        boolean useDefaultConfig = true;
        try {
            
            if(cloudServicesDesc == null ) { 
                cloudServicesDesc = CloudServiceConfigParser.loadCloudServicesConfiguration();
            }

            if (originalService != null &&
                !originalService.equals(StratosConstants.CLOUD_MANAGER_SERVICE) ) {
                CloudServicesUtil.activateOriginalAndCompulsoryServices(cloudServicesDesc,
                                                                        originalService, tenantId);
                useDefaultConfig = false;
            }

            if (useDefaultConfig) {
                CloudServicesUtil.activateAllServices(cloudServicesDesc, tenantId);
            }
        } catch (Exception e) {
            log.error("Error registering the originated service", e);
            throw e;
        }
        
    }

    /**
     * Validates that the chosen AdminUserName is valid.
     *
     * @param tenant Tenant information
     * @throws TenantMgtException if admin username validation fails.
     */
    private void validateAdminUserName(Tenant tenant) throws TenantMgtException {

        UserRealm superTenantUserRealm;
        try {
            superTenantUserRealm = TenantMgtCoreServiceComponent.getRealmService().getBootstrapRealm();
        } catch (UserStoreException e) {
            String msg = "Error while getting bootstrapRealm";
            throw new TenantManagementServerException(msg, e);
        }

        RealmConfiguration realmConfig = TenantMgtCoreServiceComponent.getBootstrapRealmConfiguration();
        String uniqueAcrossTenants = realmConfig.getUserStoreProperty(
                UserCoreConstants.RealmConfig.PROPERTY_USERNAME_UNIQUE);
        if ("true".equals(uniqueAcrossTenants)) {
            try {
                String adminName = tenant.getAdminName();
                if (superTenantUserRealm.getUserStoreManager().isExistingUser(adminName)) {
                    throw new TenantManagementClientException(ERROR_CODE_EXISTING_USER_NAME.getCode(), String.format
                            (ERROR_CODE_EXISTING_USER_NAME.getMessage(), adminName));
                }
            } catch (UserStoreException e) {
                String msg = "Error in checking whether the user already exists in the system";
                throw new TenantManagementServerException(msg, e);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Admin User Name has been validated.");
        }
    }

    /**
     * Persists the given tenant
     * @param tenant - tenant to be persisted
     * @return tenant Id
     * @throws Exception, if persisting tenant failed.
     */
    public int persistTenant(Tenant tenant) throws Exception {
        String tenantDomain = tenant.getDomain();
        int tenantId;
        validateAdminUserName(tenant);
        boolean isDomainAvailable = CommonUtil.isDomainNameAvailable(tenantDomain);
        if (!isDomainAvailable) {
            throw new Exception("Domain is not available to register");
        }

        tenantId = addTenant(tenant);
        tenant.setId(tenantId);

        try {
            doPostTenantCreationActions(tenant, null);
        } catch (Exception e) {
            String msg = "Error performing post tenant creation actions";
            if(log.isDebugEnabled()) {
                log.debug(msg, e);
            }
            throw new Exception(msg);
        }
        return tenantId;
    }
}
