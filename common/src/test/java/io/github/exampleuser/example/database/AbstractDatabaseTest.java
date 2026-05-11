package io.github.exampleuser.example.database;

import io.github.exampleuser.example.database.config.DatabaseConfig;
import io.github.exampleuser.example.database.exception.DatabaseInitializationException;
import io.github.exampleuser.example.utility.DB;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

/**
 * Contains all test cases.
 */
@Tag("database")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
abstract class AbstractDatabaseTest {
    private final DatabaseTestParams testConfig;
    public DatabaseConfig databaseConfig;
    public Logger logger = LoggerFactory.getLogger("Database");

    AbstractDatabaseTest(DatabaseTestParams testConfig) {
        this.testConfig = testConfig;
    }

    /**
     * Exposes the database parameters of this test.
     *
     * @return the database test config
     */
    public DatabaseTestParams getTestConfig() {
        return testConfig;
    }

    @BeforeEach
    void beforeEachTest() {
    }

    @AfterEach
    void afterEachTest() {
    }

    @AfterAll
    void afterAllTests() {
        DB.getHandler().doShutdown(); // Shut down the connection pool after all tests have been run
    }

    @Test
    @Order(1) // This forces migrations to be run before any other queries are tested (User queries won't work if the migrations failed)
    @DisplayName("Flyway migrations")
    void testMigrations() throws DatabaseInitializationException {
        DB.getHandler().migrate();
    }

    @Test
    @DisplayName("Upsert")
    void testUpsert() {
        Queries.upsert();
        Queries.upsert(); // Updates instead of inserts
    }

    @Test
    @DisplayName("Upsert Returning")
    void testUpsertReturning() {
        BigInteger value = Queries.upsertReturning();
        Assertions.assertNotNull(value);
        Assertions.assertEquals(BigInteger.valueOf(1), value);
        BigInteger value2 = Queries.upsertReturning();
        Assertions.assertNotNull(value2);
        Assertions.assertEquals(BigInteger.valueOf(2), value2);
    }

    @Test
    @DisplayName("Batch")
    void testQueryBatch() {
        Queries.saveAll();
    }

    @Test
    @DisplayName("Transaction")
    void testQueryTransaction() {
        Queries.saveAllTransaction();
    }

    @Test
    @DisplayName("Select")
    void testQuerySelect() {
        Queries.loadAll();
    }
}
