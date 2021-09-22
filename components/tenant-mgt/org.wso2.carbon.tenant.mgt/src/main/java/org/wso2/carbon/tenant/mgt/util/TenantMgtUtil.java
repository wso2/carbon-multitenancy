/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.tenant.mgt.util;

import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.multitenancy.utils.TenantAxisUtils;
import org.wso2.carbon.identity.claim.metadata.mgt.exception.ClaimMetadataException;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.dataaccess.JDBCDataAccessManager;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.stratos.common.exception.TenantManagementClientException;
import org.wso2.carbon.stratos.common.exception.TenantManagementServerException;
import org.wso2.carbon.stratos.common.exception.TenantMgtException;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.stratos.common.util.ClaimsMgtUtil;
import org.wso2.carbon.stratos.common.util.CommonUtil;
import org.wso2.carbon.tenant.mgt.internal.TenantMgtServiceComponent;
import org.wso2.carbon.tenant.mgt.message.TenantDeleteClusterMessage;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.TenantMgtConfiguration;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.config.multitenancy.MultiTenantRealmConfigBuilder;
import org.wso2.carbon.user.core.jdbc.JDBCRealmConstants;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import static org.wso2.carbon.stratos.common.constants.TenantConstants.ErrorMessage.ERROR_CODE_EMPTY_DOMAIN_NAME;
import static org.wso2.carbon.stratos.common.constants.TenantConstants.ErrorMessage.ERROR_CODE_EMPTY_EXTENSION;
import static org.wso2.carbon.stratos.common.constants.TenantConstants.ErrorMessage.ERROR_CODE_ILLEGAL_CHARACTERS_IN_DOMAIN;
import static org.wso2.carbon.stratos.common.constants.TenantConstants.ErrorMessage.ERROR_CODE_INVALID_DOMAIN;
import static org.wso2.carbon.stratos.common.constants.TenantConstants.ErrorMessage.ERROR_CODE_TENANT_DOES_NOT_MATCH_REGEX_PATTERN;

/**
 * Utility methods for tenant management.
 */
public class TenantMgtUtil {

    private static final Log log = LogFactory.getLog(TenantMgtUtil.class);
    private static final String ILLEGAL_CHARACTERS_FOR_TENANT_DOMAIN = ".*[^a-z0-9\\._\\-].*";
    private static final String DOT = ".";
    private static ThreadLocal<Boolean> isTenantAdminCreationOperation = new ThreadLocal<>();
    private static final int DEFAULT_ITEMS_PER_PAGE = 15;
    private static final int DEFAULT_MAXIMUM_ITEMS_PER_PAGE = 100;

    /**
     * Prepares string to show theme management page.
     *
     * @param tenantId tenant id
     * @return UUID uniques id to refer the tenant.
     * @throws TenantMgtException if failed.
     */
    public static void prepareStringToShowThemeMgtPage(int tenantId, String resourceId) throws
            TenantMgtException {

        try {
            UserRegistry systemRegistry =
                    TenantMgtServiceComponent.getRegistryService().getGovernanceSystemRegistry();
            // store it in the registry.
            Resource resource = systemRegistry.newResource();
            String tenantIdStr = Integer.toString(tenantId);
            resource.setProperty(MultitenantConstants.TENANT_ID, tenantIdStr);
            String uuidPath = StratosConstants.TENANT_CREATION_THEME_PAGE_TOKEN
                    + RegistryConstants.PATH_SEPARATOR + resourceId;
            systemRegistry.put(uuidPath, resource);

            // restrict access
            CommonUtil.denyAnonAuthorization(uuidPath, systemRegistry.getUserRealm());
        } catch (RegistryException e) {
            throw new TenantManagementServerException("Error while creating the path for theme page.", e);
        }
    }

    /**
     * Prepares string to show theme management page.
     *
     * @param tenantId - tenant id
     * @return UUID
     * @throws RegistryException, if failed.
     */
    public static String prepareStringToShowThemeMgtPage(int tenantId) throws RegistryException {
        // first we generate a UUID
        UserRegistry systemRegistry =
                TenantMgtServiceComponent.getRegistryService().getGovernanceSystemRegistry();
        String uuid = UUIDGenerator.generateUUID();
        // store it in the registry.
        Resource resource = systemRegistry.newResource();
        String tenantIdStr = Integer.toString(tenantId);
        resource.setProperty(MultitenantConstants.TENANT_ID, tenantIdStr);
        String uuidPath = StratosConstants.TENANT_CREATION_THEME_PAGE_TOKEN
                          + RegistryConstants.PATH_SEPARATOR + uuid;
        systemRegistry.put(uuidPath, resource);

        // restrict access
        CommonUtil.denyAnonAuthorization(uuidPath, systemRegistry.getUserRealm());
        return uuid;
    }

    /**
     * Triggers adding the tenant for TenantMgtListener
     *
     * @param tenantInfo tenant
     * @throws StratosException, trigger failed
     */
    public static void triggerAddTenant(TenantInfoBean tenantInfo) throws StratosException {
        // initializeRegistry(tenantInfoBean.getTenantId());
        for (TenantMgtListener tenantMgtListener :
                TenantMgtServiceComponent.getTenantMgtListeners()) {
            tenantMgtListener.onTenantCreate(tenantInfo);
        }
    }

    /**
     * Triggers pre tenant add listeners.
     *
     * @param tenantInfo tenant
     * @throws StratosException
     */
    public static void triggerPreAddTenant(TenantInfoBean tenantInfo) throws StratosException {

        for (TenantMgtListener tenantMgtListener : TenantMgtServiceComponent.getTenantMgtListeners()) {
            tenantMgtListener.onPreTenantCreate(tenantInfo);
        }
    }

    /**
     * Triggers pre tenant delete for TenantMgtListener
     *
     * @param tenantId int
     * @throws StratosException , trigger failed
     */
    public static void triggerPreTenantDelete(int tenantId)
            throws StratosException {
        for (TenantMgtListener tenantMgtListener : TenantMgtServiceComponent
                .getTenantMgtListeners()) {
            log.debug("Executing OnPreDelete on Listener Impl Class Name : "
                      + tenantMgtListener.getClass().getName());
            tenantMgtListener.onPreDelete(tenantId);
        }
    }

    /**
     * Triggers an update for the tenant for TenantMgtListener
     *
     * @param tenantInfoBean tenantInfoBean
     * @throws org.wso2.carbon.stratos.common.exception.StratosException, if update failed
     */
    public static void triggerUpdateTenant(
            TenantInfoBean tenantInfoBean) throws StratosException {
        for (TenantMgtListener tenantMgtListener :
                TenantMgtServiceComponent.getTenantMgtListeners()) {
            tenantMgtListener.onTenantUpdate(tenantInfoBean);
        }
    }
    
    public static void triggerTenantInitialActivation(
                                  TenantInfoBean tenantInfoBean) throws StratosException {
        for (TenantMgtListener tenantMgtListener :
                TenantMgtServiceComponent.getTenantMgtListeners()) {
            tenantMgtListener.onTenantInitialActivation(tenantInfoBean.getTenantId());
        }
    }
    
    public static void triggerTenantActivation(int tenantId) throws StratosException {
        for (TenantMgtListener tenantMgtListener : 
                TenantMgtServiceComponent.getTenantMgtListeners()) {
            tenantMgtListener.onTenantActivation(tenantId);
        }
    }
    
    public static void triggerTenantDeactivation(int tenantId) throws StratosException {
        for (TenantMgtListener tenantMgtListener :
                TenantMgtServiceComponent.getTenantMgtListeners()) {
            tenantMgtListener.onTenantDeactivation(tenantId);
        }
    }

    /**
     * Validate the tenant domain.
     *
     * @param domainName tenant domain name.
     * @throws Exception if invalid tenant domain name is given.
     */
    public static void validateDomain(String domainName) throws Exception {

        if (StringUtils.isBlank(domainName)) {
            throw new TenantManagementClientException(ERROR_CODE_EMPTY_DOMAIN_NAME);
        }

        if (CommonUtil.isPublicCloudSetup()) {
            int lastIndexOfDot = domainName.lastIndexOf(DOT);
            if (lastIndexOfDot <= 0) {
                throw new TenantManagementClientException(ERROR_CODE_EMPTY_EXTENSION);
            }
        }
        // Regex validation will be skipped if it is not configured.
        String regex = CommonUtil.getTenantDomainRegexPattern();
        if (StringUtils.isNotBlank(regex)) {
            if (!isFormatCorrect(regex, domainName)) {
                throw new TenantManagementClientException(ERROR_CODE_TENANT_DOES_NOT_MATCH_REGEX_PATTERN.getCode(),
                        String.format(ERROR_CODE_TENANT_DOES_NOT_MATCH_REGEX_PATTERN.getMessage(), domainName, regex));
            }
            return;
        }
        int indexOfDot = domainName.indexOf(DOT);
        if (indexOfDot == 0) {
            // Can't start a domain with ".".
            throw new TenantManagementClientException(ERROR_CODE_INVALID_DOMAIN);
        }
        // Check tenant domain contains any illegal characters.
        if (domainName.matches(ILLEGAL_CHARACTERS_FOR_TENANT_DOMAIN)) {
            throw new TenantManagementClientException(ERROR_CODE_ILLEGAL_CHARACTERS_IN_DOMAIN.getCode(),
                    String.format(ERROR_CODE_ILLEGAL_CHARACTERS_IN_DOMAIN.getMessage(), domainName));
        }
    }

    /**
     * gets the UserStoreManager for a tenant
     *
     * @param tenant   - a tenant
     * @param tenantId - tenant Id. To avoid the sequences where tenant.getId() may
     *                 produce the super tenant's tenant Id.
     * @return UserStoreManager
     * @throws Exception UserStoreException
     */
    public static UserStoreManager getUserStoreManager(Tenant tenant, int tenantId)
            throws Exception {
        // get the system registry for the tenant
        RealmConfiguration realmConfig = TenantMgtServiceComponent.getBootstrapRealmConfiguration();
        TenantMgtConfiguration tenantMgtConfiguration =
                TenantMgtServiceComponent.getRealmService().getTenantMgtConfiguration();
        UserRealm userRealm;
        try {
            MultiTenantRealmConfigBuilder builder = TenantMgtServiceComponent.getRealmService().
                    getMultiTenantRealmConfigBuilder();
            RealmConfiguration realmConfigToPersist = builder.
                    getRealmConfigForTenantToPersist(realmConfig, tenantMgtConfiguration,
                                                             tenant, tenantId);
            RealmConfiguration realmConfigToCreate =
                    builder.getRealmConfigForTenantToCreateRealmOnTenantCreation(
                            realmConfig, realmConfigToPersist, tenantId);
            userRealm = TenantMgtServiceComponent.getRealmService().
                    getUserRealm(realmConfigToCreate);
        } catch (UserStoreException e) {
            String msg = "Error in creating Realm for tenant, tenant domain: " + tenant.getDomain();
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        UserStoreManager userStoreManager;
        try {
            userStoreManager = userRealm.getUserStoreManager();

            return userStoreManager;
        } catch (UserStoreException e) {
            String msg = "Error in getting the userstore/authorization manager for tenant: " +
                         tenant.getDomain();
            log.error(msg);
            throw new Exception(msg, e);
        }
    }

    /**
     * initializes tenant from the user input (tenant info bean)
     *
     * @param tenantInfoBean input
     * @return tenant
     */
    public static Tenant initializeTenant(TenantInfoBean tenantInfoBean) {
        Tenant tenant = new Tenant();
        tenant.setDomain(tenantInfoBean.getTenantDomain());
        tenant.setEmail(tenantInfoBean.getEmail());
        tenant.setAdminName(tenantInfoBean.getAdmin());

        // set tenantId given in tenantInfoBean, if it is set,
        // underline tenant manager will try to create the tenant with given tenant Id.
        tenant.setId(tenantInfoBean.getTenantId());

        // we are duplicating the params stored in the claims here as well; they
        // are in Tenant class
        // to make it work with LDAP; but they do not make it to the databases.
        tenant.setAdminFirstName(tenantInfoBean.getFirstname());
        tenant.setAdminLastName(tenantInfoBean.getLastname());

        tenant.setAdminPassword(tenantInfoBean.getAdminPassword());

        // sets created date.
        Calendar createdDateCal = tenantInfoBean.getCreatedDate();
        long createdDate;
        if (createdDateCal != null) {
            createdDate = createdDateCal.getTimeInMillis();
        } else {
            createdDate = System.currentTimeMillis();
        }
        tenant.setCreatedDate(new Date(createdDate));

        if (log.isDebugEnabled()) {
            log.debug("Tenant object Initialized from the TenantInfoBean");
        }
        return tenant;
    }

    /**
     * @param tenantDomain domain name of the tenant.
     * @throws Exception if there is an exception during the tenant deletion.
     */
    public static void deleteTenant(String tenantDomain) throws Exception {

        TenantManager tenantManager = TenantMgtServiceComponent.getTenantManager();
        if (tenantManager != null) {
            int tenantId = tenantManager.getTenantId(tenantDomain);
            ServerConfigurationService serverConfigurationService =
                    TenantMgtServiceComponent.getServerConfigurationService();
            /*
             * TODO: 2/7/19 We need to fix listeners to enable this by default
             */
            if (Boolean.parseBoolean(
                    serverConfigurationService.getFirstProperty("Tenant.ListenerInvocationPolicy.InvokeOnDelete"))) {
                triggerPreTenantDelete(tenantId);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Tenant.ListenerInvocationPolicy.InvokeOnDelete flag is not set to true in " +
                            "carbon.xml. Listener invocation ignored.");
                }
            }
            Tenant tenant = (Tenant) tenantManager.getTenant(tenantId);
            String adminUserUuid = getTenantAdminUuid(tenant);
            String tenantUuid = tenant.getTenantUniqueID();
            TenantMgtUtil.deleteWorkernodesTenant(tenantId);
            if (TenantMgtServiceComponent.getBillingService() != null) {
                TenantMgtServiceComponent.getBillingService().deleteBillingData(tenantId);
            }
            removeAllClaims(tenantId);
            TenantMgtUtil.unloadTenantConfigurations(tenantDomain, tenantId);
            TenantMgtUtil.deleteTenantRegistryData(tenantId);
            TenantMgtUtil.deleteTenantDir(tenantId);
            tenantManager.deleteTenant(tenantId);
            log.info(String.format("Deleted tenant with domain: %s and tenant id: %d from the system.", tenantDomain,
                    tenantId));
            triggerPostTenantDelete(tenantId, tenantUuid, adminUserUuid);
        }
    }

    private static void removeAllClaims(int tenantId) throws TenantManagementServerException {

        try {
            TenantMgtServiceComponent.getClaimMetadataManagementService().removeAllClaims(tenantId);
        } catch (ClaimMetadataException e) {
            throw new TenantManagementServerException(String.format(
                    "Error occurred while deleting claims for tenant: %s.", tenantId), e);
        }
    }

    private static String getTenantAdminUuid(Tenant tenant) throws TenantManagementServerException {

        String adminUserUuid = tenant.getAdminUserId();
        if (StringUtils.isNotBlank(adminUserUuid)) {
            return adminUserUuid;
        }
        // If the admin user uuid is not in the tenant object, we need to get it from user store.
        String adminUsername = tenant.getAdminName();
        try {
            UserStoreManager userStoreManager = (UserStoreManager) TenantMgtServiceComponent.getRealmService().
                    getTenantUserRealm(tenant.getId()).getUserStoreManager();
            adminUserUuid = ((AbstractUserStoreManager) userStoreManager).getUserIDFromUserName(adminUsername);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new TenantManagementServerException(String.format(
                    "Error occurred while getting user store manager for tenantId: %s.", tenant.getId()), e);
        }
        if (StringUtils.isBlank(adminUserUuid)) {
            throw new TenantManagementServerException(String.format(
                    "No UUID found for the admin user of tenant: %s.", tenant.getId()));
        }
        return adminUserUuid;
    }

    /**
     * Initializes a tenantInfoBean object for a given tenant.
     *
     * @param tenantId tenant id.
     * @param tenant   a tenant.
     * @return tenantInfoBean
     * @throws Exception , exception in getting the adminUserName from tenantId
     */
    public static TenantInfoBean initializeTenantInfoBean(
            int tenantId, Tenant tenant) throws Exception {

        TenantInfoBean bean = getTenantInfoBeanfromTenant(tenantId, tenant);
        if (tenant != null) {
            try {
                bean.setAdmin(ClaimsMgtUtil.getAdminUserNameFromTenantId(
                        TenantMgtServiceComponent.getRealmService(), tenantId));
            } catch (Exception e) {
                throw new TenantManagementServerException("Error while getting admin username from tenant id.", e);
            }
        }
        return bean;
    }

    /**
     * initializes a TenantInfoBean object from the tenant
     * @param tenantId, tenant id
     * @param tenant, tenant
     * @return TenantInfoBean.
     */
    public static TenantInfoBean getTenantInfoBeanfromTenant(int tenantId, Tenant tenant) {
        TenantInfoBean bean = new TenantInfoBean();
        if (tenant != null) {
            bean.setTenantId(tenantId);
            bean.setTenantDomain(tenant.getDomain());
            bean.setEmail(tenant.getEmail());

            /*gets the created date*/
            Calendar createdDate = Calendar.getInstance();
            createdDate.setTimeInMillis(tenant.getCreatedDate().getTime());
            bean.setCreatedDate(createdDate);

            bean.setActive(tenant.isActive());
            if(log.isDebugEnabled()) {
                log.debug("The TenantInfoBean object has been created from the tenant.");
            }
        } else {
            if(log.isDebugEnabled()) {
                log.debug("The tenant is null.");
            }
        }
        return bean;
    }

    /**
     * Adds claims to UserStoreManager
     *
     * @param tenant a tenant
     * @throws Exception if error in adding claims to the user.
     */
    public static void addClaimsToUserStoreManager(Tenant tenant) throws Exception {

        try {
            Map<String, String> claimsMap = new HashMap<String, String>();

            if (StringUtils.isNotEmpty(tenant.getAdminFirstName())) {
                claimsMap.put(UserCoreConstants.ClaimTypeURIs.GIVEN_NAME, tenant.getAdminFirstName());
            }
            if (StringUtils.isNotEmpty(tenant.getAdminLastName())) {
                claimsMap.put(UserCoreConstants.ClaimTypeURIs.SURNAME, tenant.getAdminLastName());
            }
            if (StringUtils.isNotEmpty(tenant.getEmail())) {
                claimsMap.put(UserCoreConstants.ClaimTypeURIs.EMAIL_ADDRESS, tenant.getEmail());
            }

            // Can be extended to store other user information.
            UserStoreManager userStoreManager =
                    (UserStoreManager) TenantMgtServiceComponent.getRealmService().
                            getTenantUserRealm(tenant.getId()).getUserStoreManager();
            if (!userStoreManager.isReadOnly()) {
                userStoreManager.setUserClaimValues(tenant.getAdminName(), claimsMap,
                        UserCoreConstants.DEFAULT_PROFILE);
            }

        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            String msg = "Error in adding claims to the user.";
            throw new TenantManagementServerException(msg, e);
        }
    }

    public static void addAdditionalClaimsToUserStoreManager(Tenant tenant) throws Exception {

        try {
            Map<String, String> claimsMap = new HashMap<String, String>();

            if (tenant.getClaimsMap() != null) {
                for (Map.Entry<String, String> entry : tenant.getClaimsMap().entrySet()) {
                    claimsMap.put(entry.getKey(), entry.getValue());
                }
            }

            // Can be extended to store other user information.
            UserStoreManager userStoreManager =
                    (UserStoreManager) TenantMgtServiceComponent.getRealmService().
                            getTenantUserRealm(tenant.getId()).getUserStoreManager();
            if (!userStoreManager.isReadOnly()) {
                userStoreManager.setUserClaimValues(tenant.getAdminName(), claimsMap,
                        UserCoreConstants.DEFAULT_PROFILE);
            }
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            String msg = "Error in adding claims to the user.";
            throw new TenantManagementServerException(msg, e);
        }
    }

    /**
     * Activate a tenant during the time of the tenant creation.
     *
     * @param tenantInfoBean tenant information
     * @param tenantId       tenant Id
     * @throws Exception UserStoreException.
     */
    public static void activateTenantInitially(TenantInfoBean tenantInfoBean,
                                               int tenantId) throws Exception {

        TenantManager tenantManager = TenantMgtServiceComponent.getTenantManager();
        String tenantDomain = tenantInfoBean.getTenantDomain();

        try {
            TenantMgtUtil.activateTenant(tenantDomain, tenantManager, tenantId);
            if (log.isDebugEnabled()) {
                log.debug("Activated the tenant " + tenantDomain + " at the time of tenant creation");
            }
            // Notify tenant activation
            TenantMgtUtil.triggerTenantInitialActivation(tenantInfoBean);
        } catch (StratosException e) {
            String msg = "Error in notifying tenant initial activation.";
            throw new TenantManagementServerException(msg, e);
        } catch (Exception e) {
            String msg = "Error while activating the tenant.";
            throw new TenantManagementServerException(msg, e);
        }
    }

    /**
     * Activate the given tenant, either at the time of tenant creation, or later by super admin.
     *
     * @param tenantDomain tenant domain
     * @param tenantManager TenantManager object
     * @param tenantId tenant Id
     * @throws Exception if tenant activation fails.
     */
    public static void activateTenant(String tenantDomain, TenantManager tenantManager,
                                      int tenantId) throws Exception {
        try {
            tenantManager.activateTenant(tenantId);
        } catch (UserStoreException e) {
            throw new TenantManagementServerException("Error in activating the tenant for tenant domain: " +
                    tenantDomain + DOT, e);
        }

        //activating the subscription
        /*try {
            if (TenantMgtServiceComponent.getBillingService() != null) {
                TenantMgtServiceComponent.getBillingService().activateUsagePlan(tenantDomain);
            }
        } catch (Exception e) {
            String msg = "Error while activating subscription for domain: " + tenantDomain + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }*/
    }

    /**
     * Deactivate the given tenant, by super admin.
     *
     * @param tenantDomain tenant domain
     * @param tenantManager TenantManager object
     * @param tenantId tenant Id
     * @throws Exception if tenant deactivation fails.
     */
    public static void deactivateTenant(String tenantDomain, TenantManager tenantManager,
                                        int tenantId) throws Exception {
        try {
            tenantManager.deactivateTenant(tenantId);
            unloadTenantConfigurations(tenantDomain, tenantId);
        } catch (UserStoreException e) {
            String msg = "Error in deactivating tenant for tenant domain: " + tenantDomain + DOT;
            log.error(msg, e);
            throw new TenantManagementServerException(msg, e);
        }

        //deactivating the subscription
        /*try {
            if (TenantMgtServiceComponent.getBillingService() != null) {
                TenantMgtServiceComponent.getBillingService().deactivateActiveUsagePlan(tenantDomain);
            }
        } catch (Exception e) {
            String msg = "Error while deactivating subscription for domain: " + tenantDomain + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }*/
    }

    /**
     * Unloading the deactivated tenant in order to avoid serving requests to the tenant.
     *
     * @param tenantDomain tenant domain
     * @param tenantId tenant Id
     * */
    public static void unloadTenantConfigurations(String tenantDomain, int tenantId) {

        Map<String, ConfigurationContext> tenantConfigContexts = TenantAxisUtils.getTenantConfigurationContexts(
                TenantMgtServiceComponent.getConfigurationContext());
        ConfigurationContext tenantConfigurationContext = tenantConfigContexts.get(tenantDomain);

        if (tenantConfigurationContext != null && tenantConfigurationContext.getAxisConfiguration() != null) {
            try {
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
                carbonContext.setTenantDomain(tenantDomain);
                carbonContext.setTenantId(tenantId);

                TenantAxisUtils.terminateTenantConfigContext(tenantConfigurationContext);
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
            tenantConfigContexts.remove(tenantDomain);
        }
    }

    public static void deleteTenantRegistryData(int tenantId) throws Exception {
        // delete data from mounted config registry database
        JDBCDataAccessManager configMgr = (JDBCDataAccessManager) TenantMgtServiceComponent.getRegistryService().
                getConfigUserRegistry().getRegistryContext().getDataAccessManager();
        TenantRegistryDataDeletionUtil.deleteTenantRegistryData(tenantId, configMgr.getDataSource().getConnection());

        // delete data from mounted governance registry database
        JDBCDataAccessManager govMgr = (JDBCDataAccessManager) TenantMgtServiceComponent.getRegistryService().
                getGovernanceUserRegistry().getRegistryContext().getDataAccessManager();
        TenantRegistryDataDeletionUtil.deleteTenantRegistryData(tenantId, govMgr.getDataSource().getConnection());

    }

    public static void deleteTenantUMData(int tenantId) throws Exception {
        RealmConfiguration realmConfig = TenantMgtServiceComponent.getRealmService().
                getBootstrapRealmConfiguration();
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(realmConfig.getRealmProperty(JDBCRealmConstants.DRIVER_NAME));
        dataSource.setUrl(realmConfig.getRealmProperty(JDBCRealmConstants.URL));
        dataSource.setUsername(realmConfig.getRealmProperty(JDBCRealmConstants.USER_NAME));
        dataSource.setPassword(realmConfig.getRealmProperty(JDBCRealmConstants.PASSWORD));
        dataSource.setMaxActive(Integer.parseInt(realmConfig.getRealmProperty(JDBCRealmConstants.MAX_ACTIVE)));
        dataSource.setMinIdle(Integer.parseInt(realmConfig.getRealmProperty(JDBCRealmConstants.MIN_IDLE)));
        dataSource.setMaxWait(Integer.parseInt(realmConfig.getRealmProperty(JDBCRealmConstants.MAX_WAIT)));

        TenantUMDataDeletionUtil.deleteTenantUMData(tenantId, dataSource.getConnection());
    }

    /**
     * Broadcast TenantDeleteClusterMessage to all worker nodes
     *
     * @param tenantId
     * @throws Exception
     */
    public static void deleteWorkernodesTenant(int tenantId) throws Exception {

        TenantDeleteClusterMessage clustermessage = new TenantDeleteClusterMessage(tenantId);
        ConfigurationContext configContext = TenantMgtServiceComponent.getConfigurationContext();

        if (configContext != null) {
            AxisConfiguration axisConfiguration = configContext.getAxisConfiguration();
            ClusteringAgent agent = axisConfiguration.getClusteringAgent();

            if (agent != null) {
                try {
                    agent.sendMessage(clustermessage, true);
                } catch (ClusteringFault e) {
                    log.error("Error occurred while broadcasting TenantDeleteClusterMessage : " + e.getMessage());
                }
            }
        }
    }


    /**
     * Delete tenant data specific to product from database.
     *
     * @param dataSourceName
     * @param tableName
     * @param tenantId
     */
    public static void deleteProductSpecificTenantData(String dataSourceName, String tableName, int tenantId) {
        try {
            TenantDataDeletionUtil.deleteProductSpecificTenantData(((DataSource) InitialContext.doLookup(dataSourceName)).
                    getConnection(), tableName, tenantId);
        } catch (Exception e) {
            throw new RuntimeException("Error in looking up data source: " + e.getMessage(), e);
        }
    }

    /**
     * Delete the tenant directory of a given tenant id
     *
     * @param tenantId Id of the tenant
     */
    public static void deleteTenantDir(int tenantId) {

        if (log.isDebugEnabled()) {
            log.debug("Deleting tenant directory of tenant: " + tenantId);
        }

        String tenantDirPath = CarbonUtils.getCarbonTenantsDirPath() + File.separator + tenantId;
        File tenantDir = new File(tenantDirPath);

        try {
            FileUtils.deleteDirectory(tenantDir);
        } catch (IOException e) {
            log.error("Error in deleting tenant directory: " + tenantDirPath, e);
        }
    }

    public static boolean isTenantAdminCreationOperation() {
        if (isTenantAdminCreationOperation == null) {
            //This is the default behaviour
            return false;
        }
        if (isTenantAdminCreationOperation.get() == null) {
            return false;
        }
        return isTenantAdminCreationOperation.get();
    }

    public static void setTenantAdminCreationOperation(boolean isAdminCreationOperation) {
        isTenantAdminCreationOperation.set(isAdminCreationOperation);
    }

    public static void clearTenantAdminCreationOperation() {
        isTenantAdminCreationOperation.remove();
    }

    /**
     * Get the Default Items per Page needed to display.
     *
     * @return defaultItemsPerPage need to display.
     */
    public static int getDefaultItemsPerPage() {

        int defaultItemsPerPage = DEFAULT_ITEMS_PER_PAGE;
        try {
            String defaultItemsPerPageProperty = ServerConfiguration.getInstance().getFirstProperty("ItemsPerPage");
            if (StringUtils.isNotBlank(defaultItemsPerPageProperty)) {
                int defaultItemsPerPageConfig = Integer.parseInt(defaultItemsPerPageProperty);
                if (defaultItemsPerPageConfig > 0) {
                    defaultItemsPerPage = defaultItemsPerPageConfig;
                }
            }
        } catch (NumberFormatException e) {
            defaultItemsPerPage = DEFAULT_ITEMS_PER_PAGE;
            log.warn("Error occurred while parsing the 'ItemsPerPage' property value in carbon.xml.", e);
        }
        return defaultItemsPerPage;
    }

    /**
     * Get the Maximum Item per Page need to display.
     *
     * @return maximumItemsPerPage need to display.
     */
    public static int getMaximumItemPerPage() {

        int maximumItemsPerPage = DEFAULT_MAXIMUM_ITEMS_PER_PAGE;
        String maximumItemsPerPagePropertyValue = ServerConfiguration.getInstance().getFirstProperty(
                "MaximumItemsPerPage");
        if (StringUtils.isNotBlank(maximumItemsPerPagePropertyValue)) {
            try {
                maximumItemsPerPage = Integer.parseInt(maximumItemsPerPagePropertyValue);
            } catch (NumberFormatException e) {
                maximumItemsPerPage = DEFAULT_MAXIMUM_ITEMS_PER_PAGE;
                log.warn("Error occurred while parsing the 'MaximumItemsPerPage' property value in carbon.xml.", e);
            }
        }
        return maximumItemsPerPage;
    }

    private static boolean isFormatCorrect(String regularExpression, String domainName) {

        Pattern p2 = Pattern.compile(regularExpression);
        Matcher m2 = p2.matcher(domainName);
        return m2.matches();
    }

    /**
     * Triggers post tenant delete for TenantMgtListener.
     *
     * @param tenantId      int Tenant id.
     * @param tenantUuid    String Tenant unique identifier.
     * @param adminUserUuid String Tenant admin user unique identifier.
     * @throws StratosException If trigger failed.
     */
    private static void triggerPostTenantDelete(int tenantId, String tenantUuid, String adminUserUuid)
            throws StratosException {

        for (TenantMgtListener tenantMgtListener : TenantMgtServiceComponent.getTenantMgtListeners()) {
            if (log.isDebugEnabled()) {
                log.debug("Executing OnPostDelete on Listener Impl Class Name: "
                        + tenantMgtListener.getClass().getName());
            }
            tenantMgtListener.onPostDelete(tenantId, tenantUuid, adminUserUuid);
        }
    }
}
