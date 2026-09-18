package com.gameexpert.presence;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Clock;

import org.springframework.data.redis.connection.RedisZSetCommands.ZAddArgs;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PresenceService implements com.gameexpert.api.PresenceOperations {

    private static final Duration TTL = Duration.ofSeconds(90);
    private static final Duration KEY_TTL = Duration.ofSeconds(180);

    private final StringRedisTemplate redisTemplate;
    private final Clock clock = Clock.systemUTC();

    private String key(Long worldId) {
        return "world:" + worldId + ":presence";
    }

    public long onlineCount(Long worldId) {
        String key = key(worldId);
        double now = clock.millis();
        redisTemplate.opsForZSet().removeRangeByScore(key, Double.NEGATIVE_INFINITY, now);
        Long size = redisTemplate.opsForZSet().zCard(key);
        return (size != null) ? size : 0L;
    }

    public void join(Long worldId, String connectionId) {
        String key = key(worldId);
        redisTemplate.opsForZSet().add(key, connectionId, expiresAt());
        redisTemplate.expire(key, KEY_TTL);
    }

    public void leave(Long worldId, String connectionId) {
        redisTemplate.opsForZSet().remove(key(worldId), connectionId);
    }

    public void heartbeat(Long worldId, String connectionId) {
        String key = key(worldId);
        renewExisting(key, connectionId);
        redisTemplate.expire(key, KEY_TTL);
    }

    // 종료된 연결을 다시 추가하지 않도록 ZADD XX로 기존 원소의 점수만 갱신합니다.
    private void renewExisting(String key, String connectionId) {
        byte[] rawKey = key.getBytes(StandardCharsets.UTF_8);
        byte[] rawMember = connectionId.getBytes(StandardCharsets.UTF_8);
        double score = expiresAt();
        redisTemplate.execute((RedisCallback<Boolean>) connection -> connection.zSetCommands()
                .zAdd(rawKey, score, rawMember, ZAddArgs.ifExists()));
    }

    private double expiresAt() {
        return clock.millis() + TTL.toMillis();
    }
}
