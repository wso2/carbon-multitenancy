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
package org.wso2.carbon.keystore.mgt.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.keystore.mgt.KeystoreTenantMgtListener;
import org.wso2.carbon.keystore.mgt.util.RealmServiceHolder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(
        name = "org.wso2.carbon.keystore.mgt",
        immediate = true)
public class KeyStoreMgtServiceComponent {

    private static Log log = LogFactory.getLog(KeyStoreMgtServiceComponent.class);

    @Activate
    protected void activate(ComponentContext ctxt) {

        KeystoreTenantMgtListener keystoreTenantMgtListener = new KeystoreTenantMgtListener();
        ctxt.getBundleContext().registerService(org.wso2.carbon.stratos.common.listeners.TenantMgtListener.class
                .getName(), keystoreTenantMgtListener, null);
        if (log.isDebugEnabled()) {
            log.debug("*************Stratos Keystore mgt component is activated.**************");
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext ctxt) {

        if (log.isDebugEnabled()) {
            log.debug("************Stratos keystore mgt component is decativated.*************");
        }
    }

    @Reference(
            name = "user.realmservice.default",
            service = org.wso2.carbon.user.core.service.RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        RealmServiceHolder.setRealmService(realmService);
        if (log.isDebugEnabled()) {
            log.debug("Realm Service is set for KeyStoreMgtServiceComponent.");
        }
    }

    protected void unsetRealmService(RealmService realmService) {

        RealmServiceHolder.setRealmService(null);
        if (log.isDebugEnabled()) {
            log.debug("Realm Service is unset for KeyStoreMgtServiceComponent.");
        }
    }
}
