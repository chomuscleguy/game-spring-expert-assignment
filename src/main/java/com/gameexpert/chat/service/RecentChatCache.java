package com.gameexpert.chat.service;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import com.gameexpert.chat.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class RecentChatCache {
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    private String key(Long worldId, int limit) {
        return "world:" + worldId + ":chat:recent:" + limit;
    }

    public List<ChatMessageResponse> read(Long worldId, int limit) {
        try {
            // TODO Lv 18: 해당 키의 JSON 문자열을 Redis에서 조회합니다.
            String json = null;
            return json == null ? null : Arrays.asList(mapper.readValue(json, ChatMessageResponse[].class));
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    public void write(Long worldId, int limit, List<ChatMessageResponse> messages) {
        try {
            String json = mapper.writeValueAsString(messages);
            // TODO Lv 18: json을 Redis에 저장하고 5초의 TTL을 설정합니다.
        } catch (RuntimeException unavailable) {
            // 캐시는 보조 저장소이므로 DB 조회 결과를 그대로 응답합니다.
        }
    }

    public void invalidate(Long worldId) {
        List<String> keys = IntStream.rangeClosed(1, 100)
                .mapToObj(limit -> key(worldId, limit)).toList();
        try {
            // TODO Lv 18: keys에 담긴 캐시를 Redis에서 삭제합니다.
        } catch (RuntimeException unavailable) {
            // 무효화에 실패한 캐시는 최대 5초 뒤 만료됩니다.
        }
    }
}
