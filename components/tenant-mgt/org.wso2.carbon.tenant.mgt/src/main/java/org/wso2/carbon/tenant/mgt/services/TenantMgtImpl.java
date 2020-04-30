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
package org.wso2.carbon.tenant.mgt.services;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.stratos.common.constants.TenantConstants;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.stratos.common.exception.TenantManagementClientException;
import org.wso2.carbon.stratos.common.exception.TenantManagementServerException;
import org.wso2.carbon.stratos.common.exception.TenantMgtException;
import org.wso2.carbon.stratos.common.util.CommonUtil;
import org.wso2.carbon.tenant.mgt.core.TenantPersistor;
import org.wso2.carbon.tenant.mgt.internal.TenantMgtServiceComponent;
import org.wso2.carbon.tenant.mgt.util.TenantMgtUtil;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.TenantMgtConfiguration;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.common.User;
import org.wso2.carbon.user.core.config.multitenancy.MultiTenantRealmConfigBuilder;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.user.core.tenant.TenantSearchResult;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.Date;

import static org.wso2.carbon.stratos.common.constants.TenantConstants.ErrorMessage.ERROR_CODE_INVALID_OFFSET;
import static org.wso2.carbon.stratos.common.constants.TenantConstants.ErrorMessage.ERROR_CODE_RESOURCE_NOT_FOUND;
import static org.wso2.carbon.tenant.mgt.util.TenantMgtUtil.initializeTenantInfoBean;

/**
 * Default implementation of {@link TenantMgtService} interface.
 */
public class TenantMgtImpl implements TenantMgtService {

    private static final Log log = LogFactory.getLog(TenantMgtImpl.class);
    private static final String DEFAULT_SORT_BY = "UM_DOMAIN_NAME";
    private static final String DESC_SORT_ORDER = "DESC";
    private static final String ASC_SORT_ORDER = "ASC";
    public static final String DOMAIN_NAME = "domainName";
    public static final String TENANT_ADMIN_ASK_PASSWORD_CLAIM =
            "http://wso2.org/claims/identity/tenantAdminAskPassword";
    public static final String INVITE_VIA_EMAIL = "invite-via-email";

    public String addTenant(Tenant tenant) throws TenantMgtException {

        String tenantDomain = tenant.getDomain();
        int tenantId;

        validateInputs(tenant);
        try {
            // Set a thread local variable to identify the operations triggered for a tenant admin user.
            TenantMgtUtil.setTenantAdminCreationOperation(true);

            addAttributeValues(tenant);
            createTenant(tenant);
            addTenantAdminUser(tenant);
            tenantId = tenant.getId();
            addClaimsToUserStore(tenant);

            TenantInfoBean tenantInfoBean = initializeTenantInfoBean(tenantId, tenant);
            notifyTenantAddition(tenantInfoBean);

            // For the super tenant tenant creation, tenants are always activated as they are created.
            TenantMgtUtil.activateTenantInitially(tenantInfoBean, tenantId);
            if (INVITE_VIA_EMAIL.equalsIgnoreCase(tenant.getProvisioningMethod())) {
                tenant.getClaimsMap().put(TENANT_ADMIN_ASK_PASSWORD_CLAIM, "true");
            }
            // This was separate out to support handlers invocation.
            addAdditionalClaimsToUserStore(tenant);
        } catch (Exception e) {
            if (e instanceof TenantMgtException) {
                throw (TenantMgtException) e;
            } else {
                throw new TenantManagementServerException(e.getMessage(), e);
            }
        }
        log.info("Added the tenant '" + tenantDomain + " [" + tenantId +
                "]' by '" + PrivilegedCarbonContext.getThreadLocalCarbonContext().
                getUsername() + "'");
        TenantMgtUtil.prepareStringToShowThemeMgtPage(tenant.getId(), tenant.getTenantUniqueID());

        return tenant.getTenantUniqueID();
    }

    public TenantSearchResult listTenants(Integer limit, Integer offset, String sortOrder, String sortBy, String filter)
            throws TenantMgtException {

        TenantManager tenantManager = TenantMgtServiceComponent.getTenantManager();
        try {
            TenantSearchResult tenantSearchResult = new TenantSearchResult();
            setParameters(limit, offset, sortOrder, sortBy, filter, tenantSearchResult);

            tenantSearchResult = tenantManager
                    .listTenants(tenantSearchResult.getLimit(), tenantSearchResult.getOffSet(),
                            tenantSearchResult.getSortOrder(), tenantSearchResult.getSortBy(),
                            tenantSearchResult.getFilter());
            return tenantSearchResult;
        } catch (UserStoreException e) {
            throw new TenantManagementServerException("Error in retrieving the tenant information.", e);
        }
    }

    public Tenant getTenant(String tenantUniqueID) throws TenantMgtException {

        TenantManager tenantManager = TenantMgtServiceComponent.getTenantManager();
        Tenant tenant;
        try {
            tenant = tenantManager.getTenant(tenantUniqueID);
            if (tenant == null) {
                throw new TenantManagementClientException(ERROR_CODE_RESOURCE_NOT_FOUND.getCode(),
                        String.format(ERROR_CODE_RESOURCE_NOT_FOUND.getMessage(), tenantUniqueID));
            }
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new TenantManagementServerException("Error while getting the tenant.", e);
        }
        return tenant;
    }

    public void activateTenant(String tenantUniqueID) throws TenantMgtException {

        TenantManager tenantManager = TenantMgtServiceComponent.getTenantManager();
        int tenantId;
        String tenantDomain = null;
        try {
            Tenant tenant = tenantManager.getTenant(tenantUniqueID);
            if (tenant != null) {
                tenantManager.activateTenant(tenantUniqueID);
                tenantId = tenant.getId();
                tenantDomain = tenant.getDomain();
                // Notify tenant activation all listeners.
                TenantMgtUtil.triggerTenantActivation(tenantId);
            } else {
                throw new TenantManagementClientException(ERROR_CODE_RESOURCE_NOT_FOUND.getCode(),
                        String.format(ERROR_CODE_RESOURCE_NOT_FOUND.getMessage(), tenantUniqueID));
            }
        } catch (UserStoreException e) {
            throw new TenantManagementServerException("Error in activating or getting the tenant using tenant " +
                    "unique id: " + tenantUniqueID + " .", e);
        } catch (StratosException e) {
            throw new TenantManagementServerException("Error in notifying tenant activation of tenant: " +
                    tenantDomain + " .", e);
        }

        log.info("Activated the tenant '" + tenantDomain + " [" + tenantId + "]' by '" +
                PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername() + "'");
    }

    public void deactivateTenant(String tenantUniqueID) throws TenantMgtException {

        TenantManager tenantManager = TenantMgtServiceComponent.getTenantManager();
        int tenantId;
        String tenantDomain = null;
        try {
            Tenant tenant = tenantManager.getTenant(tenantUniqueID);
            if (tenant != null) {
                tenantId = tenant.getId();
                tenantDomain = tenant.getDomain();
                // Notify tenant deactivation to all listeners.
                TenantMgtUtil.triggerTenantDeactivation(tenantId);
                tenantManager.deactivateTenant(tenantUniqueID);
                TenantMgtUtil.unloadTenantConfigurations(tenantDomain, tenantId);
                log.info("Deactivated the tenant '" + tenantDomain + " [" + tenantId +
                        "]' by '" + PrivilegedCarbonContext.getThreadLocalCarbonContext().
                        getUsername() + "'");
            } else {
                throw new TenantManagementClientException(ERROR_CODE_RESOURCE_NOT_FOUND.getCode(),
                        String.format(ERROR_CODE_RESOURCE_NOT_FOUND.getMessage(), tenantUniqueID));
            }
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new TenantManagementServerException("Error in retrieving or deactivating the tenant using " +
                    "tenant unique id: " + tenantUniqueID + " .", e);
        } catch (StratosException e) {
            throw new TenantManagementServerException("Error while triggering tenant deactivation for the tenant: " +
                    tenantDomain + " .", e);
        }
    }

    public User getOwner(String tenantUniqueID) throws TenantMgtException {

        Tenant tenant = getTenantFromTenantManager(tenantUniqueID);
        if (tenant != null) {
            return createOwner(tenant);
        }
        throw new TenantManagementClientException(ERROR_CODE_RESOURCE_NOT_FOUND.getCode(),
                String.format(ERROR_CODE_RESOURCE_NOT_FOUND.getMessage(), tenantUniqueID));
    }

    private void createTenant(Tenant tenant) throws Exception {

        TenantPersistor persistor = new TenantPersistor();
        persistor.persistTenant(tenant);
    }

    private void addClaimsToUserStore(Tenant tenant) throws Exception {

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantDomain(tenant.getDomain());
            carbonContext.setTenantId(tenant.getId());

            TenantMgtUtil.addClaimsToUserStoreManager(tenant);
        } finally {
            // Remove thread local variable set to identify operation triggered for a tenant admin user.
            TenantMgtUtil.clearTenantAdminCreationOperation();
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    private void addAdditionalClaimsToUserStore(Tenant tenant) throws Exception {

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantDomain(tenant.getDomain());
            carbonContext.setTenantId(tenant.getId());

            TenantMgtUtil.addAdditionalClaimsToUserStoreManager(tenant);
        } finally {
            // Remove thread local variable set to identify operation triggered for a tenant admin user.
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    private void validateInputs(Tenant tenant) throws TenantMgtException {

        try {
            CommonUtil.validateEmail(tenant.getEmail());
            TenantMgtUtil.validateDomain(tenant.getDomain());
            checkIsSuperTenantInvoking();
        } catch (Exception e) {
            if (e instanceof TenantMgtException) {
                throw (TenantMgtException) e;
            } else {
                throw new TenantManagementServerException(e.getMessage(), e);
            }
        }
    }

    private void addAttributeValues(Tenant tenant) throws TenantMgtException {

        tenant.setCreatedDate(createDate());
        tenant.setTenantUniqueID(UUIDGenerator.generateUUID());

        RealmConfiguration realmConfiguration = getRealmConfigForTenant(tenant);
        tenant.setRealmConfig(realmConfiguration);
    }

    private RealmConfiguration getRealmConfigForTenant(Tenant tenant) throws TenantMgtException {

        RealmService realmService = TenantMgtServiceComponent.getRealmService();
        RealmConfiguration realmConfig = realmService.getBootstrapRealmConfiguration();
        TenantMgtConfiguration tenantMgtConfiguration = realmService.getTenantMgtConfiguration();
        try {
            MultiTenantRealmConfigBuilder builder = TenantMgtServiceComponent.
                    getRealmService().getMultiTenantRealmConfigBuilder();
            RealmConfiguration realmConfigToPersist =
                    builder.getRealmConfigForTenantToPersist(realmConfig, tenantMgtConfiguration,
                            tenant, -1);
            // Make AddAdmin true since user creation should happen even AddAdmin false.
            realmService.getBootstrapRealm().getRealmConfiguration().setAddAdmin("true");
            return realmConfigToPersist;

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

        RealmService realmService = TenantMgtServiceComponent.getRealmService();
        try {
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

        // Notify tenant addition.
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
     * Set pagination parameters to tenant search result.
     *
     * @param limit     page limit.
     * @param offset    offset value.
     * @param filter    filter value for tenant search.
     * @param sortOrder order of tenant(ASC/DESC).
     * @param sortBy    the column value need to sort.
     * @param result    result object.
     * @throws TenantManagementClientException Error while validating pagination parameters.
     */
    private void setParameters(Integer limit, Integer offset, String sortOrder, String sortBy, String filter,
                               TenantSearchResult result) throws TenantManagementClientException {

        result.setLimit(validateLimit(limit));
        result.setOffSet(validateOffset(offset));
        result.setSortOrder(validateSortOrder(sortOrder));
        result.setSortBy(validateSortBy(sortBy));
        result.setFilter(filter);
    }

    /**
     * Validate sortBy.
     *
     * @param sortBy sortBy attribute.
     * @return Validated sortBy.
     */
    private String validateSortBy(String sortBy) {

        if (StringUtils.isBlank(sortBy)) {
            if (log.isDebugEnabled()) {
                log.debug("sortBy attribute is empty. Therefore we set the default sortBy attribute: " +
                        DEFAULT_SORT_BY);
            }
            return DEFAULT_SORT_BY;
        }

        switch (sortBy) {
            case DOMAIN_NAME:
                sortBy = DEFAULT_SORT_BY;
                break;
            default:
                sortBy = DEFAULT_SORT_BY;
                if (log.isDebugEnabled()) {
                    log.debug("sortBy attribute is incorrect. Therefore we set the default sortBy attribute. " +
                            "sortBy: " + DEFAULT_SORT_BY);
                }
                break;
        }
        return sortBy;
    }

    /**
     * Validate sortOrder.
     *
     * @param sortOrder sortOrder ASC/DESC.
     * @return Validated sortOrder.
     */
    private String validateSortOrder(String sortOrder) {

        if (StringUtils.isBlank(sortOrder)) {
            sortOrder = ASC_SORT_ORDER;
            if (log.isDebugEnabled()) {
                log.debug("sortOrder is empty. Therefore we set the default sortOrder value as: " +
                        ASC_SORT_ORDER);
            }
        } else if (!(sortOrder.equals(DESC_SORT_ORDER) || sortOrder.equals(ASC_SORT_ORDER))) {
            sortOrder = ASC_SORT_ORDER;
            if (log.isDebugEnabled()) {
                log.debug("sortOrder is incorrect. Therefore we set the default sortOrder value as: " +
                        ASC_SORT_ORDER + ". SortOrder: " + sortOrder);
            }
        }
        return sortOrder;
    }

    /**
     * Validate limit.
     * @param limit limit value.
     * @return validated limit value.
     * @throws TenantManagementClientException if limit validation fails.
     */
    private int validateLimit(Integer limit) throws TenantManagementClientException {

        if (limit == null) {
            if (log.isDebugEnabled()) {
                log.debug("Given limit is null. Therefore we get the default limit from carbon.xml.");
            }
            limit = TenantMgtUtil.getDefaultItemsPerPage();
        }
        if (limit < 0) {
            throw new TenantManagementClientException(TenantConstants.ErrorMessage.ERROR_CODE_INVALID_LIMIT);
        }

        int maxLimit = TenantMgtUtil.getMaximumItemPerPage();

        if (limit > maxLimit) {
            if (log.isDebugEnabled()) {
                log.debug("Given limit exceed the maximum limit. Therefore we get the default max limit: " +
                        maxLimit + " from carbon.xml.");
            }
            limit = maxLimit;
        }
        return limit;
    }

    /**
     * Validate offset.
     *
     * @param offset offset value.
     * @return validated offset value.
     * @throws TenantManagementClientException Error while setting offset.
     */
    private int validateOffset(Integer offset) throws TenantManagementClientException {

        if (offset == null) {
            // Return first page offset.
            offset = 0;
        }

        if (offset < 0) {
            throw new TenantManagementClientException(ERROR_CODE_INVALID_OFFSET);
        }
        return offset;
    }

    private User createOwner(Tenant tenant) throws TenantManagementServerException {

        User owner = new User();
        owner.setUsername(tenant.getAdminName());
        owner.setUserID(tenant.getAdminUserId());
        return owner;
    }

    private Tenant getTenantFromTenantManager(String tenantUniqueID) throws TenantManagementServerException {

        TenantManager tenantManager = TenantMgtServiceComponent.getTenantManager();
        Tenant tenant;
        try {
            tenant = tenantManager.getTenant(tenantUniqueID);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new TenantManagementServerException("Error while getting the tenant: " + tenantUniqueID + " .", e);
        }
        return tenant;
    }
}
