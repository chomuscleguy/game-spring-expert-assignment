package com.gameexpert.player.entity;

import java.time.LocalDateTime;

import com.gameexpert.api.persistence.PlayerAccess;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "players")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Player implements PlayerAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String nickname;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public Player(String nickname) {
        this.nickname = nickname;
    }
}
