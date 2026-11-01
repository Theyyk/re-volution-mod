package com.example.revolution.admin.service;

import com.example.revolution.achievement.AchievementService;
import com.example.revolution.aura.AuraModel;
import com.example.revolution.aura.AuraService;
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