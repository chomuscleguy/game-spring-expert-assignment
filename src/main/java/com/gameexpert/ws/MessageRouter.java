package com.gameexpert.ws;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.gameexpert.engine.ActionQueueOverflowException;
import com.gameexpert.ws.dto.WsMessages.Error;
import com.gameexpert.ws.handler.EngineMessageHandler;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class MessageRouter {

    private static final Logger log = LoggerFactory.getLogger(MessageRouter.class);

    private final ObjectMapper objectMapper;
    private final WorldBroadcaster broadcaster;
    private final Map<String, EngineMessageHandler> handlers;

    public MessageRouter(ObjectMapper objectMapper, WorldBroadcaster broadcaster,
            List<EngineMessageHandler> handlers) {
        this.objectMapper = objectMapper;
        this.broadcaster = broadcaster;
        this.handlers = handlers.stream()
                .flatMap(handler -> handler.supportedTypes().stream().map(type -> Map.entry(type, handler)))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void route(WsMessageContext context, String payload) {
        final JsonNode message;
        try {
            message = objectMapper.readTree(payload);
        } catch (Exception exception) {
            log.debug("잘못된 JSON 수신: world={}, nickname={}",
                    context.worldId(), context.nickname(), exception);
            error(context, "INVALID_JSON");
            return;
        }
        if (message == null || !message.isObject()) {
            error(context, "INVALID_MESSAGE");
            return;
        }
        JsonNode typeNode = message.get("type");
        String type = typeNode != null && typeNode.isString() ? typeNode.asString() : null;
        EngineMessageHandler handler = findHandler(type);
        if (handler == null) {
            error(context, "UNKNOWN_TYPE");
            return;
        }
        try {
            // TODO Lv 11: handler에 context와 message를 전달해 handle()을 호출합니다.
        } catch (ActionQueueOverflowException exception) {
            log.warn("액션 큐 상한 초과로 거부: type={}, world={}, nickname={}",
                    type, context.worldId(), context.nickname());
            error(context, "QUEUE_FULL");
        } catch (IllegalArgumentException exception) {
            log.debug("잘못된 메시지 거부: type={}, world={}, nickname={}",
                    type, context.worldId(), context.nickname(), exception);
            error(context, "INVALID_MESSAGE");
        } catch (Exception exception) {
            log.error("메시지 처리 실패: type={}, world={}, nickname={}",
                    type, context.worldId(), context.nickname(), exception);
            error(context, "INTERNAL_ERROR");
        }
    }

    private EngineMessageHandler findHandler(String type) {
        return type == null ? null : handlers.get(type);
    }

    private void error(WsMessageContext context, String code) {
        broadcaster.sendTo(context.session(), new Error(code));
    }
}
