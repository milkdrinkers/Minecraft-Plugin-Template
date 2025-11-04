package io.github.exampleuser.exampleplugin.messaging.broker.redis;

import io.github.exampleuser.exampleplugin.messaging.MessageConsumer;
import io.github.exampleuser.exampleplugin.messaging.adapter.task.TaskAdapter;
import io.github.exampleuser.exampleplugin.messaging.broker.AbstractBroker;
import io.github.exampleuser.exampleplugin.messaging.config.MessagingConfig;
import io.github.exampleuser.exampleplugin.messaging.message.BidirectionalMessage;
import io.github.exampleuser.exampleplugin.messaging.message.OutgoingMessage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPubSub;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of jedis client as a message broker
 */
@SuppressWarnings({"FieldCanBeLocal", "unused"})
public final class RedisBroker extends AbstractBroker {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisBroker.class);

    private final String name;
    private final String channelName;
    private final TaskAdapter task;
    private final Subscriber subscriber;

    private RedisClient client;
    private volatile boolean closing;

    public RedisBroker(MessageConsumer messageConsumer, String name, TaskAdapter task) {
        super(messageConsumer);
        this.name = name;
        this.channelName = "%s:message".formatted(name.toLowerCase());
        this.task = task;
        this.subscriber = new Subscriber();
    }

    @Override
    public <T> void send(@NotNull OutgoingMessage<T> message) {
        client.publish(channelName, message.encode());
    }

    @Override
    public void init(MessagingConfig config) throws IOException, InterruptedException, NoSuchAlgorithmException {
        client = new RedisClient(config);
    }

    @Override
    public void enable(MessagingConfig config) throws IOException, InterruptedException, NoSuchAlgorithmException {
        task.init(subscriber, 0, 5, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        closing = true;
        subscriber.unsubscribeSafely();
        task.cancel();
        client.close();
    }

    /**
     * Subscriber that defines handling of incoming messages
     */
    private final class Subscriber extends JedisPubSub implements Runnable {
        @Override
        public void onMessage(String channel, String message) {
            if (!channel.equals(channelName))
                return;

            final BidirectionalMessage<?> message2 = BidirectionalMessage.from(message);
            getMessageConsumer().consumeMessage(message2);
        }

        @Override
        public void run() {
            boolean firstStartup = true;
            while (!closing && !Thread.interrupted() && client.isAlive()) {
                try {
                    if (firstStartup) {
                        firstStartup = false;
                    } else {
                        LOGGER.info("Connection to Redis instance reestablished!");
                    }
                    client.subscribe(this, channelName);
                } catch (Exception e) {
                    if (closing)
                        return;

                    LOGGER.warn("Unable to connect to Redis instance, retrying in 5 seconds...", e);
                    unsubscribeSafely();
                    sleepBeforeRetry();
                }
            }
        }

        private void unsubscribeSafely() {
            try {
                unsubscribe();
            } catch (Exception ignored) {
            }
        }

        private void sleepBeforeRetry() {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
