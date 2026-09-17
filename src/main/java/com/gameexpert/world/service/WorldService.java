package com.gameexpert.world.service;

import java.util.List;

import com.gameexpert.common.ConflictException;
import com.gameexpert.common.ServiceUnavailableException;
import com.gameexpert.world.WorldBaselineReadiness;
import com.gameexpert.common.ForbiddenException;
import com.gameexpert.common.NotFoundException;
import com.gameexpert.engine.Difficulty;
import com.gameexpert.player.entity.Player;
import com.gameexpert.player.repository.PlayerRepository;
import com.gameexpert.world.dto.ConditionalWorldDeleteRequest;
import com.gameexpert.world.dto.CreateWorldRequest;
import com.gameexpert.world.dto.WorldSummaryResponse;
import com.gameexpert.world.entity.World;
import com.gameexpert.world.repository.WorldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorldService {
    private static final int MAX_WORLDS = 3;
    private final WorldRepository worldRepository;
    private final PlayerRepository playerRepository;
    private final WorldOperations worldOperations;
    private final WorldBaselineReadiness baselineReadiness;

    public record CommittedWorldCreation(
            long worldId,
            String name,
            long seed,
            Difficulty difficulty,
            String canonicalNickname) {
        public CommittedWorldCreation {
            if (worldId <= 0) {
                throw new IllegalArgumentException("world ID must be positive");
            }
            if (name == null || name.isBlank() || name.length() > 30) {
                throw new IllegalArgumentException("world name must be non-blank and at most 30 characters");
            }
            if (seed < Integer.MIN_VALUE || seed > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("world seed must be a signed int32");
            }
            if (difficulty == null) {
                throw new IllegalArgumentException("world difficulty must be present");
            }
        }
    }

    public List<WorldSummaryResponse> listWorlds() {
        if (!baselineReadiness.isReady()) {
            throw new ServiceUnavailableException("WORLD_BASELINE_INITIALIZING");
        }
        return worldRepository.findRootWorlds().stream()
                .map(world -> new WorldSummaryResponse(
                        world.getId(),
                        world.getName(),
                        world.getSeed(),
                        worldOperations.worldOnlineCount(world.getId()),
                        world.getDifficulty()
                )).toList();
    }

    @Transactional
    public CommittedWorldCreation createWorld(CreateWorldRequest request) {
        if (!baselineReadiness.isReady()) {
            throw new ServiceUnavailableException("WORLD_BASELINE_INITIALIZING");
        }
        // TODO Lv 4: duringCreation() 안에서 기본 월드 3개 제한을 검사하고 createPreparedWorld(request)를 호출합니다.
        throw new UnsupportedOperationException("Lv 4: 월드 생성을 구현하세요.");
    }

    // 제공 코드: 생성 잠금 안에서 호출하며 엔진에 전달할 초기 월드 정보를 준비합니다.
    private CommittedWorldCreation createPreparedWorld(CreateWorldRequest request) {
        String owner = request.getNickname() == null ? null : request.getNickname().trim();
        worldOperations.validateSeed(owner, request.getDebugSeed());
        if (owner != null && !owner.isBlank() && playerRepository.findByNickname(owner).isEmpty()) {
            throw new NotFoundException("PLAYER_NOT_FOUND");
        }
        long seed = worldOperations.seed(request.getName(), request.getDebugSeed());
        World savedWorld = worldRepository.save(new World(
                request.getName(),
                seed,
                request.difficultyOrDefault(),
                owner
        ));
        Long savedWorldId = savedWorld == null ? null : savedWorld.getId();
        if (savedWorldId == null || savedWorldId <= 0) {
            throw new IllegalStateException("world repository did not assign a positive ID");
        }
        return new CommittedWorldCreation(
                savedWorldId,
                savedWorld.getName(),
                savedWorld.getSeed(),
                savedWorld.getDifficulty(),
                savedWorld.getOwnerNickname()
        );
    }

    @Transactional
    public void deleteWorld(Long id, String requesterNickname) {
        if (!baselineReadiness.isReady()) {
            throw new ServiceUnavailableException("WORLD_BASELINE_INITIALIZING");
        }
        World world = worldRepository.findById(id).orElseThrow(this::worldNotFound);
        if (worldRepository.isDimensionChild(id)) throw worldNotFound();
        authorizeDeletion(id, world, requesterNickname);

        worldOperations.deleteWorld(id, () -> worldRepository.delete(world));
    }

    @Transactional
    public void deleteWorldIfMatches(Long id, String requesterNickname,
            ConditionalWorldDeleteRequest expectedIdentity) {
        if (!baselineReadiness.isReady()) {
            throw new ServiceUnavailableException("WORLD_BASELINE_INITIALIZING");
        }
        World world = worldRepository.findByIdForUpdate(id).orElseThrow(this::worldNotFound);
        if (worldRepository.isDimensionChild(id)) throw worldNotFound();
        authorizeDeletion(id, world, requesterNickname);

        if (!matchesCreationIdentity(world, expectedIdentity)) {
            throw new ConflictException("WORLD_IDENTITY_MISMATCH");
        }

        worldOperations.deleteWorld(id, () -> worldRepository.delete(world));
    }

    private boolean matchesCreationIdentity(World world,
            ConditionalWorldDeleteRequest expectedIdentity) {
        return expectedIdentity != null
                && world.getName().equals(expectedIdentity.name())
                && expectedIdentity.seed() != null
                && world.getSeed() == expectedIdentity.seed().longValue()
                && world.getDifficulty().key().equals(expectedIdentity.difficulty())
                && world.getOwnerNickname() != null
                && world.getOwnerNickname().equals(expectedIdentity.ownerNickname());
    }

    private void authorizeDeletion(Long id, World world, String requesterNickname) {
        String nickname = requesterNickname == null ? "" : requesterNickname.trim();
        if (nickname.isEmpty()) throw notWorldOwner();

        Player requester = playerRepository.findByNickname(nickname).orElseThrow(this::notWorldOwner);

        if (world.hasOwner()) {
            if (!world.isOwnedBy(nickname)) throw notWorldOwner();
            return;
        }
        if (!worldOperations.hasParticipants(id)) return;
        if (!worldOperations.hasParticipated(requester.getId(), id)) {
            throw notWorldOwner();
        }
    }

    private ForbiddenException notWorldOwner() {
        return new ForbiddenException("NOT_WORLD_OWNER");
    }

    private NotFoundException worldNotFound() {
        return new NotFoundException("WORLD_NOT_FOUND");
    }
}
