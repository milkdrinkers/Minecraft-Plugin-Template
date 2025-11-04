package io.github.exampleuser.exampleplugin.messaging;

import io.github.exampleuser.exampleplugin.messaging.config.MessagingConfig;
import io.github.exampleuser.exampleplugin.utility.Messaging;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("externalmessaging")
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractExternalMessagingTest extends AbstractMessagingTest {
    @Container
    public static GenericContainer<?> container;

    AbstractExternalMessagingTest(GenericContainer<?> container, MessengerTestParams testConfig) {
        super(testConfig);
        AbstractExternalMessagingTest.container = container;
        container.start();
    }

    @BeforeAll
    @DisplayName("Initialize message broker")
    void beforeAllTests() {
        Assertions.assertTrue(container.isRunning());

        final String username = switch (getTestConfig().type()) {
            case "redis" -> "default";
            case "rabbitmq" -> "guest";
            default -> "";
        };

        final String password = switch (getTestConfig().type()) {
            case "redis" -> "default";
            case "rabbitmq" -> "guest";
            default -> "";
        };

        messagingConfig = MessagingConfig.builder()
            .withEnabled(true)
            .withBroker(getTestConfig().type())
            .withAddresses("%s:%s".formatted(container.getHost(), container.getFirstMappedPort()))
            .withUsername(username)
            .withPassword(password)
            .withSSL(false)
            .withRabbitMq("/")
            .build();

        Messaging.init(
            MessagingHandler.builder()
                .withConfig(messagingConfig)
                .withTesting(true)
                .withLogger(logger)
                .withName("Test")
                .withTaskAdapter(new MockTaskAdapter())
                .withReceiverAdapter(new MockReceiverAdapter())
                .build()
        );
        Messaging.getHandler().doStartup();
        Messaging.getHandler().scheduleTasks();
    }

    @AfterAll
    @Override
    void afterAllTests() {
        super.afterAllTests();
        container.stop();
    }
}
