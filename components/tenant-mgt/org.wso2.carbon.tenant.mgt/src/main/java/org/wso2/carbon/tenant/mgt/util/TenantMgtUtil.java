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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.caching.impl.CacheManagerFactoryImpl;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.registry.indexing.indexer.IndexerException;
import org.wso2.carbon.registry.indexing.solr.SolrClient;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.stratos.common.util.ClaimsMgtUtil;
import org.wso2.carbon.stratos.common.util.CommonUtil;
import org.wso2.carbon.tenant.mgt.core.internal.TenantMgtCoreServiceComponent;
import org.wso2.carbon.tenant.mgt.exception.TenantManagementException;
import org.wso2.carbon.tenant.mgt.internal.TenantMgtServiceComponent;
import org.wso2.carbon.tenant.mgt.message.TenantDeleteClusterMessage;
import org.wso2.carbon.tenant.mgt.message.TenantUnloadClusterMessage;
import org.wso2.carbon.user.api.*;
import org.wso2.carbon.user.core.*;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.config.multitenancy.MultiTenantRealmConfigBuilder;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.cache.Caching;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods for tenant management.
 */
public class TenantMgtUtil {

    private static final Log log = LogFactory.getLog(TenantMgtUtil.class);
    private static final String ILLEGAL_CHARACTERS_FOR_TENANT_DOMAIN = ".*[^a-zA-Z0-9\\._\\-].*";

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
     * Validate the tenant domain
     *
     * @param domainName tenant domain
     * @throws Exception , if invalid tenant domain name is given
     */
    public static void validateDomain(String domainName) throws Exception {
        if (domainName == null || domainName.equals("")) {
            String msg = "Provided domain name is empty.";
            log.error(msg);
            throw new Exception(msg);
        }
        // ensures the .ext for the public clouds.
        if (CommonUtil.isPublicCloudSetup()) {
            int lastIndexOfDot = domainName.lastIndexOf(".");
            if (lastIndexOfDot <= 0) {
                String msg = "You should have an extension to your domain.";
                log.error(msg);
                throw new Exception(msg);
            }
        }
        int indexOfDot = domainName.indexOf(".");
        if (indexOfDot == 0) {
            // can't start a domain starting with ".";
            String msg = "Invalid domain, starting with '.'";
            log.error(msg);
            throw new Exception(msg);
        }
        // check the tenant domain contains any illegal characters
        if (domainName.matches(ILLEGAL_CHARACTERS_FOR_TENANT_DOMAIN)) {
            String msg = "The tenant domain ' " + domainName +
                         " ' contains one or more illegal characters. the valid characters are " +
                         "letters, numbers, '.', '-' and '_'";
            log.error(msg);
            throw new Exception(msg);
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
            bean.setAdmin(ClaimsMgtUtil.getAdminUserNameFromTenantId(
                    TenantMgtServiceComponent.getRealmService(), tenantId));
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

            claimsMap.put(UserCoreConstants.ClaimTypeURIs.GIVEN_NAME, tenant.getAdminFirstName());
            claimsMap.put(UserCoreConstants.ClaimTypeURIs.SURNAME, tenant.getAdminLastName());
            claimsMap.put(UserCoreConstants.ClaimTypeURIs.EMAIL_ADDRESS, tenant.getEmail());

            // can be extended to store other user information.
            UserStoreManager userStoreManager =
                    (UserStoreManager) TenantMgtServiceComponent.getRealmService().
                            getTenantUserRealm(tenant.getId()).getUserStoreManager();
            userStoreManager.setUserClaimValues(tenant.getAdminName(), claimsMap,
                                                UserCoreConstants.DEFAULT_PROFILE);

        } catch (Exception e) {
            String msg = "Error in adding claims to the user.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
    }

    /**
     * Activate a tenant during the time of the tenant creation.
     *
     * @param tenantInfoBean tenant information
     * @param tenantId tenant Id
     * @throws Exception UserStoreException.
     */
    public static void activateTenantInitially(TenantInfoBean tenantInfoBean,
                                               int tenantId) throws Exception {
        TenantManager tenantManager = TenantMgtServiceComponent.getTenantManager();
        String tenantDomain = tenantInfoBean.getTenantDomain();

        TenantMgtUtil.activateTenant(tenantDomain, tenantManager, tenantId);
        if (log.isDebugEnabled()) {
            log.debug("Activated the tenant " + tenantDomain + " at the time of tenant creation");
        }

        //Notify tenant activation
        try {
            TenantMgtUtil.triggerTenantInitialActivation(tenantInfoBean);
        } catch (StratosException e) {
            String msg = "Error in notifying tenant initial activation.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
    }

    /**
     * Activate the given tenant, either at the time of tenant creation, or later by super admin.
     *
     * @param tenantDomain tenant domain
     * @param tenantManager TenantManager object
     * @param tenantId tenant Id
     * @throws Exception UserStoreException.
     */
    public static void activateTenant(String tenantDomain, TenantManager tenantManager,
                                      int tenantId) throws Exception {
        try {
            tenantManager.activateTenant(tenantId);
        } catch (UserStoreException e) {
            String msg = "Error in activating the tenant for tenant domain: " + tenantDomain + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
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
     * @throws Exception TenantManagementException.
     */
    public static void deactivateTenant(String tenantDomain, TenantManager tenantManager, int tenantId)
            throws TenantManagementException {
        try {
            tenantManager.deactivateTenant(tenantId);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            String msg = "Error in deactivating tenant for tenant domain: " + tenantDomain + ".";
            log.error(msg, e);
            throw new TenantManagementException(msg, e);
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
     * Broadcast TenantDeleteClusterMessage to all worker nodes
     *
     * @param tenantId deleting tenant's tenant id
     * @throws Exception
     */
    public static void notifyTenantDeletionInCluster(int tenantId) throws TenantManagementException {
        TenantDeleteClusterMessage clusterMessage = new TenantDeleteClusterMessage(
                tenantId);
        ConfigurationContext configContext = TenantMgtServiceComponent.getConfigurationContext();
        ClusteringAgent agent = configContext.getAxisConfiguration()
                .getClusteringAgent();
        try {
            if (agent != null) {
                agent.sendMessage(clusterMessage, true);
            }
        } catch (ClusteringFault e) {
            log.error("Error occurred while broadcasting TenantDeleteClusterMessage : " + e.getMessage());
            throw new TenantManagementException(e.getMessage(),e);
        }

    }

    /**
     * Broadcast TenantUnloadClusterMessage to all worker nodes
     *
     * @param tenantId unloading tenant's tenant id
     * @throws Exception
     */
    public static void notifyTenantUnloadInCluster(int tenantId) throws TenantManagementException {
        TenantUnloadClusterMessage clusterMessage = new TenantUnloadClusterMessage(
                tenantId);
        ConfigurationContext configContext = TenantMgtServiceComponent.getConfigurationContext();
        ClusteringAgent agent = configContext.getAxisConfiguration()
                .getClusteringAgent();
        try {
            if (agent != null) {
                agent.sendMessage(clusterMessage, true);
            }
        } catch (ClusteringFault e) {
            log.error("Error occurred while broadcasting TenantUnloadClusterMessage : " + e.getMessage());
            throw new TenantManagementException(e.getMessage(),e);
        }

    }

    /**
     * Delete the tenant related activities which carried in the tenant addition
     *
     * @param tenantManager
     * @param tenantId tenant id of the removing tenant
     */
    public static void removeAllTenantRelatedData(TenantManager tenantManager, int tenantId)
            throws TenantManagementException {
        try {
            Tenant tenant = (Tenant) tenantManager.getTenant(tenantId);
            tenantManager.deleteTenant(tenantId);

            deleteTenantRegistryData(tenant.getId());
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new TenantManagementException(e.getMessage(), e);
        } catch (RegistryException e) {
            throw new TenantManagementException(e.getMessage(), e);
        }
    }

    /**
     * This method will delete all the tenant registry data
     *
     * @param tenantId deleting tenant id
     * @throws RegistryException
     */
    private static void deleteTenantRegistryData(int tenantId) throws RegistryException {
        TenantMgtCoreServiceComponent.getRegistryLoader().unloadTenantRegistry(tenantId);
        try {
            //Deleting the solr index of all the resources
            //This will do a wild card search for resources having tenant id and delete the resource
            String query = "id:*tenantId" + tenantId;
            SolrClient.getInstance().deleteIndexByQuery(query);
        } catch (IndexerException e) {
            throw new RegistryException(e.getMessage(), e);
        }
    }

    /**
     * Remove global tenant cache
     *
     * @param tenantDomain - Tenant domain name
     * @throws Exception
     */
    public static void removeGlobalTenantCache(String tenantDomain) throws Exception {
        ((CacheManagerFactoryImpl) Caching.getCacheManagerFactory()).removeCacheManagerMap(tenantDomain);
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
}
