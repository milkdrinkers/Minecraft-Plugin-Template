package io.github.exampleuser.exampleplugin.messaging.message;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a decodable message.
 *
 * @param <R> the message type
 */
public interface IncomingMessage<T, R extends IncomingMessage<T, R>> extends Message<T> {
    /**
     * Gets the message object representation {@link R} of a JSON string.
     *
     * @param json the JSON string to decode
     * @return the message object
     */
    @NotNull R decode(final String json);
}
