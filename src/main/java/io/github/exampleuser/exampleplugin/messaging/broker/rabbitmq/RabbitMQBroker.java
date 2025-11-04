package io.github.exampleuser.exampleplugin.messaging.broker.rabbitmq;

import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import io.github.exampleuser.exampleplugin.messaging.MessageConsumer;
import io.github.exampleuser.exampleplugin.messaging.adapter.task.TaskAdapter;
import io.github.exampleuser.exampleplugin.messaging.broker.AbstractBroker;
import io.github.exampleuser.exampleplugin.messaging.broker.MessagingUtils;
import io.github.exampleuser.exampleplugin.messaging.config.MessagingConfig;
import io.github.exampleuser.exampleplugin.messaging.message.BidirectionalMessage;
import io.github.exampleuser.exampleplugin.messaging.message.OutgoingMessage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Implementation of rabbitmq client as a message broker
 */
@SuppressWarnings({"FieldCanBeLocal", "unused"})
public final class RabbitMQBroker extends AbstractBroker {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQBroker.class);

    private final String name;
    private final String exchangeName;
    private final String routingKey;
    private final TaskAdapter task;
    private final Subscriber subscriber;

    private RabbitMQClient client;

    public RabbitMQBroker(MessageConsumer messageConsumer, String name, TaskAdapter task) {
        super(messageConsumer);
        this.name = name;
        this.exchangeName = "%s".formatted(name.toLowerCase());
        this.routingKey = "%s:message".formatted(name.toLowerCase());
        this.task = task;
        this.subscriber = new Subscriber();
    }

    @Override
    public <T> void send(@NotNull OutgoingMessage<T> message) throws IOException {
        client.publish(exchangeName, routingKey, MessagingUtils.ByteUtil.to(message));
    }

    @Override
    public void init(MessagingConfig config) throws IOException, InterruptedException, NoSuchAlgorithmException {
        client = new RabbitMQClient(config);
        if (!client.connect(true))
            throw new IOException("RabbitMQ client failed to initialize.");
        client.setupQueueAndExchange(exchangeName, routingKey, subscriber);
    }

    @Override
    public void enable(MessagingConfig config) throws IOException, InterruptedException, NoSuchAlgorithmException {
        task.init(() -> client.connect(false));
    }

    @Override
    public void close() {
        try {
            task.cancel();
            client.close();
        } catch (Exception e) {
            LOGGER.error("Exception while closing RabbitMQ client", e);
        }
    }

    /**
     * Subscriber that defines handling of incoming messages
     */
    private final class Subscriber implements DeliverCallback {
        @Override
        public void handle(String consumerTag, Delivery delivery) {
            final BidirectionalMessage<?> message = MessagingUtils.ByteUtil.from(delivery.getBody());
            getMessageConsumer().consumeMessage(message);
        }
    }
}
