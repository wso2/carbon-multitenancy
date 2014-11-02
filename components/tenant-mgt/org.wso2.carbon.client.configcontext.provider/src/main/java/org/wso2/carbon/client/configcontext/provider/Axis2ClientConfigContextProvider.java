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

package org.wso2.carbon.client.configcontext.provider;

import java.rmi.RemoteException;

import org.apache.axis2.context.ConfigurationContext;

public interface Axis2ClientConfigContextProvider {

	String AXIS2_CLIENT_CONTEXT_PROVIDER_KEY = "axis2_client_context_provider.key";

	/**
	 * Returns the Configuration Context for the specified tenant ID,
	 * Create a Configuration Context if not already created, and return it. 
	 * If the client Configuration Context for that tenant is available, it will return it.
	 * 
	 * @param tenantID
	 * @return
	 * @throws RemoteException
	 */
	ConfigurationContext getConfigurationContext()
			throws RemoteException;

}
