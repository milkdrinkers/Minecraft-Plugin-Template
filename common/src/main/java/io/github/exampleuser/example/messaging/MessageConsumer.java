package io.github.exampleuser.example.messaging;

import io.github.exampleuser.example.messaging.message.Message;

/**
 * Implemented by anything that can receive and handle messages from the broker.
 */
public interface MessageConsumer {
    /**
     * Called when a message arrives from the broker.
     *
     * @param message the received message
     */
    void consumeMessage(final Message<?> message);
}
