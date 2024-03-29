package it.gov.pagopa.forwarder.config;

import org.jets3t.service.security.EncryptionUtil;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
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

    private static RSAPrivateKey getPrivateKey(String pemKey) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(fromPem2Der(pemKey));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) keyFactory.generatePrivate(spec);
    }

    private static byte[] fromPem2Der(String pemKey) throws IOException {
        InputStream pemStream = new ByteArrayInputStream(pemKey.getBytes());
        return EncryptionUtil.convertRsaPemToDer(pemStream);
    }

    public static SSLContext getSSLContext(final String cert, final String key, final String password)
            throws IOException, CertificateException, KeyManagementException, UnrecoverableKeyException,
            NoSuchAlgorithmException, KeyStoreException, InvalidKeySpecException {

        /*
         * Init private key
         */
        RSAPrivateKey privateKey = getPrivateKey(key);

        /*
         * Load client certificate
         */
        InputStream certInputStream = new ByteArrayInputStream(cert.getBytes());

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate caCert = cf.generateCertificate(certInputStream);

        /*
         * Client key and certificates are sent to server, so it can authenticate the client
         */
        KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        clientKeyStore.load(null, null);
        clientKeyStore.setCertificateEntry("private-certificate", caCert);
        clientKeyStore.setKeyEntry("private-key", privateKey, password != null ? password.toCharArray() : null,
                new Certificate[] { caCert });

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(clientKeyStore, null);

        /*
         * Create SSL socket factory
         */
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(keyManagerFactory.getKeyManagers(), null, null);

        /*
          Return the newly created socket factory object
         */
        return context;
    }
}
