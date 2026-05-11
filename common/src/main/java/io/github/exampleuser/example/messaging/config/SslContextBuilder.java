package io.github.exampleuser.example.messaging.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

/**
 * Builds an {@link SSLContext} from a {@link MessagingConfig.SslConfig}.
 *
 * <h3>Supported configurations</h3>
 * <ul>
 *   <li><b>SSL disabled</b>: returns {@code null}; callers should fall back to system defaults.</li>
 *   <li><b>SSL enabled, no cert paths</b>: returns an SSLContext backed by the JVM's default trust store.</li>
 *   <li><b>SSL + caPath</b>: loads the CA certificate(s) from a PEM file and uses them as the trust anchor,
 *       replacing the JVM default trust store for this connection.</li>
 *   <li><b>SSL + certPath + keyPath</b>: enables mutual TLS (mTLS) using a client certificate and PKCS#8
 *       private key. Both files must be in PEM format.</li>
 *   <li><b>verifyServerCert=false</b>: installs a trust-all trust manager. Use only in development.</li>
 * </ul>
 *
 * <h3>Key format requirement</h3>
 * Private keys must be in PKCS#8 format ({@code -----BEGIN PRIVATE KEY-----}). If you have a traditional
 * PKCS#1 key ({@code -----BEGIN RSA PRIVATE KEY-----}), convert it first:
 * <pre>{@code openssl pkcs8 -topk8 -nocrypt -in key.pem -out key.pkcs8.pem}</pre>
 */
public final class SslContextBuilder {
    private SslContextBuilder() {
    }

    /**
     * Builds an {@link SSLContext} from the given SSL config, or returns {@code null} if SSL is disabled.
     *
     * @param config the SSL configuration
     * @return a configured SSLContext, or {@code null} if SSL is disabled
     * @throws GeneralSecurityException if certificate or key loading fails
     * @throws IOException              if a certificate or key file cannot be read
     */
    @Nullable
    public static SSLContext build(@NotNull MessagingConfig.SslConfig config) throws GeneralSecurityException, IOException {
        if (!config.enabled())
            return null;

        final SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(buildKeyManagers(config), buildTrustManagers(config), null);
        return ctx;
    }

    /**
     * Returns a {@link HostnameVerifier} that matches the config's {@code verifyHostname} setting.
     * When hostname verification is disabled, the returned verifier accepts any hostname. Use with care.
     *
     * @param config the SSL configuration
     * @return a hostname verifier appropriate for the config
     */
    @NotNull
    public static HostnameVerifier hostnameVerifier(@NotNull MessagingConfig.SslConfig config) {
        return config.verifyHostname()
            ? HttpsURLConnection.getDefaultHostnameVerifier()
            : (hostname, session) -> true;
    }

    @Nullable
    private static TrustManager[] buildTrustManagers(@NotNull MessagingConfig.SslConfig config) throws GeneralSecurityException, IOException {
        if (!config.verifyServerCert())
            return new TrustManager[]{TRUST_ALL};

        if (config.caPath().isEmpty())
            return null; // null = SSLContext uses the JVM's default trust store

        // Load CA certificate(s); supports single cert and chain-in-one-file (CA bundle)
        final KeyStore trustStore = emptyKeyStore();
        final CertificateFactory cf = CertificateFactory.getInstance("X.509");
        try (final FileInputStream fis = new FileInputStream(config.caPath())) {
            final Collection<? extends Certificate> certs = cf.generateCertificates(fis);
            int i = 0;
            for (final Certificate cert : certs) {
                trustStore.setCertificateEntry("ca-" + i++, cert);
            }
        }

        final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        return tmf.getTrustManagers();
    }

    @Nullable
    private static KeyManager[] buildKeyManagers(@NotNull MessagingConfig.SslConfig config) throws GeneralSecurityException, IOException {
        if (config.certPath().isEmpty() || config.keyPath().isEmpty())
            return null;

        final CertificateFactory cf = CertificateFactory.getInstance("X.509");
        final Certificate clientCert;
        try (final FileInputStream fis = new FileInputStream(config.certPath())) {
            clientCert = cf.generateCertificate(fis);
        }

        final PrivateKey privateKey = loadPrivateKey(config.keyPath());

        final KeyStore keyStore = emptyKeyStore();
        keyStore.setKeyEntry("client", privateKey, new char[0], new Certificate[]{clientCert});

        final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, new char[0]);
        return kmf.getKeyManagers();
    }

    /**
     * Loads a PKCS#8 PEM private key, trying RSA, EC, and DSA in order until one succeeds.
     * The PEM header and footer are stripped before decoding.
     */
    private static PrivateKey loadPrivateKey(String path) throws GeneralSecurityException, IOException {
        final String pem = Files.readString(Path.of(path));
        final String base64 = pem
            .replaceAll("-----[^-]+-----", "")
            .replaceAll("\\s+", "");
        final byte[] der = Base64.getDecoder().decode(base64);
        final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);

        for (final String alg : List.of("RSA", "EC", "DSA")) {
            try {
                return KeyFactory.getInstance(alg).generatePrivate(spec);
            } catch (InvalidKeySpecException ignored) {
            }
        }

        throw new InvalidKeySpecException(
            "Cannot determine algorithm for private key at '" + path + "'. " +
                "Key must be in PKCS#8 PEM format (-----BEGIN PRIVATE KEY-----). " +
                "Convert with: openssl pkcs8 -topk8 -nocrypt -in key.pem -out key.pkcs8.pem"
        );
    }

    private static KeyStore emptyKeyStore() throws GeneralSecurityException, IOException {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        return ks;
    }

    // used only when verify-server-cert is false; accepts any certificate without validation
    private static final X509TrustManager TRUST_ALL = new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };
}
