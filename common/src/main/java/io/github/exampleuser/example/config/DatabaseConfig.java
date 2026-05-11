package io.github.exampleuser.example.config;

import io.github.exampleuser.example.config.exception.ConfigValidationException;
import io.github.exampleuser.example.config.migration.Migration;
import io.github.exampleuser.example.database.handler.DatabaseType;
import io.github.exampleuser.example.messaging.broker.BrokerType;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.interfaces.meta.Exclude;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigSerializable
public class DatabaseConfig implements VersionedConfig {
    @Comment("Do not change this value!")
    public int configVersion = 1;

    @Override
    @Exclude
    public int configVersion() {
        return configVersion;
    }

    @Override
    @Exclude
    public @NotNull Map<Integer, Migration> migrations() {
        return Map.of();
    }

    @Override
    @Exclude
    public void validate() throws ConfigValidationException {
        if (messaging.pollingInterval >= messaging.cleanupInterval) {
            throw new ConfigValidationException(
                "messaging.polling-interval (" + messaging.pollingInterval + "ms) " +
                    "must be less than messaging.cleanup-interval (" + messaging.cleanupInterval + "ms)"
            );
        }
    }

    @Comment("Database Settings")
    public Database database = new Database();

    @ConfigSerializable
    public static class Database {
        @Comment("Available types: \"sqlite\", \"h2\", \"mysql\", \"mariadb\"")
        public DatabaseType type = DatabaseType.SQLITE;
        public String tablePrefix = "example_";

        @Comment("Authentication")
        public String host = "localhost";
        public int port = 3306;
        public String database = "database_name";
        public String username = "root";
        public String password = "";

        @Comment("Advanced settings (Please don't touch unless you know what you're doing!)")
        public Advanced advanced = new Advanced();

        @ConfigSerializable
        public static class Advanced {
            @Comment("Should we try to repair broken migrations to the database")
            public boolean repair = false;

            @Comment("Configure the HikariCP connection pool")
            public ConnectionPool connectionPool = new ConnectionPool();

            @Comment("SSL/TLS configuration for the database connection.\nOnly applies to MySQL and MariaDB; ignored for SQLite and H2.")
            public SSL ssl = new SSL();

            @ConfigSerializable
            public static class SSL {
                @Comment("Enable TLS/SSL for the database connection")
                public boolean enabled = false;

                @Comment(
                    "How to verify the server's identity:\n" +
                        "  \"require\"          encrypt without validating the server certificate\n" +
                        "  \"verify-ca\"        encrypt and verify the server certificate against the configured CA\n" +
                        "  \"verify-identity\"  additionally check that the hostname matches the certificate\n" +
                        "Note: when this section is enabled, any useSSL property in connection-properties is ignored."
                )
                public String mode = "require";

                @Comment("Path to the Certificate Authority certificate in PEM format.\nRequired when mode is \"verify-ca\" or \"verify-identity\".")
                public String caPath = "";

                @Comment("Path to the client certificate in PEM format.\nOnly required for mutual TLS (mTLS).")
                public String certPath = "";

                @Comment(
                    "Path to the client private key in PKCS#8 PEM format.\n" +
                        "Only required for mutual TLS alongside cert-path.\n" +
                        "Convert a PKCS#1 key with: openssl pkcs8 -topk8 -nocrypt -in key.pem -out key.pkcs8.pem"
                )
                public String keyPath = "";
            }

            @ConfigSerializable
            public static class ConnectionPool {
                public int maxpoolsize = 10;
                public int minidle = 10;
                public long maxlifetime = 180000;
                public long keepalivetime = 60000;
                public long connectiontimeout = 20000;
            }

            @Comment("A list of connection parameters, you can include more by adding them on a new line")
            public Map<String, Object> connectionProperties = defaultValues();

            private Map<String, Object> defaultValues() {
                final Map<String, Object> map = new HashMap<>();
                map.put("useSSL", false);
                map.put("cachePrepStmts", true);
                map.put("prepStmtCacheSize", 250);
                map.put("prepStmtCacheSqlLimit", 2048);
                map.put("useServerPrepStmts", true);
                map.put("useLocalSessionState", true);
                map.put("rewriteBatchedStatements", true);
                map.put("cacheResultSetMetadata", true);
                map.put("cacheServerConfiguration", true);
                map.put("elideSetAutoCommits", true);
                map.put("maintainTimeStats", false);
                return map;
            }
        }
    }

    @Comment("Message Broker Settings")
    public Messaging messaging = new Messaging();

    @ConfigSerializable
    public static class Messaging {
        @Comment("Enable or disable the message broker\nOnly required when running on a server network (BungeeCord, Velocity, etc.)")
        public boolean enabled = false;

        @Comment("How often to poll for new messages (in milliseconds)\nMust be less than cleanup-interval, ideally 1/3 or less")
        public int pollingInterval = 1000;

        @Comment("How often to clean up old messages (in milliseconds)\nMust be at least 3x the polling-interval")
        public int cleanupInterval = 30000;

        @Comment("Available broker types: \"database\", \"plugin\", \"redis\", \"rabbitmq\", \"nats\"")
        public BrokerType type = BrokerType.DATABASE;

        @Comment("One or more broker addresses. A plain string works for a single broker; use a YAML list for clusters:\n  - node1:6379\n  - node2:6379\nDefault port varies by broker: Redis 6379, RabbitMQ 5672, NATS 4222")
        public List<String> addresses = List.of("localhost:6379");

        @Comment("Authentication credentials (used with auth-method: \"password\")")
        public String username = "";
        public String password = "";

        @Comment("Advanced broker configuration")
        public Advanced advanced = new Advanced();

        @ConfigSerializable
        public static class Advanced {
            @Comment(
                "Authentication method. Supported values per broker:\n" +
                    "  Redis:    \"password\" (username + password or password-only)\n" +
                    "            \"token\"    (Redis AUTH token)\n" +
                    "  RabbitMQ: \"password\" (PLAIN mechanism)\n" +
                    "            \"token\"    (token as password)\n" +
                    "            \"certificate\" (mutual TLS, no credentials needed; cert CN maps to a RabbitMQ user)\n" +
                    "  NATS:     \"password\" (username + password)\n" +
                    "            \"token\"    (auth token)\n" +
                    "            \"nkey\"     (NKey seed file, optionally paired with a JWT file)\n" +
                    "            \"credentials\" (combined JWT + NKey .creds file)"
            )
            public String authMethod = "password";

            @Comment("Auth token used with auth-method: \"token\" (JWT, API key, Redis AUTH token, etc.)")
            public String authToken = "";

            @Comment("SSL/TLS configuration")
            public SSL ssl = new SSL();

            @ConfigSerializable
            public static class SSL {
                @Comment("Enable TLS/SSL for the broker connection")
                public boolean enabled = false;

                @Comment("Path to the client certificate in PEM format (.crt or .pem)\nRequired for mutual TLS (auth-method: \"certificate\") and optional for TLS identity")
                public String certPath = "";

                @Comment(
                    "Path to the client private key in PKCS#8 PEM format (.pem)\n" +
                        "Required alongside cert-path for mutual TLS.\n" +
                        "If you have a PKCS#1 key (-----BEGIN RSA PRIVATE KEY-----), convert it first:\n" +
                        "  openssl pkcs8 -topk8 -nocrypt -in key.pem -out key.pkcs8.pem"
                )
                public String keyPath = "";

                @Comment("Path to the Certificate Authority file in PEM format (.crt or .pem)\nCA bundles (multiple certificates in one file) are supported.\nLeave empty to use the JVM's built-in trust store.")
                public String caPath = "";

                @Comment("Verify the server's TLS certificate against the trust store\nDisable only in development, not in production")
                public boolean verifyServerCert = true;

                @Comment("Verify that the server hostname matches the certificate CN/SAN\nDisable only when connecting via IP address or with a wildcard certificate")
                public boolean verifyHostname = true;
            }

            @Comment("RabbitMQ-specific settings")
            public RabbitMQ rabbitmq = new RabbitMQ();

            @ConfigSerializable
            public static class RabbitMQ {
                @Comment("The RabbitMQ virtual host to connect to")
                public String virtualHost = "/";
            }

            @Comment("NATS-specific settings")
            public Nats nats = new Nats();

            @ConfigSerializable
            public static class Nats {
                @Comment("Path to the NKey seed file, used with auth-method: \"nkey\"")
                public String nkeySeedPath = "";

                @Comment("Path to the JWT token file, used with auth-method: \"nkey\" alongside nkey-seed-path.\nOmit for challenge-only NKey auth (no user JWT).")
                public String jwtFilePath = "";

                @Comment("Path to a combined credentials file (.creds) containing both JWT and NKey\nUsed with auth-method: \"credentials\"")
                public String credentialsPath = "";
            }
        }
    }
}
