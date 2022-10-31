package it.gov.pagopa.microservice.config;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class SslConfig {

    private SslConfig() {

        throw new IllegalStateException();
    }

    private static RSAPrivateKey getPrivateKey(String filename) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        DataInputStream dis = new DataInputStream(fis);

        byte[] keyBytes = new byte[(int) file.length()];
        dis.readFully(keyBytes);
        dis.close();

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(spec);

        return privateKey;

    }

    public static SSLContext getSSLSocketFactory(final String cert, final String key, final String password)
            throws IOException, CertificateException, KeyManagementException, UnrecoverableKeyException,
            NoSuchAlgorithmException, KeyStoreException, InvalidKeySpecException {

        /**
         * Init private key
         */
        RSAPrivateKey privateKey = getPrivateKey("/Users/pasqualespica/my_data/__TEMP/nodejs-ssl-mutual-authentication/certs2/client-key2.der");

        /**
         * Load client certificate
         */
        InputStream certInputStream = new ByteArrayInputStream(cert.getBytes());

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate caCert = cf.generateCertificate(certInputStream);

        /**
         * Client key and certificates are sent to server so it can authenticate the
         * client
         */
        KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        clientKeyStore.load(null, null);
        clientKeyStore.setCertificateEntry("private-certificate", caCert);
        clientKeyStore.setKeyEntry("private-key", privateKey, password != null ? password.toCharArray() : null,
                new Certificate[] { caCert });

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(clientKeyStore, null);

        /**
         * Create SSL socket factory
         */
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(keyManagerFactory.getKeyManagers(), null, null);

        /**
         * Return the newly created socket factory object
         */
        return context;
    }
}
