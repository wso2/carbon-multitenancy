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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.wso2.carbon.tenant.configcontext.provider.TenantConfigurationContextProvider;
import org.wso2.carbon.utils.Axis2ConfigurationContextObserver;

public class Axis2ConfigurationContextActivator implements BundleActivator {

	private Axis2ConfigurationContextObserverImpl axis2ConfigurationContextObserverImpl = new Axis2ConfigurationContextObserverImpl();
	private TenantConfigurationContextProvider contextProvider = new TenantConfigurationContextProvider();
	
	@Override
	public void start(BundleContext context){
		context.registerService(Axis2ConfigurationContextObserver.class.getName(), axis2ConfigurationContextObserverImpl, null);
		context.registerService(TenantConfigurationContextProvider.class.getName(), contextProvider, null);
		
	}

	@Override
	public void stop(BundleContext context){
	}

}
