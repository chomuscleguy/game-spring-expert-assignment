package com.gameexpert.chat.service;

import com.gameexpert.ws.WorldBroadcaster;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalChatSender {
    private final WorldBroadcaster broadcaster;

    public void send(Long worldId, Object message) {
        broadcaster.broadcast(worldId,message);
    }
}
