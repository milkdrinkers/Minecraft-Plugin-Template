package io.github.exampleuser.example.messaging.broker.nats;

import io.github.exampleuser.example.messaging.MessageConsumer;
import io.github.exampleuser.example.messaging.broker.AbstractBroker;
import io.github.exampleuser.example.messaging.config.MessagingConfig;
import io.github.exampleuser.example.messaging.config.SslContextBuilder;
import io.github.exampleuser.example.messaging.exception.MessagingInitializationException;
import io.github.exampleuser.example.messaging.message.BidirectionalMessage;
import io.github.exampleuser.example.messaging.message.OutgoingMessage;
import io.nats.client.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

/**
 * Implementation using nats client as a message broker
 */
@SuppressWarnings({"FieldCanBeLocal", "unused"})
public final class NatsBroker extends AbstractBroker {
    private static final Logger LOGGER = LoggerFactory.getLogger(NatsBroker.class);

    private final String name;
    private final String channelName;

    private Connection connection;
    private Dispatcher dispatcher;

    public NatsBroker(MessageConsumer messageConsumer, String name) {
        super(messageConsumer);
        this.name = name;
        this.channelName = "%s:message".formatted(name.toLowerCase());
    }

    @Override
    public <T> void send(@NotNull OutgoingMessage<T> message) {
        connection.publish(channelName, message.encode());
    }

    @Override
    public void init(MessagingConfig config) throws IOException, InterruptedException, NoSuchAlgorithmException {
        final Options.Builder builder = new Options.Builder()
            .reconnectWait(Duration.ofSeconds(5))
            .maxReconnects(Integer.MAX_VALUE)
            .connectionName(name);

        configureAddresses(builder, config);
        configureAuth(builder, config);
        configureSsl(builder, config);

        connection = Nats.connect(builder.build());
        dispatcher = connection.createDispatcher(new Handler()).subscribe(channelName);
    }

    @Override
    public void close() {
        try {
            connection.closeDispatcher(dispatcher);
            connection.close();
        } catch (InterruptedException e) {
            LOGGER.error("Exception while closing NATS connection", e);
        }
    }

    private void configureAddresses(Options.Builder builder, MessagingConfig config) {
        if (config.addressList().isSingle()) {
            builder.server("nats://%s".formatted(config.addressList().getFirst()));
        } else {
            builder.servers(
                config.addressList().getAll().stream()
                    .map("nats://%s"::formatted)
                    .toArray(String[]::new)
            );
        }
    }

    private void configureAuth(Options.Builder builder, MessagingConfig config) {
        switch (config.authMethod().toLowerCase()) {
            case "password" -> {
                if (!config.username().isEmpty() || !config.password().isEmpty())
                    builder.userInfo(config.username(), config.password());
            }
            case "token" -> {
                if (!config.authToken().isEmpty())
                    builder.token(config.authToken().toCharArray());
            }
            case "nkey" -> {
                // Separate JWT + NKey files. Pass null for jwtFile for challenge-only (no JWT) auth.
                final String jwtFile = config.nats().jwtFilePath().isEmpty() ? null : config.nats().jwtFilePath();
                if (!config.nats().nkeySeedPath().isEmpty())
                    builder.authHandler(Nats.credentials(jwtFile, config.nats().nkeySeedPath()));
            }
            case "credentials" -> {
                // Single combined credentials file (JWT + NKey seed in one file)
                if (!config.nats().credentialsPath().isEmpty())
                    builder.authHandler(Nats.credentials(config.nats().credentialsPath()));
            }
        }
    }

    private void configureSsl(Options.Builder builder, MessagingConfig config) {
        final MessagingConfig.SslConfig ssl = config.ssl();
        if (!ssl.enabled())
            return;

        try {
            final SSLContext sslCtx = SslContextBuilder.build(ssl);
            if (sslCtx != null) {
                builder.sslContext(sslCtx);
            } else {
                builder.secure(); // use JVM default SSL
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new MessagingInitializationException("Failed to configure SSL for NATS", e);
        }
    }

    /**
     * Handles incoming messages from the NATS subscription.
     */
    private final class Handler implements MessageHandler {
        @Override
        public void onMessage(io.nats.client.Message msg) {
            final BidirectionalMessage<?> message = BidirectionalMessage.from(msg.getData());
            getMessageConsumer().consumeMessage(message);
        }
    }
}
