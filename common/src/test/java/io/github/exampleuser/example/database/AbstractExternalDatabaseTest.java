package io.github.exampleuser.example.database;

import io.github.exampleuser.example.database.config.DatabaseConfig;
import io.github.exampleuser.example.database.handler.DatabaseHandler;
import io.github.exampleuser.example.utility.DB;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("externaldatabase")
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractExternalDatabaseTest extends AbstractDatabaseTest {
    @Container
    public static GenericContainer<?> container;

    AbstractExternalDatabaseTest(GenericContainer<?> container, DatabaseTestParams testConfig) {
        super(testConfig);
        AbstractExternalDatabaseTest.container = container;
        container.start();
    }

    @BeforeAll
    @DisplayName("Initialize connection pool")
    void beforeAllTests() {
        Assertions.assertTrue(container.isRunning());

        databaseConfig = DatabaseConfig.builder()
            .withDatabaseType(getTestConfig().databaseType())
            .withDatabase("testing")
            .withHost(container.getHost())
            .withPort(container.getFirstMappedPort())
            .withUsername("root")
            .withPassword("")
            .withTablePrefix(getTestConfig().tablePrefix())
            .build();
        Assertions.assertEquals(getTestConfig().requiredDatabaseType(), databaseConfig.getDatabaseType());

        DB.init(
            DatabaseHandler.builder()
                .withConfig(databaseConfig)
                .withLogger(logger)
                .build()
        );
        DB.getHandler().doStartup();
    }

    @AfterAll
    @Override
    void afterAllTests() {
        super.afterAllTests();
        container.stop();
    }
}
