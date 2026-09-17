package com.gameexpert.ws;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import com.gameexpert.player.repository.PlayerRepository;
import com.gameexpert.player.entity.Player;
import com.gameexpert.world.entity.World;
import com.gameexpert.world.repository.WorldRepository;
import com.gameexpert.world.WorldBaselineReadiness;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NicknameHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_NICKNAME = "ws.nickname";
    public static final String ATTR_WORLD_ID = "ws.worldId";
    public static final String ATTR_PLAYER_ID = "ws.playerId";
    public static final String ATTR_WORLD_SEED = "ws.worldSeed";
    public static final String ATTR_WORLD_DIFFICULTY = "ws.worldDifficulty";
    public static final String ATTR_ERROR_CODE = "ws.errorCode";

    private static final Pattern WORLD_PATH = Pattern.compile("/ws/worlds/(\\d+)/?$");

    private final PlayerRepository playerRepository;
    private final WorldRepository worldRepository;
    private final WorldBaselineReadiness baselineReadiness;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!baselineReadiness.isReady()) {
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return false;
        }

        String nickname = readNickname(request);
        if (nickname == null || nickname.isBlank()) {
            attributes.put(ATTR_ERROR_CODE, 4000);
            return true;
        }

        // TODO Lv 7: 닉네임으로 플레이어를 조회합니다. 없으면 null을 사용합니다.
        Player player = null;
        if (player == null) {
            attributes.put(ATTR_ERROR_CODE, 4000);
            return true;
        }

        Long worldId = readWorldId(request);
        if (worldId == null) {
            attributes.put(ATTR_ERROR_CODE, 4001);
            return true;
        }
        // TODO Lv 7: worldId로 월드를 조회합니다. 없으면 null을 사용합니다.
        World world = null;
        if (world == null || worldRepository.isDimensionChild(worldId)) {
            attributes.put(ATTR_ERROR_CODE, 4001);
            return true;
        }

        // TODO Lv 7: nickname과 worldId를 ATTR_NICKNAME, ATTR_WORLD_ID 키로 attributes에 저장합니다.

        attributes.put(ATTR_PLAYER_ID, player.getId());
        attributes.put(ATTR_WORLD_SEED, (int) world.getSeed());
        attributes.put(ATTR_WORLD_DIFFICULTY, world.getDifficulty());
        return true;
    }

    private String readNickname(ServerHttpRequest request) {
        String nickname;
        if (request instanceof ServletServerHttpRequest servletRequest) {
            nickname = servletRequest.getServletRequest().getParameter("nickname");
        } else {
            nickname = UriComponentsBuilder.fromUri(request.getURI()).build()
                    .getQueryParams().getFirst("nickname");
        }
        return nickname == null ? null : nickname.trim();
    }

    private Long readWorldId(ServerHttpRequest request) {
        Matcher matcher = WORLD_PATH.matcher(request.getURI().getPath());
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.valueOf(matcher.group(1));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
    }
}
