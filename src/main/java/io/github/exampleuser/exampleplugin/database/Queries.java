package io.github.exampleuser.exampleplugin.database;

import io.github.exampleuser.exampleplugin.cooldown.CooldownType;
import io.github.exampleuser.exampleplugin.cooldown.Cooldowns;
import io.github.exampleuser.exampleplugin.database.handler.DatabaseType;
import io.github.exampleuser.exampleplugin.database.schema.tables.records.CooldownsRecord;
import io.github.exampleuser.exampleplugin.messenger.message.IncomingMessage;
import io.github.exampleuser.exampleplugin.messenger.message.Message;
import io.github.exampleuser.exampleplugin.messenger.message.OutgoingMessage;
import io.github.exampleuser.exampleplugin.utility.DB;
import io.github.exampleuser.exampleplugin.utility.Logger;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.*;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.exampleuser.exampleplugin.database.QueryUtils.BooleanUtil;
import static io.github.exampleuser.exampleplugin.database.QueryUtils.UUIDUtil;
import static io.github.exampleuser.exampleplugin.database.schema.Tables.*;
import static org.jooq.impl.DSL.*;

/**
 * A class providing access to all SQL queries.
 */
@SuppressWarnings({"LoggingSimilarMessage", "StringConcatenationArgumentToLogCall"})
public final class Queries {
    /**
     * Example add data to database.
     * <p>
     * Inserts or updates entries dependent on whether a duplicate row exists.
     */
    public static void upsert() {
        try (
            Connection con = DB.getConnection()
        ) {
            DSLContext context = DB.getContext(con);

            context
                .insertInto(SOME_LIST, SOME_LIST.UUID, SOME_LIST._NAME)
                .values(
                    UUIDUtil.toBytes(UUID.randomUUID()),
                    "testname"
                )
                .onDuplicateKeyUpdate()
                .set(SOME_LIST._NAME, "testname")
                .execute();
        } catch (SQLException e) {
            Logger.get().error("SQL Query threw an error!", e);
        }
    }

    /**
     * Example insert with returning key.
     * <p>
     * Inserts or updates entries dependent on whether a duplicate row exists, and returns the relevant auto-incrementing field from the table.
     *
     * @return a resulting integer or null if null if the query failed
     */
    public static @Nullable BigInteger upsertReturning() {
        try (
            Connection con = DB.getConnection()
        ) {
            DSLContext context = DB.getContext(con);

            // jOOQ is broken in implementation when returning auto-increment field for SQLite!
            // We need to use the returning result for all RDBMS's except SQLite, where we use "#lastID()" instead.
            @Nullable Record1<Integer> record = context
                .insertInto(COLORS, COLORS.SOME_FIELD, COLORS.ENABLED)
                .values(
                    "testname",
                    BooleanUtil.toByte(true)
                )
                .onDuplicateKeyUpdate()
                .set(COLORS.SOME_FIELD, "testname")
                .returningResult(COLORS.COLOR_ID) // Return the auto-incrementing id from the db
                .fetchOne();

            if (!DB.getHandler().getDatabaseConfig().getDatabaseType().equals(DatabaseType.SQLITE) && record != null && record.component1() != null) {
                return BigInteger.valueOf(record.component1()); // For H2, MySQL, MariaDB
            } else {
                return context.lastID(); // For SQLite
            }

        } catch (SQLException e) {
            Logger.get().error("SQL Query threw an error!", e);
        }
        return null;
    }

    /**
     * Example save all data to database.
     * <p>
     * Batch executes multiple upserts.
     * Read <a href="https://www.jooq.org/doc/latest/manual/sql-execution/batch-execution/">jOOQ Batch Documentation</a> for more info.
     */
    public static void saveAll() {
        try (
            @NotNull Connection con = DB.getConnection()
        ) {
            DSLContext context = DB.getContext(con);

            context
                .batch(
                    context
                        .insertInto(SOME_LIST, SOME_LIST.UUID, SOME_LIST._NAME)
                        .values(
                            UUIDUtil.toBytes(UUID.randomUUID()),
                            "testname"
                        )
                        .onDuplicateKeyUpdate()
                        .set(SOME_LIST._NAME, "testname"),
                    context
                        .insertInto(SOME_LIST, SOME_LIST.UUID, SOME_LIST._NAME)
                        .values(
                            UUIDUtil.toBytes(UUID.randomUUID()),
                            "othername"
                        )
                        .onDuplicateKeyUpdate()
                        .set(SOME_LIST._NAME, "othername")
                )
                .execute();
        } catch (SQLException e) {
            Logger.get().error("SQL Query threw an error!", e);
        }
    }

    /**
     * Example save all data to database.
     * <p>
     * Transaction to execute upserts. If one operation should fail, everything is rolled back.
     * Read <a href="https://www.jooq.org/doc/latest/manual/sql-execution/transaction-management/">jOOQ Transaction Documentation</a> for more info.
     */
    public static void saveAllTransaction() {
        try (
            @NotNull Connection con = DB.getConnection()
        ) {
            DSLContext context = DB.getContext(con);

            context
                .transaction(configuration -> {
                    DSLContext internalContext = configuration.dsl();

                    internalContext
                        .insertInto(SOME_LIST, SOME_LIST.UUID, SOME_LIST._NAME)
                        .values(
                            UUIDUtil.toBytes(UUID.randomUUID()),
                            "testname"
                        )
                        .onDuplicateKeyUpdate()
                        .set(SOME_LIST._NAME, "testname")
                        .execute();

                    internalContext
                        .insertInto(SOME_LIST, SOME_LIST.UUID, SOME_LIST._NAME)
                        .values(
                            UUIDUtil.toBytes(UUID.randomUUID()),
                            "othername"
                        )
                        .onDuplicateKeyUpdate()
                        .set(SOME_LIST._NAME, "othername")
                        .execute();
                });
        } catch (SQLException e) {
            Logger.get().error("SQL Query threw an error!", e);
        }
    }

    /**
     * Example load all data from database.
     * <p>
     * You should make this method return whatever it is you're grabbing from database.
     *
     * @return the result
     */
    @SuppressWarnings("UnusedReturnValue")
    public static @Nullable Result<Record2<String, byte[]>> loadAll() {
        try (
            Connection con = DB.getConnection()
        ) {
            DSLContext context = DB.getContext(con);

            return context
                .select(SOME_LIST._NAME, SOME_LIST.UUID)
                .from(SOME_LIST)
                .fetch();
        } catch (SQLException e) {
            Logger.get().error("SQL Query threw an error!", e);
        }
        return null;
    }

    /**
     * Holds all queries related to using the database as a messaging service.
     */
    @ApiStatus.Internal
    public static final class Sync {
        /**
         * Fetch the latest (greatest) message ID from the database.
         *
         * @return the message id or empty if no messages are queued
         */
        public static Optional<Integer> fetchLatestMessageId() {
            try (
                Connection con = DB.getConnection()
            ) {
                DSLContext context = DB.getContext(con);

                return context
                    .select(max(MESSAGING.ID))
                    .from(MESSAGING)
                    .fetchOptional(0, Integer.class);
            } catch (SQLException e) {
                Logger.get().error("SQL Query threw an error!" + e);
                return Optional.empty();
            }
        }

        /**
         * Adds a message to the database.
         *
         * @param message the outgoing message to send
         * @return the new message id or empty if insert failed
         */
        public static <T> Optional<Integer> send(OutgoingMessage<T> message) {
            try (
                Connection con = DB.getConnection()
            ) {
                DSLContext context = DB.getContext(con);

                return context
                    .insertInto(MESSAGING, MESSAGING.TIMESTAMP, MESSAGING.MESSAGE)
                    .values(
                        currentLocalDateTime(),
                        val(message.encode())
                    )
                    .returningResult(MESSAGING.ID)
                    .fetchOptional(0, Integer.class);
            } catch (SQLException e) {
                Logger.get().error("SQL Query threw an error!" + e);
                return Optional.empty();
            }
        }

        /**
         * Fetch all messages from the database.
         *
         * @param latestSyncId    the currently synced to message id
         * @param cleanupInterval the configured cleanup interval
         * @return the messages
         */
        public static Map<Integer, IncomingMessage<?, ?>> receive(int latestSyncId, long cleanupInterval) {
            try (
                Connection con = DB.getConnection()
            ) {
                DSLContext context = DB.getContext(con);

                return context
                    .selectFrom(MESSAGING)
                    .where(MESSAGING.ID.greaterThan(latestSyncId)
                        .and(MESSAGING.TIMESTAMP.greaterOrEqual(localDateTimeSub(currentLocalDateTime(), cleanupInterval / 1000, DatePart.SECOND))) // Checks TIMESTAMP >= now() - cleanupInterval
                    )
                    .orderBy(MESSAGING.ID.asc())
                    .fetch()
                    .intoMap(MESSAGING.ID, r -> BidirectionalMessage.from(r.getMessage()));
            } catch (SQLException e) {
                Logger.get().error("SQL Query threw an error!" + e);
                return Map.of();
            }
        }

        /**
         * Deletes all outdate messages from the database.
         *
         * @param cleanupInterval the configured cleanup interval
         */
        public static void cleanup(long cleanupInterval) {
            try (
                Connection con = DB.getConnection()
            ) {
                DSLContext context = DB.getContext(con);

                context
                    .deleteFrom(MESSAGING)
                    .where(MESSAGING.TIMESTAMP.lessThan(localDateTimeSub(currentLocalDateTime(), cleanupInterval / 1000, DatePart.SECOND))) // Checks TIMESTAMP < now() - cleanupInterval
                    .execute();
            } catch (SQLException e) {
                Logger.get().error("SQL Query threw an error!" + e);
            }
        }
    }

    /**
     * Wrapper class to organize cooldown-related queries.
     */
    public static final class Cooldown {
        public static Map<CooldownType, Instant> load(OfflinePlayer player) {
            return load(player.getUniqueId());
        }

        public static Map<CooldownType, Instant> load(UUID uuid) {
            try (
                Connection con = DB.getConnection()
            ) {
                DSLContext context = DB.getContext(con);

                final Result<CooldownsRecord> cooldownsRecords = context
                    .selectFrom(COOLDOWNS)
                    .where(COOLDOWNS.UUID.eq(UUIDUtil.toBytes(uuid)))
                    .fetch();

                return cooldownsRecords.stream()
                    .collect(Collectors.toMap(
                        r -> CooldownType.valueOf(r.getCooldownType()),
                        r -> QueryUtils.InstantUtil.fromDateTime(r.getCooldownTime())
                    ));
            } catch (SQLException e) {
                Logger.get().error("SQL Query threw an error!", e);
            }
            return Collections.emptyMap();
        }

        public static void save(OfflinePlayer player) {
            save(player.getUniqueId());
        }

        public static void save(UUID uuid) {
            try (
                Connection con = DB.getConnection()
            ) {
                DSLContext context = DB.getContext(con);

                context.transaction(config -> {
                    DSLContext ctx = config.dsl();

                    // Delete old cooldowns
                    ctx.deleteFrom(COOLDOWNS)
                        .where(COOLDOWNS.UUID.eq(UUIDUtil.toBytes(uuid)))
                        .execute();

                    // Insert new cooldowns
                    final List<CooldownsRecord> cooldownsRecords = new ArrayList<>();

                    for (CooldownType cooldownType : CooldownType.values()) {
                        if (!Cooldowns.has(uuid, cooldownType))
                            continue;

                        cooldownsRecords.add(new CooldownsRecord(
                            UUIDUtil.toBytes(uuid),
                            cooldownType.name(),
                            QueryUtils.InstantUtil.toDateTime(Cooldowns.get(uuid, cooldownType))
                        ));
                    }

                    if (!cooldownsRecords.isEmpty())
                        ctx.batchInsert(cooldownsRecords).execute();
                });
            } catch (SQLException e) {
                Logger.get().error("SQL Query threw an error!", e);
            }
        }
    }
}
