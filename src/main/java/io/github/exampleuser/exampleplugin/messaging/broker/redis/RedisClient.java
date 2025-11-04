package io.github.exampleuser.exampleplugin.messaging.broker.redis;

import io.github.exampleuser.exampleplugin.messaging.config.Address;
import io.github.exampleuser.exampleplugin.messaging.config.MessagingConfig;
import redis.clients.jedis.*;

import java.util.stream.Collectors;

/**
 * A wrapping client implementation of Jedis
 */
final class RedisClient {
    private final UnifiedJedis jedis;

    RedisClient(MessagingConfig config) {
        this.jedis = createJedisClient(config);
    }

    private UnifiedJedis createJedisClient(MessagingConfig config) {
        final DefaultJedisClientConfig.Builder configBuilder = DefaultJedisClientConfig.builder()
            .timeoutMillis(Protocol.DEFAULT_TIMEOUT);

        switch (config.authMethod()) {
            case "password" -> {
                if (!config.username().isEmpty()) {
                    configBuilder.user(config.username()).password(config.password());
                } else if (!config.password().isEmpty()) {
                    configBuilder.password(config.password());
                }
            }
            case "token" -> configBuilder.password(config.authToken());
        }

        if (config.ssl())
            configBuilder.ssl(true);

        final DefaultJedisClientConfig jedisClientConfig = configBuilder.build();

        if (config.addressList().isSingle()) {
            final Address address = config.addressList().getFirst();
            return new JedisPooled(
                new HostAndPort(
                    address.host(),
                    address.getPort().orElse(6379)
                ),
                jedisClientConfig
            );
        } else {
            return new JedisCluster(
                config.addressList().getAll().stream()
                    .map(address -> new HostAndPort(
                        address.host(),
                        address.getPort().orElse(6379)
                    ))
                    .collect(Collectors.toSet()),
                jedisClientConfig
            );
        }
    }

    public void publish(String channel, String message) {
        jedis.publish(channel, message);
    }

    public void subscribe(JedisPubSub subscriber, String channel) {
        jedis.subscribe(subscriber, channel);
    }

    public boolean isAlive() {
        if (jedis instanceof JedisPooled jedisPooled) {
            return !jedisPooled.getPool().isClosed();
        } else if (jedis instanceof JedisCluster jedisCluster) {
            return !jedisCluster.getClusterNodes().isEmpty();
        } else {
            throw new RuntimeException("Unknown jedis type: " + jedis.getClass().getName());
        }
    }

    public void close() {
        jedis.close();
    }
}
