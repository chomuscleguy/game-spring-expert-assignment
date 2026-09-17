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
            ChatRelay relay
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // TODO Lv 20: 제공된 relay를 채팅 채널의 수신 리스너로 등록합니다.
        return container;
    }
}
