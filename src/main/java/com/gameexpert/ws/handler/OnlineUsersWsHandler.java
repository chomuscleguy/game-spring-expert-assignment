package com.gameexpert.ws.handler;

import com.gameexpert.api.SessionRegistry;
import com.gameexpert.ws.NicknameHandshakeInterceptor;
import com.gameexpert.ws.WorldBroadcaster;
import com.gameexpert.ws.WorldSessionRegistry;
import com.gameexpert.ws.WsMessageContext;
import com.gameexpert.ws.dto.OnlineUsersResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OnlineUsersWsHandler implements WsMessageHandler {
    private final WorldSessionRegistry registry;
    private final WorldBroadcaster broadcaster;

    @Override
    public String type() {
        return "onlineUsers";
    }

    @Override
    public void handle(WsMessageContext context, JsonNode message) {
        List<String> users = registry.entries(context.worldId()).stream()
                .map(SessionRegistry.Entry::session)
                .filter(WebSocketSession::isOpen)
                .map(session -> (String) session.getAttributes()
                        .get(NicknameHandshakeInterceptor.ATTR_NICKNAME))
                .sorted()
                .toList();

        broadcaster.sendTo(context.session(), new OnlineUsersResponse(users, users.size()));
    }
}
