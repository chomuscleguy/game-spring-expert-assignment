package com.gameexpert.world.dto;

import com.gameexpert.engine.Difficulty;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WorldSummaryResponse {

    private final Long id;
    private final String name;
    private final long seed;
    private final long onlineCount;
    private final Difficulty difficulty;
}
