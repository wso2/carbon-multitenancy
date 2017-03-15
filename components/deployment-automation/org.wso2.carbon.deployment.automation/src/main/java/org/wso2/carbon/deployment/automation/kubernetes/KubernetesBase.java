/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.deployment.automation.kubernetes;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Base class for Kubernetes service providers.
 */
public class KubernetesBase {

    private static final Logger log = LoggerFactory.getLogger(KubernetesBase.class);

    private static final String DEFAULT_KUBERNETES_MASTER_IP = "172.17.8.101";
    private static final String DEFAULT_KUBERNETES_MASTER_PORT = "8080";
    private static final String KUBERNETES_MASTER_IP_ENV = "KUBERNETES_MASTER_IP";
    private static final String KUBERNETES_MASTER_PORT_ENV = "KUBERNETES_MASTER_PORT";

    protected KubernetesClient client;

    /**
     * Initializes the Kubernetes client by providing the master node endpoint. Initially it looks for the master node
     * IP address and the port from the KUBERNETES_MASTER_IP and KUBERNETES_MASTER_PORT environment variables and if not
     * available it falls back to the default endpoint URL.
     */
    public KubernetesBase() {
        String endpointIP = System.getenv(KUBERNETES_MASTER_IP_ENV);
        String endpointPort = System.getenv(KUBERNETES_MASTER_PORT_ENV);

        if (endpointIP == null || endpointIP.isEmpty()) {
            endpointIP = DEFAULT_KUBERNETES_MASTER_IP;
        }

        if (endpointPort == null || endpointPort.isEmpty()) {
            endpointPort = DEFAULT_KUBERNETES_MASTER_PORT;
        }

        URL url;
        try {
            url = new URL("http", endpointIP, Integer.parseInt(endpointPort), "");
            this.client = new DefaultKubernetesClient(url.toString());
        } catch (MalformedURLException e) {
            log.error("Unable to identify the Kubernetes master node.", e);
        } catch (NullPointerException e) {
            log.error("Unable to identify the Kubernetes master node.", e);
        }
    }
}
