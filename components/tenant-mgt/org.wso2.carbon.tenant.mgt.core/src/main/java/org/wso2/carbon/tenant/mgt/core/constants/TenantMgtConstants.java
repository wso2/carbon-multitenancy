/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.tenant.mgt.core.constants;

public class TenantMgtConstants {

    /**
     * Enum for error messages.
     */
    public enum ErrorMessage {

        ERROR_CODE_EMPTY_DOMAIN_NAME("100004",
                "Provided domain name is empty."),
        ERROR_CODE_EMPTY_EXTENSION("100005",
                "You should have an extension to your domain."),
        ERROR_CODE_INVALID_DOMAIN("100006",
                "Invalid domain. Domain should not start with '.'"),
        ERROR_CODE_ILLEGAL_CHARACTERS_IN_DOMAIN("100007",
                "The tenant domain %s contains one or more illegal characters. The valid characters are lowercase " +
                        "letters, numbers, '.', '-' and '_'."),
        ERROR_CODE_EXISTING_USER_NAME("100009",
                "User name : %s exists in the system. Please pick another user name for tenant Administrator."),
        ERROR_CODE_EXISTING_DOMAIN("100010",
                "A tenant with same domain %s already exist. Please use a different domain name."),
        ERROR_CODE_EXISTING_EXTERNAL_CLAIM_URI("10009",
                "Claim URI : %s already exists for claim dialect : %s"),
        ERROR_CODE_EXISTING_LOCAL_CLAIM_URI("100010",
                "Local claim URI : %s already exists."),
        ERROR_CODE_EMPTY_TENANT_DOMAIN("60000", "Empty tenant domain in the request"),
        ERROR_CODE_DELETE_IDN_CLAIM_MAPPED_ATTRIBUTE("65001", "Error occurred while deleting claim " +
                "mapped attributes for domain : %s with tenant Id : %s from table : IDN_CLAIM_MAPPED_ATTRIBUTE"),
        ERROR_CODE_SERVER_ERROR_DELETING_CLAIM_MAPPINGS("65001", "Error occurred while deleting the " +
                "claim mapping for the tenant : %s with domain : %s");

        private final String code;
        private final String message;

        ErrorMessage(String code, String message) {

            this.code = code;
            this.message = message;
        }

        public String getCode() {

            return code;
        }

        public String getMessage() {

            return message;
        }
    }
}
