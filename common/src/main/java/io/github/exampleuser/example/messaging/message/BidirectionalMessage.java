package io.github.exampleuser.example.messaging.message;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A message that can be both sent and received over any configured broker, carrying a typed payload.
 *
 * <p>Payload types must have a {@link MessageCodec} registered before the first send or receive.
 * Common types ({@code String}, {@code Integer}, {@code Long}, {@code Double}, {@code Boolean},
 * {@code UUID}) are pre-registered. For custom types, define a codec as a static constant on the
 * payload class and register it at plugin startup:
 * <pre>{@code
 * record BalanceUpdate(UUID playerId, BigDecimal newBalance) {
 *     static final MessageCodec<BalanceUpdate> CODEC = MessageCodec.of(
 *         BalanceUpdate.class,
 *         (v, out) -> { CodecHelper.writeUUID(out, v.playerId()); out.writeUTF(v.newBalance().toPlainString()); },
 *         in -> new BalanceUpdate(CodecHelper.readUUID(in), new BigDecimal(in.readUTF()))
 *     );
 * }
 *
 * // At startup:
 * BidirectionalMessage.registerCodec(BalanceUpdate.CODEC);
 *
 * // Sending:
 * BidirectionalMessage.<BalanceUpdate>builder()
 *     .channelId("balance-update")
 *     .payload(new BalanceUpdate(playerId, newBalance))
 *     .build();
 * }</pre>
 *
 * <p><b>Wire format</b> (binary, produced by {@link #encode()}):
 * <ol>
 *   <li>UUID: two 8-byte longs (most significant bits first)</li>
 *   <li>Channel ID: modified UTF-8 string</li>
 *   <li>Payload class name: modified UTF-8 string (used to look up the codec on decode)</li>
 *   <li>Payload: whatever bytes the registered codec writes</li>
 * </ol>
 * Text-based transports (Redis, database) use {@link #encodeAsString()}, which wraps
 * the binary format in standard Base64.
 *
 * @param <T> the payload type
 */
@SuppressWarnings("unused")
public final class BidirectionalMessage<T> implements OutgoingMessage<T> {
    private static final Map<String, MessageCodec<?>> CODECS = new ConcurrentHashMap<>();

    static {
        registerCodec(MessageCodecs.STRING);
        registerCodec(MessageCodecs.INTEGER);
        registerCodec(MessageCodecs.LONG);
        registerCodec(MessageCodecs.DOUBLE);
        registerCodec(MessageCodecs.BOOLEAN);
        registerCodec(MessageCodecs.UUID);
    }

    private final UUID uuid;
    private final String channelId;
    private final @NotNull T payload;
    private final @NotNull Class<T> payloadType;

    private BidirectionalMessage(UUID uuid, String channelId, @NotNull T payload, @NotNull Class<T> payloadType) {
        this.uuid = uuid;
        this.channelId = channelId;
        this.payload = payload;
        this.payloadType = payloadType;
    }

    @Override
    public @NotNull UUID getUUID() {
        return uuid;
    }

    @Override
    public @NotNull String getChannelID() {
        return channelId;
    }

    @Override
    public @NotNull T getPayload() {
        return payload;
    }

    @Override
    public @NotNull Class<T> getPayloadType() {
        return payloadType;
    }

    /**
     * Encodes this message to bytes for binary-capable transports.
     * See the class-level javadoc for the wire format specification.
     *
     * @return the encoded message bytes
     * @throws IllegalStateException if no codec is registered for the payload type
     * @throws RuntimeException      if encoding fails due to an I/O error
     */
    @Override
    public byte[] encode() {
        final MessageCodec<T> codec = requireCodec(payloadType);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final DataOutputStream out = new DataOutputStream(baos)) {
            out.writeLong(uuid.getMostSignificantBits());
            out.writeLong(uuid.getLeastSignificantBits());
            out.writeUTF(channelId);
            out.writeUTF(payloadType.getName());
            codec.encode(payload, out);
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode message " + uuid, e);
        }
        return baos.toByteArray();
    }

    /**
     * Decodes a message from bytes previously produced by {@link #encode()}.
     *
     * @param data the encoded bytes
     * @param <T>  the expected payload type
     * @return the decoded message
     * @throws IllegalStateException if no codec is registered for the payload type in the data
     * @throws RuntimeException      if decoding fails due to an I/O error or corrupt data
     */
    @SuppressWarnings("unchecked")
    public static <T> @NotNull BidirectionalMessage<T> from(byte[] data) {
        try (final DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            final UUID uuid = new UUID(in.readLong(), in.readLong());
            final String channelId = in.readUTF();
            final String className = in.readUTF();

            final MessageCodec<T> codec = (MessageCodec<T>) CODECS.get(className);
            if (codec == null)
                throw new IllegalStateException("No codec registered for payload type '" + className + "'. Call BidirectionalMessage.registerCodec() first.");

            final T payload = codec.decode(in);
            return new BidirectionalMessage<>(uuid, channelId, payload, codec.type());
        } catch (IOException e) {
            throw new RuntimeException("Failed to decode message", e);
        }
    }

    /**
     * Decodes a message from a Base64 string previously produced by {@link #encodeAsString()}.
     * Used by text-based transports (Redis, database).
     *
     * @param encoded a Base64-encoded message string
     * @param <T>     the expected payload type
     * @return the decoded message
     */
    public static <T> @NotNull BidirectionalMessage<T> from(@NotNull String encoded) {
        return from(Base64.getDecoder().decode(encoded));
    }

    /**
     * Registers a codec for a payload type. Any existing registration for the same type
     * is silently replaced, so callers can override built-in codecs if needed.
     *
     * <p>Registration is global and thread-safe. Codecs should be registered once at
     * plugin startup, before any messages of that type are sent or received.
     *
     * @param codec the codec to register
     */
    public static void registerCodec(@NotNull MessageCodec<?> codec) {
        CODECS.put(codec.type().getName(), codec);
    }

    @SuppressWarnings("unchecked")
    private static <T> @NotNull MessageCodec<T> requireCodec(@NotNull Class<T> type) {
        final MessageCodec<T> codec = (MessageCodec<T>) CODECS.get(type.getName());
        if (codec == null)
            throw new IllegalStateException("No codec registered for payload type '" + type.getName() + "'. Call BidirectionalMessage.registerCodec() before sending.");
        return codec;
    }

    /**
     * Returns a new builder for constructing a {@link BidirectionalMessage}.
     *
     * @param <T> the payload type; inferred from the {@link Builder#payload(Object)} call
     * @return a new builder
     */
    public static <T> @NotNull Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Fluent builder for a {@link BidirectionalMessage}.
     *
     * <p>A codec must be registered for the payload type before {@link #build()} is called.
     * The check happens eagerly in {@code build()} so you get a clear error at construction
     * time rather than on the first attempted send.
     *
     * @param <T> the payload type
     */
    public static final class Builder<T> {
        private UUID uuid;
        private String channelId;
        private T payload;

        private Builder() {
        }

        /**
         * Sets the message UUID. If omitted, a random UUID is generated on {@link #build()}.
         *
         * @param uuid the UUID to use
         * @return this builder
         */
        public Builder<T> uuid(@NotNull UUID uuid) {
            this.uuid = uuid;
            return this;
        }

        /**
         * Sets the channel this message will be published on.
         *
         * @param channelId the channel identifier; must not be null or empty
         * @return this builder
         */
        public Builder<T> channelId(@NotNull String channelId) {
            this.channelId = channelId;
            return this;
        }

        /**
         * Sets the payload. The runtime class of the value is used to look up
         * the registered {@link MessageCodec} when {@link #build()} is called.
         *
         * @param payload the payload value; must not be null
         * @return this builder
         */
        public Builder<T> payload(@NotNull T payload) {
            this.payload = payload;
            return this;
        }

        /**
         * Builds the message, verifying that all required fields are set and that
         * a codec exists for the payload type.
         *
         * @return a new {@link BidirectionalMessage}
         * @throws IllegalStateException if {@code channelId} or {@code payload} is missing,
         *                               or no codec is registered for the payload type
         */
        @SuppressWarnings("unchecked")
        public BidirectionalMessage<T> build() {
            if (uuid == null)
                uuid = UUID.randomUUID();

            if (channelId == null || channelId.isEmpty())
                throw new IllegalStateException("Channel ID must be set before building a message");

            if (payload == null)
                throw new IllegalStateException("Payload must be set before building a message");

            final Class<T> type = (Class<T>) payload.getClass();
            requireCodec(type); // validate before returning so failures surface at construction time
            return new BidirectionalMessage<>(uuid, channelId, payload, type);
        }
    }
}
