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
package org.wso2.carbon.redirector.servlet.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.activation.service.ActivationService;
import org.wso2.carbon.redirector.servlet.util.Util;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(
        name = "org.wso2.carbon.redirector.servlet.activation",
        immediate = true)
public class RedirectorServletActivationServiceComponent {

    private static Log log = LogFactory.getLog(RedirectorServletActivationServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) {

        try {
            log.debug("******* Multitenancy Redirector Servlet admin service bundle is activated ******* ");
        } catch (Exception e) {
            log.error("******* Multitenancy Redirector Servlet admin service bundle failed activating ****", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        log.debug("******* Multitenancy Redirector Servlet admin service bundle is deactivated ******* ");
    }

    @Reference(
            name = "activation.service",
            service = org.wso2.carbon.activation.service.ActivationService.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetActivationService")
    protected void setActivationService(ActivationService activationService) {

        Util.setActivationService(activationService);
    }

    protected void unsetActivationService(ActivationService activationService) {

        Util.setActivationService(null);
    }
}
