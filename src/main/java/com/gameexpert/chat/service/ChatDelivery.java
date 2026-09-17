package com.gameexpert.chat.service;

import com.gameexpert.chat.relay.ChatRelay;
import com.gameexpert.ws.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatDelivery {
    private final LocalChatSender localChatSender;
    private final ChatRelay relay;

    @Value("${webcraft.chat.pubsub-enabled:false}")
    private boolean pubsubEnabled;

    public void send(Long worldId, ChatResponse response) {
        if (pubsubEnabled) {
            relay.publish(worldId, response);
        } else {
            localChatSender.send(worldId, response);
        }
    }
}
