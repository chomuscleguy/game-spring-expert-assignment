package com.gameexpert.ws;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WorldSessionRegistryTest {
    // @Test
    void registersAndFindsConnectionIgnoringNicknameCase() {
        WorldSessionRegistry registry = new WorldSessionRegistry();
        WebSocketSession session = mock(WebSocketSession.class);
        WorldSessionRegistry.Entry entry = registry.register(11L, "Alex", session);

        assertThat(entry).isNotNull();
        assertThat(entry.session()).isSameAs(session);
        assertThat(registry.get(11L, "ALEX")).isSameAs(entry);
        assertThat(registry.get(11L, "Other")).isNull();
        assertThat(registry.get(12L, "Alex")).isNull();
    }

    // @Test
    void duplicateCannotReplaceOriginalAndDifferentWorldIsIndependent() {
        WorldSessionRegistry registry = new WorldSessionRegistry();
        WebSocketSession original = mock(WebSocketSession.class);
        WebSocketSession duplicate = mock(WebSocketSession.class);
        WorldSessionRegistry.Entry first = registry.register(11L, "Alex", original);

        assertThat(first).isNotNull();
        assertThat(registry.register(11L, "alex", duplicate)).isNull();
        assertThat(registry.get(11L, "Alex").session()).isSameAs(original);
        assertThat(registry.register(12L, "Alex", duplicate)).isNotNull();
        assertThat(registry.get(12L, "Alex").session()).isSameAs(duplicate);
    }
}
