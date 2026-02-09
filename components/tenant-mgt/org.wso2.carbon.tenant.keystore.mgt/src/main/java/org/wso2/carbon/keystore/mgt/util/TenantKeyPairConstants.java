/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
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
 */
package org.wso2.carbon.keystore.mgt.util;

/*
 * Constants required for Tenant KeyPair Utility.
 */
public class TenantKeyPairConstants {

    // Constants required for EC key pair generation for existing tenants for backward compatibility
    public static final String EC_KEY_ALG = "EC";
    public static final String EC_CURVE = "secp256r1";
    public static final String EC_SHA256 = "SHA256withECDSA";

    // Constants required for EdDSA key pair generation
    public static final String ED_KEY_ALG = "Ed25519";
    public static final String ED_SHA512 = "Ed25519";

    // Supported signature algorithms for public certificate generation.
    public static final String RSA_MD5 = "MD5withRSA";
    public static final String RSA_SHA1 = "SHA1withRSA";
    public static final String RSA_SHA256 = "SHA256withRSA";
    public static final String RSA_SHA384 = "SHA384withRSA";
    public static final String RSA_SHA512 = "SHA512withRSA";

    public static final String RSA_KEY_ALG = "RSA";
}
