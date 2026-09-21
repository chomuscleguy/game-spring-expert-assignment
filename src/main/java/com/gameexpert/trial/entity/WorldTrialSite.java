package com.gameexpert.trial.entity;

import com.gameexpert.engine.raid.RaidLedger;
import com.gameexpert.engine.trial.persistence.TrialPersistenceCodec;
import com.gameexpert.engine.trial.TrialSpawnerRuntime;
import jakarta.persistence.*;

import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "world_trial_sites",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_world_trial_id", columnNames = {"world_id", "trial_id"}),
                @UniqueConstraint(name = "uk_world_trial_position",
                        columnNames = {"world_id", "block_x", "block_y", "block_z"})
        }, indexes = @Index(name = "idx_world_trial_pending_reward",
        columnList = "world_id, reward_pending"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorldTrialSite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private long revision;
    @Column(name = "world_id", nullable = false)
    private Long worldId;
    @Column(name = "trial_id", nullable = false)
    private long trialId;
    @Column(name = "block_x", nullable = false)
    private int x;
    @Column(name = "block_y", nullable = false)
    private int y;
    @Column(name = "block_z", nullable = false)
    private int z;
    @Column(name = "armed_tick", nullable = false)
    private long armedTick;
    @Column(name = "detected_players", nullable = false)
    private int detectedPlayers;
    @Column(nullable = false)
    private boolean armed;
    @Column(name = "cooldown_until_tick", nullable = false)
    private long cooldownUntilTick;
    @Column(name = "phase_state", nullable = false)
    private int phaseState;
    @Lob
    @Column(name = "ledger_payload")
    private String ledgerPayload;
    @Column(name = "hero_nickname", length = 64)
    private String heroNickname;
    @Column(name = "reward_identity", length = 96)
    private String rewardIdentity;
    @Column(name = "reward_pending", nullable = false)
    private boolean rewardPending;
    @Column(name = "reward_entity_id", nullable = false,
            columnDefinition = "bigint not null default 0")
    private long rewardEntityId;
    @Lob
    @Column(name = "activated_vaults", nullable = false)
    private String activatedVaults = "WCTV1";
    @Column(name = "trial_state", nullable = false, columnDefinition = "int not null default -1")
    private int trialState = TrialSpawnerRuntime.LEGACY_STATE;
    @Column(name = "ominous", nullable = false, columnDefinition = "boolean not null default false")
    private boolean ominous;
    @Column(name = "ejections_remaining", nullable = false,
            columnDefinition = "int not null default 0")
    private int ejectionsRemaining;
    @Column(name = "ejected_count", nullable = false, columnDefinition = "int not null default 0")
    private int ejectedCount;
    @Column(name = "next_spawn_tick", nullable = false,
            columnDefinition = "bigint not null default 0")
    private long nextSpawnTick;

    @Column(name = "reward_item_type", nullable = false,
            columnDefinition = "smallint not null default 1253")
    private short rewardItemType = com.gameexpert.engine.inventory.PlayerInventory.TRIAL_KEY;
    @Column(name = "reward_count", nullable = false, columnDefinition = "int not null default 1")
    private int rewardCount = 1;

    public WorldTrialSite(Long worldId, TrialSpawnerRuntime.SiteSnapshot snapshot) {
        this.worldId = worldId;
        this.trialId = snapshot.trialId();
        apply(snapshot);
    }

    public void apply(TrialSpawnerRuntime.SiteSnapshot snapshot) {
        if (trialId != snapshot.trialId()) throw new IllegalArgumentException("trial identity cannot change");
        x = snapshot.x();
        y = snapshot.y();
        z = snapshot.z();
        armedTick = snapshot.armedTick();
        detectedPlayers = snapshot.detectedPlayers();
        armed = snapshot.armed();
        cooldownUntilTick = snapshot.cooldownUntilTick();
        phaseState = snapshot.phaseState();
        ledgerPayload = TrialPersistenceCodec.encodeLedger(snapshot.ledger());
        heroNickname = snapshot.heroNickname();
        rewardIdentity = snapshot.rewardIdentity();
        rewardPending = snapshot.rewardPending();
        rewardEntityId = snapshot.rewardEntityId();
        activatedVaults = TrialPersistenceCodec.encodePositions(snapshot.activatedVaults());
        trialState = snapshot.trialState();
        ominous = snapshot.ominous();
        ejectionsRemaining = snapshot.ejectionsRemaining();
        ejectedCount = snapshot.ejectedCount();
        nextSpawnTick = snapshot.nextSpawnTick();
        rewardItemType = snapshot.rewardItemType();
        rewardCount = snapshot.rewardCount();
    }

    public TrialSpawnerRuntime.SiteSnapshot snapshot() {
        RaidLedger.InstanceSnapshot ledger = TrialPersistenceCodec.decodeLedger(ledgerPayload);
        List<int[]> vaults = TrialPersistenceCodec.decodePositions(activatedVaults);
        return new TrialSpawnerRuntime.SiteSnapshot(x, y, z, trialId, armedTick,
                detectedPlayers, armed, cooldownUntilTick, phaseState, ledger,
                heroNickname, rewardIdentity, rewardPending, rewardEntityId, vaults,
                trialState, ominous, ejectionsRemaining, ejectedCount, nextSpawnTick,
                rewardItemType, rewardCount);
    }

    public void markRewardSettled() {
        rewardPending = false;
    }
}
