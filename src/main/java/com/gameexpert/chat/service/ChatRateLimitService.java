package com.gameexpert.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRateLimitService {
    private static final int LIMIT = 5;
    private static final int WINDOW_SECONDS = 10;

    private static final RedisScript<Long> ALLOW_SCRIPT = new DefaultRedisScript<>("""
                local count = tonumber(redis.call('GET', KEYS[1]) or '0')
                if count >= tonumber(ARGV[1]) then
                    return 0
                end
                if redis.call('INCR', KEYS[1]) == 1 then
                    redis.call('EXPIRE', KEYS[1], ARGV[2])
                end
                return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public boolean allow(Long playerId) {
        String key = "chat:limit:" + playerId;
        Long allowed = redisTemplate.execute(ALLOW_SCRIPT, List.of(key),
                String.valueOf(LIMIT), String.valueOf(WINDOW_SECONDS));
        return allowed != null && allowed == 1L;
    }
}
