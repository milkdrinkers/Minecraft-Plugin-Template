package io.github.exampleuser.example.database;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Utility class containing converters and other logic for queries.
 */
public final class QueryUtils {
    @SuppressWarnings("unused")
    public static final class BooleanUtil {
        /**
         * Convert boolean to a byte.
         *
         * @param bool the boolean
         * @return the byte
         */
        public static byte toByte(boolean bool) {
            return (byte) (bool ? 1 : 0);
        }

        /**
         * Convert byte to boolean.
         *
         * @param _byte the byte array
         * @return the uuid
         */
        public static boolean fromByte(byte _byte) {
            return _byte == 1;
        }
    }

    @SuppressWarnings("unused")
    public static final class UUIDUtil {
        /**
         * Convert animal tamer object to an array of bytes.
         *
         * @param value the animal tamer
         * @return the byte array
         */
        public static byte[] toBytes(AnimalTamer value) {
            return toBytes(value.getUniqueId());
        }

        /**
         * Convert offline player object to an array of bytes.
         *
         * @param value the offline player
         * @return the byte array
         */
        public static byte[] toBytes(Entity value) {
            return toBytes(value.getUniqueId());
        }

        /**
         * Convert offline player object to an array of bytes.
         *
         * @param value the offline player
         * @return the byte array
         */
        public static byte[] toBytes(OfflinePlayer value) {
            return toBytes(value.getUniqueId());
        }

        /**
         * Convert world info object to an array of bytes.
         *
         * @param value the world info
         * @return the byte array
         */
        public static byte[] toBytes(WorldInfo value) {
            return toBytes(value.getUID());
        }

        /**
         * Convert uuid to an array of bytes.
         *
         * @param uuid the uuid
         * @return the byte array
         */
        public static byte[] toBytes(@Nullable java.util.UUID uuid) {
            if (uuid == null) return null;

            ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
            bb.putLong(uuid.getMostSignificantBits());
            bb.putLong(uuid.getLeastSignificantBits());
            return bb.array();
        }

        /**
         * Convert byte array to uuid.
         *
         * @param bytes the byte array
         * @return the uuid
         */
        public static java.util.UUID fromBytes(byte[] bytes) {
            if (bytes == null) return null;

            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
            long high = byteBuffer.getLong();
            long low = byteBuffer.getLong();
            return new java.util.UUID(high, low);
        }
    }

    @SuppressWarnings("unused")
    public static final class InstantUtil {
        /**
         * Convert instant to a date time.
         *
         * @param instant instant
         * @return the date time
         */
        public static LocalDateTime toDateTime(@Nullable java.time.Instant instant) {
            if (instant == null) return null;
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        }

        /**
         * Convert date time to an instant.
         *
         * @param time date time
         * @return the instant
         */
        public static java.time.Instant fromDateTime(@Nullable LocalDateTime time) {
            if (time == null) return null;
            return time.toInstant(ZoneOffset.UTC);
        }

        /**
         * Convert instant to an epoch.
         *
         * @param instant instant
         * @return the epoch
         */
        public static Long toEpoch(@Nullable java.time.Instant instant) {
            if (instant == null) return null;
            return instant.getEpochSecond();
        }

        /**
         * Convert epoch to an instant.
         *
         * @param epoch epoch
         * @return the instant
         */
        public static java.time.Instant fromEpoch(@Nullable Long epoch) {
            if (epoch == null) return null;
            return java.time.Instant.ofEpochSecond(epoch);
        }
    }
}
