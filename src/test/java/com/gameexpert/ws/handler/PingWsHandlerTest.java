package com.gameexpert.ws.handler;

import com.gameexpert.presence.PresenceService;
import com.gameexpert.ws.WorldBroadcaster;
import com.gameexpert.ws.WorldSessionRegistry;
import com.gameexpert.ws.WsMessageContext;
import com.gameexpert.ws.dto.PongResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PingWsHandlerTest {
    // @Test
    void renewsCurrentConnectionAndSendsPongOnlyToRequester() {
        WorldSessionRegistry registry = mock(WorldSessionRegistry.class);
        WorldBroadcaster broadcaster = mock(WorldBroadcaster.class);
        PresenceService presence = mock(PresenceService.class);
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("connection-A");
        when(registry.get(37L, "Alex")).thenReturn(new WorldSessionRegistry.Entry(session));
        WsMessageContext context = new WsMessageContext(37L, "Alex", session);

        new PingWsHandler(broadcaster, registry, presence).handle(context,
                new ObjectMapper().readTree("{\"type\":\"ping\"}"));

        verify(presence).heartbeat(37L, "connection-A");
        ArgumentCaptor<PongResponse> response = ArgumentCaptor.forClass(PongResponse.class);
        verify(broadcaster).sendTo(same(session), response.capture());
        assertThat(response.getValue().getType()).isEqualTo("pong");
        verifyNoMoreInteractions(presence, broadcaster);
    }
}
