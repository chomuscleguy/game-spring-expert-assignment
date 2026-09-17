package com.gameexpert.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.gameexpert.chat.service.ChatRateLimitService;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

class ChatRateLimitTest {
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
    void allowsFiveMessagesAndRejectsSixth() {
        ChatRateLimitService service = new ChatRateLimitService(redisTemplate);
        List<Boolean> results = IntStream.range(0, 6)
                .mapToObj(index -> service.allow(101L))
                .toList();

        assertThat(results).containsExactly(true, true, true, true, true, false);
        assertThat(redisTemplate.getExpire("chat:limit:101", TimeUnit.MILLISECONDS))
                .isBetween(1L, 10_000L);
        assertThat(service.allow(102L)).as("다른 플레이어의 제한은 독립적입니다").isTrue();
        ChatRateLimitService reconnectedService = new ChatRateLimitService(redisTemplate);
        assertThat(reconnectedService.allow(101L))
                .as("서비스 인스턴스가 달라도 같은 플레이어의 제한을 공유합니다")
                .isFalse();
    }

    // @Test
    void subsequentMessagesMustNotExtendTheOriginalWindow() {
        String key = "chat:limit:401";
        redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(5));
        ChatRateLimitService service = new ChatRateLimitService(redisTemplate);

        assertThat(service.allow(401L)).isTrue();
        assertThat(redisTemplate.getExpire(key, TimeUnit.MILLISECONDS))
                .as("메시지를 추가로 보내도 최초 요청에서 시작한 제한 시간이 연장되지 않습니다")
                .isBetween(1L, 5_000L);
    }

    // @Test
    void concurrentMessagesMustNotExceedFive() throws Exception {
        int requestCount = 12;
        String key = "chat:limit:201";
        redisTemplate.opsForValue().set(key, "0", Duration.ofSeconds(10));
        CyclicBarrier readsCompleted = new CyclicBarrier(requestCount);
        ValueOperations<String, String> realValues = redisTemplate.opsForValue();
        ValueOperations<String, String> watchedValues = spy(realValues);
        StringRedisTemplate watchedTemplate = spy(redisTemplate);
        doReturn(watchedValues).when(watchedTemplate).opsForValue();
        doAnswer(invocation -> {
            String value = realValues.get((String) invocation.getArgument(0));
            // 실제 Redis 조회를 끝낸 요청들이 동시에 다음 명령으로 넘어가게 합니다.
            readsCompleted.await(10, TimeUnit.SECONDS);
            return value;
        }).when(watchedValues).get(anyString());
        ChatRateLimitService service = new ChatRateLimitService(watchedTemplate);

        try (ExecutorService executor = Executors.newFixedThreadPool(requestCount)) {
            List<Future<Boolean>> requests = IntStream.range(0, requestCount)
                    .mapToObj(index -> executor.submit(() -> service.allow(201L)))
                    .toList();
            long accepted = requests.stream().filter(ChatRateLimitTest::accepted).count();
            assertThat(accepted)
                    .as("동시에 요청해도 같은 플레이어의 메시지는 5개까지만 허용합니다")
                    .isEqualTo(5);
        }
        assertThat(redisTemplate.getExpire(key, TimeUnit.MILLISECONDS)).isBetween(1L, 10_000L);
    }

    // @Test
    void acceptsMessagesAgainAfterTheWindowExpires() {
        ChatRateLimitService service = new ChatRateLimitService(redisTemplate);
        String key = "chat:limit:301";
        redisTemplate.opsForValue().set(key, "5", Duration.ofSeconds(1));
        assertThat(service.allow(301L)).isFalse();
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(3))
                .until(() -> !Boolean.TRUE.equals(redisTemplate.hasKey(key)));
        assertThat(service.allow(301L)).isTrue();
        assertThat(redisTemplate.getExpire(key, TimeUnit.MILLISECONDS)).isBetween(1L, 10_000L);
    }

    private static boolean accepted(Future<Boolean> request) {
        try {
            return request.get(15, TimeUnit.SECONDS);
        } catch (Exception failure) {
            throw new AssertionError("동시 요청이 정상적으로 완료되어야 합니다", failure);
        }
    }
}
