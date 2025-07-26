package io.github.exampleuser.exampleplugin.database.handler;

import com.zaxxer.hikari.HikariDataSource;
import io.github.exampleuser.exampleplugin.AbstractService;
import io.github.exampleuser.exampleplugin.ExamplePlugin;
import io.github.exampleuser.exampleplugin.Reloadable;
import io.github.exampleuser.exampleplugin.config.ConfigHandler;
import io.github.exampleuser.exampleplugin.database.config.DatabaseConfig;
import io.github.exampleuser.exampleplugin.database.exception.DatabaseInitializationException;
import io.github.exampleuser.exampleplugin.database.exception.DatabaseMigrationException;
import io.github.exampleuser.exampleplugin.database.jooq.JooqContext;
import io.github.exampleuser.exampleplugin.database.migration.MigrationHandler;
import io.github.exampleuser.exampleplugin.database.pool.ConnectionPoolFactory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Class that handles HikariCP connection pool, jOOQ and Flyway migrations.
 */
public class DatabaseHandler extends AbstractService implements Reloadable {
    private final Logger logger;
    private JooqContext jooqContext;
    private HikariDataSource connectionPool;
    private DatabaseConfig config;
    private final boolean migrateOnStartup;

    /**
     * Instantiates a new Database handler.
     *
     * @param logger        the logger
     */
    private DatabaseHandler(Logger logger, boolean migrateOnStartup) {
        this.logger = logger;
        this.migrateOnStartup = migrateOnStartup;
    }

    /**
     * Instantiates a new Database handler.
     *
     * @param config the database config
     * @param logger         the logger
     */
    @TestOnly
    private DatabaseHandler(@NotNull DatabaseConfig config, Logger logger, boolean migrateOnStartup) {
        this.config = config;
        this.logger = logger;
        this.migrateOnStartup = migrateOnStartup;
    }

    /**
     * On plugin load.
     */
    @Override
    public void onLoad(ExamplePlugin plugin) {
        if (config == null)
            config = DatabaseConfig.fromConfig(plugin.getConfigHandler().getDatabaseConfig());

        try {
            doStartup(); // Start connection pool
        } catch (DatabaseInitializationException e) {
            logger.error("[DB] Database initialization error: {}", e.getMessage());
        } finally {
            if (!isReady()) {
                logger.warn("[DB] Error while initializing database. Functionality will be limited.");
            }
        }
    }

    /**
     * On plugin enable.
     */
    @Override
    public void onEnable(ExamplePlugin plugin) {
    }

    /**
     * On plugin disable.
     */
    @Override
    public void onDisable(ExamplePlugin plugin) {
        try {
            doShutdown();
        } catch (Exception e) {
            logger.error("[DB] Error while shutting down database:", e);
        }
    }

    /**
     * Returns if the database is setup and functioning properly.
     *
     * @return the boolean
     */
    public boolean isReady() {
        return isStarted();
    }

    /**
     * Gets database.
     *
     * @return the database
     */
    public DatabaseType getDB() {
        return getDatabaseConfig().getDatabaseType();
    }

    /**
     * Gets jooq context.
     *
     * @return the jooq context
     */
    public JooqContext getJooqContext() {
        return jooqContext;
    }

    /**
     * Gets connection pool.
     *
     * @return the connection pool
     */
    public HikariDataSource getConnectionPool() {
        return connectionPool;
    }

    /**
     * Gets database config.
     *
     * @return the database config
     */
    public DatabaseConfig getDatabaseConfig() {
        if (config == null)
            throw new IllegalStateException("Database config is still null but was accessed in getDatabaseConfig!");

        return config;
    }

    /**
     * Gets a connection from the connection pool.
     *
     * @return the connection
     * @throws SQLException the sql exception
     */
    @NotNull
    public Connection getConnection() throws SQLException {
        if (connectionPool == null)
            throw new SQLException("[DB] Unable to getConnection a connection from the pool. (connectionPool is null)");

        final Connection connection = connectionPool.getConnection();
        if (connection == null)
            throw new SQLException("[DB] Unable to getConnection a connection from the pool. (connectionPool#getConnection returned null)");

        return connection;
    }

    /**
     * Creates a connection pool using HikariCP and setups jOOQ DSLContext.
     * Should always be followed by running Flyway migrations with {@link #migrate()}.
     */
    @Override
    @ApiStatus.Internal
    public void startup() throws DatabaseInitializationException {
        if (config == null)
            throw new DatabaseInitializationException("Attempted to start a database connection pool but database config is null!");

        if (connectionPool != null)
            throw new DatabaseInitializationException("Attempted to create a new database connection pool while running! (connectionPool is not null)");

        // Initialize connection pool
        connectionPool = ConnectionPoolFactory.create(
            config,
            logger
        );

        // Check if using invalid database type (Note, these cases throw in the previous method)
        try (Connection connection = connectionPool.getConnection()) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            String vendorVersionName = databaseMetaData.getDatabaseProductVersion().toLowerCase();

            if (vendorVersionName.contains("mariadb") && getDB().equals(DatabaseType.MYSQL)) {
                throw new RuntimeException("Attempted to connect to a mariadb database using mysql as database type! (Please change the type to mariadb in database.yml)");
            } else if (vendorVersionName.contains("mysql") && getDB().equals(DatabaseType.MARIADB)) {
                throw new RuntimeException("Attempted to connect to a mysql database using mariadb as database type! (Please change the type to mysql in database.yml)");
            }
        } catch (Throwable t) {
            throw new DatabaseInitializationException(t.getMessage());
        }

        // Disable JOOQ nonsense
        System.setProperty("org.jooq.no-logo", "true");
        System.setProperty("org.jooq.no-tips", "true");

        // Setup JOOQ
        jooqContext = new JooqContext(
            config.getDatabaseType().getSQLDialect(),
            config.getTablePrefix()
        );

        // Migrate
        if (migrateOnStartup)
            migrate();
    }

    /**
     * Closes the connection pool.
     */
    @Override
    @ApiStatus.Internal
    public void shutdown() {
        logger.info("[DB] Shutting down database pool...");

        if (connectionPool == null) {
            logger.error("[DB] Skipped closing database pool because the connection pool is null. Was there a previous error which needs to be fixed? Check your logs!");
            return;
        }

        if (connectionPool.isClosed()) {
            logger.error("[DB] Skipped closing database pool: connection is already closed.");
            return;
        }

        jooqContext = null;
        connectionPool.close();
        connectionPool = null;

        logger.info("[DB] Closed database pool.");
    }

    /**
     * Execute database migrations with Flyway.
     * Should always be run following the database being started using {@link #doStartup()}.
     */
    public void migrate() throws DatabaseInitializationException {
        try {
            new MigrationHandler(
                logger,
                connectionPool,
                config
            )
                .migrate();
        } catch (DatabaseMigrationException e) {
            throw new DatabaseInitializationException("Failed to migrate database schemas to new version! Please backup your database and report the issue.", e);
        }
    }

    /**
     * Get a builder instance for this class.
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The type Database handler builder.
     */
    public static class Builder {
        private ConfigHandler configHandler;
        private Logger logger;
        private DatabaseConfig config;
        private boolean migrateOnStartup = false;

        private Builder() {}

        /**
         * With config handler database handler builder.
         *
         * @param configHandler the config handler
         * @return the database handler builder
         */
        public Builder withConfigHandler(@NotNull ConfigHandler configHandler) {
            this.configHandler = configHandler;
            return this;
        }

        /**
         * With logger database handler builder.
         *
         * @param logger the logger
         * @return the database handler builder
         */
        public Builder withLogger(Logger logger) {
            this.logger = logger;
            return this;
        }

        /**
         * With database config database handler builder.
         *
         * @param config the database config
         * @return the database handler builder
         */
        @TestOnly
        public Builder withConfig(@NotNull DatabaseConfig config) {
            this.config = config;
            return this;
        }

        /**
         * With migrateOnStartup database handler builder.
         *
         * @param migrateOnStartup should the database migrate on startup
         * @return the database handler builder
         */
        public Builder withMigrate(boolean migrateOnStartup) {
            this.migrateOnStartup = migrateOnStartup;
            return this;
        }

        /**
         * Build database handler.
         *
         * @return the database handler
         */
        public DatabaseHandler build() {
            if (configHandler == null && config == null)
                throw new RuntimeException("Failed to build database handler as configHandler and config are null!");

            if (configHandler != null && logger != null)
                return new DatabaseHandler(logger, migrateOnStartup);

            if (config != null && logger != null)
                return new DatabaseHandler(config, logger, migrateOnStartup);

            throw new RuntimeException("Failed to build database handler!");
        }
    }
}
