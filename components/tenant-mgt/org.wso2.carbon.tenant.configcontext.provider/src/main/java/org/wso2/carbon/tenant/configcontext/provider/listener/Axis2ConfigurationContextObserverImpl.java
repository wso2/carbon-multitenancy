/*
 *  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.tenant.configcontext.provider.listener;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.tenant.configcontext.provider.store.TenantConfigurationContextStore;
import org.wso2.carbon.utils.AbstractAxis2ConfigurationContextObserver;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

public class Axis2ConfigurationContextObserverImpl extends AbstractAxis2ConfigurationContextObserver{

    private static Log log = LogFactory.getLog(Axis2ConfigurationContextObserverImpl.class);
    
	@Override
	public void terminatingConfigurationContext(ConfigurationContext configCtx) {
		//Remove the configuration context for the map when tenant unload.
        int tenantID = MultitenantUtils.getTenantId(configCtx);
        ConfigurationContext clientContext = TenantConfigurationContextStore.getInstance().getTenantConfigurationContextMap().get(tenantID);
        
        //cleanup contexts
        clientContext.cleanupContexts();
        TenantConfigurationContextStore.getInstance().getTenantConfigurationContextMap().remove(tenantID);
		log.info("Configuration Context for Tenant ID: "+tenantID+" is removed");
	}
}
