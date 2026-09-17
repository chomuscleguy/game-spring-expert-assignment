package com.gameexpert.chat.controller;

import java.time.LocalDateTime;
import com.gameexpert.chat.dto.ChatHistoryPage;
import com.gameexpert.chat.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ChatHistoryController {
    private final ChatHistoryService service;

    @GetMapping("/worlds/{worldId}/chats/history")
    public ResponseEntity<ChatHistoryPage> history(@PathVariable Long worldId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime beforeCreatedAt,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(service.getHistory(worldId, beforeCreatedAt, beforeId, limit));
    }
}
