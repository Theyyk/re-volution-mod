package com.example.revolution.achievement;

import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.gameplay.GameplayEvent;
import com.example.revolution.gameplay.GameplayEventSink;
import com.example.revolution.gameplay.GameplayEventType;
import com.example.revolution.resource.definition.ResourceDefinitions;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-authoritative permanent Achievement progress. High-frequency facts are cached/dirty, not persisted per tick. */
public final class AchievementService implements GameplayEventSink {
    public enum ClaimResult { APPLIED, UNKNOWN_ACHIEVEMENT, NOT_COMPLETED, OUT_OF_ORDER, FINAL_STAGE_CLAIMED, REWARD_FAILED }

    private final AchievementRepository repository;
    private final AchievementCatalog catalog;
    private final AchievementRewardGateway rewards;
    private final Map<UUID, AchievementPlayerState> cache = new HashMap<>();
    private final Set<UUID> dirty = new HashSet<>();

    public AchievementService(AchievementRepository repository, AchievementCatalog catalog, AchievementRewardGateway rewards) {
        if (repository == null || catalog == null || rewards == null) throw new IllegalArgumentException("Achievement dependencies are required");
        this.repository = repository;
        this.catalog = catalog;
        this.rewards = rewards;
    }

    @Override
    public synchronized void confirmedDamage(UUID playerUuid, GameplayDamageSource source, BigDecimal amount) {
        if (playerUuid == null || source == null || amount == null || amount.signum() <= 0) return;
        route(playerUuid, GameplayEventType.TOTAL_DAMAGE, amount, Collections.<String, String>emptyMap());
        route(playerUuid, GameplayEventType.DAMAGE_BY_CHANNEL, amount,
                Collections.singletonMap("channel", source.name()));
    }

    @Override
    public synchronized void earnedResource(UUID playerUuid, String resource, BigDecimal amount) {
        if (playerUuid == null || resource == null || amount == null || amount.signum() <= 0) return;
        GameplayEventType type = resourceEvent(resource);
        if (type != null) route(playerUuid, type, amount, Collections.singletonMap("resource", ResourceDefinitions.normalize(resource)));
    }

    @Override
    public synchronized void factualEvent(GameplayEvent event) {
        if (event == null || event.playerUuid == null || event.amount == null || event.amount.signum() <= 0) return;
        if (event.type == GameplayEventType.TOTAL_DAMAGE || event.type == GameplayEventType.DAMAGE_BY_CHANNEL
                || event.type == GameplayEventType.COINS_EARNED || event.type == GameplayEventType.RUNE_DUST_EARNED
                || event.type == GameplayEventType.SHARDS_EARNED || event.type == GameplayEventType.RUNE_CORES_EARNED) return;

        if (event.type == GameplayEventType.MASTERY_BRANCH_SELECTED) {
            String branch = event.metadata.get("branch");
            if (branch == null || !state(event.playerUuid).everSelectedMasteryIds.add(branch)) return;
            dirty.add(event.playerUuid);
            route(event.playerUuid, event.type, BigDecimal.ONE, event.metadata);
            return;
        }
        if (event.type == GameplayEventType.PET_PURCHASE_SPECIFIC) {
            String petId = event.metadata.get("petId");
            if (petId == null || !state(event.playerUuid).everObtainedPetIds.add(petId)) return;
            dirty.add(event.playerUuid);
            route(event.playerUuid, event.type, BigDecimal.ONE, event.metadata);
            return;
        }
        if (event.type == GameplayEventType.LOCATION_REACHED) {
            String locationId = event.metadata.get("locationId");
            if (locationId != null) {
                if (!state(event.playerUuid).visitedLocationIds.add(locationId)) return;
                dirty.add(event.playerUuid);
            }
        }
        route(event.playerUuid, event.type, event.amount, event.metadata);
    }

    /** expectedStageIndex is part of the client intent so replaying Claim I can never advance Claim II. */
    public synchronized ClaimResult claim(UUID playerUuid, String achievementId, int expectedStageIndex) {
        AchievementDefinition definition = catalog.get(achievementId);
        if (playerUuid == null || definition == null) return ClaimResult.UNKNOWN_ACHIEVEMENT;
        AchievementPlayerState state = state(playerUuid);
        int nextStage = state.claimedStage(achievementId) + 1;
        if (expectedStageIndex != nextStage) return ClaimResult.OUT_OF_ORDER;
        AchievementStage stage = definition.stage(nextStage);
        if (stage == null) return ClaimResult.FINAL_STAGE_CLAIMED;
        if (state.progress(achievementId).compareTo(stage.target) < 0) return ClaimResult.NOT_COMPLETED;
        String claimKey = achievementId + ":" + nextStage;
        if (!rewards.grantStageRewards(playerUuid, claimKey, stage)) return ClaimResult.REWARD_FAILED;
        state.highestClaimedStage.put(achievementId, nextStage);
        state.achievementPoints = safeAdd(state.achievementPoints, stage.achievementPoints);
        repository.save(playerUuid, state.copy());
        dirty.remove(playerUuid);
        return ClaimResult.APPLIED;
    }

    public synchronized AchievementPlayerState snapshot(UUID playerUuid) { return state(playerUuid).copy(); }

    public synchronized int highestCompletedStage(UUID playerUuid, String achievementId) {
        AchievementDefinition definition = catalog.get(achievementId);
        if (definition == null) return 0;
        BigDecimal progress = state(playerUuid).progress(achievementId);
        int completed = 0;
        for (AchievementStage stage : definition.stages) {
            if (progress.compareTo(stage.target) < 0) break;
            completed = stage.stageIndex;
        }
        return completed;
    }

    public synchronized void flush(UUID playerUuid) {
        if (playerUuid == null || !dirty.remove(playerUuid)) return;
        repository.save(playerUuid, state(playerUuid).copy());
    }

    public synchronized void flushAll() {
        for (UUID playerUuid : new HashSet<>(dirty)) flush(playerUuid);
    }

    public synchronized void unload(UUID playerUuid) {
        flush(playerUuid);
        cache.remove(playerUuid);
        dirty.remove(playerUuid);
    }

    private void route(UUID playerUuid, GameplayEventType type, BigDecimal amount, Map<String, String> metadata) {
        if (amount == null || amount.signum() <= 0) return;
        AchievementPlayerState state = state(playerUuid);
        boolean changed = false;
        for (AchievementDefinition definition : catalog.interested(type)) {
            if (!definition.matches(metadata)) continue;
            BigDecimal before = state.progress(definition.achievementId);
            BigDecimal after = before.add(amount);
            state.achievementProgress.put(definition.achievementId, after);
            changed = true;
            if (definition.hidden && !state.revealedHiddenAchievements.contains(definition.achievementId)
                    && crossedAnyStage(definition, before, after)) {
                state.revealedHiddenAchievements.add(definition.achievementId);
            }
        }
        if (changed) dirty.add(playerUuid);
    }

    private AchievementPlayerState state(UUID playerUuid) {
        AchievementPlayerState state = cache.get(playerUuid);
        if (state == null) {
            state = repository.load(playerUuid);
            if (state == null) state = new AchievementPlayerState();
            cache.put(playerUuid, state);
        }
        return state;
    }

    private static boolean crossedAnyStage(AchievementDefinition definition, BigDecimal before, BigDecimal after) {
        for (AchievementStage stage : definition.stages) {
            if (before.compareTo(stage.target) < 0 && after.compareTo(stage.target) >= 0) return true;
        }
        return false;
    }

    private static GameplayEventType resourceEvent(String resource) {
        String normalized = ResourceDefinitions.normalize(resource);
        if ("coins".equals(normalized)) return GameplayEventType.COINS_EARNED;
        if (ResourceDefinitions.RUNE_DUST.equals(normalized)) return GameplayEventType.RUNE_DUST_EARNED;
        if (ResourceDefinitions.RUNE_SHARDS.equals(normalized)) return GameplayEventType.SHARDS_EARNED;
        if (ResourceDefinitions.RUNE_CORES.equals(normalized)) return GameplayEventType.RUNE_CORES_EARNED;
        return null;
    }

    private static long safeAdd(long a, long b) {
        if (b <= 0L) return a;
        return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b;
    }
}
