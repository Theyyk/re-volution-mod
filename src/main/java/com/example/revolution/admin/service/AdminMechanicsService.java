package com.example.revolution.admin.service;

import com.example.revolution.achievement.AchievementService;
import com.example.revolution.aura.AuraModel;
import com.example.revolution.aura.AuraService;
import com.example.revolution.god.GodWorshipModel;
import com.example.revolution.god.GodWorshipRules;
import com.example.revolution.god.GodWorshipService;
import com.example.revolution.pet.service.PetService;
import com.example.revolution.prestige.PrestigeService;
import com.example.revolution.unique.UniqueRuneService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Read-only Admin API projection for mechanic-specific player state. */
public final class AdminMechanicsService {
    public static final int SNAPSHOT_SCHEMA_VERSION = 1;
    public static final int TIME_SIMULATION_SCHEMA_VERSION = 1;

    private final PrestigeService prestige;
    private final PetService pets;
    private final GodWorshipService gods;
    private final AuraService auras;
    private final AchievementService achievements;
    private final UniqueRuneService uniqueRunes;

    public AdminMechanicsService(PrestigeService prestige, PetService pets, GodWorshipService gods,
                                 AuraService auras, AchievementService achievements, UniqueRuneService uniqueRunes) {
        if (prestige == null || pets == null || gods == null || auras == null || achievements == null || uniqueRunes == null)
            throw new IllegalArgumentException("Admin mechanic dependencies are required");
        this.prestige = prestige;
        this.pets = pets;
        this.gods = gods;
        this.auras = auras;
        this.achievements = achievements;
        this.uniqueRunes = uniqueRunes;
    }

    public Map<String, Object> snapshot(UUID playerUuid) {
        if (playerUuid == null) throw new IllegalArgumentException("playerUuid is required");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schemaVersion", SNAPSHOT_SCHEMA_VERSION);
        result.put("playerUuid", playerUuid.toString());
        result.put("mastery", prestige.snapshot(playerUuid));
        result.put("pets", pets.snapshot(playerUuid));

        Map<String, Object> godState = new LinkedHashMap<>();
        godState.put("run", gods.runState(playerUuid));
        godState.put("persistent", gods.persistentState(playerUuid));
        result.put("gods", godState);

        result.put("aura", safeAuraState(auras.state(playerUuid)));
        result.put("achievements", achievements.snapshot(playerUuid));
        result.put("uniqueRunes", uniqueRunes.snapshot(playerUuid));
        return result;
    }

    /**
     * Pure dry-run of factual God phase transitions at a future timestamp.
     * It intentionally excludes runtime-only passive charge and auto-worship actions.
     */
    public Map<String, Object> simulateGodTime(UUID playerUuid, long nowMillis, long deltaMillis) {
        if (playerUuid == null) throw new IllegalArgumentException("playerUuid is required");
        if (nowMillis < 0L || deltaMillis < 0L) throw new IllegalArgumentException("time values must be non-negative");
        long targetMillis = safeAdd(nowMillis, deltaMillis);
        GodWorshipModel.RunState run = gods.runState(playerUuid);
        GodWorshipService.Policy policy = gods.policy();

        Map<String, Object> rows = new LinkedHashMap<>();
        for (GodWorshipModel.GodId god : GodWorshipModel.GodId.values()) {
            GodWorshipModel.State stored = run.gods.get(god);
            GodWorshipModel.State before = stored == null ? new GodWorshipModel.State() : stored.copy();
            GodWorshipModel.State after = before.copy();
            GodWorshipRules.normalize(after, targetMillis, policy.cooldownDurationMillis(god));

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("enabled", policy.enabled(god));
            row.put("before", safeGodState(before, nowMillis));
            row.put("after", safeGodState(after, targetMillis));
            row.put("changed", before.phase != after.phase
                    || before.buffActiveUntilMillis != after.buffActiveUntilMillis
                    || before.cooldownUntilMillis != after.cooldownUntilMillis
                    || before.chargeProgress != after.chargeProgress);
            rows.put(god.name(), row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schemaVersion", TIME_SIMULATION_SCHEMA_VERSION);
        result.put("playerUuid", playerUuid.toString());
        result.put("simulatedFromMillis", nowMillis);
        result.put("simulatedToMillis", targetMillis);
        result.put("deltaMillis", deltaMillis);
        result.put("mode", "DRY_RUN_PHASE_TRANSITIONS_ONLY");
        result.put("mutatesState", false);
        result.put("gods", rows);
        return result;
    }

    private static Map<String, Object> safeGodState(GodWorshipModel.State state, long atMillis) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("unlocked", state.unlocked);
        result.put("level", state.level);
        result.put("phase", state.phase.name());
        result.put("chargeProgress", state.chargeProgress);
        result.put("buffActiveUntilMillis", state.buffActiveUntilMillis);
        result.put("cooldownUntilMillis", state.cooldownUntilMillis);
        result.put("buffRemainingMillis", Math.max(0L, state.buffActiveUntilMillis - atMillis));
        result.put("cooldownRemainingMillis", Math.max(0L, state.cooldownUntilMillis - atMillis));
        return result;
    }

    private static long safeAdd(long a, long b) {
        if (b > 0L && a > Long.MAX_VALUE - b) return Long.MAX_VALUE;
        return a + b;
    }

    private static Map<String, Object> safeAuraState(AuraModel.PlayerState state) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activeAuraId", state.activeAuraId == null ? null : state.activeAuraId.toString());
        result.put("totalChestOpens", state.totalChestOpens);
        result.put("totalChestAuraDrops", state.totalChestAuraDrops);
        result.put("skipOpeningAnimation", state.skipOpeningAnimation);

        List<Map<String, Object>> owned = new ArrayList<>();
        for (AuraModel.AuraInstance aura : state.ownedAuras) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("instanceId", aura.instanceId.toString());
            item.put("rarity", aura.rarity.name());
            item.put("origin", aura.origin.name());
            item.put("pinned", aura.pinned);
            item.put("createdAtMillis", aura.createdAtMillis);
            item.put("aggregatedEffects", aura.aggregatedEffects);
            owned.add(item);
        }
        result.put("ownedAuras", owned);
        return result;
    }
}
