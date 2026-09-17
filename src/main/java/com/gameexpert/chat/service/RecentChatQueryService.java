package com.gameexpert.chat.service;

import java.util.List;
import com.gameexpert.chat.dto.ChatMessageResponse;
import com.gameexpert.common.NotFoundException;
import com.gameexpert.world.repository.WorldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecentChatQueryService {
    private final ChatService chatService;
    private final RecentChatCache cache;
    private final WorldRepository worlds;

    public List<ChatMessageResponse> getRecentMessages(Long worldId, int limit) {
        if (!worlds.existsById(worldId)) {
            throw new NotFoundException("WORLD_NOT_FOUND");
        }
        int capped = Math.min(Math.max(limit, 1), 100);
        List<ChatMessageResponse> cached = cache.read(worldId, capped);
        if (cached != null) {
            return cached;
        }
        List<ChatMessageResponse> messages = chatService.getRecentMessages(worldId, capped);
        cache.write(worldId, capped, messages);
        return messages;
    }
}
