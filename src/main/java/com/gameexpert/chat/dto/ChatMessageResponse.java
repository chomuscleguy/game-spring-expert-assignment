package com.gameexpert.chat.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatMessageResponse {

    private final String sender;
    private final String content;
    private final LocalDateTime createdAt;
}
