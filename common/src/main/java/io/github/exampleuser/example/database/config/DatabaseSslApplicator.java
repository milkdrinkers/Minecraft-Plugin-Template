package io.github.exampleuser.example.database.config;

import com.zaxxer.hikari.HikariConfig;
import io.github.exampleuser.example.database.exception.DatabaseInitializationException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

/**
 * Translates a {@link DatabaseConfig.SslConfig} into HikariCP data source properties for MySQL
 * and MariaDB connections.
 *
 * <p>MySQL Connector/J requires CA and client certificates as PKCS12 keystore files, so this class
 * generates them from PEM input at startup and places them in a {@code ssl/} subdirectory next to
 * the database directory. MariaDB Connector/J accepts PEM paths directly, so no conversion is
 * needed on that side.
 *
 * <p>Keystore files are always regenerated on startup to pick up certificate changes without a
 * config reload.
 */
final class DatabaseSslApplicator {
    private DatabaseSslApplicator() {
    }

    static void apply(HikariConfig hikari, DatabaseConfig config) {
        final DatabaseConfig.SslConfig ssl = config.getSslConfig();
        if (!ssl.enabled())
            return;

        switch (config.getDatabaseType()) {
            case MYSQL -> applyMysql(hikari, ssl, sslDir(config));
            case MARIADB -> applyMariaDb(hikari, ssl, sslDir(config));
            default -> {
            } // SQLite and H2 are local; SSL does not apply
        }
    }

    private static void applyMysql(HikariConfig hikari, DatabaseConfig.SslConfig ssl, Path sslDir) {
        final String sslMode = switch (ssl.mode().toLowerCase()) {
            case "verify-ca" -> "VERIFY_CA";
            case "verify-identity" -> "VERIFY_IDENTITY";
            default -> "REQUIRED";
        };
        hikari.addDataSourceProperty("sslMode", sslMode);

        if (!ssl.caPath().isEmpty()) {
            try {
                final Path trustStore = buildTrustStore(ssl.caPath(), sslDir);
                hikari.addDataSourceProperty("trustCertificateKeyStoreUrl", trustStore.toUri().toString());
                hikari.addDataSourceProperty("trustCertificateKeyStoreType", "PKCS12");
                hikari.addDataSourceProperty("trustCertificateKeyStorePassword", "");
            } catch (GeneralSecurityException | IOException e) {
                throw new DatabaseInitializationException("Failed to load CA certificate for MySQL SSL", e);
            }
        }

        if (!ssl.certPath().isEmpty() && !ssl.keyPath().isEmpty()) {
            try {
                final Path keyStore = buildKeyStore(ssl.certPath(), ssl.keyPath(), sslDir, "mysql-client.p12");
                hikari.addDataSourceProperty("clientCertificateKeyStoreUrl", keyStore.toUri().toString());
                hikari.addDataSourceProperty("clientCertificateKeyStoreType", "PKCS12");
                hikari.addDataSourceProperty("clientCertificateKeyStorePassword", "");
            } catch (GeneralSecurityException | IOException e) {
                throw new DatabaseInitializationException("Failed to load client certificate for MySQL mTLS", e);
            }
        }
    }

    private static void applyMariaDb(HikariConfig hikari, DatabaseConfig.SslConfig ssl, Path sslDir) {
        final String sslMode = switch (ssl.mode().toLowerCase()) {
            case "verify-ca" -> "VERIFY_CA";
            case "verify-identity" -> "VERIFY_FULL";
            default -> "TRUST";
        };
        hikari.addDataSourceProperty("sslMode", sslMode);

        // MariaDB Connector/J accepts PEM paths directly
        if (!ssl.caPath().isEmpty())
            hikari.addDataSourceProperty("serverSslCert", ssl.caPath());

        if (!ssl.certPath().isEmpty() && !ssl.keyPath().isEmpty()) {
            try {
                final Path keyStore = buildKeyStore(ssl.certPath(), ssl.keyPath(), sslDir, "mariadb-client.p12");
                hikari.addDataSourceProperty("keyStore", keyStore.toAbsolutePath().toString());
                hikari.addDataSourceProperty("keyStoreType", "PKCS12");
                hikari.addDataSourceProperty("keyStorePassword", "");
            } catch (GeneralSecurityException | IOException e) {
                throw new DatabaseInitializationException("Failed to load client certificate for MariaDB mTLS", e);
            }
        }
    }

    private static Path buildTrustStore(String caPath, Path sslDir) throws GeneralSecurityException, IOException {
        final CertificateFactory cf = CertificateFactory.getInstance("X.509");
        final KeyStore ks = emptyPkcs12();

        try (final FileInputStream fis = new FileInputStream(caPath)) {
            final Collection<? extends Certificate> certs = cf.generateCertificates(fis);
            int i = 0;
            for (final Certificate cert : certs)
                ks.setCertificateEntry("ca-" + i++, cert);
        }

        final Path out = sslDir.resolve("trust.p12");
        writePkcs12(ks, out);
        return out;
    }

    private static Path buildKeyStore(String certPath, String keyPath, Path sslDir, String fileName)
        throws GeneralSecurityException, IOException {

        final CertificateFactory cf = CertificateFactory.getInstance("X.509");
        final Certificate cert;
        try (final FileInputStream fis = new FileInputStream(certPath)) {
            cert = cf.generateCertificate(fis);
        }

        final PrivateKey key = loadPrivateKey(keyPath);
        final KeyStore ks = emptyPkcs12();
        ks.setKeyEntry("client", key, new char[0], new Certificate[]{cert});

        final Path out = sslDir.resolve(fileName);
        writePkcs12(ks, out);
        return out;
    }

    private static KeyStore emptyPkcs12() throws GeneralSecurityException, IOException {
        final KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        return ks;
    }

    private static void writePkcs12(KeyStore ks, Path dest) throws GeneralSecurityException, IOException {
        Files.createDirectories(dest.getParent());
        try (final FileOutputStream fos = new FileOutputStream(dest.toFile())) {
            ks.store(fos, new char[0]);
        }
    }

    private static PrivateKey loadPrivateKey(String path) throws GeneralSecurityException, IOException {
        final String pem = Files.readString(Path.of(path));
        final byte[] der = Base64.getDecoder().decode(
            pem.replaceAll("-----[^-]+-----", "").replaceAll("\\s+", "")
        );
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

    private static Path sslDir(DatabaseConfig config) {
        return config.getPath()
            .map(p -> p.resolveSibling("ssl"))
            .orElse(Path.of("ssl"));
    }
}
