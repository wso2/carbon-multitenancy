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
import org.wso2.carbon.tenant.mgt.internal.TenantMgtServiceComponent;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.FileManipulator;

import java.io.File;

/**
 * Tenant delete cluster message use to send to all worker nodes and delete the Cache
 */
public class TenantDeleteClusterMessage extends ClusteringMessage {

    private static final long serialVersionUID = -5348082601467389829L;
    private int tenantId;
    private transient static final Log log = LogFactory.getLog(TenantUnloadClusterMessage.class);
    private final static boolean IS_DELETE_PERSISTENCE_STORAGE = false;

    /**
     * Overloaded constructor to pass the tenant id
     *
     * @param tenantId
     */
    public TenantDeleteClusterMessage(int tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * Get Response
     *
     * @return ClusteringCommand
     */
    @Override
    public ClusteringCommand getResponse() {
        return null;
    }

    /**
     * Deleting cache data in JDBCTenantManager
     *
     * @param arg0 - ConfigurationContext
     */
    @Override
    public void execute(ConfigurationContext arg0) throws ClusteringFault {
        try {
            PrivilegedCarbonContext.startTenantFlow();
            // Creating CarbonContext object for these threads.
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantId(tenantId, true);

            TenantManager tenantManager = TenantMgtServiceComponent.getTenantManager();
            try {
                tenantManager.deleteTenant(tenantId, IS_DELETE_PERSISTENCE_STORAGE);
                deleteTenantRepo();
            } catch (UserStoreException e) {
                log.error("Error occurred while deleting cache : " + e.getMessage());
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    private void deleteTenantRepo(){
        // get the deleting tenants repository url
        String tenantRepoUrl = CarbonUtils.getCarbonTenantsDirPath() + File.separator + tenantId;
        FileManipulator.deleteDir(tenantRepoUrl);
    }
}
