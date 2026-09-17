package com.gameexpert.chat.service;

import com.gameexpert.chat.entity.ChatMessage;
import com.gameexpert.chat.event.ChatSavedEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.gameexpert.chat.repository.ChatMessageRepository;

import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gameexpert.chat.dto.ChatMessageResponse;
import com.gameexpert.common.NotFoundException;
import com.gameexpert.world.repository.WorldRepository;
import com.gameexpert.world.entity.World;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_LIMIT = 100;

    private final ChatMessageRepository chatMessageRepository;
    private final WorldRepository worldRepository;
    private final ApplicationEventPublisher events;

    @Transactional
    public ChatMessageResponse saveMessage(Long worldId, String sender, String content) {
        World world = worldRepository.findById(worldId).
                orElseThrow(() -> new NotFoundException("WORLD_NOT_FOUND"));
        ChatMessage chat = chatMessageRepository.save(new ChatMessage(world, sender, content));
        return savedResponse(worldId, chat);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getRecentMessages(Long worldId, int limit) {
        if (!worldRepository.existsById(worldId)) {
            throw new NotFoundException("WORLD_NOT_FOUND");
        }

        int capped = Math.min(Math.max(limit, 1), MAX_LIMIT);

        List<ChatMessage> recent = chatMessageRepository
                .findByWorldIdOrderByCreatedAtDescIdDesc(worldId, PageRequest.of(0, capped));

        Collections.reverse(recent);

        return recent.stream()
                .map(message -> new ChatMessageResponse(
                        message.getSenderNickname(),
                        message.getContent(),
                        message.getCreatedAt()))
                .toList();
    }

    private ChatMessageResponse savedResponse(Long worldId, ChatMessage saved) {
        events.publishEvent(new ChatSavedEvent(
                worldId,
                saved.getSenderNickname(),
                saved.getContent(),
                saved.getCreatedAt()
        ));
        return new ChatMessageResponse(
                saved.getSenderNickname(),
                saved.getContent(),
                saved.getCreatedAt()
        );
    }
}
