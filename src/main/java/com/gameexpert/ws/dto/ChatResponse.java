package com.gameexpert.ws.dto;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class ChatResponse {
    private final String type = "chat";
    private final String sender;
    private final String content;
    private final LocalDateTime timestamp;

    public ChatResponse(String sender, String content, LocalDateTime timestamp) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
    }
}
