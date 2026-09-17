package com.gameexpert.trial.service;

import com.gameexpert.engine.trial.TrialSpawnerRuntime;
import com.gameexpert.api.trial.TrialStorage;
import com.gameexpert.engine.trial.persistence.TrialWorldStatePersistence;
import com.gameexpert.trial.entity.WorldTrialSite;
import com.gameexpert.trial.repository.WorldTrialSiteRepository;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrialPersistenceService implements TrialStorage {
    private final WorldTrialSiteRepository repository;
    private final TrialWorldStatePersistence worldState;

    @Transactional(readOnly = true)
    public List<TrialSpawnerRuntime.SiteSnapshot> hydrateWorld(Long worldId) {
        requireWorld(worldId);
        return repository.findAllByWorldIdOrderByTrialIdAsc(worldId).stream().map(WorldTrialSite::snapshot).toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> hydrateVaultTimers(Long worldId) {
        requireWorld(worldId);
        return worldState.hydrateVaultTimers(worldId);
    }

    @Transactional
    public void saveWorld(Long worldId, List<TrialSpawnerRuntime.SiteSnapshot> snapshots) {
        saveWorldWithMutation(worldId, snapshots, List.of(), () -> { });
    }

    @Transactional
    public void saveWorldWithMutation(Long worldId,
            List<TrialSpawnerRuntime.SiteSnapshot> snapshots, RewardMutation mutation) {
        saveWorldWithMutation(worldId, snapshots, List.of(), mutation);
    }

    @Transactional
    public void saveWorldWithMutation(Long worldId,
            List<TrialSpawnerRuntime.SiteSnapshot> snapshots,
            List<TrialSpawnerRuntime.StateChange> stateChanges, RewardMutation mutation) {
        requireWorld(worldId);
        if (snapshots == null || stateChanges == null || mutation == null) {
            throw new IllegalArgumentException("trial snapshots, changes and mutation are required");
        }
        List<WorldTrialSite> existing = repository.findAllByWorldIdOrderByTrialIdAsc(worldId);
        Map<Long, WorldTrialSite> existingByTrialId = existing.stream()
                .collect(Collectors.toMap(WorldTrialSite::getTrialId, Function.identity(),
                        (previous, current) -> current, LinkedHashMap::new));
        Set<Long> retained = new HashSet<>();
        snapshots.stream().forEachOrdered(snapshot -> {
            if (snapshot == null || !retained.add(snapshot.trialId())) throw new IllegalArgumentException("duplicate or null trial snapshot");
            WorldTrialSite row = existingByTrialId.get(snapshot.trialId());
            if (row == null) row = new WorldTrialSite(worldId, snapshot);
            row.apply(snapshot);
            repository.save(row);
        });
        repository.deleteAll(existing.stream().filter(row -> !retained.contains(row.getTrialId())).toList());
        worldState.saveChanges(worldId, stateChanges);
        mutation.persist();
        repository.flush();
    }

    @Transactional
    public SettlementOutcome settleReward(Long worldId, long trialId, String rewardIdentity,
            RewardMutation mutation) {
        requireWorld(worldId);
        if (rewardIdentity == null || rewardIdentity.isBlank() || mutation == null) throw new IllegalArgumentException("reward identity and mutation are required");
        WorldTrialSite row = repository.lockByWorldIdAndTrialId(worldId, trialId).orElse(null);
        if (row == null || !rewardIdentity.equals(row.getRewardIdentity())) return SettlementOutcome.REJECTED;
        if (!row.isRewardPending()) return SettlementOutcome.IDEMPOTENT;
        mutation.persist();
        row.markRewardSettled();
        repository.flush();
        return SettlementOutcome.COMMITTED;
    }

    @Transactional public void deleteWorld(Long worldId) {
        requireWorld(worldId);
        repository.deleteAllByWorldId(worldId);
        worldState.deleteWorld(worldId);
    }
    private static void requireWorld(Long worldId) { if (worldId == null) throw new IllegalArgumentException("worldId must not be null"); }
}
