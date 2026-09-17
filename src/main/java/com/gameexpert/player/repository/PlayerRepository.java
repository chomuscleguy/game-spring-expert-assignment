package com.gameexpert.player.repository;

import com.gameexpert.player.entity.Player;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    boolean existsByNickname(String nickname);

    Optional<Player> findByNickname(String nickname);
}
