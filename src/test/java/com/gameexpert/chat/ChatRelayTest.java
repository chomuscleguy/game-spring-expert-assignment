package com.gameexpert.chat;

import com.gameexpert.chat.relay.ChatRelay;
import com.gameexpert.chat.relay.ChatSubscriptionConfig;
import com.gameexpert.chat.service.ChatDelivery;
import com.gameexpert.chat.service.LocalChatSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.testcontainers.containers.GenericContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatRelayTest {

    // @Test
    void enablesPubSubInApplicationProperties() throws Exception {
        Properties properties = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/application.properties")) {
            assertNotNull(input);
            properties.load(input);
        }
        assertEquals("true", properties.getProperty("webcraft.chat.pubsub-enabled"),
                "application.properties에서 Pub/Sub을 활성화합니다.");
    }

    // @Test
    void publishesWorldIdAndMessageAsJson() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ObjectMapper mapper = new ObjectMapper();
        LocalChatSender sender = mock(LocalChatSender.class);
        ChatRelay relay = new ChatRelay(redis, mapper, sender);
        Map<String, String> message = Map.of("type", "chat", "content", "안녕하세요");

        relay.publish(7L, message);

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(redis, times(1)).convertAndSend(eq(ChatRelay.CHANNEL), json.capture());
        JsonNode envelope = mapper.readTree(json.getValue());
        assertEquals(7L, envelope.path("worldId").asLong());
        assertEquals(mapper.valueToTree(message), envelope.path("message"));
        verifyNoInteractions(sender);
    }

    // @Test
    void receivedMessageIsSentLocallyWithoutPublishingAgain() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ObjectMapper mapper = new ObjectMapper();
        LocalChatSender sender = mock(LocalChatSender.class);
        ChatRelay relay = new ChatRelay(redis, mapper, sender);
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn("""
                {"worldId":7,"message":{"type":"chat","content":"안녕하세요"}}
                """.getBytes(StandardCharsets.UTF_8));

        relay.onMessage(message, null);

        verify(sender, times(1)).send(7L, mapper.readTree("""
                {"type":"chat","content":"안녕하세요"}
                """));
        verifyNoInteractions(redis);
    }

    // @Test
    void bothServersReceiveOneMessageThroughRedis() throws Exception {
        try (GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
                .withExposedPorts(6379)) {
            redis.start();
            try (AnnotationConfigApplicationContext first = server(redis);
                 AnnotationConfigApplicationContext second = server(redis)) {
                assertEquals(1, first.getBeansOfType(RedisMessageListenerContainer.class).size());
                assertEquals(1, second.getBeansOfType(RedisMessageListenerContainer.class).size());
                Map<String, String> message = Map.of("type", "chat", "content", "서버 간 채팅");
                JsonNode expected = first.getBean(ObjectMapper.class).valueToTree(message);

                first.getBean(ChatRelay.class).publish(23L, message);

                LocalChatSender firstSender = first.getBean(LocalChatSender.class);
                LocalChatSender secondSender = second.getBean(LocalChatSender.class);
                verify(firstSender, timeout(5000).times(1)).send(23L, expected);
                verify(secondSender, timeout(5000).times(1)).send(23L, expected);
                verify(firstSender, after(300).times(1)).send(23L, expected);
                verify(secondSender, after(300).times(1)).send(23L, expected);
                verifyNoMoreInteractions(firstSender, secondSender);
                verify(first.getBean(StringRedisTemplate.class), times(1))
                        .convertAndSend(eq(ChatRelay.CHANNEL), any());
                verify(second.getBean(StringRedisTemplate.class), never())
                        .convertAndSend(anyString(), any());
            }
        }
    }

    private AnnotationConfigApplicationContext server(GenericContainer<?> redis) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                "test", Map.of("webcraft.chat.pubsub-enabled", "true")
        ));
        context.registerBean(LettuceConnectionFactory.class,
                () -> new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379)));
        context.registerBean(StringRedisTemplate.class,
                () -> spy(new StringRedisTemplate(context.getBean(LettuceConnectionFactory.class))));
        context.registerBean(ObjectMapper.class, () -> new ObjectMapper());
        context.registerBean(LocalChatSender.class, () -> mock(LocalChatSender.class));
        context.register(ChatRelay.class, ChatDelivery.class, ChatSubscriptionConfig.class);
        context.refresh();
        return context;
    }
}
