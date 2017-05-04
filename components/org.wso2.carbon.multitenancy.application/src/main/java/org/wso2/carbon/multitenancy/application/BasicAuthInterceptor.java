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

package org.wso2.carbon.multitenancy.application;

import org.wso2.msf4j.security.basic.AbstractBasicAuthSecurityInterceptor;

/**
 * Basic authentication interceptor: Provide user credentials via the following
 * environment variables or system properties:
 * - CARBON_MULTITENANCY_USERNAME
 * - CARBON_MULTITENANCY_PASSWORD
 */
public class BasicAuthInterceptor extends AbstractBasicAuthSecurityInterceptor {

    private static final String CARBON_MULTITENANCY_USERNAME = "CARBON_MULTITENANCY_USERNAME";
    private static final String CARBON_MULTITENANCY_PASSWORD = "CARBON_MULTITENANCY_PASSWORD";

    private String username;
    private String password;

    public BasicAuthInterceptor() {
        username = readInputParameter(CARBON_MULTITENANCY_USERNAME);
        password = readInputParameter(CARBON_MULTITENANCY_PASSWORD);
    }

    @Override
    protected boolean authenticate(String username, String password) {
        return (this.username.equals(username) && this.password.equals(password));
    }

    private String readInputParameter(String propertyName) {
        String value = System.getenv(propertyName);
        if (isEmpty(value)) {
            value = System.getProperty(propertyName);
        }
        if (isEmpty(value)) {
            throw new RuntimeException("An environment variable or a system property not found for " + propertyName);
        }
        return value;
    }

    private boolean isEmpty(String value) {
        return (value == null || value.trim().equals(""));
    }
}
