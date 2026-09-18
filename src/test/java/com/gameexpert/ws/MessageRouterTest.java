package com.gameexpert.ws;

import com.gameexpert.ws.handler.EngineMessageHandler;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MessageRouterTest {
    @Test
    void invokesSelectedHandlerWithSameContextAndParsedPayload() {
        EngineMessageHandler selected = mock(EngineMessageHandler.class);
        EngineMessageHandler other = mock(EngineMessageHandler.class);
        when(selected.supportedTypes()).thenReturn(Set.of("ping"));
        when(other.supportedTypes()).thenReturn(Set.of("chat"));
        WorldBroadcaster broadcaster = mock(WorldBroadcaster.class);
        MessageRouter router = new MessageRouter(new ObjectMapper(), broadcaster, List.of(selected, other));
        WsMessageContext context = new WsMessageContext(71L, "Alex", mock(WebSocketSession.class));

        router.route(context, "{\"type\":\"ping\",\"value\":42}");

        ArgumentCaptor<JsonNode> payload = ArgumentCaptor.forClass(JsonNode.class);
        verify(selected).handle(same(context), payload.capture());
        assertThat(payload.getValue().get("type").asString()).isEqualTo("ping");
        assertThat(payload.getValue().get("value").asInt()).isEqualTo(42);
        verify(other, never()).handle(any(), any());
        verifyNoInteractions(broadcaster);
    }
}
