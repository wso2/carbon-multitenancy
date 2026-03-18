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
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.wso2.carbon.core.util.CryptoUtil;
import org.wso2.carbon.keystore.mgt.KeyStoreMgtException;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
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

        if(LOG.isDebugEnabled()){
            LOG.info("Adding key entry for tenant: " + tenantDomain + " with alias: " + alias
                    + " and key type: " + keyType);
        }

        try {
            CryptoUtil.getDefaultCryptoUtil();
            KeyPairGenerator kpg;

            if (EC_KEY_ALG.equals(keyType)) {
                kpg = KeyPairGenerator.getInstance(EC_KEY_ALG);
                kpg.initialize(new ECGenParameterSpec(EC_CURVE));
            } else if (RSA_KEY_ALG.equals(keyType)) {
                kpg = KeyPairGenerator.getInstance(RSA_KEY_ALG);
                kpg.initialize(2048);
            } else {
                throw new IllegalArgumentException("Unsupported key type: " + keyType);
            }
            KeyPair keyPair = Objects.requireNonNull(kpg).generateKeyPair();
            X509Certificate cert = generateCertificate(tenantDomain, keyPair, sigAlgId);
            keyStore.setKeyEntry(alias, keyPair.getPrivate(), ksPassword.toCharArray(),
                    new Certificate[]{cert});
            return cert;
        } catch (Exception e) {
            String errorMsg = "Error while generating the certificate for tenant :" +
                    tenantDomain + ".";
            LOG.error(errorMsg, e);
            throw new KeyStoreMgtException(errorMsg, e);
        }
    }

    private static X509Certificate generateCertificate(String tenantDomain, KeyPair keyPair, String algorithmId)
            throws CertificateException, OperatorCreationException {

        String commonName = "CN=" + tenantDomain + ", OU=None, O=None, L=None, C=None";
        //generate certificate
        X500Name distinguishedName = new X500Name(commonName);

        Date notBefore = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30);
        Date notAfter = new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10));

        SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
        BigInteger serialNumber = new BigInteger(32, new SecureRandom());

        X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(distinguishedName, serialNumber,
                notBefore, notAfter, distinguishedName, subPubKeyInfo);
        JcaContentSignerBuilder signerBuilder =
                new JcaContentSignerBuilder(algorithmId).setProvider(getJCEProvider());

        return new JcaX509CertificateConverter().setProvider(getJCEProvider())
                .getCertificate(certificateBuilder.build(signerBuilder.build(keyPair.getPrivate())));
    }
}
