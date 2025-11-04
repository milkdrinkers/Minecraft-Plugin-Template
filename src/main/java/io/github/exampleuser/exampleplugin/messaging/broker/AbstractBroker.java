package io.github.exampleuser.exampleplugin.messaging.broker;

import io.github.exampleuser.exampleplugin.messaging.MessageConsumer;
import io.github.exampleuser.exampleplugin.messaging.config.MessagingConfig;
import io.github.exampleuser.exampleplugin.messaging.message.OutgoingMessage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Class used to provide a common interface for pubsub/message broker implementations.
 */
public abstract class AbstractBroker implements AutoCloseable {
    private final MessageConsumer messageConsumer;

    protected AbstractBroker(MessageConsumer messageConsumer) {
        this.messageConsumer = messageConsumer;
    }

    /**
     * Returns the message consumer for this broker.
     *
     * @return the message consumer
     */
    @NotNull
    public MessageConsumer getMessageConsumer() {
        return messageConsumer;
    }

    /**
     * Sends a message using this broker implementation.
     *
     * @param message the message to send
     * @param <T>     the type of the message
     * @throws IOException      if an I/O error occurs
     * @throws RuntimeException if a runtime error occurs
     */
    public abstract <T> void send(@NotNull OutgoingMessage<T> message) throws IOException, RuntimeException;

    /**
     * Initializes this broker with the given configuration.
     *
     * @param config the messaging configuration
     * @throws IOException              if an I/O error occurs
     * @throws InterruptedException     if the thread is interrupted
     * @throws NoSuchAlgorithmException if a required algorithm is not found
     */
    public void init(MessagingConfig config) throws IOException, InterruptedException, NoSuchAlgorithmException {
    }

    /**
     * Enables this broker with the given configuration.
     *
     * @param config the messaging configuration
     * @throws IOException              if an I/O error occurs
     * @throws InterruptedException     if the thread is interrupted
     * @throws NoSuchAlgorithmException if a required algorithm is not found
     */
    public void enable(MessagingConfig config) throws IOException, InterruptedException, NoSuchAlgorithmException {
    }

    @Override
    public void close() {
    }
}
