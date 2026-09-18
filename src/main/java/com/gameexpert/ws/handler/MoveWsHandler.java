package com.gameexpert.ws.handler;

import com.gameexpert.engine.PlayerAction;
import com.gameexpert.engine.WorldEngineManager;
import com.gameexpert.ws.WsMessageContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import static com.gameexpert.ws.handler.WsFields.finiteFloat;

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
        PlayerAction.Move move = new PlayerAction.Move(
                context.nickname(),
                WsFields.finiteNumber(message, "x"),
                WsFields.finiteNumber(message, "y"),
                WsFields.finiteNumber(message, "z"),
                finiteFloat(message, "yaw"),
                finiteFloat(message, "pitch"),
                WsFields.booleanValue(message, "crouching"),
                WsFields.booleanValue(message, "gliding"),
                finalSceneActionId
        );

        engineManager.enqueue(context.worldId(), move);
    }
}
