package com.gameexpert.world.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gameexpert.world.service.WorldCreationHeaders;
import com.gameexpert.world.dto.ConditionalWorldDeleteRequest;
import com.gameexpert.world.dto.CreateWorldRequest;
import com.gameexpert.world.dto.CreatedWorldResponse;
import com.gameexpert.world.dto.WorldSummaryResponse;
import com.gameexpert.world.service.WorldService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class WorldController {

    private final WorldService worldService;
    private final WorldCreationHeaders worldCreationHeaders;

    @GetMapping("/worlds")
    public ResponseEntity<List<WorldSummaryResponse>> list() {
        return ResponseEntity.ok(worldService.listWorlds());
    }

    @PostMapping("/worlds")
    public ResponseEntity<CreatedWorldResponse> create(@Valid @RequestBody CreateWorldRequest request) {
        WorldService.CommittedWorldCreation result = worldService.createWorld(request);
        CreatedWorldResponse response = new CreatedWorldResponse(
                result.worldId(),
                result.name(),
                result.seed(),
                result.difficulty(),
                result.canonicalNickname());
        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(worldCreationHeaders.forCreation(
                        request.getDebugSeed(), result.worldId(), result.canonicalNickname()))
                .body(response);
    }

    @DeleteMapping("/worlds/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
            @RequestParam(required = false) String nickname) {
        worldService.deleteWorld(id, nickname);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/worlds/{id}/if-matches")
    public ResponseEntity<Void> deleteIfMatches(@PathVariable Long id,
            @RequestParam(required = false) String nickname,
            @Valid @RequestBody ConditionalWorldDeleteRequest expectedIdentity) {
        worldService.deleteWorldIfMatches(id, nickname, expectedIdentity);
        return ResponseEntity.noContent().build();
    }
}
