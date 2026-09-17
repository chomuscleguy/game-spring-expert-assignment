package com.gameexpert.chat.repository;

import com.gameexpert.chat.entity.ChatMessage;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long>, com.gameexpert.api.WorldContentCleanup {

    List<ChatMessage> findByWorldIdOrderByCreatedAtDescIdDesc(Long worldId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.world.id = :worldId")
    void deleteByWorldId(@Param("worldId") Long worldId);
}
