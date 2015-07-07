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
package org.wso2.carbon.tenant.mgt.services;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.core.multitenancy.utils.TenantAxisUtils;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.stratos.common.util.ClaimsMgtUtil;
import org.wso2.carbon.stratos.common.util.CommonUtil;
import org.wso2.carbon.tenant.mgt.beans.PaginatedTenantInfoBean;
import org.wso2.carbon.tenant.mgt.core.TenantPersistor;
import org.wso2.carbon.tenant.mgt.exception.TenantManagementException;
import org.wso2.carbon.tenant.mgt.internal.TenantMgtServiceComponent;
import org.wso2.carbon.tenant.mgt.util.TenantMgtUtil;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.DataPaginator;
import org.wso2.carbon.utils.FileManipulator;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This is the admin Web service which is used for managing tenants
 */
public class TenantMgtAdminService extends AbstractAdmin {
    private static final Log log = LogFactory.getLog(TenantMgtAdminService.class);

    /**
     * super admin adds a tenant
     *
     * @param tenantInfoBean tenant info bean
     * @return UUID
     * @throws Exception if error in adding new tenant.
     */
    public String addTenant(TenantInfoBean tenantInfoBean) throws Exception {
        try {
            CommonUtil.validateEmail(tenantInfoBean.getEmail());
        } catch (Exception e) {
            String msg = "Invalid email is provided.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
        String tenantDomain = tenantInfoBean.getTenantDomain();
        TenantMgtUtil.validateDomain(tenantDomain);
        isSuperTenant();
        Tenant tenant = TenantMgtUtil.initializeTenant(tenantInfoBean);
        TenantPersistor  persistor = new TenantPersistor();
        // not validating the domain ownership, since created by super tenant
        int tenantId = persistor.persistTenant(tenant, false, tenantInfoBean.getSuccessKey(),
                                tenantInfoBean.getOriginatedService(),false);
        tenantInfoBean.setTenantId(tenantId);

        PrivilegedCarbonContext.startTenantFlow();

        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        carbonContext.setTenantDomain(tenantDomain);
        carbonContext.setTenantId(tenantId);
        TenantMgtUtil.addClaimsToUserStoreManager(tenant);

        PrivilegedCarbonContext.endTenantFlow();
        notifyTenantAddition(tenantInfoBean);
        //adding the subscription entry
        /*try {
            if (TenantMgtServiceComponent.getBillingService() != null) {
                TenantMgtServiceComponent.getBillingService().
                        addUsagePlan(tenant, tenantInfoBean.getUsagePlan());
                if (log.isDebugEnabled()) {
                    log.debug("Subscription added successfully for the tenant: " +
                            tenantInfoBean.getTenantDomain());
                }
            }
        } catch (Exception e) {
            String msg = "Error occurred while adding the subscription for tenant: " + tenantDomain;
            log.error(msg, e);
        }*/

        // For the super tenant tenant creation, tenants are always activated as they are created.
        TenantMgtUtil.activateTenantInitially(tenantInfoBean, tenantId);

        return TenantMgtUtil.prepareStringToShowThemeMgtPage(tenant.getId());
    }

    private void notifyTenantAddition(TenantInfoBean tenantInfoBean) throws Exception {
        //Notify tenant addition
        try {
            TenantMgtUtil.triggerAddTenant(tenantInfoBean);
        } catch (StratosException e) {
            String msg = "Error in notifying tenant addition.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
    }

    /**
     * Notifying Tenant deletion listeners
     *
     * @param tenantId tenant id of the deleting tenant
     * @throws Exception
     */
    private void notifyTenantDeletionListener(int tenantId) throws Exception {
        try {
            TenantMgtUtil.triggerPreTenantDelete(tenantId);
        } catch (StratosException e) {
            String msg = "Error in notifying tenant addition.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
    }
    private void isSuperTenant() throws Exception {
        int loggedInTenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        if (loggedInTenantID != MultitenantConstants.SUPER_TENANT_ID) {
            log.error("Security Alert! Non super tenant trying to create a tenant.");
            throw new Exception("Invalid data."); // obscure error message.
        }
    }

    /**
     * Super admin add tenant.This method will be used whenever the user store is shared between two deployment.
     * This method will persist tenant not in user store level but will do other post tenant creation actions.
     *
     * @param tenantInfoBean
     * @return
     * @throws Exception
     */
    public String addSkeletonTenant(TenantInfoBean tenantInfoBean) throws Exception {
        int tenantId;
        isSuperTenant();
        try {
            tenantId=TenantMgtServiceComponent.getTenantManager().getTenantId(tenantInfoBean.getTenantDomain());
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            String msg = "Error in getting tenant id";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
        if(tenantId<0){
            String msg = "Tenant is not added in user store "+tenantInfoBean.getTenantDomain();
            log.error(msg);
            throw new Exception(msg);
        }
        Tenant tenant = TenantMgtUtil.initializeTenant(tenantInfoBean);
        tenant.setId(tenantId);
        TenantPersistor persistor = new TenantPersistor();
        // not validating the domain ownership, since created by super tenant
        persistor.persistTenant(tenant, false, tenantInfoBean.getSuccessKey(),
                tenantInfoBean.getOriginatedService(), true);
        tenantInfoBean.setTenantId(tenantId);
        notifyTenantAddition(tenantInfoBean);
        return TenantMgtUtil.prepareStringToShowThemeMgtPage(tenantId);
    }
    /**
     * Get the list of the tenants
     *
     * @return List<TenantInfoBean>
     * @throws Exception UserStorException
     */
    private List<TenantInfoBean> getAllTenants() throws Exception {
        TenantManager tenantManager = TenantMgtServiceComponent.getTenantManager();
        Tenant[] tenants;
        try {
            tenants = (Tenant[]) tenantManager.getAllTenants();
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant information.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        List<TenantInfoBean> tenantList = new ArrayList<TenantInfoBean>();
        for (Tenant tenant : tenants) {
            TenantInfoBean bean = TenantMgtUtil.getTenantInfoBeanfromTenant(tenant.getId(), tenant);
            tenantList.add(bean);
        }
        return tenantList;
    }
    
    /**
     * Get the list of the tenants
     *
     * @return List<TenantInfoBean>
     * @throws Exception UserStorException
     */
    private List<TenantInfoBean> searchPartialTenantsDomains(String domain) throws Exception {
        TenantManager tenantManager = TenantMgtServiceComponent.getTenantManager();
        Tenant[] tenants;
        try {
        	domain = domain.trim();
            tenants = (Tenant[]) tenantManager.getAllTenantsForTenantDomainStr(domain);
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant information.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        List<TenantInfoBean> tenantList = new ArrayList<TenantInfoBean>();
        for (Tenant tenant : tenants) {
            TenantInfoBean bean = TenantMgtUtil.getTenantInfoBeanfromTenant(tenant.getId(), tenant);
            tenantList.add(bean);
        }
        return tenantList;
    }

    /**
     * Retrieve all the tenants
     *
     * @return tenantInfoBean[]
     * @throws Exception if failed to get Tenant Manager
     */
    public TenantInfoBean[] retrieveTenants() throws Exception {
        List<TenantInfoBean> tenantList = getAllTenants();
        return tenantList.toArray(new TenantInfoBean[tenantList.size()]);
    }
    
    /**
     * Retrieve all the tenants which matches the partial search domain
     *
     * @return tenantInfoBean[]
     * @throws Exception if failed to get Tenant Manager
     */
    public TenantInfoBean[] retrievePartialSearchTenants(String domain) throws Exception {
        List<TenantInfoBean> tenantList = searchPartialTenantsDomains(domain);
        return tenantList.toArray(new TenantInfoBean[tenantList.size()]);
    }

    /**
     * Method to retrieve all the partial search domain tenants paginated
     *
     * @param pageNumber Number of the page.
     * @return PaginatedTenantInfoBean
     * @throws Exception if failed to getTenantManager;
     */
    public PaginatedTenantInfoBean retrievePaginatedPartialSearchTenants(String domain,int pageNumber) throws Exception {
        List<TenantInfoBean> tenantList = searchPartialTenantsDomains(domain);

        // Pagination
        PaginatedTenantInfoBean paginatedTenantInfoBean = new PaginatedTenantInfoBean();
        DataPaginator.doPaging(pageNumber, tenantList, paginatedTenantInfoBean);
        return paginatedTenantInfoBean;
    }
    
    /**
     * Method to retrieve all the tenants paginated
     *
     * @param pageNumber Number of the page.
     * @return PaginatedTenantInfoBean
     * @throws Exception if failed to getTenantManager;
     */
    public PaginatedTenantInfoBean retrievePaginatedTenants(int pageNumber) throws Exception {
        List<TenantInfoBean> tenantList = getAllTenants();

        // Pagination
        PaginatedTenantInfoBean paginatedTenantInfoBean = new PaginatedTenantInfoBean();
        DataPaginator.doPaging(pageNumber, tenantList, paginatedTenantInfoBean);
        return paginatedTenantInfoBean;
    }

    /**
     * Get a specific tenant
     *
     * @param tenantDomain tenant domain
     * @return tenantInfoBean
     * @throws Exception UserStoreException
     */
    public TenantInfoBean getTenant(String tenantDomain) throws Exception {
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

        TenantInfoBean bean = TenantMgtUtil.initializeTenantInfoBean(tenantId, tenant);

        // retrieve first and last names from the UserStoreManager
        bean.setFirstname(ClaimsMgtUtil.getFirstNamefromUserStoreManager(
                TenantMgtServiceComponent.getRealmService(), tenantId));
        bean.setLastname(ClaimsMgtUtil.getLastNamefromUserStoreManager(
                TenantMgtServiceComponent.getRealmService(), tenantId));

        //getting the subscription plan
        String activePlan = "";
        if(TenantMgtServiceComponent.getBillingService() != null){
            activePlan = TenantMgtServiceComponent.getBillingService().
                    getActiveUsagePlan(tenantDomain);
        }

        if(activePlan != null && activePlan.trim().length() > 0){
            bean.setUsagePlan(activePlan);
        }else{
            bean.setUsagePlan("");
        }

        return bean;
    }

    /**
     * Updates a given tenant
     *
     * @param tenantInfoBean tenant information
     * @throws Exception UserStoreException
     */
    public void updateTenant(TenantInfoBean tenantInfoBean) throws Exception {
        TenantManager tenantManager = TenantMgtServiceComponent.getTenantManager();
        UserStoreManager userStoreManager;

        // filling the non-set admin and admin password first
        UserRegistry configSystemRegistry = TenantMgtServiceComponent.getConfigSystemRegistry(
                tenantInfoBean.getTenantId());

        String tenantDomain = tenantInfoBean.getTenantDomain();

        int tenantId;
        try {
            tenantId = tenantManager.getTenantId(tenantDomain);
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant id for the tenant domain: " + tenantDomain
                         + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        Tenant tenant;
        try {
            tenant = (Tenant) tenantManager.getTenant(tenantId);
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant id for the tenant domain: " +
                         tenantDomain + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        // filling the first and last name values
        if (tenantInfoBean.getFirstname() != null &&
            !tenantInfoBean.getFirstname().trim().equals("")) {
            try {
                CommonUtil.validateName(tenantInfoBean.getFirstname(), "First Name");
            } catch (Exception e) {
                String msg = "Invalid first name is provided.";
                log.error(msg, e);
                throw new Exception(msg, e);
            }
        }
        if (tenantInfoBean.getLastname() != null &&
            !tenantInfoBean.getLastname().trim().equals("")) {
            try {
                CommonUtil.validateName(tenantInfoBean.getLastname(), "Last Name");
            } catch (Exception e) {
                String msg = "Invalid last name is provided.";
                log.error(msg, e);
                throw new Exception(msg, e);
            }
        }

        tenant.setAdminFirstName(tenantInfoBean.getFirstname());
        tenant.setAdminLastName(tenantInfoBean.getLastname());
        TenantMgtUtil.addClaimsToUserStoreManager(tenant);

        // filling the email value
        if (tenantInfoBean.getEmail() != null && !tenantInfoBean.getEmail().equals("")) {
            // validate the email
            try {
                CommonUtil.validateEmail(tenantInfoBean.getEmail());
            } catch (Exception e) {
                String msg = "Invalid email is provided.";
                log.error(msg, e);
                throw new Exception(msg, e);
            }
            tenant.setEmail(tenantInfoBean.getEmail());
        }

        UserRealm userRealm = configSystemRegistry.getUserRealm();
        try {
            userStoreManager = userRealm.getUserStoreManager();
        } catch (UserStoreException e) {
            String msg = "Error in getting the user store manager for tenant, tenant domain: " +
                         tenantDomain + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        boolean updatePassword = false;
        if (tenantInfoBean.getAdminPassword() != null
            && !tenantInfoBean.getAdminPassword().equals("")) {
            updatePassword = true;
        }
        if (!userStoreManager.isReadOnly() && updatePassword) {
            // now we will update the tenant admin with the admin given
            // password.
            try {
                userStoreManager.updateCredentialByAdmin(tenantInfoBean.getAdmin(),
                                                         tenantInfoBean.getAdminPassword());
            } catch (UserStoreException e) {
                String msg = "Error in changing the tenant admin password, tenant domain: " +
                             tenantInfoBean.getTenantDomain() + ". " + e.getMessage() + " for: " +
                             tenantInfoBean.getAdmin();
                log.error(msg, e);
                throw new Exception(msg, e);
            }
        } else {
            //Password should be empty since no password update done
            tenantInfoBean.setAdminPassword("");
        }

        try {
            tenantManager.updateTenant(tenant);
        } catch (UserStoreException e) {
            String msg = "Error in updating the tenant for tenant domain: " + tenantDomain + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
        
        //Notify tenant update to all listeners
        try {
            TenantMgtUtil.triggerUpdateTenant(tenantInfoBean);
        } catch (StratosException e) {
            String msg = "Error in notifying tenant update.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        //updating the usage plan
        /*try{
            if(TenantMgtServiceComponent.getBillingService() != null){
                TenantMgtServiceComponent.getBillingService().
                        updateUsagePlan(tenantInfoBean.getTenantDomain(), tenantInfoBean.getUsagePlan());
            }
        }catch(Exception e){
            String msg = "Error when updating the usage plan: " + e.getMessage();
            log.error(msg, e);
            throw new Exception(msg, e);
        }*/
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
    }

    /**
     * Delete a specific tenant
     *
     * @param tenantDomain The domain name of the tenant that needs to be deleted
     */
    public void deleteTenant(String tenantDomain)
            throws TenantManagementException, org.wso2.carbon.user.api.UserStoreException {
        TenantManager tenantManager = TenantMgtServiceComponent.getTenantManager();

        if (tenantManager != null) {
            int tenantId = tenantManager.getTenantId(tenantDomain);
            try {
                //Only super tenant users can delete tenant
                isSuperTenant();

                log.info("Starting Tenant Deletion process.");

                //deactivating the tenant if they are loaded
                if (tenantManager.isTenantActive(tenantId)) {
                    deactivateTenant(tenantDomain);
                }

                // Taking the loaded tenant configuration contexts.
                Map<String, ConfigurationContext> tenantConfigContexts = TenantAxisUtils
                        .getTenantConfigurationContexts(TenantMgtServiceComponent.getConfigurationContext());
                try {
                    PrivilegedCarbonContext.startTenantFlow();
                    // Creating CarbonContext object for these threads.
                    PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
                    carbonContext.setTenantDomain(tenantDomain, true);
                    //TODO remove starting tenant flow after fixing terminateTenantConfigContext
                    //Terminating the tenant configuration context
                    if (tenantConfigContexts.containsKey(tenantDomain)) {
                        TenantAxisUtils.terminateTenantConfigContext(tenantConfigContexts.get(tenantDomain));
                    }
                } finally {
                    PrivilegedCarbonContext.endTenantFlow();
                }

                // get the deleting tenants repository url
                String tenantRepoUrl = CarbonUtils.getCarbonTenantsDirPath() + File.separator + tenantId;

                // before deleting the tenant worker node tenant should also need to unload, because still
                // worker node tenant can receive requests
                TenantMgtUtil.notifyTenantUnloadInCluster(tenantId);
                FileManipulator.deleteDir(tenantRepoUrl);
                notifyTenantDeletionListener(tenantId);
                // After unloading the tenant, deletion process will happen
                TenantMgtUtil.notifyTenantDeletionInCluster(tenantId);

                // Enabling login ability after recreating tenant with same domain name
                TenantMgtUtil.removeGlobalTenantCache(tenantDomain);

                String tenantDelete = System.getProperty("tenantDelete");
                // String tenantDelete = TenantMgtServiceComponent.getServerConfigurationService().getFirstProperty("TenantDelete");

                if (log.isDebugEnabled()) {
                    log.debug("Tenant Delete Flag is " + tenantDelete);
                }

                if ((tenantDelete != null) && ("true".equals(tenantDelete))) {

                    //TODO remove this check and add this in to listener
                    if (TenantMgtServiceComponent.getBillingService() != null) {
                        TenantMgtServiceComponent.getBillingService().deleteBillingData(tenantId);
                    }

                    TenantMgtUtil.removeAllTenantRelatedData(tenantManager, tenantId);

                    log.info("Deleted tenant with domain: " + tenantDomain + " and tenant id: " + tenantId
                            + " from the system.");

                }
                //TODO need to handle specific exception after removing stratos exception
            } catch (Exception e) {
                String msg = "Error deleting tenant with domain: " + tenantDomain + " and tenant id: " +
                        tenantId + ".";
                log.error(msg, e);
                throw new TenantManagementException(msg, e);
            }
        }

    }
}
