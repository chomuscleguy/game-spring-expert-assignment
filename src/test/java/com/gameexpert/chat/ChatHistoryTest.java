package com.gameexpert.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gameexpert.chat.dto.ChatHistoryPage;
import com.gameexpert.chat.entity.ChatMessage;
import com.gameexpert.chat.repository.ChatHistoryRepository;
import com.gameexpert.chat.service.ChatHistoryService;
import com.gameexpert.world.entity.World;
import com.gameexpert.world.repository.WorldRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class ChatHistoryTest {
    private static final Long WORLD_ID = 1L;
    private static final LocalDateTime TIME = LocalDateTime.of(2026, 1, 1, 12, 0);
    private ChatHistoryRepository repository;
    private ChatHistoryService service;

    @BeforeEach
    void prepare() {
        repository = mock(ChatHistoryRepository.class);
        WorldRepository worlds = mock(WorldRepository.class);
        when(worlds.existsById(WORLD_ID)).thenReturn(true);
        service = new ChatHistoryService(repository, worlds);
    }

    // @Test
    void nextCursorUsesLastReturnedItemInsteadOfExtraRow() {
        List<ChatMessage> found = List.of(
                message(30L, TIME),
                message(20L, TIME.minusSeconds(1)),
                message(10L, TIME.minusSeconds(2))
        );
        givenMessages(found);

        ChatHistoryPage page = service.getHistory(WORLD_ID, null, null, 2);

        assertThat(page.getItems()).hasSize(2);
        assertThat(page.isHasNext()).isTrue();
        assertThat(page.getNextId()).isEqualTo(20L);
        assertThat(page.getNextCreatedAt()).isEqualTo(TIME.minusSeconds(1));
    }

    // @Test
    void lastPageHasNoCursorEvenWhenItContainsMessages() {
        givenMessages(List.of(message(20L, TIME), message(10L, TIME.minusSeconds(1))));

        ChatHistoryPage page = service.getHistory(WORLD_ID, null, null, 2);

        assertThat(page.getItems()).hasSize(2);
        assertThat(page.isHasNext()).isFalse();
        assertThat(page.getNextId()).isNull();
        assertThat(page.getNextCreatedAt()).isNull();
    }

    // @Test
    void emptyPageHasNoCursor() {
        givenMessages(List.of());

        ChatHistoryPage page = service.getHistory(WORLD_ID, null, null, 2);

        assertThat(page.getItems()).isEmpty();
        assertThat(page.isHasNext()).isFalse();
        assertThat(page.getNextId()).isNull();
        assertThat(page.getNextCreatedAt()).isNull();
    }

    private void givenMessages(List<ChatMessage> messages) {
        when(repository.findHistory(eq(WORLD_ID), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(messages);
    }

    private ChatMessage message(Long id, LocalDateTime createdAt) {
        ChatMessage message = new ChatMessage(BeanUtils.instantiateClass(World.class), "Alice", "hello");
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(message, "createdAt", createdAt);
        return message;
    }
}
