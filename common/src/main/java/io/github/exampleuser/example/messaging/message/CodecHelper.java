package io.github.exampleuser.example.messaging.message;

import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;

/**
 * Static helpers for writing and reading common types inside composite {@link MessageCodec} lambdas.
 *
 * <p>These are useful when a payload type contains fields that don't map to a single
 * {@link DataOutput} primitive, such as a {@link UUID} which requires two {@code long} writes.
 * Use them inside {@link MessageCodec#of} lambdas to keep encoder/decoder pairs concise:
 * <pre>{@code
 * record PlayerUpdate(UUID playerId, String name) {
 *     static final MessageCodec<PlayerUpdate> CODEC = MessageCodec.of(
 *         PlayerUpdate.class,
 *         (v, out) -> { CodecHelper.writeUUID(out, v.playerId()); out.writeUTF(v.name()); },
 *         in -> new PlayerUpdate(CodecHelper.readUUID(in), in.readUTF())
 *     );
 * }
 * }</pre>
 */
public final class CodecHelper {
    private CodecHelper() {
    }

    /**
     * Writes a {@link UUID} as two consecutive longs (most significant bits first).
     *
     * @param out  the output stream
     * @param uuid the UUID to write
     * @throws IOException if writing fails
     */
    public static void writeUUID(@NotNull DataOutput out, @NotNull UUID uuid) throws IOException {
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());
    }

    /**
     * Reads a {@link UUID} written by {@link #writeUUID}.
     *
     * @param in the input stream
     * @return the decoded UUID
     * @throws IOException if reading fails
     */
    public static @NotNull UUID readUUID(@NotNull DataInput in) throws IOException {
        return new UUID(in.readLong(), in.readLong());
    }
}
