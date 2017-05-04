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
 * Environment variables:
 * - CARBON_MULTITENANCY_USERNAME
 * - CARBON_MULTITENANCY_PASSWORD
 * System properties:
 * - carbon.multitenancy.username
 * - carbon.multitenancy.password
 */
public class BasicAuthInterceptor extends AbstractBasicAuthSecurityInterceptor {

    private static final String ENV_VAR_CARBON_MULTITENANCY_USERNAME = "CARBON_MULTITENANCY_USERNAME";
    private static final String ENV_VAR_CARBON_MULTITENANCY_PASSWORD = "CARBON_MULTITENANCY_PASSWORD";
    private static final String SYS_PROPERTY_CARBON_MULTITENANCY_USERNAME = "carbon.multitenancy.username";
    private static final String SYS_PROPERTY_CARBON_MULTITENANCY_PASSWORD = "carbon.multitenancy.password";

    private String username;
    private String password;

    public BasicAuthInterceptor() {
        username = readInputParameter(ENV_VAR_CARBON_MULTITENANCY_USERNAME, SYS_PROPERTY_CARBON_MULTITENANCY_USERNAME);
        password = readInputParameter(ENV_VAR_CARBON_MULTITENANCY_PASSWORD, SYS_PROPERTY_CARBON_MULTITENANCY_PASSWORD);
    }

    @Override
    protected boolean authenticate(String username, String password) {
        return (this.username.equals(username) && this.password.equals(password));
    }

    private String readInputParameter(String envVarName, String sysPropertyName) {
        String value = System.getenv(envVarName);
        if (isEmpty(value)) {
            value = System.getProperty(sysPropertyName);
        }
        if (isEmpty(value)) {
            throw new RuntimeException("Environment variable " + envVarName + " or system property " + sysPropertyName
                    + " not found");
        }
        return value;
    }

    private boolean isEmpty(String value) {
        return (value == null || value.trim().equals(""));
    }
}
