package com.gameexpert.ws;

import static com.gameexpert.ws.NicknameHandshakeInterceptor.ATTR_ERROR_CODE;
import static com.gameexpert.ws.NicknameHandshakeInterceptor.ATTR_NICKNAME;
import static com.gameexpert.ws.NicknameHandshakeInterceptor.ATTR_WORLD_ID;

import com.gameexpert.presence.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameWebSocketHandler extends TextWebSocketHandler implements ConnectionEndpoint {
    private final WorldSessionRegistry registry;
    private final PresenceService presenceService;
    private final MessageRouter messageRouter;
    private final GameConnectionRuntime game;
    private final WorldSessionLifecycle lifecycle;

    public void attachDimensions(DimensionTravelCoordinator dimensions) {
        game.attachDimensions(dimensions);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Integer error = (Integer) session.getAttributes().get(ATTR_ERROR_CODE);
        if (error != null) {
            session.close(new CloseStatus(error));
            return;
        }
        if (session.getAttributes().get(ATTR_WORLD_ID) == null
                || session.getAttributes().get(ATTR_NICKNAME) == null
                || session.getAttributes().get(NicknameHandshakeInterceptor.ATTR_PLAYER_ID) == null
                || session.getAttributes().get(NicknameHandshakeInterceptor.ATTR_WORLD_SEED) == null
                || session.getAttributes().get(NicknameHandshakeInterceptor.ATTR_WORLD_DIFFICULTY) == null) {
            log.info("연결 정보 누락: HandshakeInterceptor 구현과 등록을 확인하세요.");
            session.close(new CloseStatus(4000));
            return;
        }
        session = game.open(session);
        Long worldId = (Long) session.getAttributes().get(ATTR_WORLD_ID);
        String nickname = (String) session.getAttributes().get(ATTR_NICKNAME);
        game.prepare(session);
        WorldSessionRegistry.Entry entry = registry.register(worldId, nickname, session);
        if (entry == null) {
            game.reject(session);
            session.close(new CloseStatus(4002));
            return;
        }
        try {
            game.joined(session, entry);
            // 초기 로딩이 끝난 현재 연결만 Redis에 등록합니다.
            synchronized (session) {
                if (session.isOpen() && registry.get(worldId, nickname) == entry) {
                    presenceService.join(worldId, entry.connectionId());
                }
            }
        } catch (Exception exception) {
            log.error("접속 처리 실패: world={}, nickname={}", worldId, nickname, exception);
            lifecycle.release(session);
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        game.receive(session, message, messageRouter::route);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        game.closed(session);
    }
}
