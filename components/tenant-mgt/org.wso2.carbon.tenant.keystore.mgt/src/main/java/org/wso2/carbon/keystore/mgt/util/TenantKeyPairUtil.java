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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.util.CryptoUtil;
import org.wso2.carbon.keystore.mgt.KeyStoreMgtException;
import sun.security.x509.AlgorithmId;

import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertInfo;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;
import java.util.Objects;

import static org.wso2.carbon.core.util.CryptoUtil.getJCEProvider;
import static org.wso2.carbon.keystore.mgt.util.TenantKeyPairConstants.EC_CURVE;
import static org.wso2.carbon.keystore.mgt.util.TenantKeyPairConstants.EC_KEY_ALG;
import static org.wso2.carbon.keystore.mgt.util.TenantKeyPairConstants.ED_KEY_ALG;
import static org.wso2.carbon.keystore.mgt.util.TenantKeyPairConstants.RSA_KEY_ALG;

/**
 * Utility for provisioning and validating tenant specific key pairs required.
 */
public class TenantKeyPairUtil {

    private static final Log LOG = LogFactory.getLog(TenantKeyPairUtil.class);

    private TenantKeyPairUtil() {
    }

    /**
     * Generate and store key pair with self-signed certificate in tenant keystore.
     *
     * @param tenantDomain tenant domain
     * @param ksPassword   keystore password
     * @param keyStore     tenant keystore
     * @param alias        key alias
     * @param keyType      key type
     * @param sigAlgId     signature algorithm ID
     * @return generated certificate
     * @throws KeyStoreMgtException keystore exception
     */
    public static X509Certificate addKeyEntry(String tenantDomain, String ksPassword, KeyStore keyStore,
                                              String alias, String keyType, String sigAlgId)
            throws KeyStoreMgtException {

        try {
            CryptoUtil.getDefaultCryptoUtil();
            KeyPairGenerator kpg = null;
            KeyPair keyPair;

            if (EC_KEY_ALG.equals(keyType)) {
                kpg = KeyPairGenerator.getInstance(EC_KEY_ALG, getJCEProvider());
                kpg.initialize(new ECGenParameterSpec(EC_CURVE));
            } else if (RSA_KEY_ALG.equals(keyType)) {
                kpg = KeyPairGenerator.getInstance(RSA_KEY_ALG);
                kpg.initialize(2048);
            } else if (ED_KEY_ALG.equals(keyType)) {
                kpg = KeyPairGenerator.getInstance(ED_KEY_ALG, getJCEProvider());
                // Ed25519 doesn't need initialization parameters
            }
            keyPair = Objects.requireNonNull(kpg).generateKeyPair();
            X509CertImpl cert = generateCertificate(tenantDomain, keyPair, sigAlgId);

            cert.sign(keyPair.getPrivate(), sigAlgId, getJCEProvider());
            // Add private key to Keystore
            keyStore.setKeyEntry(alias, keyPair.getPrivate(), ksPassword.toCharArray(),
                    new Certificate[] { cert });
            return cert;
        } catch (Exception e) {
            String errorMsg = "Error while generating the certificate for tenant :" +
                    tenantDomain + ".";
            LOG.error(errorMsg, e);
            throw new KeyStoreMgtException(errorMsg, e);
        }
    }

    private static X509CertImpl generateCertificate(String tenantDomain, KeyPair keyPair, String algorithmId)
            throws IOException, CertificateException, NoSuchAlgorithmException {

        String commonName = "CN=" + tenantDomain + ", OU=None, O=None, L=None, C=None";
        X500Name dn = new X500Name(commonName);

        X509CertInfo info = new X509CertInfo();

        Date notBefore = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30);
        Date notAfter  = new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10));

        CertificateValidity interval = new CertificateValidity(notBefore, notAfter);
        BigInteger serialNumber = BigInteger.valueOf(Math.abs(new SecureRandom().nextInt()));

        info.set(X509CertInfo.VALIDITY, interval);
        info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(serialNumber));
        info.set(X509CertInfo.SUBJECT, dn);
        info.set(X509CertInfo.ISSUER, dn);
        info.set(X509CertInfo.KEY, new CertificateX509Key(keyPair.getPublic()));
        info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
        AlgorithmId signatureAlgoId = AlgorithmId.get(algorithmId);
        info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(signatureAlgoId));

        return new X509CertImpl(info);
    }
}
