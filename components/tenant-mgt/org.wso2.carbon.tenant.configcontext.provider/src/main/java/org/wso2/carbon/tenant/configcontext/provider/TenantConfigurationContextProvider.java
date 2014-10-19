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

package org.wso2.carbon.tenant.configcontext.provider;

import java.rmi.RemoteException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.tenant.configcontext.provider.store.TenantConfigurationContextStore;

public class TenantConfigurationContextProvider {

    private static Log log = LogFactory.getLog(TenantConfigurationContextProvider.class);
	private static final String CONFIG_LOCATION = "repository/deployment/client";

	/**
	 * Returns the Configuration Context for the specified tenant ID,
	 * Create a Configuration Context if not already created, and return it. 
	 * If the client Configuration Context for that tenant is available, it will return it.
	 * 
	 * @param tenantID
	 * @return
	 * @throws RemoteException
	 */
	public ConfigurationContext getTenantConfigurationContext()
			throws RemoteException {
		ConfigurationContext configurationContext = null;
		int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();

		if (TenantConfigurationContextStore.getInstance()
				.getTenantConfigurationContextMap().containsKey(tenantID)) {
			configurationContext = TenantConfigurationContextStore.getInstance()
					.getTenantConfigurationContextMap().get(tenantID);
			log.debug("Configuration context for the Tenant: "+tenantID+ " already exists.");
		} else {

			try {
				configurationContext = (ConfigurationContext) AccessController
						.doPrivileged(new PrivilegedExceptionAction<Object>() {
							public Object run() throws RemoteException {
								return ConfigurationContextFactory
										.createConfigurationContextFromFileSystem(
												CONFIG_LOCATION, null);
							}
						});
				log.info("Created new configuration context for the Tenant: "+tenantID);
			} catch (PrivilegedActionException e) {
				throw (RemoteException) e.getException();
			}
			TenantConfigurationContextStore.getInstance()
					.getTenantConfigurationContextMap()
					.put(tenantID, configurationContext);
		}
		return configurationContext;
	}
}
