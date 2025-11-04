package io.github.exampleuser.exampleplugin.messaging.config;

import io.github.exampleuser.exampleplugin.messaging.broker.BrokerType;
import io.github.milkdrinkers.crate.Config;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Config object for messaging services
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
    boolean ssl,
    RabbitMqConfig rabbitMq,
    NatsConfig nats
) {
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
     * Gets messaging config from file.
     *
     * @param cfg the cfg
     * @return the messaging config from file
     */
    public static MessagingConfig fromConfig(Config cfg) {
        return MessagingConfig.builder()
            .withEnabled(cfg.getOrDefault("messaging.enabled", true))
            .withPollingInterval(cfg.getLong("messaging.polling-interval"))
            .withCleanupInterval(cfg.getLong("messaging.cleanup-interval"))
            .withBroker(cfg.getString("messaging.type"))
            .withAddresses(cfg.get("messaging.address"))
            .withUsername(cfg.getString("messaging.username"))
            .withPassword(cfg.getString("messaging.password"))
            .withAuthMethod(cfg.getString("messaging.advanced.auth-method"))
            .withAuthToken(cfg.getString("messaging.advanced.auth-token"))
            .withSSL(cfg.getOrDefault("messaging.advanced.ssl.enabled", false))
//            .withSsl(
//                cfg.getOrDefault("messaging.advanced.ssl.enabled", false),
//                cfg.getString("messaging.advanced.ssl.cert-path"),
//                cfg.getString("messaging.advanced.ssl.key-path"),
//                cfg.getString("messaging.advanced.ssl.ca-path"),
//                cfg.getOrDefault("messaging.advanced.ssl.verify-server-cert", true),
//                cfg.getOrDefault("messaging.advanced.ssl.verify-hostname", true)
//            )
            .withRabbitMq(cfg.getString("messaging.advanced.rabbitmq.virtual-host"))
            .withNats(
                cfg.getString("messaging.advanced.nats.nkey-seed-path"),
                cfg.getString("messaging.advanced.nats.jwt-file-path"),
                cfg.getString("messaging.advanced.nats.credentials-path")
            )
            .build();
    }

    /**
     * Get a config builder instance.
     *
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The type Messaging config builder.
     */
    public static final class Builder {
        private static final Logger LOGGER = LoggerFactory.getLogger(MessagingConfig.class);

        private Builder() {
        }

        private @Nullable Boolean enabled;
        private @Nullable Long pollingInterval;
        private @Nullable Long cleanupInterval;
        private @Nullable String broker;
        private @Nullable AddressList addressList;
        private @Nullable String username;
        private @Nullable String password;
        private @Nullable String authMethod;
        private @Nullable String authToken;
        private boolean ssl;
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

        public Builder withBroker(String broker) {
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

        public Builder withSSL(boolean enabled) {
            this.ssl = enabled;
            return this;
        }

//        public Builder withSSL(boolean enabled, String certPath, String keyPath,
//                               String caPath, boolean verifyServerCert, boolean verifyHostname) {
//            this.ssl = new SslConfig(enabled, certPath, keyPath, caPath, verifyServerCert, verifyHostname);
//            return this;
//        }

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
                pollingInterval = 1000L; // Default to 1 second

            if (cleanupInterval == null)
                cleanupInterval = 30000L; // Default to 30 seconds

            if (cleanupInterval < 10000L) {
                LOGGER.warn("Messaging \"cleanup-interval\" was set to less than the minimum 10000ms ({}ms), using default.", cleanupInterval);
                cleanupInterval = 10000L; // Minimum cleanup interval of 10 seconds
            }

            if (pollingInterval > cleanupInterval / 3) {
                LOGGER.warn("Messaging \"polling-interval\" was set to more than the maximum \"cleanup-interval\" divided by three ({}ms), using default.", pollingInterval);
                pollingInterval = cleanupInterval / 3;
            }

            BrokerType brokerType = BrokerType.fromName(broker);
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

            if (rabbitMq == null)
                rabbitMq = new RabbitMqConfig("/");

            if (nats == null)
                nats = new NatsConfig("", "", "");

            return new MessagingConfig(enabled, pollingInterval, cleanupInterval, brokerType,
                addressList, username, password, authMethod, authToken, ssl, rabbitMq, nats);
        }
    }
}
