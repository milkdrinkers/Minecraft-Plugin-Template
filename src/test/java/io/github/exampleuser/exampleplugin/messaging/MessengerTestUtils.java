package io.github.exampleuser.exampleplugin.messaging;

import com.redis.testcontainers.RedisContainer;
import org.jetbrains.annotations.TestOnly;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Stores utility method used for testing message broker functionality.
 */
final class MessengerTestUtils {
    /**
     * Test messaging broker container boilerplate for Redis.
     *
     * @return container
     */
    @SuppressWarnings({"resource"})
    @TestOnly
    public static GenericContainer<RedisContainer> setupRedisContainer() {
        return new RedisContainer(DockerImageName.parse("redis:7.4.5-alpine"))
            .withExposedPorts(6379);
    }

    /**
     * Test messaging broker container boilerplate for Nats.
     *
     * @return container
     */
    @SuppressWarnings({"resource"})
    @TestOnly
    public static GenericContainer<?> setupNatsContainer() {
        return new GenericContainer<>(DockerImageName.parse("nats:2.11.6-alpine"))
            .withExposedPorts(4222);
    }

    /**
     * Test messaging broker container boilerplate for RabbitMQ.
     *
     * @return container
     */
    @SuppressWarnings({"resource"})
    @TestOnly
    public static GenericContainer<RabbitMQContainer> setupRabbitMQContainer() {
        return new RabbitMQContainer(DockerImageName.parse("rabbitmq:4.1.2-management-alpine"))
            .withExposedPorts(5672)
            ;
    }

    /**
     * {@link MessengerTestParams} factory method used for Database tests.
     *
     * @return a messenger test config object
     */
    @TestOnly
    public static MessengerTestParams database() {
        return MessengerTestParams.builder()
            .withType("sql")
            .build();
    }


    /**
     * {@link MessengerTestParams} factory method used for Redis tests.
     *
     * @return a messenger test config object
     */
    @TestOnly
    public static MessengerTestParams redis() {
        return MessengerTestParams.builder()
            .withType("redis")
            .build();
    }

    /**
     * {@link MessengerTestParams} factory method used for Nats tests.
     *
     * @return a messenger test config object
     */
    @TestOnly
    public static MessengerTestParams nats() {
        return MessengerTestParams.builder()
            .withType("nats")
            .build();
    }

    /**
     * {@link MessengerTestParams} factory method used for RabbitMQ tests.
     *
     * @return a messenger test config object
     */
    @TestOnly
    public static MessengerTestParams rabbitmq() {
        return MessengerTestParams.builder()
            .withType("rabbitmq")
            .build();
    }
}
