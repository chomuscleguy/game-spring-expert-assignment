package com.gameexpert.config;

import com.gameexpert.ws.GameWebSocketHandler;
import com.gameexpert.ws.NicknameHandshakeInterceptor;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;

import static org.mockito.Mockito.*;

class WebSocketConfigTest {
    @Test
    void registersProvidedInterceptorOnWorldEndpoint() {
        GameWebSocketHandler handler = mock(GameWebSocketHandler.class);
        NicknameHandshakeInterceptor interceptor = mock(NicknameHandshakeInterceptor.class);
        EngineProperties properties = mock(EngineProperties.class);
        when(properties.wsAllowedOrigins()).thenReturn(List.of("http://localhost:8080"));
        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        WebSocketHandlerRegistration registration = mock(WebSocketHandlerRegistration.class, RETURNS_SELF);
        when(registry.addHandler(handler, "/ws/worlds/{worldId}")).thenReturn(registration);

        new WebSocketConfig(handler, interceptor, properties).registerWebSocketHandlers(registry);

        verify(registry).addHandler(handler, "/ws/worlds/{worldId}");
        verify(registration).addInterceptors(interceptor);
    }
}
