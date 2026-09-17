package com.gameexpert.ws;

import com.gameexpert.presence.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConnectionCleanup implements SessionCleanup {
    private final WorldSessionRegistry registry;
    private final PresenceService presenceService;

    @Override
    public WorldSessionRegistry.Entry remove(Long worldId, String nickname, WebSocketSession session) {
        synchronized (session) {
            WorldSessionRegistry.Entry removed = registry.remove(worldId, nickname, session);
            if (removed == null) {
                return null;
            }
            try {
                presenceService.leave(worldId, removed.connectionId());
            } catch (RuntimeException exception) {
                log.error("퇴장 presence 해제 실패: world={}, nickname={}", worldId, nickname, exception);
            }
            return removed;
        }
    }
}
