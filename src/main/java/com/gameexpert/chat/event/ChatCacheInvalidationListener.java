package com.gameexpert.chat.event;

import com.gameexpert.chat.service.RecentChatCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChatCacheInvalidationListener {
    private final RecentChatCache cache;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatSaved(ChatSavedEvent event) {
        cache.invalidate(event.getWorldId());
    }
}
