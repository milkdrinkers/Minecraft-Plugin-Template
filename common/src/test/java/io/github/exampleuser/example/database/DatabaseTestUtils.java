package io.github.exampleuser.example.database;

import io.github.exampleuser.example.database.handler.DatabaseType;
import org.jetbrains.annotations.TestOnly;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Stores utility method used for testing database functionality.
 */
public final class DatabaseTestUtils {
    /**
     * Different database table prefixes used in integrated tests to confirm query operations don't break.
     */
    public enum TablePrefix {
        EMPTY(""),
        NORMAL("test_"),
        COMPLEX("verylong_prefix_");

        private final String tablePrefix;

        TablePrefix(String tablePrefix) {
            this.tablePrefix = tablePrefix;
        }

        public String prefix() {
            return tablePrefix;
        }
    }

    /**
     * Test database container boilerplate for MySQL.
     *
     * @return container
     */
    @SuppressWarnings({"resource"})
    @TestOnly
    public static GenericContainer<?> setupMySQLContainer() {
        return new MySQLContainer<>(DockerImageName.parse("mysql:8.0.31"))
            .withDatabaseName("testing")
            .withUsername("root")
            .withPassword("");
    }

    /**
     * Test database container boilerplate for MySQL.
     *
     * @return container
     */
    @SuppressWarnings({"resource"})
    @TestOnly
    public static GenericContainer<?> setupMariaDBContainer() {
        return new MariaDBContainer<>(DockerImageName.parse("mariadb:10.7"))
            .withDatabaseName("testing")
            .withUsername("root")
            .withPassword("");
    }

    /**
     * {@link DatabaseTestParams} factory method used for MySQL tests.
     *
     * @param tablePrefix the database table prefix to use in the tests
     * @return a database test config object
     */
    @TestOnly
    public static DatabaseTestParams mysql(final TablePrefix tablePrefix) {
        return DatabaseTestParams.builder()
            .withDatabaseType(DatabaseType.MYSQL)
            .withRequiredDatabaseType(DatabaseType.MYSQL)
            .withTablePrefix(tablePrefix.prefix())
            .build();
    }

    /**
     * {@link DatabaseTestParams} factory method used for MariaDB tests.
     *
     * @param tablePrefix the database table prefix to use in the tests
     * @return a database test config object
     */
    @TestOnly
    public static DatabaseTestParams mariadb(final TablePrefix tablePrefix) {
        return DatabaseTestParams.builder()
            .withDatabaseType(DatabaseType.MARIADB)
            .withRequiredDatabaseType(DatabaseType.MARIADB)
            .withTablePrefix(tablePrefix.prefix())
            .build();
    }

    /**
     * {@link DatabaseTestParams} factory method used for SQLite tests.
     *
     * @param tablePrefix the database table prefix to use in the tests
     * @return a database test config object
     */
    @TestOnly
    public static DatabaseTestParams sqlite(final TablePrefix tablePrefix) {
        return DatabaseTestParams.builder()
            .withDatabaseType(DatabaseType.SQLITE)
            .withRequiredDatabaseType(DatabaseType.SQLITE)
            .withTablePrefix(tablePrefix.prefix())
            .build();
    }

    /**
     * {@link DatabaseTestParams} factory method used for H2 tests.
     *
     * @param tablePrefix the database table prefix to use in the tests
     * @return a database test config object
     */
    @TestOnly
    public static DatabaseTestParams h2(final TablePrefix tablePrefix) {
        return DatabaseTestParams.builder()
            .withDatabaseType(DatabaseType.H2)
            .withRequiredDatabaseType(DatabaseType.H2)
            .withTablePrefix(tablePrefix.prefix())
            .build();
    }
}
