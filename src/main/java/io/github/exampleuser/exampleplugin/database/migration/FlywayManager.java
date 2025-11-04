package io.github.exampleuser.exampleplugin.database.migration;

import io.github.exampleuser.exampleplugin.database.config.DatabaseConfig;
import io.github.exampleuser.exampleplugin.database.exception.DatabaseMigrationException;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogCreator;
import org.flywaydb.core.api.logging.LogFactory;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.core.api.output.RepairResult;
import org.flywaydb.core.api.output.ValidateResult;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Objects;

/**
 * Handles Flyway migrations.
 */
@SuppressWarnings({"UnusedReturnValue"})
public final class FlywayManager {
    private static final String LOG_PREFIX = "[Database] ";
    private final DataSource dataSource;
    private final DatabaseConfig databaseConfig;
    private final Logger logger;
    private Flyway flyway;

    /**
     * Instantiates a new Database migration handler.
     *
     * @param logger         the logger
     * @param dataSource     the data source
     * @param databaseConfig the database config
     */
    public FlywayManager(Logger logger, DataSource dataSource, DatabaseConfig databaseConfig) {
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
        this.dataSource = Objects.requireNonNull(dataSource, "DataSource cannot be null");
        this.databaseConfig = Objects.requireNonNull(databaseConfig, "DatabaseConfig cannot be null");
        initializeFlywayInstance();
    }

    private void initializeFlywayInstance() {
        final String packagePath = getClass().getPackageName().replace('.', '/');
        final Map<String, String> SQL_PLACEHOLDERS = Map.of(
            "tablePrefix", databaseConfig.getTablePrefix()
        );

        this.flyway = Flyway
            .configure(getClass().getClassLoader())
            .loggers("slf4j")
            .baselineOnMigrate(true)
            .baselineVersion("0.0")
            .validateMigrationNaming(true)
            .dataSource(dataSource)
            .locations(
                "classpath:%s/migrations".formatted(packagePath),
                "db/migration/%s".formatted(databaseConfig.getDatabaseType().getJdbcPrefix())
            )
            .table(databaseConfig.getTablePrefix() + "schema_history") // Configure tables and migrations
            .placeholders(SQL_PLACEHOLDERS)
            .load();
    }

    /**
     * Execute Flyway migration. All pending migrations will be applied in order.
     *
     * @return MigrateResult with migration details
     * @throws DatabaseMigrationException database migration exception
     */
    public MigrateResult migrate() throws DatabaseMigrationException {
        try {
            LogFactory.setLogCreator(new FlywaySlf4jLogCreator(logger));
            logger.info(LOG_PREFIX + "Starting database migration...");

            final MigrateResult result = this.flyway.migrate();

            logger.info(LOG_PREFIX + "Migration completed successfully. Applied {} migrations.", result.migrationsExecuted);

            return result;
        } catch (FlywayException e) {
            logger.error(LOG_PREFIX + "Migration failed: {}", e.getMessage(), e);
            throw new DatabaseMigrationException(e);
        } finally {
            LogFactory.setLogCreator(null);
        }
    }

    /**
     * Executes Flyway repair. Repairs the Flyway schema history table. This will perform the following actions:
     *
     * <ul>
     * <li>Remove any failed migrations on databases without DDL transactions (User objects left behind must still be cleaned up manually)</li>
     * <li>Realign the checksums, descriptions and types of the applied migration</li>
     * </ul>
     *
     * @return RepairResult with repair details
     * @throws DatabaseMigrationException database migration exception
     */
    @SuppressWarnings("unused")
    public @Nullable RepairResult repair() throws DatabaseMigrationException {
        try {
            LogFactory.setLogCreator(new FlywaySlf4jLogCreator(logger));
            if (databaseConfig.isRepair()) {
                logger.info(LOG_PREFIX + "Starting database repair...");
                final RepairResult result = this.flyway.repair();
                logger.info(LOG_PREFIX + "Database repair completed successfully.");
                return result;
            }
            return null;
        } catch (FlywayException e) {
            logger.error(LOG_PREFIX + "Database repair failed: {}", e.getMessage(), e);
            throw new DatabaseMigrationException(e);
        } finally {
            LogFactory.setLogCreator(null);
        }
    }

    /**
     * Validates the applied migrations against the available ones.
     *
     * @return ValidateResult with validation details
     * @throws DatabaseMigrationException database migration exception
     */
    @SuppressWarnings("unused")
    public ValidateResult validate() throws DatabaseMigrationException {
        try {
            LogFactory.setLogCreator(new FlywaySlf4jLogCreator(logger));
            logger.info(LOG_PREFIX + "Validating database migrations...");
            final ValidateResult result = this.flyway.validateWithResult();

            if (result.validationSuccessful) {
                logger.info(LOG_PREFIX + "Migration validation successful.");
            } else {
                logger.warn(LOG_PREFIX + "Migration validation failed with {} errors", result.invalidMigrations.size());
                logger.warn(LOG_PREFIX + "Validation error: {}", result.getAllErrorMessages());
            }

            return result;
        } catch (FlywayException e) {
            logger.error(LOG_PREFIX + "Migration validation failed: {}", e.getMessage(), e);
            throw new DatabaseMigrationException(e);
        } finally {
            LogFactory.setLogCreator(null);
        }
    }

    /**
     * Gets information about all migrations (applied and pending).
     *
     * @return Array of MigrationInfo objects
     */
    public MigrationInfo[] info() {
        LogFactory.setLogCreator(new FlywaySlf4jLogCreator(logger));
        final MigrationInfoService infoService = this.flyway.info();
        final MigrationInfo[] migrations = infoService.all();

        logger.info(LOG_PREFIX + "Found {} total migrations ({} applied, {} pending).",
            migrations.length,
            infoService.applied().length,
            infoService.pending().length);

        LogFactory.setLogCreator(null);
        return migrations;
    }

    /**
     * Custom {@link LogCreator} implementation that integrates Flyway logging with SLF4J.
     * Formats log messages with class context and appropriate prefixes for better readability.
     */
    @SuppressWarnings({"LoggingSimilarMessage"})
    private record FlywaySlf4jLogCreator(Logger logger) implements LogCreator {
        private static final String LOG_PREFIX = "[Database|{}] ";
        private static final String NOTICE_PREFIX = "[Database|{}] NOTICE: ";

        private FlywaySlf4jLogCreator(final Logger logger) {
            this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
        }

        @Override
        public Log createLogger(final Class<?> clazz) {
            final String className = Objects.requireNonNull(clazz, "Class cannot be null").getSimpleName();

            return new FlywaySlf4jLog(logger, className);
        }

        /**
         * SLF4J-backed implementation of Flyway's Log interface.
         */
        private record FlywaySlf4jLog(Logger logger, String className) implements Log {
            @Override
            public void debug(final String message) {
                if (logger.isDebugEnabled()) {
                    logger.debug(LOG_PREFIX + "{}", className, message);
                }
            }

            @Override
            public void info(final String message) {
                logger.info(LOG_PREFIX + "{}", className, message);
            }

            @Override
            public void warn(final String message) {
                logger.warn(LOG_PREFIX + "{}", className, message);
            }

            @Override
            public void error(final String message) {
                logger.error(LOG_PREFIX + "{}", className, message);
            }

            @Override
            public void error(final String message, final Exception e) {
                logger.error(LOG_PREFIX + "{}", className, message, e);
            }

            @Override
            public void notice(final String message) {
                logger.info(NOTICE_PREFIX + "{}", className, message);
            }
        }
    }
}
