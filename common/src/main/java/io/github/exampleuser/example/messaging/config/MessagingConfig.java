package io.github.exampleuser.example.messaging.config;

import io.github.exampleuser.example.config.DatabaseConfig;
import io.github.exampleuser.example.messaging.broker.BrokerType;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runtime configuration snapshot for the messaging system. Built once at startup from
 * {@link DatabaseConfig} via {@link #fromConfig(DatabaseConfig)}, or constructed directly
 * in tests via {@link #builder()}.
 */
@SuppressWarnings("unused")
public record MessagingConfig(
    boolean enabled,
    long pollingInterval,
    long cleanupInterval,
    BrokerType brokerType,
    AddressList addressList,
    String username,
    String password,
    String authMethod,
    String authToken,
    SslConfig ssl,
    RabbitMqConfig rabbitMq,
    NatsConfig nats
) {
    /**
     * TLS/SSL settings. When {@link #enabled} is {@code false} the remaining fields are ignored.
     *
     * <p>Certificate files must be in PEM format. The private key must be PKCS#8
     * ({@code -----BEGIN PRIVATE KEY-----}), not the traditional PKCS#1 format
     * ({@code -----BEGIN RSA PRIVATE KEY-----}). Convert with:
     * <pre>{@code openssl pkcs8 -topk8 -nocrypt -in key.pem -out key.pkcs8.pem}</pre>
     */
    public record SslConfig(
        boolean enabled,
        String certPath,
        String keyPath,
        String caPath,
        boolean verifyServerCert,
        boolean verifyHostname
    ) {
        static final SslConfig DISABLED = new SslConfig(false, "", "", "", true, true);
    }

    public record RabbitMqConfig(
        String virtualHost
    ) {
    }

    public record NatsConfig(
        String nkeySeedPath,
        String jwtFilePath,
        String credentialsPath
    ) {
    }

    /**
     * Builds a {@link MessagingConfig} from the plugin's {@link DatabaseConfig}.
     */
    public static MessagingConfig fromConfig(DatabaseConfig cfg) {
        return MessagingConfig.builder()
            .withEnabled(cfg.messaging.enabled)
            .withPollingInterval(cfg.messaging.pollingInterval)
            .withCleanupInterval(cfg.messaging.cleanupInterval)
            .withBroker(cfg.messaging.type)
            .withAddresses(cfg.messaging.addresses)
            .withUsername(cfg.messaging.username)
            .withPassword(cfg.messaging.password)
            .withAuthMethod(cfg.messaging.advanced.authMethod)
            .withAuthToken(cfg.messaging.advanced.authToken)
            .withSSL(
                cfg.messaging.advanced.ssl.enabled,
                cfg.messaging.advanced.ssl.certPath,
                cfg.messaging.advanced.ssl.keyPath,
                cfg.messaging.advanced.ssl.caPath,
                cfg.messaging.advanced.ssl.verifyServerCert,
                cfg.messaging.advanced.ssl.verifyHostname
            )
            .withRabbitMq(cfg.messaging.advanced.rabbitmq.virtualHost)
            .withNats(
                cfg.messaging.advanced.nats.nkeySeedPath,
                cfg.messaging.advanced.nats.jwtFilePath,
                cfg.messaging.advanced.nats.credentialsPath
            )
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private static final Logger LOGGER = LoggerFactory.getLogger(MessagingConfig.class);

        private Builder() {
        }

        private @Nullable Boolean enabled;
        private @Nullable Long pollingInterval;
        private @Nullable Long cleanupInterval;
        private @Nullable BrokerType broker;
        private @Nullable AddressList addressList;
        private @Nullable String username;
        private @Nullable String password;
        private @Nullable String authMethod;
        private @Nullable String authToken;
        private @Nullable SslConfig ssl;
        private @Nullable RabbitMqConfig rabbitMq;
        private @Nullable NatsConfig nats;

        public Builder withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder withPollingInterval(long pollingInterval) {
            this.pollingInterval = pollingInterval;
            return this;
        }

        public Builder withCleanupInterval(long cleanupInterval) {
            this.cleanupInterval = cleanupInterval;
            return this;
        }

        public Builder withBroker(BrokerType broker) {
            this.broker = broker;
            return this;
        }

        public Builder withAddresses(Object address) {
            this.addressList = AddressList.from(address);
            return this;
        }

        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder withAuthMethod(String authMethod) {
            this.authMethod = authMethod;
            return this;
        }

        public Builder withAuthToken(String authToken) {
            this.authToken = authToken;
            return this;
        }

        /**
         * Convenience overload for tests that only need to toggle SSL on or off
         * without providing certificate paths.
         */
        public Builder withSSL(boolean enabled) {
            this.ssl = enabled ? new SslConfig(true, "", "", "", true, true) : SslConfig.DISABLED;
            return this;
        }

        public Builder withSSL(boolean enabled, String certPath, String keyPath,
                               String caPath, boolean verifyServerCert, boolean verifyHostname) {
            this.ssl = new SslConfig(enabled, certPath, keyPath, caPath, verifyServerCert, verifyHostname);
            return this;
        }

        public Builder withRabbitMq(String virtualHost) {
            this.rabbitMq = new RabbitMqConfig(virtualHost);
            return this;
        }

        public Builder withNats(String nkeySeedPath, String jwtFilePath, String credentialsPath) {
            this.nats = new NatsConfig(nkeySeedPath, jwtFilePath, credentialsPath);
            return this;
        }

        public MessagingConfig build() {
            if (enabled == null)
                enabled = false;

            if (pollingInterval == null)
                pollingInterval = 1000L;

            if (cleanupInterval == null)
                cleanupInterval = 30000L;

            if (cleanupInterval < 10000L) {
                LOGGER.warn("Messaging \"cleanup-interval\" was set to less than the minimum 10000ms ({}ms), using default.", cleanupInterval);
                cleanupInterval = 10000L;
            }

            if (pollingInterval > cleanupInterval / 3) {
                LOGGER.warn("Messaging \"polling-interval\" was set to more than the maximum \"cleanup-interval\" divided by three ({}ms), using default.", pollingInterval);
                pollingInterval = cleanupInterval / 3;
            }

            BrokerType brokerType = broker;
            if (brokerType == null) {
                LOGGER.warn("Messaging \"type\" is invalid, using default \"{}\".", BrokerType.DATABASE.getName());
                brokerType = BrokerType.DATABASE;
            }

            if (addressList == null)
                addressList = AddressList.from(null);

            if (username == null)
                username = "";

            if (password == null)
                password = "";

            if (authMethod == null)
                authMethod = "password";

            if (authToken == null)
                authToken = "";

            if (ssl == null)
                ssl = SslConfig.DISABLED;

            if (rabbitMq == null)
                rabbitMq = new RabbitMqConfig("/");

            if (nats == null)
                nats = new NatsConfig("", "", "");

            return new MessagingConfig(enabled, pollingInterval, cleanupInterval, brokerType,
                addressList, username, password, authMethod, authToken, ssl, rabbitMq, nats);
        }
    }
}
