package com.gameexpert.ws;

import com.gameexpert.engine.PlayerAction;
import com.gameexpert.engine.WorldEngineManager;
import com.gameexpert.ws.handler.MoveWsHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MoveWsHandlerTest {
    @Test
    void forwardsMovementValuesUsingConnectionIdentity() {
        WorldEngineManager engine = mock(WorldEngineManager.class);
        MoveWsHandler handler = new MoveWsHandler(engine);
        WsMessageContext context = new WsMessageContext(42L, "Alice", mock(WebSocketSession.class));
        handler.handle(context, JsonMapper.builder().build().readTree("""
                {"type":"move","worldId":999,"nickname":"Fake",
                 "x":1.25,"y":64.5,"z":-9.75,"yaw":120.5,"pitch":-25.25,
                 "crouching":true,"gliding":false}
                """));
        ArgumentCaptor<PlayerAction> action = ArgumentCaptor.forClass(PlayerAction.class);
        verify(engine).enqueue(eq(42L), action.capture());
        PlayerAction.Move move = assertInstanceOf(PlayerAction.Move.class, action.getValue());
        assertEquals("Alice", move.nickname());
        assertEquals(1.25, move.x());
        assertEquals(64.5, move.y());
        assertEquals(-9.75, move.z());
        assertEquals(120.5f, move.yaw());
        assertEquals(-25.25f, move.pitch());
        assertTrue(move.crouching());
        assertFalse(move.gliding());
        verifyNoMoreInteractions(engine);
    }

    @Test
    void preservesTheOppositeMovementFlags() {
        WorldEngineManager engine = mock(WorldEngineManager.class);
        new MoveWsHandler(engine).handle(
                new WsMessageContext(8L, "Bob", mock(WebSocketSession.class)),
                JsonMapper.builder().build().readTree("""
                        {"type":"move","x":-2,"y":70,"z":3,"yaw":0,"pitch":0,
                         "crouching":false,"gliding":true}
                        """)
        );
        ArgumentCaptor<PlayerAction> action = ArgumentCaptor.forClass(PlayerAction.class);
        verify(engine).enqueue(eq(8L), action.capture());
        PlayerAction.Move move = assertInstanceOf(PlayerAction.Move.class, action.getValue());
        assertFalse(move.crouching());
        assertTrue(move.gliding());
    }
}
