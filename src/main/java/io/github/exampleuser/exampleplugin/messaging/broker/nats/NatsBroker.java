package io.github.exampleuser.exampleplugin.messaging.broker.nats;

import io.github.exampleuser.exampleplugin.messaging.MessageConsumer;
import io.github.exampleuser.exampleplugin.messaging.broker.AbstractBroker;
import io.github.exampleuser.exampleplugin.messaging.broker.MessagingUtils;
import io.github.exampleuser.exampleplugin.messaging.config.MessagingConfig;
import io.github.exampleuser.exampleplugin.messaging.message.BidirectionalMessage;
import io.github.exampleuser.exampleplugin.messaging.message.OutgoingMessage;
import io.nats.client.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
        connection.publish(channelName, MessagingUtils.ByteUtil.to(message));
    }

    @Override
    public void init(MessagingConfig config) throws IOException, InterruptedException, NoSuchAlgorithmException {
        final Options.Builder builder = new Options.Builder()
            .reconnectWait(Duration.ofSeconds(5))
            .maxReconnects(Integer.MAX_VALUE)
            .connectionName(name);

        if (config.addressList().isSingle()) {
            builder.server("nats://%s".formatted(config.addressList().getFirst()));
        } else {
            builder.servers(
                config.addressList().getAll().stream()
                    .map("nats://%s"::formatted)
                    .toArray(String[]::new)
            );
        }

        switch (config.authMethod().toLowerCase()) {
            case "password" -> {
                if (!config.username().isEmpty() || !config.password().isEmpty())
                    builder.userInfo(config.username(), config.password());
            }
            case "token" -> {
                if (!config.authToken().isEmpty())
                    builder.token(config.authToken().toCharArray());
            }
        }

        if (config.ssl()) {
            builder.secure();
        }

        connection = Nats.connect(builder.build());
        dispatcher = connection.createDispatcher(new Handler()).subscribe(channelName);
    }

    @Override
    public void close() {
        try {
            connection.closeDispatcher(dispatcher);
            connection.close();
        } catch (InterruptedException e) {
            LOGGER.error("Exception while closing Nats connection", e);
        }
    }

    /**
     * Subscriber that defines handling of incoming messages
     */
    private final class Handler implements MessageHandler {
        @Override
        public void onMessage(io.nats.client.Message msg) {
            final BidirectionalMessage<?> message = MessagingUtils.ByteUtil.from(msg.getData());
            getMessageConsumer().consumeMessage(message);
        }
    }
}
