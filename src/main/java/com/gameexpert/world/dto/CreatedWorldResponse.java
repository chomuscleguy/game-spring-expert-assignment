package com.gameexpert.world.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.gameexpert.engine.Difficulty;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"id", "name", "seed", "difficulty", "ownerNickname"})
public record CreatedWorldResponse(
        long id,
        String name,
        long seed,
        Difficulty difficulty,
        String ownerNickname) {
}
