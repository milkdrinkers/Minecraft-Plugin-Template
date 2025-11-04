package io.github.exampleuser.exampleplugin.messaging;

import io.github.exampleuser.exampleplugin.AbstractService;
import io.github.exampleuser.exampleplugin.ExamplePlugin;
import io.github.exampleuser.exampleplugin.Reloadable;
import io.github.exampleuser.exampleplugin.messaging.adapter.receiver.BukkitReceiverAdapter;
import io.github.exampleuser.exampleplugin.messaging.adapter.receiver.ReceiverAdapter;
import io.github.exampleuser.exampleplugin.messaging.adapter.task.BukkitTaskAdapter;
import io.github.exampleuser.exampleplugin.messaging.adapter.task.TaskAdapter;
import io.github.exampleuser.exampleplugin.messaging.broker.AbstractBroker;
import io.github.exampleuser.exampleplugin.messaging.broker.BrokerType;
import io.github.exampleuser.exampleplugin.messaging.broker.database.DatabaseBroker;
import io.github.exampleuser.exampleplugin.messaging.broker.nats.NatsBroker;
import io.github.exampleuser.exampleplugin.messaging.broker.pluginmsg.PluginBroker;
import io.github.exampleuser.exampleplugin.messaging.broker.rabbitmq.RabbitMQBroker;
import io.github.exampleuser.exampleplugin.messaging.broker.redis.RedisBroker;
import io.github.exampleuser.exampleplugin.messaging.caching.CacheSet;
import io.github.exampleuser.exampleplugin.messaging.config.MessagingConfig;
import io.github.exampleuser.exampleplugin.messaging.exception.MessagingEnablingException;
import io.github.exampleuser.exampleplugin.messaging.exception.MessagingInitializationException;
import io.github.exampleuser.exampleplugin.messaging.message.IncomingMessage;
import io.github.exampleuser.exampleplugin.messaging.message.OutgoingMessage;
import io.github.exampleuser.exampleplugin.utility.DB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * A class handling/managing the implementation & lifecycle of the messaging service.
 */
public final class MessagingHandler extends AbstractService implements Reloadable, MessageConsumer {
    private static final String LOG_PREFIX = "[Messaging] ";
    private final boolean testing;
    private final Logger logger;
    private final String implementationName;
    private final TaskAdapter taskAdapter;
    private final ReceiverAdapter receiverAdapter;
    private MessagingConfig config;
    private @Nullable CacheSet<UUID> receivedMessageIds = null; // Tracks messages consumed by this instance, preventing itself from processing them
    private @Nullable AbstractBroker broker = null;

    /**
     * Instantiates a new Messaging handler.
     *
     * @param logger             the logger
     * @param implementationName the implementation name
     */
    private MessagingHandler(@NotNull Logger logger, @NotNull String implementationName) {
        this.testing = false;
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
        this.implementationName = Objects.requireNonNull(implementationName, "Implementation name cannot be null");
        this.taskAdapter = new BukkitTaskAdapter();
        this.receiverAdapter = new BukkitReceiverAdapter();
    }

    /**
     * Instantiates a new Messaging handler.
     *
     * @param config             the messaging config
     * @param testing            the testing
     * @param logger             the logger
     * @param implementationName the implementation name
     * @param taskAdapter        the task adapter
     * @param receiverAdapter    the receiver adapter
     */
    @TestOnly
    private MessagingHandler(@NotNull MessagingConfig config, boolean testing, @NotNull Logger logger, @NotNull String implementationName, @NotNull TaskAdapter taskAdapter, @NotNull ReceiverAdapter receiverAdapter) {
        this.config = Objects.requireNonNull(config, "Messaging config cannot be null");
        this.testing = testing;
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
        this.implementationName = Objects.requireNonNull(implementationName, "Implementation name cannot be null");
        this.taskAdapter = Objects.requireNonNull(taskAdapter, "Task adapter cannot be null");
        this.receiverAdapter = Objects.requireNonNull(receiverAdapter, "Receiver adapter cannot be null");
    }

    @Override
    public void onLoad(ExamplePlugin plugin) {
        if (config == null)
            this.config = MessagingConfig.fromConfig(plugin.getConfigHandler().getDatabaseConfig());

        try {
            doStartup();
        } catch (Exception e) {
            logger.error(LOG_PREFIX + "Message broker initialization error: {}", e.getMessage());
        }
    }

    @Override
    public void onEnable(ExamplePlugin plugin) {
        try {
            scheduleTasks();
        } catch (Exception e) {
            logger.error(LOG_PREFIX + "Message broker enable error:", e);
        }
    }

    @Override
    public void onDisable(ExamplePlugin plugin) {
        try {
            doShutdown();
        } catch (Exception e) {
            logger.error(LOG_PREFIX + "Error while shutting down message broker:", e);
        }
    }

    @Override
    protected void startup() {
        // Ignore if broker is not enabled
        if (config == null || !config.enabled())
            return;

        logger.info(LOG_PREFIX + "Starting message broker...");

        receivedMessageIds = new CacheSet<>(10, TimeUnit.MINUTES);
        broker = switch (config.brokerType()) {
            case PLUGIN_MESSAGING -> new PluginBroker(this, implementationName);
            case REDIS -> new RedisBroker(this, implementationName, taskAdapter);
            case RABBITMQ -> new RabbitMQBroker(this, implementationName, taskAdapter);
            case NATS -> new NatsBroker(this, implementationName);
            default -> new DatabaseBroker(this, implementationName, taskAdapter, taskAdapter);
        };

        if (config.brokerType().equals(BrokerType.DATABASE) && !DB.isStarted())
            throw new MessagingInitializationException("Database is required for this message broker but the database has failed to initialize!");

        if (broker == null)
            throw new MessagingInitializationException("Attempted to initialize message broker but broker is null!");

        try {
            broker.init(config);
        } catch (Exception e) {
            throw new MessagingInitializationException("Attempt to initialize message broker threw an exception!", e);
        }

        logger.info(LOG_PREFIX + "Successfully started message broker.");
    }

    /**
     * Schedules the tasks for the message broker.
     *
     * @throws MessagingEnablingException if the broker is null or fails to enable
     * @implSpec This method must be called after the broker has been initialized.
     */
    public void scheduleTasks() {
        // Ignore if broker is not enabled
        if (config == null || !config.enabled())
            return;

        if (broker == null)
            throw new MessagingEnablingException("Attempted to enable message broker but broker is null!");

        try {
            broker.enable(config);
        } catch (Exception e) {
            throw new MessagingEnablingException("Attempt to enable message broker threw an exception!", e);
        }
    }

    @Override
    protected void shutdown() {
        // Ignore if broker is not enabled
        if (config == null || !config.enabled())
            return;

        logger.info(LOG_PREFIX + "Shutting down message broker...");

        if (broker != null)
            broker.close();

        if (receivedMessageIds != null)
            receivedMessageIds.close();

        broker = null;
        receivedMessageIds = null;
        config = null;

        logger.info(LOG_PREFIX + "Shutdown completed.");
    }

    /**
     * Sends a message using the configured message broker.
     *
     * @param message the outgoing message
     * @return if the message was successfully sent
     */
    public <T> CompletableFuture<Boolean> send(final OutgoingMessage<T> message) {
        if (!isStarted() || receivedMessageIds == null || broker == null)
            return CompletableFuture.completedFuture(false);

        return CompletableFuture.supplyAsync(() -> {
                try {
                    if (!testing)
                        receivedMessageIds.add(message.getUUID()); // Allow receiving sent messages in testing environments
                    broker.send(message);
                    logger.debug(LOG_PREFIX + "Sent message with uuid \"{}\", channel id \"{}\" and payload of type \"{}\".", message.getUUID(), message.getChannelID(), message.getPayloadType().getName());
                    return true;
                } catch (IOException e) {
                    return false;
                }
            })
            .exceptionally(throwable -> false);
    }

    @Override
    public void consumeMessage(final IncomingMessage<?, ?> message) {
        if (!isStarted() || receivedMessageIds == null || receivedMessageIds.contains(message.getUUID()))
            return;

        logger.debug(LOG_PREFIX + "Received message with uuid \"{}\", channel id \"{}\" and payload of type \"{}\"...", message.getUUID(), message.getChannelID(), message.getPayloadType().getName());
        receivedMessageIds.add(message.getUUID());
        receiverAdapter.accept(message);
    }

    /**
     * Returns if the broker is setup and functioning properly.
     *
     * @return the boolean
     */
    public boolean isReady() {
        return isStarted();
    }

    /**
     * Returns the type of the currently configured message broker.
     *
     * @return the broker type, defaults to {@link BrokerType#DATABASE} if not configured
     */
    public BrokerType getType() {
        if (config == null)
            return BrokerType.DATABASE;

        return config.brokerType();
    }

    /**
     * Get a builder instance for this class.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The type Messaging handler builder.
     */
    public static class Builder {
        private boolean testing = false;
        private Logger logger;
        private String implementationName;
        private TaskAdapter taskAdapter;
        private ReceiverAdapter receiverAdapter;
        private MessagingConfig config;

        private Builder() {
        }

        /**
         * With testing messaging handler builder.
         *
         * @param testing the testing state
         * @return the messaging handler builder
         */
        public Builder withTesting(boolean testing) {
            this.testing = testing;
            return this;
        }

        /**
         * With logger messaging handler builder.
         *
         * @param logger the logger
         * @return the messaging handler builder
         */
        public Builder withLogger(Logger logger) {
            this.logger = logger;
            return this;
        }

        /**
         * With implementation name handler builder.
         *
         * @param implementationName the name of this implementation
         * @return the messaging handler builder
         */
        public Builder withName(String implementationName) {
            this.implementationName = implementationName;
            return this;
        }

        /**
         * With taskAdapter handler builder.
         *
         * @param taskAdapter the taskAdapter of this implementation
         * @return the messaging handler builder
         */
        public Builder withTaskAdapter(TaskAdapter taskAdapter) {
            this.taskAdapter = taskAdapter;
            return this;
        }

        /**
         * With receiverAdapter handler builder.
         *
         * @param receiverAdapter the receiverAdapter of this implementation
         * @return the messaging handler builder
         */
        public Builder withReceiverAdapter(ReceiverAdapter receiverAdapter) {
            this.receiverAdapter = receiverAdapter;
            return this;
        }

        /**
         * With messaging config messaging handler builder.
         *
         * @param config the messaging config
         * @return the messaging handler builder
         */
        @TestOnly
        public Builder withConfig(@NotNull MessagingConfig config) {
            this.config = config;
            return this;
        }

        /**
         * Build messaging handler.
         *
         * @return the messaging handler
         */
        public MessagingHandler build() {
            if (implementationName == null)
                implementationName = "";

            if (logger != null && config == null)
                return new MessagingHandler(logger, implementationName);

            if (config == null)
                throw new RuntimeException("Failed to build messaging handler as config is null!");

            if (logger != null && taskAdapter != null)
                return new MessagingHandler(config, testing, logger, implementationName, taskAdapter, receiverAdapter);

            throw new RuntimeException("Failed to build messaging handler!");
        }
    }
}
