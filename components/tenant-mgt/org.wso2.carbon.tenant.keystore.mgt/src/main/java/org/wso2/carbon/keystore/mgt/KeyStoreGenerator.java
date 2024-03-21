/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.wso2.carbon.core.RegistryResources;
import org.wso2.carbon.core.util.CryptoUtil;
import org.wso2.carbon.keystore.mgt.util.RealmServiceHolder;
import org.wso2.carbon.keystore.mgt.util.RegistryServiceHolder;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.security.SecurityConstants;
import org.wso2.carbon.security.keystore.KeyStoreAdmin;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ServerConstants;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * This class is used to generate a key store for a tenant and store it in the governance registry.
 * This class also provides APIs for idp-mgt component to generate a trust store with a given name.
 */
public class KeyStoreGenerator {

    private static Log log = LogFactory.getLog(KeyStoreGenerator.class);
    private UserRegistry govRegistry;
    private int tenantId;
    private String tenantDomain;
    private String password;

    private static final String SIGNATURE_ALGORITHM = "MD5WithRSAEncryption";
    private static final String KEY_GENERATION_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;

    public KeyStoreGenerator(int  tenantId) throws KeyStoreMgtException {
        try {
            this.tenantId = tenantId;
            this.tenantDomain = getTenantDomainName();
            this.govRegistry = RegistryServiceHolder.getRegistryService().
                    getGovernanceSystemRegistry(tenantId);
            if(govRegistry == null){
                log.error("Governance registry instance is null");
                throw new KeyStoreMgtException("Governance registry instance is null");
            }
        } catch (RegistryException e) {
            String errorMsg = "Error while obtaining the governance registry for tenant : " +
                      tenantId;
            log.error(errorMsg, e);
            throw new KeyStoreMgtException(errorMsg, e);
        }
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
     * This method checks the existance of a keystore
     * 
     * @param tenantId
     * @return
     * @throws KeyStoreMgtException
     */
    public boolean isKeyStoreExists(int tenantId) throws KeyStoreMgtException{
    	String keyStoreName = generateKSNameFromDomainName();
    	boolean isKeyStoreExists = false;
    	try {
    		isKeyStoreExists = govRegistry.resourceExists(RegistryResources.SecurityManagement.KEY_STORES + "/" + keyStoreName);
		} catch (RegistryException e) {
			String msg = "Error while checking the existance of keystore.  ";
            log.error(msg + e.getMessage());
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

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_GENERATION_ALGORITHM);
            keyPairGenerator.initialize(KEY_SIZE);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(SIGNATURE_ALGORITHM);
            AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);

            // Certificate details
            Date notBefore = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30);
            Date notAfter = new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10));
            BigInteger serialNumber = BigInteger.valueOf(new SecureRandom().nextInt());
            String commonName = "CN=" + tenantDomain + ", OU=None, O=None, L=None, C=None";
            X509Certificate certificate = null;
            if (ServerConstants.BOUNCY_CASTLE_FIPS_PROVIDER_IDENTIFIER.equals(getPreferredJceProviderIdentifier())) {
                X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(new X500Name(commonName),
                                                                                              serialNumber, notBefore,
                                                                                              notAfter,
                                                                                              new X500Name(commonName),
                                                                                              keyPair.getPublic());
                ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).build(keyPair.getPrivate());
                X509CertificateHolder certificateHolder = certificateBuilder.build(signer);
                certificate = new JcaX509CertificateConverter().setProvider(
                        ServerConstants.BOUNCY_CASTLE_FIPS_PROVIDER_IDENTIFIER).getCertificate(certificateHolder);
            } else {
                AsymmetricKeyParameter privateKeyAsymKeyParam = PrivateKeyFactory.createKey(
                        keyPair.getPrivate().getEncoded());
                SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());

                ContentSigner sigGen = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(privateKeyAsymKeyParam);
                X509v3CertificateBuilder v3CertBuilder = new X509v3CertificateBuilder(new X500Name(commonName),
                                                                                      serialNumber, notBefore, notAfter,
                                                                                      new X500Name(commonName),
                                                                                      subPubKeyInfo);
                X509CertificateHolder certificateHolder = v3CertBuilder.build(sigGen);
                certificate = new JcaX509CertificateConverter().setProvider(
                        ServerConstants.BOUNCY_CASTLE_PROVIDER_IDENTIFIER).getCertificate(certificateHolder);
            }
            keyStore.setKeyEntry(tenantDomain, keyPair.getPrivate(), password.toCharArray(),
                                 new java.security.cert.Certificate[] { certificate });
            return certificate;
        } catch (Exception ex) {
            String msg = "Error while generating the certificate for tenant :" +
                         tenantDomain + ".";
            log.error(msg, ex);
            throw new KeyStoreMgtException(msg, ex);
        }

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
            KeyStoreAdmin keystoreAdmin = new KeyStoreAdmin(tenantId, govRegistry);
            keystoreAdmin.addKeyStore(outputStream.toByteArray(), keyStoreName,
                                      password, " ", "JKS", password);
            
            //Create the pub. key resource
            Resource pubKeyResource = govRegistry.newResource();
            pubKeyResource.setContent(PKCertificate.getEncoded());
            pubKeyResource.addProperty(SecurityConstants.PROP_TENANT_PUB_KEY_FILE_NAME_APPENDER,
                                       generatePubKeyFileNameAppender());

            govRegistry.put(RegistryResources.SecurityManagement.TENANT_PUBKEY_RESOURCE, pubKeyResource);

            //associate the public key with the keystore
            govRegistry.addAssociation(RegistryResources.SecurityManagement.KEY_STORES + "/" + keyStoreName,
                                       RegistryResources.SecurityManagement.TENANT_PUBKEY_RESOURCE,
                                       SecurityConstants.ASSOCIATION_TENANT_KS_PUB_KEY);

        } catch (RegistryException e) {
            String msg = "Error when writing the keystore/pub.cert to registry";
            log.error(msg, e);
            throw new KeyStoreMgtException(msg, e);
        }
        catch (Exception e) {
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

            KeyStoreAdmin keystoreAdmin = new KeyStoreAdmin(tenantId, govRegistry);
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

    /**
     * This method returns the preferred JCE provider identifier to be used.
     *
     * @return jce provider identifier name
     */
    private String getPreferredJceProviderIdentifier() {
        String provider = System.getProperty(ServerConstants.JCE_PROVIDER_PARAMETER);
        if (ServerConstants.BOUNCY_CASTLE_FIPS_PROVIDER_IDENTIFIER.equalsIgnoreCase(provider)) {
            return ServerConstants.BOUNCY_CASTLE_FIPS_PROVIDER_IDENTIFIER;
        }
        return ServerConstants.BOUNCY_CASTLE_PROVIDER_IDENTIFIER;
    }
}
