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
        presenceService.heartbeat(context.worldId(), connection.connectionId());
        broadcaster.sendTo(context.session(), new PongResponse());
    }
}
