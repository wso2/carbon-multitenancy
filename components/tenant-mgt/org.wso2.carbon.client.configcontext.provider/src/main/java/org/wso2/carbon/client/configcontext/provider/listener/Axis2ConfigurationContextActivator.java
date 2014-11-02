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

package org.wso2.carbon.client.configcontext.provider.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.wso2.carbon.client.configcontext.provider.Axis2ClientConfigContextProvider;
import org.wso2.carbon.client.configcontext.provider.Axis2ClientConfigContextProviderImpl;
import org.wso2.carbon.utils.Axis2ConfigurationContextObserver;

public class Axis2ConfigurationContextActivator implements BundleActivator {

	private Axis2ConfigurationContextObserverImpl axis2ConfigurationContextObserverImpl = new Axis2ConfigurationContextObserverImpl();
	private Axis2ClientConfigContextProvider contextProvider = new Axis2ClientConfigContextProviderImpl();
	private static Log log = LogFactory
			.getLog(Axis2ConfigurationContextActivator.class);

	public void start(BundleContext context) {
		try {
			context.registerService(
					Axis2ConfigurationContextObserver.class.getName(),
					axis2ConfigurationContextObserverImpl, null);
			context.registerService(
					Axis2ClientConfigContextProvider.class.getName(),
					contextProvider, null);
			log.debug("tenant configuration context bundle is activated");
		} catch (Throwable e) {
			log.error("Failed to activate tenant configuration context bundle ",e);
		}
	}

	public void stop(BundleContext context) {
	}

}
