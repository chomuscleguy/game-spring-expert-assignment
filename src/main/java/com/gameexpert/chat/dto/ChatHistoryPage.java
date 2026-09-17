package com.gameexpert.chat.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatHistoryPage {
    private final List<ChatHistoryEntry> items;
    private final boolean hasNext;
    private final LocalDateTime nextCreatedAt;
    private final Long nextId;
}
