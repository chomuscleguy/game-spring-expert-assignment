package com.gameexpert.ws;

import com.gameexpert.player.entity.Player;
import com.gameexpert.player.repository.PlayerRepository;
import com.gameexpert.world.WorldBaselineReadiness;
import com.gameexpert.world.entity.World;
import com.gameexpert.world.repository.WorldRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class NicknameHandshakeInterceptorTest {
    @Test
    void looksUpRequestedPlayerAndWorldAndStoresConnectionAttributes() {
        PlayerRepository players = mock(PlayerRepository.class);
        WorldRepository worlds = mock(WorldRepository.class);
        WorldBaselineReadiness readiness = mock(WorldBaselineReadiness.class);
        when(readiness.isReady()).thenReturn(true);
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(player.getId()).thenReturn(31L);
        when(world.getSeed()).thenReturn(123L);
        when(players.findByNickname("Alex")).thenReturn(Optional.of(player));
        when(worlds.findById(72L)).thenReturn(Optional.of(world));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws/worlds/72");
        request.setParameter("nickname", "Alex");
        Map<String, Object> attributes = new HashMap<>();
        NicknameHandshakeInterceptor interceptor = new NicknameHandshakeInterceptor(players, worlds, readiness);

        boolean accepted = interceptor.beforeHandshake(new ServletServerHttpRequest(request),
                new ServletServerHttpResponse(new MockHttpServletResponse()),
                mock(WebSocketHandler.class), attributes);

        assertThat(accepted).isTrue();
        verify(players).findByNickname("Alex");
        verify(worlds).findById(72L);
        assertThat(attributes).containsEntry(NicknameHandshakeInterceptor.ATTR_NICKNAME, "Alex")
                .containsEntry(NicknameHandshakeInterceptor.ATTR_WORLD_ID, 72L)
                .containsEntry(NicknameHandshakeInterceptor.ATTR_PLAYER_ID, 31L)
                .doesNotContainKey(NicknameHandshakeInterceptor.ATTR_ERROR_CODE);
    }

    @Test
    void missingPlayerUsesProvidedErrorResponse() {
        PlayerRepository players = mock(PlayerRepository.class);
        WorldRepository worlds = mock(WorldRepository.class);
        WorldBaselineReadiness readiness = mock(WorldBaselineReadiness.class);
        when(readiness.isReady()).thenReturn(true);
        when(players.findByNickname("Unknown")).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws/worlds/72");
        request.setParameter("nickname", "Unknown");
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = new NicknameHandshakeInterceptor(players, worlds, readiness).beforeHandshake(
                new ServletServerHttpRequest(request),
                new ServletServerHttpResponse(new MockHttpServletResponse()),
                mock(WebSocketHandler.class), attributes);

        assertThat(accepted).isTrue();
        verify(players).findByNickname("Unknown");
        assertThat(attributes).containsEntry(NicknameHandshakeInterceptor.ATTR_ERROR_CODE, 4000);
    }

    @Test
    void missingWorldUsesProvidedErrorResponse() {
        PlayerRepository players = mock(PlayerRepository.class);
        WorldRepository worlds = mock(WorldRepository.class);
        WorldBaselineReadiness readiness = mock(WorldBaselineReadiness.class);
        when(readiness.isReady()).thenReturn(true);
        when(players.findByNickname("Alex")).thenReturn(Optional.of(mock(Player.class)));
        when(worlds.findById(99L)).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws/worlds/99");
        request.setParameter("nickname", "Alex");
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = new NicknameHandshakeInterceptor(players, worlds, readiness).beforeHandshake(
                new ServletServerHttpRequest(request),
                new ServletServerHttpResponse(new MockHttpServletResponse()),
                mock(WebSocketHandler.class), attributes);

        assertThat(accepted).isTrue();
        verify(worlds).findById(99L);
        assertThat(attributes).containsEntry(NicknameHandshakeInterceptor.ATTR_ERROR_CODE, 4001);
    }
}
