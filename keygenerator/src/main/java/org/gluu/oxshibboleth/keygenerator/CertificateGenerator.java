/*
 * KeyGenerator is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */
package org.gluu.oxshibboleth.keygenerator;

import java.math.BigInteger;
import java.util.Date;
//import org.bouncycastle.asn1.x500.X500Name;
//import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
//import org.bouncycastle.asn1.x509.BasicConstraints;
//import org.bouncycastle.asn1.x509.Extension;
//import org.bouncycastle.asn1.x509.KeyUsage;
//import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
//import org.bouncycastle.cert.X509CertificateHolder;
//import org.bouncycastle.cert.X509ExtensionUtils;
//import org.bouncycastle.cert.X509v1CertificateBuilder;
//import org.bouncycastle.cert.X509v3CertificateBuilder;
//import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
//import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
//import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
//import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
//import org.bouncycastle.operator.ContentSigner;
//import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
//import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;

public class CertificateGenerator {
    
    // 1 year validity 
    static final long VALIDITY_PERIOD = 1*365*24*60*60*1000L;
    
//    /**
//     * Build a sample V1 certificate to use as a CA root certificate
//     */
//    public static X509CertificateHolder buildRootCert(AsymmetricCipherKeyPair keyPair)
//        throws Exception
//    {
//        X509v1CertificateBuilder certBldr = new X509v1CertificateBuilder(
//                new X500Name("CN=Test Root Certificate"),
//                BigInteger.valueOf(1),
//                new Date(System.currentTimeMillis()),
//                new Date(System.currentTimeMillis() + VALIDITY_PERIOD),
//                new X500Name("CN=Test Root Certificate"),
//        SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(keyPair.getPublic()));
//        DefaultDigestAlgorithmIdentifierFinder algFinder = new DefaultDigestAlgorithmIdentifierFinder();
//        AlgorithmIdentifier sigAlg = algFinder.find("SHA1withRSA");
//        AlgorithmIdentifier digAlg = algFinder.find(sigAlg);
//        ContentSigner signer = new BcRSAContentSignerBuilder(sigAlg,
//            digAlg).build(keyPair.getPrivate());
//        return certBldr.build(signer);
//    }
//    
//    /**
//     * Build a sample V3 certificate to use as an intermediate CA certificate
//     */
//    public static X509CertificateHolder buildIntermediateCert(AsymmetricKeyParameter intKey, AsymmetricKeyParameter caKey, X509CertificateHolder caCert)
//        throws Exception
//    {
//        SubjectPublicKeyInfo intKeyInfo =
//        SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(intKey);
//        X509v3CertificateBuilder certBldr = new X509v3CertificateBuilder(
//                 caCert.getSubject(),
//                 BigInteger.valueOf(1),
//                new Date(System.currentTimeMillis()),
//                 new Date(System.currentTimeMillis() + VALIDITY_PERIOD),
//                 new X500Name("CN=Test CA Certificate"),
//                 intKeyInfo);
//        
//                X509ExtensionUtils extUtils = new X509ExtensionUtils(new JcaX509ExtensionUtils.SHA1DigestCalculator());
//        certBldr.addExtension(Extension.authorityKeyIdentifier,
//                              false, extUtils.createAuthorityKeyIdentifier(caCert))
//                .addExtension(Extension.subjectKeyIdentifier,
//                              false, extUtils.createSubjectKeyIdentifier(intKeyInfo))
//                .addExtension(Extension.basicConstraints,
//                              true, new BasicConstraints(0))
//                .addExtension(Extension.keyUsage,
//                              true, new KeyUsage(KeyUsage.digitalSignature
//                                               | KeyUsage.keyCertSign
//                                               | KeyUsage.cRLSign));
//        DefaultDigestAlgorithmIdentifierFinder algFinder = new DefaultDigestAlgorithmIdentifierFinder();
//        AlgorithmIdentifier sigAlg = algFinder.find("SHA1withRSA");
//        AlgorithmIdentifier digAlg = algFinder.find(sigAlg);
//        ContentSigner signer = new BcRSAContentSignerBuilder(sigAlg, digAlg).build(caKey);
//        return certBldr.build(signer);
//    }
//    /**
//     * Build a sample V3 certificate to use as an end entity certificate
//     */
//    public static X509CertificateHolder buildEndEntityCert(AsymmetricKeyParameter entityKey, AsymmetricKeyParameter caKey, X509CertificateHolder caCert)
//    throws Exception
//    {
//        SubjectPublicKeyInfo entityKeyInfo =
//        SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(entityKey);
//        X509v3CertificateBuilder   certBldr = new X509v3CertificateBuilder(
//            caCert.getSubject(),
//            BigInteger.valueOf(1),
//            new Date(System.currentTimeMillis()),
//            new Date(System.currentTimeMillis() + VALIDITY_PERIOD),
//            new X500Name("CN=Test End Entity Certificate"),
//            entityKeyInfo);
//        X509ExtensionUtils extUtils = new X509ExtensionUtils(new JcaX509ExtensionUtils.SHA1DigestCalculator());
//        certBldr.addExtension(Extension.authorityKeyIdentifier,
//                              false, extUtils.createAuthorityKeyIdentifier(caCert))
//                .addExtension(Extension.subjectKeyIdentifier,
//                              false, extUtils.createSubjectKeyIdentifier(entityKeyInfo))
//                .addExtension(Extension.basicConstraints,
//                              true, new BasicConstraints(false))
//                .addExtension(Extension.keyUsage,
//                              true, new KeyUsage(KeyUsage.digitalSignature
//                                               | KeyUsage.keyEncipherment));
//        DefaultDigestAlgorithmIdentifierFinder algFinder = new DefaultDigestAlgorithmIdentifierFinder();
//        AlgorithmIdentifier sigAlg = algFinder.find("SHA1withRSA");
//        AlgorithmIdentifier digAlg = algFinder.find(sigAlg);
//        ContentSigner signer = new BcRSAContentSignerBuilder(sigAlg, digAlg).build(caKey);
//        return certBldr.build(signer);
//    }
}
