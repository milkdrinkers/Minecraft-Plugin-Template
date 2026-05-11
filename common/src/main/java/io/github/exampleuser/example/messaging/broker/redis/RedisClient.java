package io.github.exampleuser.example.messaging.broker.redis;

import io.github.exampleuser.example.messaging.config.Address;
import io.github.exampleuser.example.messaging.config.MessagingConfig;
import io.github.exampleuser.example.messaging.config.SslContextBuilder;
import io.github.exampleuser.example.messaging.exception.MessagingInitializationException;
import redis.clients.jedis.*;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.GeneralSecurityException;
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

        final MessagingConfig.SslConfig ssl = config.ssl();
        if (ssl.enabled()) {
            configBuilder.ssl(true);
            try {
                final SSLContext sslCtx = SslContextBuilder.build(ssl);
                if (sslCtx != null) {
                    configBuilder.sslSocketFactory(sslCtx.getSocketFactory());
                }
            } catch (GeneralSecurityException | IOException e) {
                throw new MessagingInitializationException("Failed to configure SSL for Redis", e);
            }

            if (!ssl.verifyHostname()) {
                configBuilder.hostnameVerifier(SslContextBuilder.hostnameVerifier(ssl));
            }
        }

        final DefaultJedisClientConfig jedisClientConfig = configBuilder.build();

        if (config.addressList().isSingle()) {
            final Address address = config.addressList().getFirst();
            return new redis.clients.jedis.RedisClient.Builder()
                .hostAndPort(new HostAndPort(address.host(), address.getPort().orElse(6379)))
                .clientConfig(jedisClientConfig)
                .build();
        } else {
            return RedisClusterClient.builder()
                .nodes(
                    config.addressList().getAll().stream()
                        .map(address -> new HostAndPort(address.host(), address.getPort().orElse(6379)))
                        .collect(Collectors.toSet())
                )
                .clientConfig(jedisClientConfig)
                .build();
        }
    }

    public void publish(String channel, String message) {
        jedis.publish(channel, message);
    }

    public void subscribe(JedisPubSub subscriber, String channel) {
        jedis.subscribe(subscriber, channel);
    }

    public boolean isAlive() {
        if (jedis instanceof redis.clients.jedis.RedisClient redisClient) {
            return !redisClient.getPool().isClosed();
        } else if (jedis instanceof RedisClusterClient redisClusterClient) {
            return !redisClusterClient.getClusterNodes().isEmpty();
        } else {
            throw new RuntimeException("Unknown jedis type: " + jedis.getClass().getName());
        }
    }

    public void close() {
        jedis.close();
    }
}
