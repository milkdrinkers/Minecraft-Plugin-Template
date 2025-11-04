package io.github.exampleuser.exampleplugin.database.config;

import com.zaxxer.hikari.HikariConfig;
import io.github.exampleuser.exampleplugin.database.exception.DatabaseInitializationException;
import io.github.exampleuser.exampleplugin.database.handler.DatabaseType;

import java.nio.file.Path;
import java.util.Optional;

import static io.github.exampleuser.exampleplugin.database.handler.DatabaseType.SQLITE;

/**
 * Factory class for creating HikariCP connection pool configurations based on the provided DatabaseConfig.
 * This class is responsible for setting up the connection pool parameters such as JDBC URL, username, password,
 * and other pool settings based on the type of database being used.
 */
public final class HikariConfigFactory {
    /**
     * Creates a HikariConfig based on the provided DatabaseConfig.
     *
     * @param config the database configuration
     * @return a HikariConfig instance configured for the specified database
     * @throws DatabaseInitializationException if there is an error during initialization
     */
    @SuppressWarnings("RedundantLabeledSwitchRuleCodeBlock")
    public static HikariConfig get(DatabaseConfig config) throws DatabaseInitializationException {
        final HikariConfig hikariConfig = new HikariConfig();
        final DatabaseType databaseType = config.getDatabaseType();

        // Create RDBMS specific jdbc url
        final String jdbcUrl = switch (databaseType) {
            case SQLITE, H2 -> {
                final Optional<Path> path = config.getPath();
                if (path.isEmpty())
                    throw new DatabaseInitializationException("Path was null when setting up database!");

                final String fileName = databaseType.equals(SQLITE) ? "database.sqlite" : "database";

                yield "jdbc:%s:file:%s%s".formatted(
                    databaseType.getJdbcPrefix(),
                    path.get().resolve(fileName).toAbsolutePath(),
                    databaseType.getDefaultConnectionProperties()
                );
            }
            case MYSQL, MARIADB -> {
                yield "jdbc:%s://%s:%s/%s%s%s".formatted(
                    databaseType.getJdbcPrefix(),
                    config.getHost(),
                    config.getPort(),
                    config.getDatabase(),
                    databaseType.getDefaultConnectionProperties(),
                    config.getConnectionProperties()
                );
            }
        };

        // Set username & password
        switch (databaseType) {
            case SQLITE, H2 -> {
                hikariConfig.setUsername("sa");
                hikariConfig.setPassword("");
            }
            case MYSQL, MARIADB -> {
                hikariConfig.setUsername(config.getUsername());
                hikariConfig.setPassword(config.getPassword());
            }
        }

        // Set pool configuration
        hikariConfig.setMaximumPoolSize(config.getMaxPoolSize());
        hikariConfig.setMinimumIdle(config.getMinIdle());
        hikariConfig.setMaxLifetime(config.getMaxLifeTime());
        hikariConfig.setKeepaliveTime(config.getKeepAliveTime());
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout());

        // Set database driver class and jdbc url
        try {
            Class.forName(databaseType.getDataSourceClassName()); // Load the JDBC driver class
        } catch (LinkageError | ClassNotFoundException e) {
            throw new DatabaseInitializationException("Failed to initialise jdbc driver!", e);
        }
        hikariConfig.setDataSourceClassName(databaseType.getDataSourceClassName());
        hikariConfig.addDataSourceProperty("url", jdbcUrl);

        // Misc pool configuration
        hikariConfig.setPoolName("%s-hikari".formatted(databaseType.getJdbcPrefix()));
        hikariConfig.setAutoCommit(true);
        hikariConfig.setTransactionIsolation("TRANSACTION_REPEATABLE_READ");
        hikariConfig.setIsolateInternalQueries(true);
        hikariConfig.setConnectionInitSql(databaseType.getConnectionInitSql());

        return hikariConfig;
    }
}
