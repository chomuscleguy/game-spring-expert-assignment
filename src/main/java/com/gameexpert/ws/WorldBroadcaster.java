package com.gameexpert.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
@RequiredArgsConstructor
public class WorldBroadcaster {
    private final WorldSessionRegistry registry;
    private final GameTransport transport;

    public void broadcast(Long worldId, Object message) {
        registry.entries(worldId).stream()
                .map(WorldSessionRegistry.Entry::session)
                .forEach(session -> sendTo(session, message));
    }

    public void sendTo(WebSocketSession session, Object message) {
        transport.sendTo(session, message);
    }
}
