package com.gameexpert.world.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConditionalWorldDeleteRequest(
        @NotBlank @Size(max = 30) String name,
        @NotNull @Min(Integer.MIN_VALUE) @Max(Integer.MAX_VALUE) Long seed,
        @NotNull @Pattern(regexp = "easy|normal|hard") String difficulty,
        @NotBlank @Size(min = 2, max = 12) @Pattern(regexp = "[A-Za-z0-9_]+")
        String ownerNickname) {
}
