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
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.engine.AbstractDispatcher;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.util.MessageContextBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.multitenancy.utils.TenantAxisUtils;
import org.wso2.carbon.tenant.dispatcher.internal.TenantDispatcherServiceComponent;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.xml.namespace.QName;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * If none of the dispatcher were able to find an Axis2 service or operation, this dispatcher will
 * be reached, and it will dispatch to the MultitenantService, which is associated with the
 * {@link org.wso2.carbon.core.multitenancy.MultitenantMessageReceiver}
 */
public class TenantActiveCheckDispatcher extends AbstractDispatcher {

    public static final String NAME = "TenantActiveCheckDispatcher";
    public static final String TENANT_DISPATCHER_CACHE_MANAGER = "TENANT_DISPATCHER_CACHE_MANAGER";
    public static final String TENANT_STATE_CACHE = "TENANT_STATE_CACHE";
    private Log log = LogFactory.getLog(TenantActiveCheckDispatcher.class);
    private static Timer timer = null;
    private static final ConcurrentHashMap<Integer, Boolean> activations =
            new ConcurrentHashMap<Integer, Boolean>();

    public void initDispatcher() {
        QName qn = new QName("http://wso2.org/projects/carbon", NAME);
        HandlerDescription hd = new HandlerDescription(qn.getLocalPart());
        super.init(hd);
    }

    public AxisService findService(MessageContext mc) throws AxisFault {
        AxisService service = mc.getAxisService();
        if (service == null) {
            String to = mc.getTo().getAddress();

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

    public InvocationResponse invoke(MessageContext msgctx) throws AxisFault {
        AxisConfiguration ac = msgctx.getConfigurationContext().getAxisConfiguration();
        try {
            EndpointReference toURL = msgctx.getTo();
            if (toURL != null) {
            int tenantID = TenantDispatcherServiceComponent.getTenantManager().getTenantId(TenantAxisUtils.getTenantDomain(toURL.toString()));
            Boolean status = null;
            if (tenantID != -1) {
                status = activations.get(tenantID);
                if (status == null) {
                    status = TenantDispatcherServiceComponent.getTenantManager().isTenantActive(tenantID);
                    activations.put(tenantID, status);
                }
                if (status) {
                    return InvocationResponse.CONTINUE;
                } else {
                    Throwable e = new Exception("You are trying to invoke deactivated tenant service");
                    MessageContext faultContext =
                            MessageContextBuilder.createFaultMessageContext(msgctx, e);
                    faultContext.setProperty("HTTP_SC", 403);
                    if(log.isDebugEnabled()){
                        log.debug("Trying to invoke deactivated tenant service : url " + toURL);
                    }
                    AxisEngine.sendFault(faultContext);
                    throw new AxisFault("Trying to invoke deactivated tenant service");
                }
            }
            }
        } catch (UserStoreException e) {
            log.error("Error while retrieving tenant activation information " + e);
        }
        return InvocationResponse.CONTINUE;
    }

    /**
     * Starts cleaning up cached activation records at periodic intervals.
     */
    public static void startCacheCleaner() {
        TimerTask faultyServiceRectifier = new CacheCleaner();
        timer = new Timer();
        // Retry in 1 minute
        long retryIn = 1000 * 60 * 15;
        timer.schedule(faultyServiceRectifier, 0, retryIn);
    }

    /**
     * Stops cleaning up cached activation records.
     */
    public static void stopCacheCleaner() {
        timer.cancel();
        timer = null;
    }

    private static class CacheCleaner extends TimerTask {

        /**
         * {@inheritDoc}
         */
        public void run() {
            activations.clear();
        }
    }

}
