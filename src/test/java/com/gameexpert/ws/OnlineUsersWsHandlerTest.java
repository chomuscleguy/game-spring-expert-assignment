package com.gameexpert.ws;

import com.gameexpert.ws.handler.OnlineUsersWsHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class OnlineUsersWsHandlerTest {
    @Test
    void returnsSortedOpenUsersOnlyToRequester() {
        WorldSessionRegistry registry = mock(WorldSessionRegistry.class);
        WorldBroadcaster broadcaster = mock(WorldBroadcaster.class);
        WebSocketSession requester = session("Bob", true);
        WebSocketSession alice = session("Alice", true);
        WebSocketSession closed = session("Closed", false);
        when(registry.entries(42L)).thenReturn(List.of(
                new WorldSessionRegistry.Entry(requester),
                new WorldSessionRegistry.Entry(closed),
                new WorldSessionRegistry.Entry(alice)
        ));
        JsonMapper mapper = JsonMapper.builder().build();
        new OnlineUsersWsHandler(registry, broadcaster).handle(
                new WsMessageContext(42L, "Bob", requester),
                mapper.readTree("{\"type\":\"onlineUsers\",\"worldId\":999}")
        );
        ArgumentCaptor<Object> response = ArgumentCaptor.forClass(Object.class);
        verify(broadcaster).sendTo(eq(requester), response.capture());
        JsonNode json = mapper.readTree(mapper.writeValueAsString(response.getValue()));
        assertEquals("onlineUsers", json.path("type").asString());
        assertEquals(mapper.readTree("[\"Alice\",\"Bob\"]"), json.path("users"));
        assertEquals(2, json.path("count").asInt());
        verify(registry).entries(42L);
        verifyNoMoreInteractions(registry, broadcaster);
    }

    @Test
    void returnsZeroForAnEmptyList() {
        WorldSessionRegistry registry = mock(WorldSessionRegistry.class);
        WorldBroadcaster broadcaster = mock(WorldBroadcaster.class);
        WebSocketSession requester = mock(WebSocketSession.class);
        when(registry.entries(7L)).thenReturn(List.of());
        JsonMapper mapper = JsonMapper.builder().build();
        new OnlineUsersWsHandler(registry, broadcaster).handle(
                new WsMessageContext(7L, "Alice", requester),
                mapper.readTree("{\"type\":\"onlineUsers\"}")
        );
        ArgumentCaptor<Object> response = ArgumentCaptor.forClass(Object.class);
        verify(broadcaster).sendTo(eq(requester), response.capture());
        JsonNode json = mapper.readTree(mapper.writeValueAsString(response.getValue()));
        assertEquals(mapper.readTree("[]"), json.path("users"));
        assertEquals(0, json.path("count").asInt());
    }

    private WebSocketSession session(String nickname, boolean open) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(open);
        when(session.getAttributes()).thenReturn(Map.of(NicknameHandshakeInterceptor.ATTR_NICKNAME, nickname));
        return session;
    }
}
