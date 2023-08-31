/*
 *  Copyright (c) 2005-2010, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
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
package org.wso2.carbon.keystore.mgt;

import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.core.keystore.util.KeyStoreMgtUtil;
import org.wso2.carbon.core.util.CryptoUtil;
import org.wso2.carbon.keystore.mgt.util.RealmServiceHolder;
import org.wso2.carbon.core.keystore.KeyStoreAdmin;
import org.wso2.carbon.core.keystore.KeyStoreManagementException;
import org.wso2.carbon.core.keystore.dao.KeyStoreDAO;
import org.wso2.carbon.core.keystore.dao.PubCertDAO;
import org.wso2.carbon.core.keystore.dao.impl.KeyStoreDAOImpl;
import org.wso2.carbon.core.keystore.dao.impl.PubCertDAOImpl;
import org.wso2.carbon.core.keystore.model.PubCertModel;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ServerConstants;
import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * This class is used to generate a key store for a tenant and store it in the governance registry.
 * This class also provides APIs for idp-mgt component to generate a trust store with a given name.
 */
public class KeyStoreGenerator {

    private static final Log log = LogFactory.getLog(KeyStoreGenerator.class);
    private final KeyStoreDAO keyStoreDAO;
    private final PubCertDAO pubCertDAO;
    private final int tenantId;
    private final String tenantDomain;
    private String password;

    public KeyStoreGenerator(int tenantId) throws KeyStoreMgtException {

        this.tenantId = tenantId;
        this.tenantDomain = getTenantDomainName();
        this.keyStoreDAO = new KeyStoreDAOImpl();
        this.pubCertDAO = new PubCertDAOImpl();
    }


    /**
     * This method first generates the keystore, then persist it in the gov.registry of that tenant
     *
     * @throws KeyStoreMgtException Error when generating or storing the keystore
     */
    public void generateKeyStore() throws KeyStoreMgtException {
        try {
            password = generatePassword();
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, password.toCharArray());
            X509Certificate pubCert = generateKeyPair(keyStore);
            persistKeyStore(keyStore, pubCert);
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
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, password.toCharArray());
            persistTrustStore(keyStore, trustStoreName);
        } catch (Exception e) {
            String msg = "Error while instantiating a keystore";
            log.error(msg, e);
            throw new KeyStoreMgtException(msg, e);
        }
    }
    
    /**
     * This method checks the existence of a keystore.
     *
     * @param tenantId Tenant id.
     * @return True if the keystore exists, false otherwise.
     * @throws KeyStoreMgtException Will be thrown when checking the existence of the keystore.
     */
    public boolean isKeyStoreExists(int tenantId) throws KeyStoreMgtException {

        String keyStoreName = generateKSNameFromDomainName();
        boolean isKeyStoreExists = false;
        try {
            isKeyStoreExists = keyStoreDAO.getKeyStore(KeyStoreMgtUtil.getTenantUUID(tenantId), keyStoreName)
                    .isPresent();
        } catch (KeyStoreManagementException | UserStoreException e) {
            String msg = "Error while checking the existance of keystore.  ";
            log.error(msg + e.getMessage());
            throw new KeyStoreMgtException(msg, e);
        }
        return isKeyStoreExists;
    }

    /**
     * This method generates the keypair and stores it in the keystore
     *
     * @param keyStore A keystore instance
     * @return Generated public key for the tenant
     * @throws KeyStoreMgtException Error when generating key pair
     */
    private X509Certificate generateKeyPair(KeyStore keyStore) throws KeyStoreMgtException {
        try {
            CryptoUtil.getDefaultCryptoUtil();
            //generate key pair
            KeyPairGenerator keyPairGenerator = null;
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            // Common Name and alias for the generated certificate
            String commonName = "CN=" + tenantDomain + ", OU=None, O=None L=None, C=None";

            //generate certificates
            X500Name distinguishedName = new X500Name(commonName);
            X509CertInfo x509CertInfo = new X509CertInfo();

            Date notBefore = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30);
            Date notAfter = new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10));

            CertificateValidity interval = new CertificateValidity(notBefore, notAfter);
            BigInteger serialNumber = BigInteger.valueOf(new SecureRandom().nextInt());

            x509CertInfo.set(X509CertInfo.VALIDITY, interval);
            x509CertInfo.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(serialNumber));
            x509CertInfo.set(X509CertInfo.SUBJECT, distinguishedName);
            x509CertInfo.set(X509CertInfo.ISSUER, distinguishedName);
            x509CertInfo.set(X509CertInfo.KEY, new CertificateX509Key(keyPair.getPublic()));
            x509CertInfo.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));

            AlgorithmId signatureAlgoId = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid);
            x509CertInfo.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(signatureAlgoId));
            PrivateKey privateKey = keyPair.getPrivate();
            X509CertImpl x509Cert = new X509CertImpl(x509CertInfo);
            x509Cert.sign(privateKey, "MD5withRSA", getJCEProvider());

            //add private key to KS
            keyStore.setKeyEntry(tenantDomain, keyPair.getPrivate(), password.toCharArray(),
                    new java.security.cert.Certificate[]{x509Cert});
            return x509Cert;
        } catch (Exception ex) {
            String msg = "Error while generating the certificate for tenant :" +
                         tenantDomain + ".";
            log.error(msg, ex);
            throw new KeyStoreMgtException(msg, ex);
        }

    }

    /**
     * Persist the keystore in the gov.registry.
     *
     * @param keyStore      Created Keystore of the tenant.
     * @param pkCertificate Pub. key of the tenant.
     * @throws KeyStoreMgtException Exception when storing the keystore in the registry.
     */
    private void persistKeyStore(KeyStore keyStore, X509Certificate pkCertificate)
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
                    password, " ", "JKS", password);

            PubCertModel pubCertModel = new PubCertModel();
            pubCertModel.setFileNameAppender(generatePubKeyFileNameAppender());
            pubCertModel.setContent(pkCertificate.getEncoded());

            String id = pubCertDAO.addPubCert(pubCertModel);

            //associate the public key with the keystore
            keyStoreDAO.addPubCertIdToKeyStore(KeyStoreMgtUtil.getTenantUUID(tenantId), keyStoreName, id);

        } catch (KeyStoreManagementException | CertificateException | KeyStoreException | IOException |
                 NoSuchAlgorithmException | UserStoreException e) {
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
            keystoreAdmin.addTrustStore(outputStream.toByteArray(), trustStoreName, password, " ", "JKS");
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
        return (ksName + ".jks" );
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
}
