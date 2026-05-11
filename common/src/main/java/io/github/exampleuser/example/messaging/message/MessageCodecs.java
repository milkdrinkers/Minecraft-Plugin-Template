package io.github.exampleuser.example.messaging.message;

import java.util.UUID;

/**
 * Pre-built {@link MessageCodec} instances for common Java types.
 *
 * <p>All codecs in this class are automatically registered when the messaging system
 * initializes, so {@code String}, {@code Integer}, {@code Long}, {@code Double},
 * {@code Boolean}, and {@code UUID} payloads work out of the box.
 */
public final class MessageCodecs {
    private MessageCodecs() {
    }

    /**
     * Codec for {@link String} using modified UTF-8 encoding.
     */
    public static final MessageCodec<String> STRING = MessageCodec.of(
        String.class,
        (v, out) -> out.writeUTF(v),
        in -> in.readUTF()
    );

    /**
     * Codec for {@link Integer} as a 4-byte signed integer.
     */
    public static final MessageCodec<Integer> INTEGER = MessageCodec.of(
        Integer.class,
        (v, out) -> out.writeInt(v),
        in -> in.readInt()
    );

    /**
     * Codec for {@link Long} as an 8-byte signed integer.
     */
    public static final MessageCodec<Long> LONG = MessageCodec.of(
        Long.class,
        (v, out) -> out.writeLong(v),
        in -> in.readLong()
    );

    /**
     * Codec for {@link Double} as an 8-byte IEEE 754 double.
     */
    public static final MessageCodec<Double> DOUBLE = MessageCodec.of(
        Double.class,
        (v, out) -> out.writeDouble(v),
        in -> in.readDouble()
    );

    /**
     * Codec for {@link Boolean} as a single byte.
     */
    public static final MessageCodec<Boolean> BOOLEAN = MessageCodec.of(
        Boolean.class,
        (v, out) -> out.writeBoolean(v),
        in -> in.readBoolean()
    );

    /**
     * Codec for {@link UUID} as two consecutive 8-byte longs (most significant bits first).
     */
    public static final MessageCodec<UUID> UUID = MessageCodec.of(
        java.util.UUID.class,
        (v, out) -> {
            out.writeLong(v.getMostSignificantBits());
            out.writeLong(v.getLeastSignificantBits());
        },
        in -> new java.util.UUID(in.readLong(), in.readLong())
    );
}
