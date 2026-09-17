package com.gameexpert.ws.handler;

import com.gameexpert.engine.PlayerAction;
import com.gameexpert.engine.WorldEngineManager;
import com.gameexpert.ws.WsMessageContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class MoveWsHandler implements WsMessageHandler {
    private final WorldEngineManager engineManager;

    @Override
    public String type() {
        return "move";
    }

    @Override
    public void handle(WsMessageContext context, JsonNode message) {
        String finalSceneActionId = WsFields.optionalFinalSceneActionId(message);
        // TODO Lv 12: 명세의 이동 값을 읽어 현재 사용자의 이동 요청을 엔진에 전달합니다.
    }
}
