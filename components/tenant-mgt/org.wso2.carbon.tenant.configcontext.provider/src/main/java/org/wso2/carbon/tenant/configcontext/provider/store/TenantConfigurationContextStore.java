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

package org.wso2.carbon.tenant.configcontext.provider.store;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.axis2.context.ConfigurationContext;

public final class TenantConfigurationContextStore {

	private static volatile TenantConfigurationContextStore tenantConfigContextStore;
	private ConcurrentHashMap<Integer, ConfigurationContext> tenantConfigurationContextMap = new ConcurrentHashMap<Integer, ConfigurationContext>();

	private TenantConfigurationContextStore() {
	}

	public static TenantConfigurationContextStore getInstance() {
		if (tenantConfigContextStore == null) {
			synchronized (TenantConfigurationContextStore.class) {
				if (tenantConfigContextStore == null) {
					tenantConfigContextStore = new TenantConfigurationContextStore();
				}
			}
		}

		return tenantConfigContextStore;
	}

	public ConcurrentHashMap<Integer, ConfigurationContext> getTenantConfigurationContextMap() {
		return tenantConfigurationContextMap;
	}

	public void setTenantConfigurationContextMap(
			ConcurrentHashMap<Integer, ConfigurationContext> tenantConfigurationContextMap) {
		this.tenantConfigurationContextMap = tenantConfigurationContextMap;
	}

}
