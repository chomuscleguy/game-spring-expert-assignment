package com.gameexpert.ws.handler;

import org.springframework.stereotype.Component;

import com.gameexpert.chat.service.ChatService;
import com.gameexpert.chat.service.ChatDelivery;
import com.gameexpert.chat.dto.ChatMessageResponse;
import com.gameexpert.ws.dto.ChatResponse;
import com.gameexpert.chat.service.ChatRateLimitService;
import com.gameexpert.ws.NicknameHandshakeInterceptor;
import com.gameexpert.ws.ChatCommands;
import com.gameexpert.ws.WorldBroadcaster;
import com.gameexpert.ws.WsMessageContext;
import com.gameexpert.ws.dto.WsMessages.Error;

import tools.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatWsHandler implements WsMessageHandler {

    private final ChatService chatService;
    private final ChatDelivery delivery;
    private final ChatRateLimitService rateLimit;
    private final WorldBroadcaster broadcaster;
    private final ChatCommands commands;

    @Override
    public String type() {
        return "chat";
    }

    @Override
    public void handle(WsMessageContext context, JsonNode message) {
        String content = readContent(message);

        if (content.isBlank() || content.length() > 200) {
            throw new IllegalArgumentException("채팅은 1~200자로 입력해 주세요.");
        }

        Long playerId = (Long) context.session().getAttributes()
                .get(NicknameHandshakeInterceptor.ATTR_PLAYER_ID);
        if (!rateLimit.allow(playerId)) {
            broadcaster.sendTo(context.session(), new Error("CHAT_COOLDOWN"));
            return;
        }

        content = commands.resolve(context.worldId(), context.nickname(), content);

        ChatResponse response = createResponse(context, content);
        delivery.send(context.worldId(), response);
    }

    private String readContent(JsonNode message) {
        // TODO Lv 13: API 명세의 채팅 내용을 읽습니다.
        return "";
    }

    private ChatResponse createResponse(WsMessageContext context, String content) {
        // TODO Lv 13: 현재 연결의 사용자로 저장하고 명세에 맞는 응답을 만듭니다.
        return null;
    }
}
