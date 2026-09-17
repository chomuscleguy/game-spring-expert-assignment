package com.gameexpert.chat.event;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatSavedEvent {

    private Long worldId;
    private String sender;
    private String content;
    private LocalDateTime timestamp;
}
