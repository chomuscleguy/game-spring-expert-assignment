package com.gameexpert.ws;

import java.time.LocalDateTime;
import java.util.Map;
import com.gameexpert.chat.dto.ChatMessageResponse;
import com.gameexpert.chat.service.ChatDelivery;
import com.gameexpert.chat.service.ChatRateLimitService;
import com.gameexpert.chat.service.ChatService;
import com.gameexpert.ws.handler.ChatWsHandler;
import com.gameexpert.ws.dto.ChatResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatWsHandlerTest {
    // @Test
    void savesUsingConnectionIdentityAndBuildsResponseFromSavedResult() {
        ChatService service = mock(ChatService.class);
        ChatDelivery delivery = mock(ChatDelivery.class);
        ChatRateLimitService rateLimit = mock(ChatRateLimitService.class);
        WorldBroadcaster broadcaster = mock(WorldBroadcaster.class);
        ChatCommands commands = mock(ChatCommands.class);
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(Map.of(NicknameHandshakeInterceptor.ATTR_PLAYER_ID, 7L));
        when(rateLimit.allow(7L)).thenReturn(true);
        when(commands.resolve(42L, "Alice", "안녕")).thenReturn("안녕");
        LocalDateTime timestamp = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
        when(service.saveMessage(42L, "Alice", "안녕"))
                .thenReturn(new ChatMessageResponse("SavedAlice", "저장된 내용", timestamp));
        JsonMapper mapper = JsonMapper.builder().build();
        new ChatWsHandler(service, delivery, rateLimit, broadcaster, commands).handle(
                new WsMessageContext(42L, "Alice", session),
                mapper.readTree("""
                        {"type":"chat","content":"안녕","nickname":"Fake","worldId":999}
                        """)
        );
        verify(service).saveMessage(42L, "Alice", "안녕");
        ArgumentCaptor<ChatResponse> response = ArgumentCaptor.forClass(ChatResponse.class);
        verify(delivery).send(eq(42L), response.capture());
        JsonNode json = mapper.readTree(mapper.writeValueAsString(response.getValue()));
        assertEquals("chat", json.path("type").asString());
        assertEquals("SavedAlice", json.path("sender").asString());
        assertEquals("저장된 내용", json.path("content").asString());
        assertEquals(timestamp, mapper.treeToValue(json.path("timestamp"), LocalDateTime.class));
        verifyNoMoreInteractions(service, delivery);
        verifyNoInteractions(broadcaster);
    }
}
