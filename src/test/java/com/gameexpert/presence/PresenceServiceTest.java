package com.gameexpert.presence;

import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import static org.assertj.core.api.Assertions.assertThat;

class PresenceServiceTest {
    private static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7.4-alpine")
    ).withExposedPorts(6379);
    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;

    @BeforeAll
    static void startRedis() {
        REDIS.start();
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        redisTemplate = new StringRedisTemplate(connectionFactory);
    }

    @AfterAll
    static void stopRedis() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
        REDIS.stop();
    }

    // @Test
    void joinStoresConnectionWithExpiryScoreInRequestedWorld() {
        PresenceService service = new PresenceService(redisTemplate);
        String connectionId = UUID.randomUUID().toString();
        long before = System.currentTimeMillis();
        service.join(101L, connectionId);
        long after = System.currentTimeMillis();

        assertThat(redisTemplate.opsForZSet().score("world:101:presence", connectionId))
                .isNotNull().isBetween((double) before + 90_000, (double) after + 90_000);
        assertThat(redisTemplate.opsForZSet().score("world:102:presence", connectionId)).isNull();
    }

    // @Test
    void leaveRemovesOnlySpecifiedConnectionFromSpecifiedWorld() {
        String connectionId = UUID.randomUUID().toString();
        String otherConnection = UUID.randomUUID().toString();
        redisTemplate.opsForZSet().add("world:201:presence", connectionId, 9999999999999D);
        redisTemplate.opsForZSet().add("world:201:presence", otherConnection, 9999999999999D);
        redisTemplate.opsForZSet().add("world:202:presence", connectionId, 9999999999999D);

        new PresenceService(redisTemplate).leave(201L, connectionId);

        assertThat(redisTemplate.opsForZSet().score("world:201:presence", connectionId)).isNull();
        assertThat(redisTemplate.opsForZSet().score("world:201:presence", otherConnection)).isNotNull();
        assertThat(redisTemplate.opsForZSet().score("world:202:presence", connectionId)).isNotNull();
    }
}
