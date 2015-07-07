/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.keystore.mgt;

import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is used to listen to the tenant creation events and fire the keystore creation event
 */
public class KeystoreTenantMgtListener implements TenantMgtListener {

    private static Log log = LogFactory.getLog(KeystoreTenantMgtListener.class);
    private static final int EXEC_ORDER = 20;

    /**
     * Generate the keystore when a new tenant is registered.
     * @param tenantInfo Information about the newly created tenant
     */
    public void onTenantCreate(TenantInfoBean tenantInfo) throws StratosException {
        try {
            KeyStoreGenerator ksGenerator = new KeyStoreGenerator(tenantInfo.getTenantId());
            if (!ksGenerator.isKeyStoreExists(tenantInfo.getTenantId())){
            	ksGenerator.generateKeyStore();
            }            
        } catch (KeyStoreMgtException e) {
            String message = "Error when generating the keystore";
            log.error(message, e);
            throw new StratosException(message, e);
        }
    }

    public void onTenantUpdate(TenantInfoBean tenantInfo) throws StratosException {
        // It is not required to implement this method for keystore mgt. 
    }

    public void onTenantDelete(int tenantId) {
        // It is not required to implement this method for keystore mgt.
    }

    public void onTenantRename(int tenantId, String oldDomainName,
                             String newDomainName) throws StratosException {
        // It is not required to implement this method for keystore mgt.
    }

    public int getListenerOrder() {
        return EXEC_ORDER;
    }

    public void onTenantInitialActivation(int tenantId) throws StratosException {
        // It is not required to implement this method for keystore mgt. 
    }

    public void onTenantActivation(int tenantId) throws StratosException {
        // It is not required to implement this method for keystore mgt. 
    }

    public void onTenantDeactivation(int tenantId) throws StratosException {
        // It is not required to implement this method for keystore mgt. 
    }

    public void onSubscriptionPlanChange(int tenentId, String oldPlan, 
                                         String newPlan) throws StratosException {
        // It is not required to implement this method for keystore mgt. 
    }

	@Override
	public void onPreDelete(int tenantId) throws StratosException {
       // UserRegistry userRegistry = PrivilegedCarbonContext.getThreadLocalCarbonContext().getRegistry("/");

        try {
            KeyStoreGenerator ksGenerator = new KeyStoreGenerator(tenantId);
            if (ksGenerator.isKeyStoreExists(tenantId)){
                ksGenerator.deleteKeyStore();
            }
        } catch (KeyStoreMgtException e) {
            String message = "Error when Generating the keystore";
            log.error(message, e);
            throw new StratosException(message, e);
        }
	}
}
