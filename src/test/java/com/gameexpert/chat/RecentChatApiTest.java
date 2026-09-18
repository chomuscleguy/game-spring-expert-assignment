package com.gameexpert.chat;

import java.time.LocalDateTime;
import java.util.List;

import com.gameexpert.chat.controller.WorldChatController;
import com.gameexpert.chat.dto.ChatMessageResponse;
import com.gameexpert.chat.service.RecentChatQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RecentChatApiTest {
    private RecentChatQueryService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(RecentChatQueryService.class);
        mvc = MockMvcBuilders.standaloneSetup(new WorldChatController(service)).build();
    }

    @Test
    void returnsServiceResultsInOrderWithRequestedLimit() throws Exception {
        when(service.getRecentMessages(42L, 2)).thenReturn(List.of(
                new ChatMessageResponse("Alice", "안녕", LocalDateTime.of(2026, 1, 2, 3, 4, 5)),
                new ChatMessageResponse("Bob", "반가워", LocalDateTime.of(2026, 1, 2, 3, 4, 6))
        ));
        mvc.perform(get("/worlds/42/chats").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sender").value("Alice"))
                .andExpect(jsonPath("$[0].content").value("안녕"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-01-02T03:04:05"))
                .andExpect(jsonPath("$[1].sender").value("Bob"));
        verify(service).getRecentMessages(42L, 2);
        verifyNoMoreInteractions(service);
    }

    @Test
    void usesDefaultLimitAndReturnsEmptyArray() throws Exception {
        when(service.getRecentMessages(7L, 50)).thenReturn(List.of());
        mvc.perform(get("/worlds/7/chats"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(service).getRecentMessages(7L, 50);
    }
}
