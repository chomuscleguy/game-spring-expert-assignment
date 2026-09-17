package com.gameexpert.world.entity;

import java.time.LocalDateTime;

import com.gameexpert.api.persistence.WorldAccess;

import org.hibernate.annotations.CreationTimestamp;

import com.gameexpert.engine.Difficulty;
import com.gameexpert.world.WorldBaseline;
import com.gameexpert.world.WorldGenerationProfile;
import com.gameexpert.world.WorldGenerationProfiles;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "worlds")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class World implements WorldAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false)
    private long seed;

    @Column(length = 80)
    private String baselineId;

    @Column(length = 64)
    private String baselineInputFingerprintSha256;

    @Column(length = 64)
    private String generatorSourceSha256;

    private Integer baselineWorldVersion;

    private Integer baselineDataPackMajor;

    private Integer baselineResourcePackMajor;

    private Integer baselineProtocolVersion;

    @Column(length = 16)
    private String ownerNickname;

    @Column(nullable = false)
    private long dayCount = 0;

    // 하루 안에서의 시각이며 범위는 0~11999입니다.
    @Column(nullable = false, columnDefinition = "bigint not null default 0")
    private long worldTime = 0;

    @Column(nullable = false, columnDefinition = "bigint not null default 0")
    private long gameTimeMcTicks = 0;

    private Long traderNextAttemptTick;

    private Integer traderChancePercent;

    private Integer spawnX;

    private Integer spawnY;

    private Integer spawnZ;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Difficulty difficulty;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public World(String name, long seed) {
        this(name, seed, Difficulty.DEFAULT, null);
    }

    public World(String name, long seed, Difficulty difficulty) {
        this(name, seed, difficulty, null);
    }

    public World(String name, long seed, Difficulty difficulty, String ownerNickname) {
        this(name, seed, difficulty, ownerNickname, WorldGenerationProfiles.newWorldProfile());
    }

    public World(String name, long seed, Difficulty difficulty, String ownerNickname,
            WorldGenerationProfile requestedProfile) {
        WorldGenerationProfile profile = WorldGenerationProfiles.requireSupported(requestedProfile);
        this.name = name;
        this.seed = seed;
        this.baselineId = profile.getBaselineId();
        this.baselineInputFingerprintSha256 = profile.getInputFingerprintSha256();
        this.generatorSourceSha256 = profile.getGeneratorSourceSha256();
        this.baselineWorldVersion = profile.getWorldVersion();
        this.baselineDataPackMajor = profile.getDataPackMajor();
        this.baselineResourcePackMajor = profile.getResourcePackMajor();
        this.baselineProtocolVersion = profile.getProtocolVersion();
        this.difficulty = Difficulty.orDefault(difficulty);
        this.ownerNickname = ownerNickname == null || ownerNickname.isBlank()
                ? null : ownerNickname.trim();
    }

    public WorldGenerationProfile generationProfile() {
        return WorldGenerationProfiles.requireSupported(baselineId, baselineInputFingerprintSha256,
                generatorSourceSha256, baselineWorldVersion, baselineDataPackMajor,
                baselineResourcePackMajor, baselineProtocolVersion);
    }

    public boolean usesCurrentBaseline() {
        return WorldGenerationProfiles.newWorldProfile().matches(baselineId,
                baselineInputFingerprintSha256, generatorSourceSha256, baselineWorldVersion,
                baselineDataPackMajor, baselineResourcePackMajor, baselineProtocolVersion);
    }

    public boolean isOwnedBy(String nickname) {
        return ownerNickname != null && nickname != null
                && ownerNickname.equalsIgnoreCase(nickname.trim());
    }

    public boolean hasOwner() {
        return ownerNickname != null;
    }

    public Difficulty getDifficulty() {
        return Difficulty.orDefault(difficulty);
    }

    public void updateDayCount(long dayCount) {
        if (dayCount < 0) {
            throw new IllegalArgumentException("dayCount must be non-negative");
        }
        this.dayCount = dayCount;
    }

    public void updateClock(long dayCount, long worldTime, long gameTimeMcTicks) {
        if (dayCount < 0 || worldTime < 0 || worldTime >= 12_000 || gameTimeMcTicks < 0) {
            throw new IllegalArgumentException("world clock values are out of range");
        }
        this.dayCount = dayCount;
        this.worldTime = worldTime;
        this.gameTimeMcTicks = gameTimeMcTicks;
    }

    public boolean hasCanonicalSpawn() {
        return spawnX != null && spawnY != null && spawnZ != null;
    }

    public int[] canonicalSpawn() {
        return hasCanonicalSpawn() ? new int[] { spawnX, spawnY, spawnZ } : null;
    }

    public void assignCanonicalSpawnIfAbsent(int x, int y, int z) {
        if (y < 0 || y > 319) {
            throw new IllegalArgumentException("canonical spawn Y is out of world range");
        }
        if (hasCanonicalSpawn()) return;
        this.spawnX = x;
        this.spawnY = y;
        this.spawnZ = z;
    }

    public void updateTraderWindow(long nextAttemptTick, int chancePercent) {
        if (chancePercent < 0 || chancePercent > 100) {
            throw new IllegalArgumentException("trader chance percent must be between 0 and 100");
        }
        this.traderNextAttemptTick = nextAttemptTick;
        this.traderChancePercent = chancePercent;
    }
}
