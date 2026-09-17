package com.gameexpert.world.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gameexpert.engine.Difficulty;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;

@Getter
public class CreateWorldRequest {

    @NotBlank
    @Size(min = 1, max = 30)
    private final String name;

    private final Difficulty difficulty;

    @Size(min = 2, max = 12)
    private final String nickname;

    @Min(Integer.MIN_VALUE)
    @Max(Integer.MAX_VALUE)
    private final Long debugSeed;

    public CreateWorldRequest(String name) {
        this(name, null, null);
    }

    public CreateWorldRequest(String name, Difficulty difficulty) {
        this(name, difficulty, null);
    }

    public CreateWorldRequest(String name, Difficulty difficulty, String nickname) {
        this(name, difficulty, nickname, null);
    }

    @JsonCreator
    public CreateWorldRequest(@JsonProperty("name") String name,
            @JsonProperty("difficulty") Difficulty difficulty,
            @JsonProperty("nickname") String nickname,
            @JsonProperty("debugSeed") Long debugSeed) {
        this.name = name;
        this.difficulty = difficulty;
        this.nickname = nickname;
        this.debugSeed = debugSeed;
    }

    public Difficulty difficultyOrDefault() {
        return Difficulty.orDefault(difficulty);
    }
}
