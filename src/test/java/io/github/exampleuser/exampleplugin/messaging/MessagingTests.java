package io.github.exampleuser.exampleplugin.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

/**
 * Runs agnostic tests for different message brokers.
 */
public class MessagingTests {
    @DisplayName("Database")
    @Nested
    class DatabaseTest extends AbstractEmbeddedMessagingTest {
        public DatabaseTest() {
            super(
                MessengerTestUtils.database()
            );
        }
    }

    @DisplayName("Redis")
    @Nested
    class RedisTest extends AbstractExternalMessagingTest {
        @Container
        private static final GenericContainer<?> container = MessengerTestUtils.setupRedisContainer();

        public RedisTest() {
            super(
                container,
                MessengerTestUtils.redis()
            );
        }
    }

    @DisplayName("Nats")
    @Nested
    class NatsTest extends AbstractExternalMessagingTest {
        @Container
        private static final GenericContainer<?> container = MessengerTestUtils.setupNatsContainer();

        public NatsTest() {
            super(
                container,
                MessengerTestUtils.nats()
            );
        }
    }

    @DisplayName("RabbitMQ")
    @Nested
    class RabbitMQTest extends AbstractExternalMessagingTest {
        @Container
        private static final GenericContainer<?> container = MessengerTestUtils.setupRabbitMQContainer();

        public RabbitMQTest() {
            super(
                container,
                MessengerTestUtils.rabbitmq()
            );
        }
    }
}
