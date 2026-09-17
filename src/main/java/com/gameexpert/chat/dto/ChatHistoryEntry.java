package com.gameexpert.chat.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatHistoryEntry {
    private final Long id;
    private final String sender;
    private final String content;
    private final LocalDateTime createdAt;
}
