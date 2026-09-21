package com.gameexpert.chat;

import com.gameexpert.chat.dto.ChatMessageResponse;
import com.gameexpert.chat.service.RecentChatCache;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class RecentChatCacheTest {
    private static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7.4-alpine")
    ).withExposedPorts(6379);
    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redis;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private RecentChatCache cache;

    @BeforeAll
    static void startRedis() {
        REDIS.start();
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        redis = new StringRedisTemplate(connectionFactory);
    }

    @AfterAll
    static void stopRedis() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
        REDIS.stop();
    }

    @BeforeEach
    void setUp() {
        redis.delete(IntStream.rangeClosed(1, 100).boxed()
                .flatMap(limit -> List.of(key(1L, limit), key(2L, limit)).stream()).toList());
        cache = new RecentChatCache(redis, mapper);
    }

    @Test
    void readsStoredMessagesAndEmptyListWithoutExtendingTtl() {
        List<ChatMessageResponse> expected = messages("hello");
        redis.opsForValue().set(key(1L, 2), mapper.writeValueAsString(expected), Duration.ofSeconds(2));
        redis.opsForValue().set(key(1L, 3), "[]");
        redis.opsForValue().set(key(2L, 2), mapper.writeValueAsString(messages("other")));

        assertThat(cache.read(1L, 2)).usingRecursiveComparison().isEqualTo(expected);
        assertThat(cache.read(1L, 3)).isEmpty();
        assertThat(cache.read(2L, 2)).extracting(ChatMessageResponse::getContent).containsExactly("other");
        assertThat(cache.read(2L, 3)).isNull();
        assertThat(redis.getExpire(key(1L, 2), TimeUnit.MILLISECONDS)).isBetween(1L, 2_000L);
    }

    @Test
    void writesMessagesWithFiveSecondTtlAndKeepsOtherKeys() {
        List<ChatMessageResponse> expected = messages("hello");
        redis.opsForValue().set(key(1L, 3), "same-world");
        redis.opsForValue().set(key(2L, 2), "other-world");

        cache.write(1L, 2, expected);

        String json = redis.opsForValue().get(key(1L, 2));
        assertThat(json).isNotNull();
        assertThat(mapper.readTree(json)).isEqualTo(mapper.valueToTree(expected));
        assertThat(redis.getExpire(key(1L, 2), TimeUnit.MILLISECONDS)).isBetween(1L, 5_000L);
        assertThat(redis.opsForValue().get(key(1L, 3))).isEqualTo("same-world");
        assertThat(redis.opsForValue().get(key(2L, 2))).isEqualTo("other-world");
    }

    @Test
    void writesEmptyListWithExpiry() {
        cache.write(1L, 2, List.of());

        String json = redis.opsForValue().get(key(1L, 2));
        assertThat(json).isNotNull();
        assertThat(mapper.readTree(json)).isEqualTo(mapper.readTree("[]"));
        assertThat(redis.getExpire(key(1L, 2), TimeUnit.MILLISECONDS)).isBetween(1L, 5_000L);
    }

    @Test
    void deletesAllLimitsOfOnlyTheSelectedWorld() {
        IntStream.rangeClosed(1, 100).forEach(limit -> redis.opsForValue().set(key(1L, limit), "[]"));
        redis.opsForValue().set(key(2L, 2), "[]");

        cache.invalidate(1L);

        IntStream.rangeClosed(1, 100).forEach(limit ->
                assertThat(redis.hasKey(key(1L, limit))).as("limit=%s", limit).isFalse());
        assertThat(redis.hasKey(key(2L, 2))).isTrue();
    }

    private static String key(Long worldId, int limit) {
        return "world:" + worldId + ":chat:recent:" + limit;
    }

    private static List<ChatMessageResponse> messages(String content) {
        return List.of(new ChatMessageResponse("Tester", content, LocalDateTime.of(2026, 1, 1, 12, 0)));
    }
}
