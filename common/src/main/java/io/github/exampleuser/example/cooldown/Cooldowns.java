package io.github.exampleuser.example.cooldown;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Singleton manager for handling player cooldowns across different types of actions.
 * <p>
 * This class provides thread-safe operations for setting, checking, and managing
 * cooldowns for players. It supports both online and offline players through their UUIDs.
 * </p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * // Set a 30-second cooldown for a player
 * Cooldowns.set(player, CooldownType.TELEPORT, Duration.ofSeconds(30));
 *
 * // Check if player has an active cooldown
 * if (Cooldowns.has(player, CooldownType.TELEPORT)) {
 *     Duration remaining = Cooldowns.getRemaining(player, CooldownType.TELEPORT);
 *     player.sendMessage("Wait " + remaining.getSeconds() + " seconds before teleporting again");
 * }
 * }</pre>
 *
 * @author darksaid98
 */
@SuppressWarnings("unused")
public final class Cooldowns {
    private static CooldownStorage INSTANCE;

    /**
     * Gets the singleton instance of the cooldown storage.
     *
     * @return the cooldown storage instance
     */
    @NotNull
    private static CooldownStorage getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CooldownStorage();
        }
        return INSTANCE;
    }

    /**
     * Resets all cooldowns by creating a new storage instance.
     * <p>
     * This method clears all existing cooldowns for all players and all types.
     * Use with caution as this operation cannot be undone.
     * </p>
     */
    @ApiStatus.Internal
    static void reset() {
        INSTANCE = new CooldownStorage();
    }

    /**
     * Internal storage implementation for cooldowns.
     * <p>
     * This class uses a synchronized Guava Table to store cooldown data,
     * ensuring thread safety for concurrent operations.
     * </p>
     */
    private static final class CooldownStorage {
        private final Table<UUID, CooldownType, Instant> cooldowns = Tables.synchronizedTable(HashBasedTable.create());

        private CooldownStorage() {
        }

        /**
         * Sets a cooldown for a specific player and type.
         *
         * @param uuid      the player's UUID
         * @param type      the cooldown type
         * @param expiresAt the instant when the cooldown expires
         * @return the previous cooldown expiration time, or null if none existed
         */
        @Nullable
        public Instant set(UUID uuid, CooldownType type, Instant expiresAt) {
            return cooldowns.put(uuid, type, expiresAt);
        }

        /**
         * Gets the cooldown expiration time for a specific player and type.
         *
         * @param uuid the player's UUID
         * @param type the cooldown type
         * @return the cooldown expiration time, or null if no cooldown exists
         */
        @Nullable
        public Instant get(UUID uuid, CooldownType type) {
            return cooldowns.get(uuid, type);
        }

        /**
         * Checks if a player has an active cooldown for a specific type.
         *
         * @param uuid the player's UUID
         * @param type the cooldown type
         * @return true if the player has an active cooldown, false otherwise
         */
        public boolean has(UUID uuid, CooldownType type) {
            final @Nullable Instant cooldown = cooldowns.get(uuid, type);
            return cooldown != null && Instant.now().isBefore(cooldown);
        }

        /**
         * Removes a cooldown for a specific player and type.
         *
         * @param uuid the player's UUID
         * @param type the cooldown type
         * @return the removed cooldown expiration time, or null if none existed
         */
        @Nullable
        public Instant remove(UUID uuid, CooldownType type) {
            return cooldowns.remove(uuid, type);
        }

        /**
         * Removes all cooldowns for a specific player.
         *
         * @param uuid the player's UUID
         */
        public void removeAll(UUID uuid) {
            cooldowns.row(uuid).clear();
        }

        /**
         * Gets the remaining cooldown duration for a specific player and type.
         * <p>
         * If the cooldown has expired, it will be automatically removed and
         * {@link Duration#ZERO} will be returned.
         * </p>
         *
         * @param uuid the player's UUID
         * @param type the cooldown type
         * @return the remaining cooldown duration, or {@link Duration#ZERO} if no active cooldown
         */
        public Duration getRemaining(UUID uuid, CooldownType type) {
            final Instant cooldown = cooldowns.get(uuid, type);
            final Instant now = Instant.now();
            if (cooldown != null && now.isBefore(cooldown)) {
                return Duration.between(now, cooldown);
            } else {
                remove(uuid, type);
                return Duration.ZERO;
            }
        }

        /**
         * Formats the remaining cooldown duration into a human-readable string.
         * <p>
         * This method provides a more configurable approach to time formatting
         * compared to the previous implementation.
         * </p>
         *
         * @param uuid   the player's UUID
         * @param type   the cooldown type
         * @param format the format style to use
         * @return a formatted string representation of the remaining cooldown
         */
        public String formatRemaining(UUID uuid, CooldownType type, TimeFormat format) {
            final Duration remaining = getRemaining(uuid, type);
            return format.format(remaining);
        }
    }

    /**
     * Sets a cooldown for a player using an absolute expiration time.
     *
     * @param uuid      the player's UUID
     * @param type      the cooldown type
     * @param expiresAt the instant when the cooldown expires
     * @return the previous cooldown expiration time, or null if none existed
     */
    @Nullable
    public static Instant set(UUID uuid, CooldownType type, Instant expiresAt) {
        return getInstance().set(uuid, type, expiresAt);
    }

    /**
     * Sets a cooldown for a player using a duration from now.
     *
     * @param uuid     the player's UUID
     * @param type     the cooldown type
     * @param duration the duration of the cooldown
     * @return the previous cooldown expiration time, or null if none existed
     */
    @Nullable
    public static Instant set(UUID uuid, CooldownType type, Duration duration) {
        return set(uuid, type, Instant.now().plus(duration));
    }

    /**
     * Sets a cooldown for a player using seconds.
     *
     * @param uuid    the player's UUID
     * @param type    the cooldown type
     * @param seconds the cooldown duration in seconds
     * @return the previous cooldown expiration time, or null if none existed
     */
    @Nullable
    public static Instant set(UUID uuid, CooldownType type, long seconds) {
        return set(uuid, type, Duration.ofSeconds(seconds));
    }

    /**
     * Sets a cooldown for a player using milliseconds.
     *
     * @param uuid   the player's UUID
     * @param type   the cooldown type
     * @param amount the cooldown duration in amount of time
     * @return the previous cooldown expiration time, or null if none existed
     */
    @Nullable
    public static Instant set(UUID uuid, CooldownType type, long amount, TimeUnit unit) {
        return set(uuid, type, Duration.of(amount, unit.toChronoUnit()));
    }

    /**
     * Checks if a player has an active cooldown for a specific type.
     *
     * @param uuid the player's UUID
     * @param type the cooldown type
     * @return true if the player has an active cooldown, false otherwise
     */
    public static boolean has(UUID uuid, CooldownType type) {
        return getInstance().has(uuid, type);
    }

    /**
     * Removes a cooldown for a specific player and type.
     *
     * @param uuid the player's UUID
     * @param type the cooldown type
     * @return the removed cooldown expiration time, or null if none existed
     */
    @Nullable
    public static Instant remove(UUID uuid, CooldownType type) {
        return getInstance().remove(uuid, type);
    }

    /**
     * Removes all cooldowns for a specific player.
     *
     * @param uuid the player's UUID
     */
    public static void removeAll(UUID uuid) {
        getInstance().removeAll(uuid);
    }

    /**
     * Gets the cooldown expiration time for a specific player and type.
     *
     * @param uuid the player's UUID
     * @param type the cooldown type
     * @return the cooldown expiration time, or null if no cooldown exists
     */
    @Nullable
    public static Instant get(UUID uuid, CooldownType type) {
        return getInstance().get(uuid, type);
    }

    /**
     * Gets the remaining cooldown duration for a specific player and type.
     *
     * @param uuid the player's UUID
     * @param type the cooldown type
     * @return the remaining cooldown duration, or {@link Duration#ZERO} if no active cooldown
     */
    public static Duration getRemaining(UUID uuid, CooldownType type) {
        return getInstance().getRemaining(uuid, type);
    }

    /**
     * Formats the remaining cooldown duration into a human-readable string.
     *
     * @param uuid   the player's UUID
     * @param type   the cooldown type
     * @param format the format style to use
     * @return a formatted string representation of the remaining cooldown
     */
    public static String formatRemaining(UUID uuid, CooldownType type, TimeFormat format) {
        return getInstance().formatRemaining(uuid, type, format);
    }

    /**
     * Formats the remaining cooldown duration using the default format.
     *
     * @param uuid the player's UUID
     * @param type the cooldown type
     * @return a formatted string representation of the remaining cooldown
     */
    public static String formatRemaining(UUID uuid, CooldownType type) {
        return formatRemaining(uuid, type, TimeFormat.DETAILED);
    }

    /**
     * Sets a cooldown for a player using an absolute expiration time.
     *
     * @param player    the offline player
     * @param type      the cooldown type
     * @param expiresAt the instant when the cooldown expires
     * @return the previous cooldown expiration time, or null if none existed
     */
    @Nullable
    public static Instant set(OfflinePlayer player, CooldownType type, Instant expiresAt) {
        return set(player.getUniqueId(), type, expiresAt);
    }

    /**
     * Sets a cooldown for a player using a duration from now.
     *
     * @param player   the offline player
     * @param type     the cooldown type
     * @param duration the duration of the cooldown
     * @return the previous cooldown expiration time, or null if none existed
     */
    @Nullable
    public static Instant set(OfflinePlayer player, CooldownType type, Duration duration) {
        return set(player.getUniqueId(), type, duration);
    }

    /**
     * Sets a cooldown for a player using seconds.
     *
     * @param player  the offline player
     * @param type    the cooldown type
     * @param seconds the cooldown duration in seconds
     * @return the previous cooldown expiration time, or null if none existed
     */
    @Nullable
    public static Instant set(OfflinePlayer player, CooldownType type, long seconds) {
        return set(player.getUniqueId(), type, seconds);
    }

    /**
     * Sets a cooldown for a player using a specified time unit.
     *
     * @param player the offline player
     * @param type   the cooldown type
     * @param amount the amount of time
     * @param unit   the time unit
     * @return the previous cooldown expiration time, or null if none existed
     */
    @Nullable
    public static Instant set(OfflinePlayer player, CooldownType type, long amount, TimeUnit unit) {
        return set(player.getUniqueId(), type, amount, unit);
    }

    /**
     * Checks if a player has an active cooldown for a specific type.
     *
     * @param player the offline player
     * @param type   the cooldown type
     * @return true if the player has an active cooldown, false otherwise
     */
    public static boolean has(OfflinePlayer player, CooldownType type) {
        return has(player.getUniqueId(), type);
    }

    /**
     * Removes a cooldown for a specific player and type.
     *
     * @param player the offline player
     * @param type   the cooldown type
     * @return the removed cooldown expiration time, or null if none existed
     */
    @Nullable
    public static Instant remove(OfflinePlayer player, CooldownType type) {
        return remove(player.getUniqueId(), type);
    }

    /**
     * Removes all cooldowns for a specific player.
     *
     * @param player the offline player
     */
    public static void removeAll(OfflinePlayer player) {
        removeAll(player.getUniqueId());
    }

    /**
     * Gets the cooldown expiration time for a specific player and type.
     *
     * @param player the offline player
     * @param type   the cooldown type
     * @return the cooldown expiration time, or null if no cooldown exists
     */
    @Nullable
    public static Instant get(OfflinePlayer player, CooldownType type) {
        return get(player.getUniqueId(), type);
    }

    /**
     * Gets the remaining cooldown duration for a specific player and type.
     *
     * @param player the offline player
     * @param type   the cooldown type
     * @return the remaining cooldown duration, or {@link Duration#ZERO} if no active cooldown
     */
    public static Duration getRemaining(OfflinePlayer player, CooldownType type) {
        return getRemaining(player.getUniqueId(), type);
    }

    /**
     * Formats the remaining cooldown duration into a human-readable string.
     *
     * @param player the offline player
     * @param type   the cooldown type
     * @param format the format style to use
     * @return a formatted string representation of the remaining cooldown
     */
    public static String formatRemaining(OfflinePlayer player, CooldownType type, TimeFormat format) {
        return formatRemaining(player.getUniqueId(), type, format);
    }

    /**
     * Formats the remaining cooldown duration using the default format.
     *
     * @param player the offline player
     * @param type   the cooldown type
     * @return a formatted string representation of the remaining cooldown
     */
    public static String formatRemaining(OfflinePlayer player, CooldownType type) {
        return formatRemaining(player.getUniqueId(), type);
    }

    /**
     * Enum for different time formatting styles.
     */
    public enum TimeFormat {
        /**
         * Detailed format showing hours, minutes, and seconds.
         * Example: "1 hour, 30 minutes, and 45 seconds"
         */
        DETAILED {
            @Override
            public String format(Duration duration) {
                if (duration.isZero()) {
                    return "0 seconds";
                }

                final long hours = duration.toHours();
                final long minutes = duration.toMinutesPart();
                final long seconds = duration.toSecondsPart();

                if (hours > 0) {
                    return String.format("%d hour%s, %d minute%s, and %d second%s",
                        hours, hours == 1 ? "" : "s",
                        minutes, minutes == 1 ? "" : "s",
                        seconds, seconds == 1 ? "" : "s");
                } else if (minutes > 0) {
                    return String.format("%d minute%s and %d second%s",
                        minutes, minutes == 1 ? "" : "s",
                        seconds, seconds == 1 ? "" : "s");
                } else {
                    return String.format("%d second%s", seconds, seconds == 1 ? "" : "s");
                }
            }
        },

        /**
         * Compact format using abbreviations.
         * Example: "1h 30m 45s"
         */
        COMPACT {
            @Override
            public String format(Duration duration) {
                if (duration.isZero()) {
                    return "0s";
                }

                final long hours = duration.toHours();
                final long minutes = duration.toMinutesPart();
                final long seconds = duration.toSecondsPart();

                StringBuilder sb = new StringBuilder();
                if (hours > 0) {
                    sb.append(hours).append("h ");
                }
                if (minutes > 0) {
                    sb.append(minutes).append("m ");
                }
                if (seconds > 0) {
                    sb.append(seconds).append("s");
                }

                return sb.toString().trim();
            }
        },

        /**
         * Seconds-only format.
         * Example: "3645 seconds"
         */
        SECONDS_ONLY {
            @Override
            public String format(Duration duration) {
                final long seconds = duration.getSeconds();
                return seconds + " second" + (seconds == 1 ? "" : "s");
            }
        };

        /**
         * Formats the given duration according to this format style.
         *
         * @param duration the duration to format
         * @return the formatted string representation
         */
        public abstract String format(Duration duration);
    }
}