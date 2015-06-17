/*
 *
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.tenant.mgt.message;

import org.apache.axis2.clustering.ClusteringCommand;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.ClusteringMessage;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.multitenancy.utils.TenantAxisUtils;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.tenant.mgt.exception.TenantManagementException;
import org.wso2.carbon.tenant.mgt.internal.TenantMgtServiceComponent;
import org.wso2.carbon.tenant.mgt.util.TenantMgtUtil;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.tenant.TenantManager;

import java.util.Map;

/**
 * Tenant unload cluster message use to send to all worker nodes and unload the tenant
 */
public class TenantUnloadClusterMessage extends ClusteringMessage {

    private static final long serialVersionUID = -5348082601467389829L;
    private int tenantId;
    private transient static final Log log = LogFactory.getLog(TenantUnloadClusterMessage.class);

    /**
     * Overloaded constructor to pass the tenant id
     *
     * @param tenantId
     */
    public TenantUnloadClusterMessage(int tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * Get Response
     *
     * @return ClusteringCommand
     */
    @Override public ClusteringCommand getResponse() {
        return null;
    }

    /**
     * Unloading the tenant in worker node
     *
     * @param arg0 - ConfigurationContext
     */
    @Override
    public void execute(ConfigurationContext arg0) throws ClusteringFault {

        TenantManager tenantManager = TenantMgtServiceComponent.getTenantManager();

        if (tenantManager != null) {
            try {
                String tenantDomain = tenantManager.getDomain(tenantId);
                //deactivating the tenant if the tenant is loaded in worker node
                if (tenantManager.isTenantActive(tenantId)) {
                    deactivateTenant(tenantManager);
                }

                // Taking the loaded tenant configuration contexts.
                Map<String, ConfigurationContext> tenantConfigContexts = TenantAxisUtils
                        .getTenantConfigurationContexts(TenantMgtServiceComponent.getConfigurationContext());
                try {
                    PrivilegedCarbonContext.startTenantFlow();
                    // Creating CarbonContext object for these threads.
                    PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
                    carbonContext.setTenantId(tenantId, true);
                    //TODO remove starting tenant flow after fixing terminateTenantConfigContext
                    //Terminating the tenant configuration context
                    if (tenantConfigContexts.containsKey(tenantDomain)) {
                        TenantAxisUtils.terminateTenantConfigContext(tenantConfigContexts.get(tenantDomain));
                    }

                } finally {
                    PrivilegedCarbonContext.endTenantFlow();
                }
            } catch (TenantManagementException e) {
                String errorMsg = "Error when deactivating tenant.";
                throw new ClusteringFault(errorMsg, e);
            } catch (UserStoreException e) {
                String errorMsg = "Error when retrieving tenant details.";
                throw new ClusteringFault(errorMsg, e);
            }
        }
    }

    /**
     * Deactivate the given tenant
     *
     * @param tenantManager tenant's manager object
     * @throws org.wso2.carbon.tenant.mgt.exception.TenantManagementException UserStoreException
     */
    public void deactivateTenant(TenantManager tenantManager) throws TenantManagementException {
        String tenantDomain = null;
        try {
            tenantDomain = tenantManager.getDomain(tenantId);
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant id for the tenant domain: " +
                    tenantDomain + ".";
            log.error(msg, e);
            throw new TenantManagementException(msg, e);
        }

        TenantMgtUtil.deactivateTenant(tenantDomain, tenantManager, tenantId);

        //Notify tenant deactivation all listeners
        try {
            TenantMgtUtil.triggerTenantDeactivation(tenantId);
        } catch (StratosException e) {
            String msg = "Error in notifying tenant deactivate.";
            log.error(msg, e);
            throw new TenantManagementException(msg, e);
        }
    }

}
