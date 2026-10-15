/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.keystore.mgt.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.core.util.CryptoUtil;
import org.wso2.carbon.core.util.KeyStoreUtil;
import org.wso2.carbon.keystore.mgt.KeyStoreMgtException;
import org.wso2.carbon.utils.ServerConstants;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;

/**
 * Utility to generate tenant key pairs and their self-signed certificates.
 */
public class TenantKeyPairUtil {

    private static final Log LOG = LogFactory.getLog(TenantKeyPairUtil.class);

    public static final String EC_KEY_ALGORITHM = "EC";
    public static final String EC_CURVE = "secp256r1";
    public static final String EC_SIGNATURE_ALGORITHM = "SHA256withECDSA";

    private static final String SIGNING_ALG = "Tenant.SigningAlgorithm";

    // Supported signature algorithms for the RSA public certificate generation.
    private static final String RSA_MD5 = "MD5withRSA";
    private static final String RSA_SHA1 = "SHA1withRSA";
    private static final String RSA_SHA256 = "SHA256withRSA";
    private static final String RSA_SHA384 = "SHA384withRSA";
    private static final String RSA_SHA512 = "SHA512withRSA";
    private static final String[] SIGNATURE_ALGORITHMS = new String[]{
            RSA_MD5, RSA_SHA1, RSA_SHA256, RSA_SHA384, RSA_SHA512
    };

    private TenantKeyPairUtil() {

    }

    /**
     * Generate an EC P-256 key pair with a self-signed certificate and add it to the given tenant keystore under the
     * tenant EC key alias. The keystore is not persisted.
     *
     * @param keyStore     Tenant keystore.
     * @param tenantDomain Tenant domain.
     * @param keyPassword  Password to protect the private key entry.
     * @return Generated certificate.
     * @throws KeyStoreMgtException Error when generating the key pair or adding it to the keystore.
     */
    public static X509Certificate addECKeyEntry(KeyStore keyStore, String tenantDomain, String keyPassword)
            throws KeyStoreMgtException {

        try {
            String alias = KeyStoreUtil.getTenantECKeyAlias(tenantDomain);
            CryptoUtil.getDefaultCryptoUtil();
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(EC_KEY_ALGORITHM);
            keyPairGenerator.initialize(new ECGenParameterSpec(EC_CURVE));
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            X509Certificate certificate = generateSelfSignedCertificate(tenantDomain, keyPair,
                    EC_SIGNATURE_ALGORITHM);
            keyStore.setKeyEntry(alias, keyPair.getPrivate(), keyPassword.toCharArray(),
                    new Certificate[]{certificate});
            if (LOG.isDebugEnabled()) {
                LOG.debug("Added EC key entry with alias: " + alias + " for tenant: " + tenantDomain);
            }
            return certificate;
        } catch (Exception e) {
            throw new KeyStoreMgtException("Error while generating the EC key pair for tenant: " + tenantDomain, e);
        }
    }

    /**
     * Generate a self-signed certificate for the given tenant key pair.
     *
     * @param tenantDomain       Tenant domain used as the certificate common name.
     * @param keyPair            Key pair to certify.
     * @param signatureAlgorithm Certificate signature algorithm.
     * @return Self-signed certificate.
     * @throws Exception Error when building or signing the certificate.
     */
    public static X509Certificate generateSelfSignedCertificate(String tenantDomain, KeyPair keyPair,
                                                                String signatureAlgorithm) throws Exception {

        X500Name distinguishedName = new X500Name("CN=" + tenantDomain + ", OU=None, O=None, L=None");
        Date notBefore = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30);
        Date notAfter = new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10));
        SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
        BigInteger serialNumber = new BigInteger(32, new SecureRandom());

        X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(distinguishedName, serialNumber,
                notBefore, notAfter, distinguishedName, subPubKeyInfo);
        JcaContentSignerBuilder signerBuilder =
                new JcaContentSignerBuilder(signatureAlgorithm).setProvider(getJCEProvider());
        return new JcaX509CertificateConverter().setProvider(getJCEProvider())
                .getCertificate(certificateBuilder.build(signerBuilder.build(keyPair.getPrivate())));
    }

    /**
     * Get the signature algorithm for the tenant RSA certificate from the {@code Tenant.SigningAlgorithm}
     * configuration. Falls back to {@code MD5withRSA} when the configured value is not supported.
     *
     * @return Signature algorithm for the tenant RSA certificate.
     */
    public static String getSignatureAlgorithm() {

        String algorithm = ServerConfiguration.getInstance().getFirstProperty(SIGNING_ALG);
        // Find in a list of supported signature algorithms.
        for (String supportedAlgorithm : SIGNATURE_ALGORITHMS) {
            if (supportedAlgorithm.equalsIgnoreCase(algorithm)) {
                return supportedAlgorithm;
            }
        }
        return RSA_MD5;
    }

    private static String getJCEProvider() {

        String provider = ServerConfiguration.getInstance().getFirstProperty(ServerConstants.JCE_PROVIDER);
        if (!StringUtils.isBlank(provider)) {
            return provider;
        }
        return ServerConstants.JCE_PROVIDER_BC;
    }
}
