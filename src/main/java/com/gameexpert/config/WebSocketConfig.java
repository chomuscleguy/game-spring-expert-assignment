package com.gameexpert.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.gameexpert.ws.GameWebSocketHandler;
import com.gameexpert.ws.NicknameHandshakeInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocket
@EnableScheduling
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final GameWebSocketHandler gameWebSocketHandler;
    private final NicknameHandshakeInterceptor nicknameInterceptor;
    private final EngineProperties properties;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, "/ws/worlds/{worldId}")
                .addInterceptors(nicknameInterceptor)
                .setAllowedOriginPatterns(properties.wsAllowedOrigins().toArray(String[]::new));
    }
}
