package com.gameexpert.chat.relay;

import com.gameexpert.chat.service.LocalChatSender;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
@RequiredArgsConstructor
public class ChatRelay implements MessageListener {
    public static final String CHANNEL = "webcraft:chat";
    private static final String FIELD_WORLD_ID = "worldId";
    private static final String FIELD_MESSAGE = "message";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final LocalChatSender localChatSender;

    public void publish(Long worldId, Object message) {
        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put(FIELD_WORLD_ID, worldId);
        envelope.set(FIELD_MESSAGE, objectMapper.valueToTree(message));

        redisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(envelope));
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        JsonNode envelope = objectMapper.readTree(message.getBody());

        localChatSender.send(envelope.path(FIELD_WORLD_ID).asLong(), envelope.path(FIELD_MESSAGE));
    }
}
