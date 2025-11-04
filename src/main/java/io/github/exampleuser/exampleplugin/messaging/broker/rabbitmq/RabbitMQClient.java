package io.github.exampleuser.exampleplugin.messaging.broker.rabbitmq;

import com.rabbitmq.client.*;
import io.github.exampleuser.exampleplugin.messaging.config.MessagingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * A client implementation of RabbitMQ
 */
final class RabbitMQClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQClient.class);

    private static final int RECONNECT_DELAY_MS = 5000;
    private static final boolean QUEUE_DURABLE = false;
    private static final boolean QUEUE_EXCLUSIVE = true;
    private static final boolean QUEUE_AUTO_DELETE = true;

    private final ConnectionFactory connectionFactory;
    private final MessagingConfig config;
    private Connection connection;
    private Channel channel;

    RabbitMQClient(MessagingConfig config) {
        this.connectionFactory = createConnectionFactory(config);
        this.config = config;
    }

    private ConnectionFactory createConnectionFactory(MessagingConfig config) {
        final ConnectionFactory factory = new ConnectionFactory();
        factory.setVirtualHost(config.rabbitMq().virtualHost());

        factory.setConnectionTimeout(10000);
        factory.setRequestedHeartbeat(5);
        factory.setNetworkRecoveryInterval(5000);
        factory.setAutomaticRecoveryEnabled(true);
        factory.setTopologyRecoveryEnabled(true);

        //noinspection SwitchStatementWithTooFewBranches
        switch (config.authMethod()) {
            case "password" -> {
                factory.setUsername(config.username());
                factory.setPassword(config.password());
            }
        }

        return factory;
    }

    public void publish(String exchange, String routingKey, byte[] message) throws IOException {
        channel.basicPublish(
            exchange,
            routingKey,
            new AMQP.BasicProperties.Builder().build(),
            message
        );
    }

    /**
     * Checks if there is a healthy connection and reconnects if there isn't
     *
     * @param firstStartup whether this is the first time executing this method
     * @return whether we are now successfully connected
     */
    public boolean connect(boolean firstStartup) {
        // Skip if connection is already healthy
        if (isConnectionHealthy())
            return true;

        closeChannelSafely();
        closeConnectionSafely();

        if (!firstStartup)
            LOGGER.debug("Connection dropped, reestablishing connection...");

        return attemptConnection(firstStartup);
    }

    public void setupQueueAndExchange(String exchangeName, String routingKey, DeliverCallback callback) throws IOException {
        final String queue = channel.queueDeclare("", QUEUE_DURABLE, QUEUE_EXCLUSIVE, QUEUE_AUTO_DELETE, null).getQueue();
        channel.exchangeDeclare(exchangeName, BuiltinExchangeType.TOPIC, QUEUE_DURABLE, QUEUE_AUTO_DELETE, null);
        channel.queueBind(queue, exchangeName, routingKey);
        channel.basicConsume(queue, true, callback, tag -> {
        });
    }

    /**
     * Shuts down this client by closing all channels/connections
     *
     * @throws IOException      thrown if an error is encountered
     * @throws TimeoutException thrown if we fail to shut down
     */
    public void close() throws IOException, TimeoutException {
        if (channel != null)
            channel.close();

        if (connection != null)
            connection.close();
    }

    private boolean isConnectionHealthy() {
        return connection != null && connection.isOpen() &&
            channel != null && channel.isOpen();
    }

    private void closeChannelSafely() {
        if (channel != null && channel.isOpen()) {
            try {
                channel.close();
            } catch (IOException | TimeoutException e) {
                LOGGER.error("Exception while closing RabbitMQ channel", e);
            }
        }
    }

    private void closeConnectionSafely() {
        if (connection != null && connection.isOpen()) {
            try {
                connection.close();
            } catch (IOException e) {
                LOGGER.error("Exception while closing RabbitMQ connection", e);
            }
        }
    }

    private boolean attemptConnection(boolean firstStartup) {
        try {
            connection = connectionFactory.newConnection(
                config.addressList().getAll().stream()
                    .map(address -> {
                        if (address.port() != null)
                            return new Address(address.host(), address.port());
                        return new Address(address.host());
                    })
                    .toArray(Address[]::new)
            );
            channel = connection.createChannel();

            if (!firstStartup)
                LOGGER.info("Connection to RabbitMQ instance reestablished!");

            return true;
        } catch (IOException | TimeoutException e) {
            return handleConnectionFailure(e, firstStartup);
        }
    }

    private boolean handleConnectionFailure(Exception e, boolean firstStartup) {
        if (firstStartup) {
            LOGGER.warn("Unable to connect to RabbitMQ instance, retrying in 5 seconds...", e);
            try {
                Thread.sleep(RECONNECT_DELAY_MS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return connect(false);
        } else {
            LOGGER.error("Unable to connect to RabbitMQ instance", e);
            return false;
        }
    }
}
