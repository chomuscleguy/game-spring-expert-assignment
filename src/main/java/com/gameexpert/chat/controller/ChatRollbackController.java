package com.gameexpert.chat.controller;

import com.gameexpert.chat.service.ChatService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("assignment-checks")
@RestController
@RequiredArgsConstructor
public class ChatRollbackController {

    private final ChatService chatService;

    @Transactional
    @PostMapping("/practice/worlds/{worldId}/chats/rollback")
    public ResponseEntity<Void> rollback(
            @PathVariable Long worldId,
            @RequestParam @Size(min = 2, max = 12) @Pattern(regexp = "[A-Za-z0-9_]+") String nickname,
            @RequestParam @NotBlank @Size(max = 200) String content
    ) {
        chatService.saveMessage(worldId, nickname, content);
        // 제공 코드: 저장과 이벤트 발행이 끝난 트랜잭션을 의도적으로 롤백합니다.
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        return ResponseEntity.noContent().build();
    }
}
