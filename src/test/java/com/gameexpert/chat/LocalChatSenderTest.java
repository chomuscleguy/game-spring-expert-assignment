package com.gameexpert.chat;

import java.util.Map;
import com.gameexpert.chat.service.LocalChatSender;
import com.gameexpert.ws.WorldBroadcaster;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class LocalChatSenderTest {
    @Test
    void broadcastsTheProvidedMessageOnceToTheProvidedWorld() {
        WorldBroadcaster broadcaster = mock(WorldBroadcaster.class);
        LocalChatSender sender = new LocalChatSender(broadcaster);
        Object message = Map.of("type", "chat", "sender", "Alice", "content", "안녕");
        sender.send(42L, message);
        verify(broadcaster).broadcast(eq(42L), same(message));
        verifyNoMoreInteractions(broadcaster);
    }
}
