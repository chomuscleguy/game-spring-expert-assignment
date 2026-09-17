package com.gameexpert.ws.handler;

import com.gameexpert.ws.WsMessageContext;
import tools.jackson.databind.JsonNode;

public interface WsMessageHandler extends EngineMessageHandler {
    String type();

    void handle(WsMessageContext context, JsonNode message);
}
