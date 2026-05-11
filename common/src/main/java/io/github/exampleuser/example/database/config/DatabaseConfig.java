package io.github.exampleuser.example.database.config;

import io.github.exampleuser.example.database.handler.DatabaseType;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The type Database config.
 */
public class DatabaseConfig {
    // Essential details
    private final DatabaseType databaseType;
    private String tablePrefix;

    // Connection details
    private final @Nullable Path path;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;

    private final boolean repair;

    // Pool options
    private final int maxPoolSize;
    private final int minIdle;
    private final long maxLifeTime;
    private final long keepAliveTime;
    private final long connectionTimeout;

    // JDBC properties
    private final Map<String, Object> connectionProperties;

    private final SslConfig sslConfig;

    /**
     * Immutable SSL/TLS configuration snapshot for the database connection.
     */
    public record SslConfig(
        boolean enabled,
        String mode,
        String caPath,
        String certPath,
        String keyPath
    ) {
        static final SslConfig DISABLED = new SslConfig(false, "require", "", "", "");
    }

    /**
     * Instantiates a new Database config.
     *
     * @param databaseType         the database type
     * @param tablePrefix          the table prefix
     * @param path                 the path
     * @param host                 the host
     * @param port                 the port
     * @param database             the database
     * @param username             the username
     * @param password             the password
     * @param repair               the repair
     * @param maxPoolSize          the max pool size
     * @param minIdle              the min idle
     * @param maxLifeTime          the max life time
     * @param keepAliveTime        the keep alive time
     * @param connectionTimeout    the connection timeout
     * @param connectionProperties the connection properties
     * @param sslConfig            the SSL configuration
     */
    private DatabaseConfig(
        DatabaseType databaseType,
        String tablePrefix,
        @Nullable Path path,
        String host,
        int port,
        String database,
        String username,
        String password,
        boolean repair,
        int maxPoolSize,
        int minIdle,
        long maxLifeTime,
        long keepAliveTime,
        long connectionTimeout,
        Map<String, Object> connectionProperties,
        SslConfig sslConfig
    ) {
        this.databaseType = databaseType;
        this.tablePrefix = tablePrefix;
        this.path = path;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.repair = repair;
        this.maxPoolSize = maxPoolSize;
        this.minIdle = minIdle;
        this.maxLifeTime = maxLifeTime;
        this.keepAliveTime = keepAliveTime;
        this.connectionTimeout = connectionTimeout;
        this.connectionProperties = connectionProperties;
        this.sslConfig = sslConfig;
    }

    /**
     * Gets database type.
     *
     * @return the database type
     */
    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    /**
     * Gets path.
     *
     * @return the path
     */
    public Optional<Path> getPath() {
        return Optional.ofNullable(path);
    }

    /**
     * Sets table prefix.
     *
     * @param tablePrefix the table prefix
     */
    @TestOnly
    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    /**
     * Gets table prefix.
     *
     * @return the table prefix
     */
    public String getTablePrefix() {
        return tablePrefix;
    }

    /**
     * Gets host.
     *
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * Gets port.
     *
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets database.
     *
     * @return the database
     */
    public String getDatabase() {
        return database;
    }

    /**
     * Gets username.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets password.
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Is repair boolean.
     *
     * @return the boolean
     */
    public boolean isRepair() {
        return repair;
    }

    /**
     * Gets max pool size.
     *
     * @return the max pool size
     */
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    /**
     * Gets min idle.
     *
     * @return the min idle
     */
    public int getMinIdle() {
        return minIdle;
    }

    /**
     * Gets max life time.
     *
     * @return the max life time
     */
    public long getMaxLifeTime() {
        return maxLifeTime;
    }

    /**
     * Gets keep alive time.
     *
     * @return the keep alive time
     */
    public long getKeepAliveTime() {
        return keepAliveTime;
    }

    /**
     * Gets connection timeout.
     *
     * @return the connection timeout
     */
    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Gets the SSL configuration.
     *
     * @return the SSL config
     */
    public SslConfig getSslConfig() {
        return sslConfig;
    }

    /**
     * Gets connection properties.
     *
     * @return the connection properties
     */
    public String getConnectionProperties() {
        return getDatabaseType().formatJdbcConnectionProperties(connectionProperties).isEmpty() ? "" : getDatabaseType().getJdbcPropertySeparator() + getDatabaseType().formatJdbcConnectionProperties(connectionProperties);
    }

    /**
     * Gets database config from file.
     *
     * @param cfg the cfg
     * @return the database config from file
     */
    public static DatabaseConfig fromConfig(io.github.exampleuser.example.config.DatabaseConfig cfg, Path databasePath) {
        return builder()
            .withDatabaseType(cfg.database.type)
            .withTablePrefix(cfg.database.tablePrefix)
            .withPath(databasePath)
            .withHost(cfg.database.host)
            .withPort(cfg.database.port)
            .withDatabase(cfg.database.database)
            .withUsername(cfg.database.username)
            .withPassword(cfg.database.password)
            .withRepair(cfg.database.advanced.repair)
            .withMaxPoolSize(cfg.database.advanced.connectionPool.maxpoolsize)
            .withMinIdle(cfg.database.advanced.connectionPool.minidle)
            .withMaxLifeTime(cfg.database.advanced.connectionPool.maxlifetime)
            .withKeepAliveTime(cfg.database.advanced.connectionPool.keepalivetime)
            .withConnectionTimeout(cfg.database.advanced.connectionPool.connectiontimeout)
            .withConnectionProperties(cfg.database.advanced.connectionProperties)
            .withSslConfig(new SslConfig(
                cfg.database.advanced.ssl.enabled,
                cfg.database.advanced.ssl.mode,
                cfg.database.advanced.ssl.caPath,
                cfg.database.advanced.ssl.certPath,
                cfg.database.advanced.ssl.keyPath
            ))
            .build();
    }

    /**
     * Get a config builder instance.
     *
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The type Database config builder.
     */
    public static class Builder {
        private Builder() {
        }

        private @Nullable DatabaseType databaseType;
        private @Nullable String tablePrefix;

        private @Nullable Path path;
        private @Nullable String host;
        private @Nullable Integer port;
        private @Nullable String database;
        private @Nullable String username;
        private @Nullable String password;

        private @Nullable Boolean repair;

        private @Nullable Integer maxPoolSize;
        private @Nullable Integer minIdle;
        private @Nullable Long maxLifeTime;
        private @Nullable Long keepAliveTime;
        private @Nullable Long connectionTimeout;

        private @Nullable Map<String, Object> connectionProperties;
        private @Nullable SslConfig sslConfig;

        /**
         * With database type database config builder.
         *
         * @param type the type
         * @return the database config builder
         */
        public Builder withDatabaseType(DatabaseType type) {
            this.databaseType = type;
            return this;
        }

        /**
         * With table prefix database config builder.
         *
         * @param tablePrefix the table prefix
         * @return the database config builder
         */
        public Builder withTablePrefix(String tablePrefix) {
            this.tablePrefix = tablePrefix;
            return this;
        }

        /**
         * With path database config builder.
         *
         * @param path the path
         * @return the database config builder
         */
        public Builder withPath(Path path) {
            this.path = path;
            return this;
        }

        /**
         * With host database config builder.
         *
         * @param host the host
         * @return the database config builder
         */
        public Builder withHost(String host) {
            this.host = host;
            return this;
        }

        /**
         * With port database config builder.
         *
         * @param port the port
         * @return the database config builder
         */
        public Builder withPort(Integer port) {
            this.port = port;
            return this;
        }

        /**
         * With database database config builder.
         *
         * @param database the database
         * @return the database config builder
         */
        public Builder withDatabase(String database) {
            this.database = database;
            return this;
        }

        /**
         * With username database config builder.
         *
         * @param username the username
         * @return the database config builder
         */
        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        /**
         * With password database config builder.
         *
         * @param password the password
         * @return the database config builder
         */
        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        /**
         * With repair database config builder.
         *
         * @param repair the repair
         * @return the database config builder
         */
        public Builder withRepair(boolean repair) {
            this.repair = repair;
            return this;
        }

        /**
         * With max pool size database config builder.
         *
         * @param maxPoolSize the max pool size
         * @return the database config builder
         */
        public Builder withMaxPoolSize(Integer maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        /**
         * With min idle database config builder.
         *
         * @param minIdle the min idle
         * @return the database config builder
         */
        public Builder withMinIdle(Integer minIdle) {
            this.minIdle = minIdle;
            return this;
        }

        /**
         * With max life time database config builder.
         *
         * @param maxLifeTime the max life time
         * @return the database config builder
         */
        public Builder withMaxLifeTime(Long maxLifeTime) {
            this.maxLifeTime = maxLifeTime;
            return this;
        }

        /**
         * With keep alive time database config builder.
         *
         * @param keepAliveTime the keep alive time
         * @return the database config builder
         */
        public Builder withKeepAliveTime(Long keepAliveTime) {
            this.keepAliveTime = keepAliveTime;
            return this;
        }

        /**
         * With connection timeout database config builder.
         *
         * @param connectionTimeout the connection timeout
         * @return the database config builder
         */
        public Builder withConnectionTimeout(Long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        /**
         * With connection properties database config builder.
         *
         * @param connectionProperties the connection properties
         * @return the database config builder
         */
        public Builder withConnectionProperties(Map<String, Object> connectionProperties) {
            this.connectionProperties = connectionProperties;
            return this;
        }

        /**
         * With SSL config database config builder.
         *
         * @param sslConfig the SSL configuration
         * @return the database config builder
         */
        public Builder withSslConfig(SslConfig sslConfig) {
            this.sslConfig = sslConfig;
            return this;
        }

        /**
         * Build database config.
         *
         * @return the database config
         */
        public DatabaseConfig build() {
            if (databaseType == null)
                databaseType = DatabaseType.SQLITE;

            if (tablePrefix == null)
                tablePrefix = "";

            if (host == null)
                host = "localhost";

            if (port == null)
                port = 3306;

            if (database == null)
                database = "database_name";

            if (username == null)
                username = "root";

            if (password == null)
                password = "";

            if (repair == null)
                repair = false;

            if (maxPoolSize == null)
                maxPoolSize = 10;

            if (minIdle == null)
                minIdle = 10;

            if (maxLifeTime == null)
                maxLifeTime = 180_000L;

            if (keepAliveTime == null)
                keepAliveTime = 60_000L;

            if (connectionTimeout == null)
                connectionTimeout = 20_000L;

            if (connectionProperties == null) {
                connectionProperties = new HashMap<>();

                if (databaseType.equals(DatabaseType.MYSQL) || databaseType.equals(DatabaseType.MARIADB)) {
                    connectionProperties.putIfAbsent("useSSL", "false");
                    connectionProperties.putIfAbsent("cachePrepStmts", true);
                    connectionProperties.putIfAbsent("prepStmtCacheSize", 250);
                    connectionProperties.putIfAbsent("prepStmtCacheSqlLimit", 2048L);
                    connectionProperties.putIfAbsent("useServerPrepStmts", true);
                    connectionProperties.putIfAbsent("useLocalSessionState", true);
                    connectionProperties.putIfAbsent("rewriteBatchedStatements", true);
                    connectionProperties.putIfAbsent("cacheResultSetMetadata", true);
                    connectionProperties.putIfAbsent("cacheServerConfiguration", true);
                    connectionProperties.putIfAbsent("elideSetAutoCommits", true);
                    connectionProperties.putIfAbsent("maintainTimeStats", false);
                }
            }

            if (sslConfig == null)
                sslConfig = SslConfig.DISABLED;

            return new DatabaseConfig(databaseType, tablePrefix, path, host, port, database, username, password, repair, maxPoolSize, minIdle, maxLifeTime, keepAliveTime, connectionTimeout, connectionProperties, sslConfig);
        }
    }
}
