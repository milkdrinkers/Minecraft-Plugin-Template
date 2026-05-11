package io.github.exampleuser.example.messaging.adapter.receiver;

import io.github.exampleuser.example.messaging.message.Message;

import java.util.function.Consumer;

/**
 * Platform-specific handler for messages received from the broker.
 * Subclasses decide how to dispatch the message (e.g., fire a Bukkit event, post to a queue).
 */
public abstract class ReceiverAdapter implements Consumer<Message<?>> {
    @Override
    public abstract void accept(final Message<?> message);
}
