package io.github.exampleuser.exampleplugin.messaging.message;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Representation of a basic message.
 */
public interface Message<T> {
    /**
     * Gets the {@link UUID} of this message.
     *
     * @return the UUID of this message
     */
    @NotNull UUID getUUID();

    /**
     * Gets the channel id of this message
     *
     * @return the namespace
     */
    @NotNull String getChannelID();

    /**
     * Gets the message payload.
     *
     * @return the payload of this message
     */
    @NotNull T getPayload();

    /**
     * Gets the message payload type.
     *
     * @return the payload type of this message
     */
    @NotNull Class<T> getPayloadType();
}
