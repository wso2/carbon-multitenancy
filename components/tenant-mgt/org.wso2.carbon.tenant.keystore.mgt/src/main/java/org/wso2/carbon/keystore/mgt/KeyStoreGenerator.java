/*
 * Copyright (c) (2005-2023), WSO2 LLC. (http://www.wso2.com).
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
package org.wso2.carbon.keystore.mgt;

import org.apache.axiom.om.util.UUIDGenerator;
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
import org.wso2.carbon.keystore.mgt.util.RealmServiceHolder;
import org.wso2.carbon.security.keystore.KeyStoreAdmin;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ServerConstants;
import org.wso2.carbon.utils.security.KeystoreUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import static org.wso2.carbon.keystore.mgt.util.TenantKeyPairConstants.EC_KEY_ALG;
import static org.wso2.carbon.keystore.mgt.util.TenantKeyPairConstants.EC_SHA256;
import static org.wso2.carbon.keystore.mgt.util.TenantKeyPairConstants.ED_KEY_ALG;
import static org.wso2.carbon.keystore.mgt.util.TenantKeyPairConstants.ED_SHA512;
import static org.wso2.carbon.keystore.mgt.util.TenantKeyPairConstants.RSA_KEY_ALG;
import static org.wso2.carbon.keystore.mgt.util.TenantKeyPairConstants.RSA_MD5;
import static org.wso2.carbon.keystore.mgt.util.TenantKeyPairConstants.RSA_SHA1;
import static org.wso2.carbon.keystore.mgt.util.TenantKeyPairConstants.RSA_SHA256;
import static org.wso2.carbon.keystore.mgt.util.TenantKeyPairConstants.RSA_SHA384;
import static org.wso2.carbon.keystore.mgt.util.TenantKeyPairConstants.RSA_SHA512;

import static org.wso2.carbon.keystore.mgt.util.TenantKeyPairUtil.addKeyEntry;
import static org.wso2.carbon.utils.multitenancy.MultitenantConstants.TENANT_EC_KEY_SUFFIX;
import static org.wso2.carbon.utils.multitenancy.MultitenantConstants.TENANT_ED_KEY_SUFFIX;

/**
 * This class is used to generate a key store for a tenant and store it in the governance registry.
 * This class also provides APIs for idp-mgt component to generate a trust store with a given name.
 */
public class KeyStoreGenerator {

    private static Log log = LogFactory.getLog(KeyStoreGenerator.class);
    private int tenantId;
    private String tenantDomain;
    private String password;

    private static final String SIGNING_ALG = "Tenant.SigningAlgorithm";

    private static final String[] signatureAlgorithms = new String[]{
            RSA_MD5, RSA_SHA1, RSA_SHA256, RSA_SHA384, RSA_SHA512
    };

    public KeyStoreGenerator(int  tenantId) throws KeyStoreMgtException {

        this.tenantId = tenantId;
        this.tenantDomain = getTenantDomainName();
    }


    /**
     * This method first generates the keystore, then persist it in the gov.registry of that tenant
     *
     * @throws KeyStoreMgtException Error when generating or storing the keystore
     */
    public void generateKeyStore() throws KeyStoreMgtException {
        try {
            password = generatePassword();
            KeyStore keyStore = KeystoreUtils.getKeystoreInstance(KeystoreUtils.StoreFileType.defaultFileType());
            keyStore.load(null, password.toCharArray());
            // RSA based key pair entry
            X509Certificate pubCertRSA =  addKeyEntry(tenantDomain, password, keyStore, tenantDomain,
                    RSA_KEY_ALG, getSignatureAlgorithm());
            // EC based key pair entry
            addKeyEntry(tenantDomain, password, keyStore, tenantDomain + TENANT_EC_KEY_SUFFIX,
                    EC_KEY_ALG, EC_SHA256);
            // EdDSA based key pair entry
            addKeyEntry(tenantDomain, password, keyStore, tenantDomain + TENANT_ED_KEY_SUFFIX,
                    ED_KEY_ALG, ED_SHA512);
            persistKeyStore(keyStore, pubCertRSA);
        } catch (Exception e) {
            String msg = "Error while instantiating a keystore";
            log.error(msg, e);
            throw new KeyStoreMgtException(msg, e);
        }
    }

    /**
     * This method first generates the keystore, then persist it in the gov.registry of that tenant
     *
     * @throws KeyStoreMgtException Error when generating or storing the keystore
     */
    public void generateTrustStore(String trustStoreName) throws KeyStoreMgtException {
        try {
            password = generatePassword();
            KeyStore keyStore = KeystoreUtils.getKeystoreInstance(KeystoreUtils.getTrustStoreFileType());
            keyStore.load(null, password.toCharArray());
            persistTrustStore(keyStore, trustStoreName);
        } catch (Exception e) {
            String msg = "Error while instantiating a keystore";
            log.error(msg, e);
            throw new KeyStoreMgtException(msg, e);
        }
    }
    
    /**
     * This method checks the existance of a keystore
     *
     * @param tenantId
     * @return
     * @throws KeyStoreMgtException
     */
    public boolean isKeyStoreExists(int tenantId) throws KeyStoreMgtException{

        String keyStoreName = generateKSNameFromDomainName();
        try {
            KeyStoreAdmin keystoreAdmin = new KeyStoreAdmin(tenantId);
            KeyStore keyStore = keystoreAdmin.getKeyStore(keyStoreName);
            if (keyStore != null) {
                return true;
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Error while checking the existence of keystore.", e);
            }
        }
        return false;
    }



    /**
     * Persist the keystore in the gov.registry
     *
     * @param keyStore created Keystore of the tenant
     * @param PKCertificate pub. key of the tenant
     * @throws KeyStoreMgtException Exception when storing the keystore in the registry
     */
    private void persistKeyStore(KeyStore keyStore, X509Certificate PKCertificate)
            throws KeyStoreMgtException {

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            keyStore.store(outputStream, password.toCharArray());
            outputStream.flush();
            outputStream.close();

            String keyStoreName = generateKSNameFromDomainName();
            // Use the keystore using the keystore admin
            KeyStoreAdmin keystoreAdmin = new KeyStoreAdmin(tenantId);
            keystoreAdmin.addKeyStore(outputStream.toByteArray(), keyStoreName,
                                      password, " ", KeystoreUtils.StoreFileType.defaultFileType(), password);
        } catch (Exception e) {
            String msg = "Error when processing keystore/pub. cert to be stored in registry";
            log.error(msg, e);
            throw new KeyStoreMgtException(msg, e);
        }
    }

    /**
     * Persist the trust store in the gov.registry
     *
     * @param trustStore created trust store of the tenant
     * @throws KeyStoreMgtException Exception when storing the trust store in the registry
     */
    private void persistTrustStore(KeyStore trustStore, String trustStoreName) throws KeyStoreMgtException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            trustStore.store(outputStream, password.toCharArray());
            outputStream.flush();
            outputStream.close();

            KeyStoreAdmin keystoreAdmin = new KeyStoreAdmin(tenantId);
            keystoreAdmin.addTrustStore(outputStream.toByteArray(), trustStoreName, password, " ",
                    KeystoreUtils.getTrustStoreFileType());
        } catch (Exception e) {
            String msg = "Error when processing keystore/pub. cert to be stored in registry";
            log.error(msg, e);
            throw new KeyStoreMgtException(msg, e);
        }
    }

    /**
     * This method is used to generate a random password for the generated keystore
     *
     * @return generated password
     */
    private String generatePassword() {
        SecureRandom random = new SecureRandom();
        String randString = new BigInteger(130, random).toString(12);
        return randString.substring(randString.length() - 10, randString.length());
    }

    /**
     * This method is used to generate a file name appender for the pub. cert, e.g.
     * example-com-343743.cert
     * @return generated string to be used as a file name appender
     */
    private String generatePubKeyFileNameAppender(){
        String uuid = UUIDGenerator.getUUID();
        return uuid.substring(uuid.length() - 6, uuid.length()-1);
    }

    /**
     * This method generates the key store file name from the Domain Name
     * @return
     */
    private String generateKSNameFromDomainName(){
        String ksName = tenantDomain.trim().replace(".", "-");
        return (ksName + KeystoreUtils.getExtensionByFileType(KeystoreUtils.StoreFileType.defaultFileType()));
    }

    private String getTenantDomainName() throws KeyStoreMgtException {
        RealmService realmService = RealmServiceHolder.getRealmService();
        if (realmService == null) {
            String msg = "Error in getting the domain name, realm service is null.";
            log.error(msg);
            throw new KeyStoreMgtException(msg);
        }
        try {
            return realmService.getTenantManager().getDomain(tenantId);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            String msg = "Error in getting the domain name for the tenant id: " + tenantId;
            log.error(msg, e);
            throw new KeyStoreMgtException(msg, e);
        }
    }

    private static String getJCEProvider() {

        String provider = ServerConfiguration.getInstance().getFirstProperty(ServerConstants.JCE_PROVIDER);
        if (!StringUtils.isBlank(provider)) {
            return provider;
        }
        return ServerConstants.JCE_PROVIDER_BC;
    }

    private static String getSignatureAlgorithm() {

        String algorithm = ServerConfiguration.getInstance().getFirstProperty(SIGNING_ALG);
        // Find in a list of supported signature algorithms.
        for (String supportedAlgorithm : signatureAlgorithms) {
            if (supportedAlgorithm.equalsIgnoreCase(algorithm)) {
                return supportedAlgorithm;
            }
        }
        return RSA_MD5;
    }
}
