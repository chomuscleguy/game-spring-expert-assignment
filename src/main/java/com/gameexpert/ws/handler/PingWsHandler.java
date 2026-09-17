package com.gameexpert.ws.handler;

import org.springframework.stereotype.Component;

import com.gameexpert.ws.WorldBroadcaster;
import com.gameexpert.ws.WorldSessionRegistry;
import com.gameexpert.presence.PresenceService;
import com.gameexpert.ws.WsMessageContext;
import com.gameexpert.ws.dto.PongResponse;

import tools.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PingWsHandler implements WsMessageHandler {

    private final WorldBroadcaster broadcaster;
    private final WorldSessionRegistry registry;
    private final PresenceService presenceService;

    @Override
    public String type() {
        return "ping";
    }

    @Override
    public void handle(WsMessageContext context, JsonNode message) {
        WorldSessionRegistry.Entry connection = registry.get(context.worldId(), context.nickname());
        if (connection == null || connection.session() != context.session()) {
            return;
        }
        // TODO Lv 11: presenceService.heartbeat()에 월드 ID와 현재 연결 ID를 전달합니다.
        // TODO Lv 11: broadcaster.sendTo()로 현재 세션에 PongResponse를 보냅니다.
    }
}
