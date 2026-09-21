package com.gameexpert.chat.service;

import com.gameexpert.chat.dto.ChatHistoryEntry;
import com.gameexpert.chat.dto.ChatHistoryPage;
import com.gameexpert.chat.entity.ChatMessage;
import com.gameexpert.chat.repository.ChatHistoryRepository;
import com.gameexpert.common.InvalidRequestException;
import com.gameexpert.common.NotFoundException;
import com.gameexpert.world.repository.WorldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatHistoryService {
    private final ChatHistoryRepository repository;
    private final WorldRepository worlds;

    @Transactional(readOnly = true)
    public ChatHistoryPage getHistory(Long worldId, LocalDateTime beforeCreatedAt, Long beforeId, int limit) {
        if ((beforeCreatedAt == null) != (beforeId == null) || limit < 1 || limit > 100) {
            throw new InvalidRequestException("VALIDATION_FAILED");
        }
        if (!worlds.existsById(worldId)) {
            throw new NotFoundException("WORLD_NOT_FOUND");
        }
        List<ChatMessage> found = repository.findHistory(
                worldId, beforeCreatedAt, beforeId, PageRequest.of(0, limit + 1));

        boolean hasNext = found.size() > limit;
        List<ChatHistoryEntry> items = found.stream().limit(limit)
                .map(message -> new ChatHistoryEntry(
                        message.getId(),
                        message.getSenderNickname(),
                        message.getContent(),
                        message.getCreatedAt()
                )).toList();

        ChatHistoryEntry last = hasNext ? items.getLast() : null;

        return new ChatHistoryPage(items, hasNext,
                last == null ? null : last.getCreatedAt(),
                last == null ? null : last.getId());
    }
}
