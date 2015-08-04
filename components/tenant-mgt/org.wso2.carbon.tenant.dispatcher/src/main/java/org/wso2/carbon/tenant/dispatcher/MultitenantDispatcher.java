/*                                                                             
 * Copyright 2004,2005 The Apache Software Foundation.                         
 *                                                                             
 * Licensed under the Apache License, Version 2.0 (the "License");             
 * you may not use this file except in compliance with the License.            
 * You may obtain a copy of the License at                                     
 *                                                                             
 *      http://www.apache.org/licenses/LICENSE-2.0                             
 *                                                                             
 * Unless required by applicable law or agreed to in writing, software         
 * distributed under the License is distributed on an "AS IS" BASIS,           
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.    
 * See the License for the specific language governing permissions and         
 * limitations under the License.                                              
 */
package org.wso2.carbon.tenant.dispatcher;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.engine.AbstractDispatcher;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.util.LoggingControl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.xml.namespace.QName;

/**
 * If none of the dispatcher were able to find an Axis2 service or operation, this dispatcher will
 * be reached, and it will dispatch to the MultitenantService, which is associated with the
 * {@link org.wso2.carbon.core.multitenancy.MultitenantMessageReceiver}
 */
public class MultitenantDispatcher extends AbstractDispatcher {

    private static final Log log = LogFactory.getLog(MultitenantDispatcher.class);
    public static final String NAME = "MultitenantDispatcher";

    public void initDispatcher() {
        QName qn = new QName("http://wso2.org/projects/carbon", NAME);
        HandlerDescription hd = new HandlerDescription(qn.getLocalPart());
        super.init(hd);
    }

    public AxisService findService(MessageContext mc) throws AxisFault {
        AxisService service = mc.getAxisService();
        if (service == null) {
            String to = mc.getTo().getAddress();

            if (!isValidPath(to, mc.getConfigurationContext().getServiceContextPath())) {
                if (LoggingControl.debugLoggingAllowed && log.isDebugEnabled()) {
                    log.debug(mc.getLogIDString() +
                              " Attempted to check for Service using target endpoint URI, but the service fragment was missing");
                }
                return null;
            }

            int tenantDelimiterIndex = to.indexOf("/t/");
            if (tenantDelimiterIndex != -1) {
                AxisConfiguration ac = mc.getConfigurationContext().getAxisConfiguration();
                return ac.getService(MultitenantConstants.MULTITENANT_DISPATCHER_SERVICE);
            }
        }
        return service;
    }

    public AxisOperation findOperation(AxisService svc, MessageContext mc) throws AxisFault {
        AxisOperation operation = mc.getAxisOperation();
        if (operation == null) {
            return svc.getOperation(MultitenantConstants.MULTITENANT_DISPATCHER_OPERATION);
        }
        return operation;
    }

    /**
     * Check given path contains servicePath value
     *
     * @param path        - incoming EPR
     * @param servicePath - Ex: 'services'
     * @return - validity status of the path
     */
    private boolean isValidPath(String path, String servicePath) {

        if (path == null) {
            return false;
        }

        //with this chances that substring matching a different place in the URL is reduced
        if (!servicePath.endsWith("/")) {
            servicePath = servicePath + "/";
        }

        int index = path.lastIndexOf(servicePath);

        if (-1 != index) {
            int serviceStart = index + servicePath.length();

            if (path.length() > serviceStart) {
                return true;
            }
        }
        return false;
    }

}
