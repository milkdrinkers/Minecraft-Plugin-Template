package io.github.exampleuser.exampleplugin.messaging.message;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a encodable message.
 */
public interface OutgoingMessage<T> extends Message<T> {
    /**
     * Gets an encoded string representing this message object.
     *
     * @return an encoded string representing this message object
     */
    @NotNull String encode();
}
