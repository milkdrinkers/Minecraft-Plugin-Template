package io.github.exampleuser.example.messaging.message;

import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Handles serialization and deserialization of a specific payload type to and from the message wire format.
 *
 * <p>Codecs must be registered with {@link BidirectionalMessage#registerCodec(MessageCodec)} before
 * any message carrying that payload type is sent or received. The common Java primitives and
 * {@code UUID} are pre-registered via {@link MessageCodecs}.
 *
 * <p>The most concise way to define a codec is via the {@link #of(Class, Encoder, Decoder)} factory,
 * typically as a static constant on the payload type itself:
 * <pre>{@code
 * record Ping(long timestamp) {
 *     static final MessageCodec<Ping> CODEC = MessageCodec.of(
 *         Ping.class,
 *         (v, out) -> out.writeLong(v.timestamp()),
 *         in -> new Ping(in.readLong())
 *     );
 * }
 * }</pre>
 *
 * <p>Implementations must be symmetric: every byte written by {@link #encode} must be read back
 * by {@link #decode} in the same order.
 *
 * @param <T> the payload type this codec handles
 */
public interface MessageCodec<T> {

    /**
     * Writes a payload value to the output stream.
     * Every byte written here must be read back by {@link #decode} in the same order.
     *
     * @param value the payload to encode; never {@code null}
     * @param out   the output stream to write to
     * @throws IOException if writing fails
     */
    void encode(@NotNull T value, @NotNull DataOutput out) throws IOException;

    /**
     * Reads a payload value from the input stream.
     * Must consume exactly the bytes written by {@link #encode}.
     *
     * @param in the input stream to read from
     * @return the decoded payload; never {@code null}
     * @throws IOException if reading fails
     */
    @NotNull T decode(@NotNull DataInput in) throws IOException;

    /**
     * Returns the class this codec handles.
     * This is used as the registry key when looking up codecs at encode and decode time.
     *
     * @return the payload class
     */
    @NotNull Class<T> type();

    /**
     * Functional interface for the encoding half of a codec.
     *
     * @param <T> the value type to encode
     */
    @FunctionalInterface
    interface Encoder<T> {
        /**
         * Writes {@code value} to {@code out}.
         *
         * @param value the value to write
         * @param out   the target output stream
         * @throws IOException if writing fails
         */
        void encode(@NotNull T value, @NotNull DataOutput out) throws IOException;
    }

    /**
     * Functional interface for the decoding half of a codec.
     *
     * @param <T> the value type to decode
     */
    @FunctionalInterface
    interface Decoder<T> {
        /**
         * Reads and returns a value from {@code in}.
         *
         * @param in the source input stream
         * @return the decoded value; never {@code null}
         * @throws IOException if reading fails
         */
        @NotNull T decode(@NotNull DataInput in) throws IOException;
    }

    /**
     * Creates a codec from a class token and a pair of encoder/decoder lambdas.
     * This is the preferred way to define a codec inline:
     * <pre>{@code
     * static final MessageCodec<MyType> CODEC = MessageCodec.of(
     *     MyType.class,
     *     (v, out) -> { out.writeUTF(v.name()); out.writeInt(v.count()); },
     *     in -> new MyType(in.readUTF(), in.readInt())
     * );
     * }</pre>
     *
     * @param type    the class this codec handles
     * @param encoder the encoding function
     * @param decoder the decoding function
     * @param <T>     the payload type
     * @return a new {@link MessageCodec} backed by the given functions
     */
    @NotNull
    static <T> MessageCodec<T> of(@NotNull Class<T> type, @NotNull Encoder<T> encoder, @NotNull Decoder<T> decoder) {
        return new MessageCodec<>() {
            @Override
            public void encode(@NotNull T value, @NotNull DataOutput out) throws IOException {
                encoder.encode(value, out);
            }

            @Override
            public @NotNull T decode(@NotNull DataInput in) throws IOException {
                return decoder.decode(in);
            }

            @Override
            public @NotNull Class<T> type() {
                return type;
            }
        };
    }
}
