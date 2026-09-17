package com.gameexpert.world;

import java.util.function.Supplier;

import com.gameexpert.common.ConflictException;
import com.gameexpert.player.repository.PlayerRepository;
import com.gameexpert.world.dto.CreateWorldRequest;
import com.gameexpert.world.entity.World;
import com.gameexpert.world.repository.WorldRepository;
import com.gameexpert.world.service.WorldOperations;
import com.gameexpert.world.service.WorldService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorldCreationTest {
    private WorldRepository repository;
    private WorldService service;
    private long worldCount;
    private boolean insideCreation;

    @BeforeEach
    void setUp() {
        repository = mock(WorldRepository.class);
        PlayerRepository players = mock(PlayerRepository.class);
        WorldOperations operations = mock(WorldOperations.class);
        WorldBaselineReadiness readiness = new WorldBaselineReadiness();
        readiness.markReady();
        service = new WorldService(repository, players, operations, readiness);

        when(operations.duringCreation(any())).thenAnswer(invocation -> {
            Supplier<?> action = invocation.getArgument(0);
            insideCreation = true;
            try {
                return action.get();
            } finally {
                insideCreation = false;
            }
        });
        when(repository.countRootWorlds()).thenAnswer(invocation -> {
            assertTrue(insideCreation, "월드 개수는 duringCreation() 안에서 확인해야 합니다.");
            return worldCount;
        });
        when(operations.seed("새 월드", null)).thenReturn(123L);
        when(repository.save(any(World.class))).thenAnswer(invocation -> {
            assertTrue(insideCreation, "월드는 duringCreation() 안에서 생성해야 합니다.");
            World world = invocation.getArgument(0);
            ReflectionTestUtils.setField(world, "id", 1L);
            return world;
        });
    }

    @Test
    void createsFirstWorld() {
        worldCount = 0;
        assertWorldCreated();
    }

    @Test
    void allowsThirdWorld() {
        worldCount = 2;
        assertWorldCreated();
    }

    @Test
    void rejectsWhenThreeWorldsExist() {
        worldCount = 3;
        assertLimitRejected();
    }

    @Test
    void rejectsWhenLimitAlreadyExceeded() {
        worldCount = 4;
        assertLimitRejected();
    }

    private void assertWorldCreated() {
        WorldService.CommittedWorldCreation result = service.createWorld(new CreateWorldRequest("새 월드"));

        assertNotNull(result);
        assertEquals(1L, result.worldId());
        assertEquals("새 월드", result.name());
        assertEquals(123L, result.seed());
        ArgumentCaptor<World> saved = ArgumentCaptor.forClass(World.class);
        verify(repository).save(saved.capture());
        assertEquals("새 월드", saved.getValue().getName());
    }

    private void assertLimitRejected() {
        ConflictException error = assertThrows(ConflictException.class,
                () -> service.createWorld(new CreateWorldRequest("새 월드")));

        assertEquals("WORLD_LIMIT_REACHED", error.getError());
        verify(repository, never()).save(any(World.class));
    }
}
