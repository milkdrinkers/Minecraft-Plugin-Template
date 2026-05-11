package io.github.exampleuser.example.messaging.message;

import org.jetbrains.annotations.NotNull;

import java.util.Base64;

/**
 * A message that can be encoded and sent over a broker.
 *
 * @param <T> the payload type
 */
public interface OutgoingMessage<T> extends Message<T> {

    /**
     * Encodes this message to bytes for binary-capable transports
     * (NATS, RabbitMQ, plugin messaging).
     *
     * @return the encoded message as a byte array
     */
    byte[] encode();

    /**
     * Encodes this message to a Base64 string for text-only transports (Redis, database).
     * The default implementation wraps {@link #encode()} with standard Base64 encoding.
     *
     * @return the encoded message as a Base64 string
     */
    @NotNull
    default String encodeAsString() {
        return Base64.getEncoder().encodeToString(encode());
    }
}
