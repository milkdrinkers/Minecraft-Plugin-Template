package io.github.exampleuser.example.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

/**
 * Runs agnostic tests for different RDBMS vendors.
 */
public class DatabaseTests {
    @DisplayName("MySQL - Empty Prefix")
    @Nested
    class MySQLTest extends AbstractExternalDatabaseTest {
        @Container
        private static final GenericContainer<?> container = DatabaseTestUtils.setupMySQLContainer();

        public MySQLTest() {
            super(
                container,
                DatabaseTestUtils.mysql(DatabaseTestUtils.TablePrefix.EMPTY)
            );
        }
    }

    @DisplayName("MySQL - Normal Prefix")
    @Nested
    class MySQLTestNormal extends AbstractExternalDatabaseTest {
        @Container
        private static final GenericContainer<?> container = DatabaseTestUtils.setupMySQLContainer();

        public MySQLTestNormal() {
            super(
                container,
                DatabaseTestUtils.mysql(DatabaseTestUtils.TablePrefix.NORMAL)
            );
        }
    }

    @DisplayName("MySQL - Complex Prefix")
    @Nested
    class MySQLTestComplex extends AbstractExternalDatabaseTest {
        @Container
        private static final GenericContainer<?> container = DatabaseTestUtils.setupMySQLContainer();

        public MySQLTestComplex() {
            super(
                container,
                DatabaseTestUtils.mysql(DatabaseTestUtils.TablePrefix.COMPLEX)
            );
        }
    }

    @DisplayName("MariaDB - Empty Prefix")
    @Nested
    class MariaDBTest extends AbstractExternalDatabaseTest {
        @Container
        private static final GenericContainer<?> container = DatabaseTestUtils.setupMariaDBContainer();

        public MariaDBTest() {
            super(
                container,
                DatabaseTestUtils.mariadb(DatabaseTestUtils.TablePrefix.EMPTY)
            );
        }
    }

    @DisplayName("MariaDB - Normal Prefix")
    @Nested
    class MariaDBTestNormal extends AbstractExternalDatabaseTest {
        @Container
        private static final GenericContainer<?> container = DatabaseTestUtils.setupMariaDBContainer();

        public MariaDBTestNormal() {
            super(
                container,
                DatabaseTestUtils.mariadb(DatabaseTestUtils.TablePrefix.NORMAL)
            );
        }
    }

    @DisplayName("MariaDB - Complex Prefix")
    @Nested
    class MariaDBTestComplex extends AbstractExternalDatabaseTest {
        @Container
        private static final GenericContainer<?> container = DatabaseTestUtils.setupMariaDBContainer();

        public MariaDBTestComplex() {
            super(
                container,
                DatabaseTestUtils.mariadb(DatabaseTestUtils.TablePrefix.COMPLEX)
            );
        }
    }

    @DisplayName("SQLite - Empty Prefix")
    @Nested
    class SQLiteTest extends AbstractEmbeddedDatabaseTest {
        public SQLiteTest() {
            super(
                DatabaseTestUtils.sqlite(DatabaseTestUtils.TablePrefix.EMPTY)
            );
        }
    }

    @DisplayName("SQLite - Normal Prefix")
    @Nested
    class SQLiteTestNormal extends AbstractEmbeddedDatabaseTest {
        public SQLiteTestNormal() {
            super(
                DatabaseTestUtils.sqlite(DatabaseTestUtils.TablePrefix.NORMAL)
            );
        }
    }

    @DisplayName("SQLite - Complex Prefix")
    @Nested
    class SQLiteTestComplex extends AbstractEmbeddedDatabaseTest {
        public SQLiteTestComplex() {
            super(
                DatabaseTestUtils.sqlite(DatabaseTestUtils.TablePrefix.COMPLEX)
            );
        }
    }

    @DisplayName("H2 - Empty Prefix")
    @Nested
    class H2Test extends AbstractEmbeddedDatabaseTest {
        public H2Test() {
            super(
                DatabaseTestUtils.h2(DatabaseTestUtils.TablePrefix.EMPTY)
            );
        }
    }

    @DisplayName("H2 - Normal Prefix")
    @Nested
    class H2TestNormal extends AbstractEmbeddedDatabaseTest {
        public H2TestNormal() {
            super(
                DatabaseTestUtils.h2(DatabaseTestUtils.TablePrefix.NORMAL)
            );
        }
    }

    @DisplayName("H2 - Complex Prefix")
    @Nested
    class H2TestComplex extends AbstractEmbeddedDatabaseTest {
        public H2TestComplex() {
            super(
                DatabaseTestUtils.h2(DatabaseTestUtils.TablePrefix.COMPLEX)
            );
        }
    }
}
