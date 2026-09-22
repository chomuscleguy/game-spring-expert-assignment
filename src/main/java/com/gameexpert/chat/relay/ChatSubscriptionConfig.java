package com.gameexpert.chat.relay;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@ConditionalOnProperty(name = "webcraft.chat.pubsub-enabled", havingValue = "true")
public class ChatSubscriptionConfig {

    @Bean
    public RedisMessageListenerContainer chatSubscription(
            RedisConnectionFactory connectionFactory,
            ChatRelay relay) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(relay,new ChannelTopic(ChatRelay.CHANNEL));
        return container;
    }
}
