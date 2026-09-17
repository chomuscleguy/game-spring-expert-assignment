package com.gameexpert.chat.repository;

import java.time.LocalDateTime;
import java.util.List;
import com.gameexpert.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ChatHistoryRepository extends Repository<ChatMessage, Long> {
    @Query("""
            select message from ChatMessage message
            where message.world.id = :worldId
              and (:beforeCreatedAt is null or message.createdAt < :beforeCreatedAt
                   or (message.createdAt = :beforeCreatedAt and message.id < :beforeId))
            order by message.createdAt desc, message.id desc
            """)
    List<ChatMessage> findHistory(@Param("worldId") Long worldId,
            @Param("beforeCreatedAt") LocalDateTime beforeCreatedAt,
            @Param("beforeId") Long beforeId, Pageable pageable);
}
