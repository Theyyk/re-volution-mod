package com.example.revolution.admin.service;

import com.example.revolution.achievement.AchievementPlayerState;
import com.example.revolution.achievement.AchievementRepository;
import com.example.revolution.achievement.AchievementService;
import com.example.revolution.aura.AuraModel;
import com.example.revolution.aura.AuraRepository;
import com.example.revolution.aura.AuraService;
import com.example.revolution.god.GodWorshipModel;
import com.example.revolution.god.GodWorshipRepository;
import com.example.revolution.god.GodWorshipRules;
import com.example.revolution.god.GodWorshipService;
import com.example.revolution.pet.repository.PetRepository;
import com.example.revolution.pet.service.PetService;
import com.example.revolution.pet.state.PetPlayerState;
import com.example.revolution.prestige.PrestigeRepository;
import com.example.revolution.prestige.PrestigeService;
import com.example.revolution.prestige.PrestigeState;
import com.example.revolution.unique.UniqueRuneModel;
import com.example.revolution.unique.UniqueRuneRepository;
import com.example.revolution.unique.UniqueRuneService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Admin API projection and DEV-only editor for mechanic-specific player state. */
public final class AdminMechanicsService {
    public static final int SNAPSHOT_SCHEMA_VERSION = 1;
    public static final int TIME_SIMULATION_SCHEMA_VERSION = 1;
    public static final int EDITOR_SCHEMA_VERSION = 1;
    private static final Gson JSON = new Gson();

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

    /** Full persistent state intended only for the authenticated local DEV editor. */
    public Map<String, Object> editableSnapshot(UUID playerUuid) {
        if (playerUuid == null) throw new IllegalArgumentException("playerUuid is required");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schemaVersion", EDITOR_SCHEMA_VERSION);
        result.put("playerUuid", playerUuid.toString());
        result.put("mastery", prestige.snapshot(playerUuid));
        result.put("pets", pets.snapshot(playerUuid));
        Map<String, Object> godState = new LinkedHashMap<>();
        godState.put("run", gods.runState(playerUuid));
        godState.put("persistent", gods.persistentState(playerUuid));
        result.put("gods", godState);
        result.put("aura", auras.state(playerUuid));
        result.put("achievements", achievements.snapshot(playerUuid));
        result.put("uniqueRunes", uniqueRunes.snapshot(playerUuid));
        return result;
    }

    /**
     * DEV admin escape hatch. The Control layer keeps this behind explicit player selection,
     * reason/audit metadata and the local Admin API authentication boundary.
     */
    public Map<String, Object> replaceSection(UUID playerUuid, String section, String json) {
        if (playerUuid == null) throw new IllegalArgumentException("playerUuid is required");
        if (section == null || section.trim().isEmpty()) throw new IllegalArgumentException("section is required");
        if (json == null || json.trim().isEmpty()) throw new IllegalArgumentException("json is required");
        String key = section.trim();
        try {
            if ("mastery".equals(key)) {
                PrestigeState value = JSON.fromJson(json, PrestigeState.class);
                if (value == null) throw new IllegalArgumentException("mastery state is required");
                repository(prestige, PrestigeRepository.class).save(playerUuid, value.copy());
            } else if ("pets".equals(key)) {
                PetPlayerState value = JSON.fromJson(json, PetPlayerState.class);
                if (value == null) throw new IllegalArgumentException("pet state is required");
                repository(pets, PetRepository.class).save(playerUuid, value.copy());
            } else if ("gods".equals(key)) {
                JsonObject value = new JsonParser().parse(json).getAsJsonObject();
                if (!value.has("run") || !value.has("persistent")) throw new IllegalArgumentException("gods requires run and persistent");
                GodWorshipModel.RunState run = JSON.fromJson(value.get("run"), GodWorshipModel.RunState.class);
                GodWorshipModel.PersistentState persistent = JSON.fromJson(value.get("persistent"), GodWorshipModel.PersistentState.class);
                if (run == null || persistent == null) throw new IllegalArgumentException("god state is required");
                normalizeGodEditorState(run, persistent);
                GodWorshipRepository repository = repository(gods, GodWorshipRepository.class);
                saveGodEditorState(playerUuid, repository, run, persistent);
            } else if ("aura".equals(key)) {
                AuraModel.PlayerState value = JSON.fromJson(json, AuraModel.PlayerState.class);
                if (value == null) throw new IllegalArgumentException("aura state is required");
                value.repairActiveReference();
                repository(auras, AuraRepository.class).save(playerUuid, value);
            } else if ("achievements".equals(key)) {
                AchievementPlayerState value = JSON.fromJson(json, AchievementPlayerState.class);
                if (value == null) throw new IllegalArgumentException("achievement state is required");
                achievements.unload(playerUuid);
                AchievementRepository repository = repository(achievements, AchievementRepository.class);
                repository.save(playerUuid, value.copy());
            } else if ("uniqueRunes".equals(key)) {
                UniqueRuneModel.PlayerState value = JSON.fromJson(json, UniqueRuneModel.PlayerState.class);
                if (value == null) throw new IllegalArgumentException("unique rune state is required");
                repository(uniqueRunes, UniqueRuneRepository.class).save(playerUuid, value.copy());
                uniqueRunes.clearReplayState();
            } else {
                throw new IllegalArgumentException("Unknown mechanics section: " + key);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Admin mechanics section replacement failed", e);
        }
        return editableSnapshot(playerUuid);
    }

    static void saveGodEditorState(UUID playerUuid, GodWorshipRepository repository,
                                   GodWorshipModel.RunState run,
                                   GodWorshipModel.PersistentState persistent) {
        GodWorshipModel.RunState oldRun = repository.loadRunState(playerUuid);
        GodWorshipModel.PersistentState oldPersistent = repository.loadPersistentState(playerUuid);
        boolean runChanged = !sameEditorState(oldRun, run);
        boolean persistentChanged = !sameEditorState(oldPersistent, persistent);

        if (!runChanged && !persistentChanged) return;

        // Persistent data is written first. If it fails, reset-scoped run state is untouched.
        if (persistentChanged) repository.savePersistentState(playerUuid, persistent);
        try {
            if (runChanged) repository.saveRunState(playerUuid, run);
        } catch (RuntimeException failure) {
            if (persistentChanged) {
                try { repository.savePersistentState(playerUuid, oldPersistent); }
                catch (RuntimeException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
            }
            throw failure;
        }
    }

    private static boolean sameEditorState(Object left, Object right) {
        return JSON.toJson(left).equals(JSON.toJson(right));
    }

    static void normalizeGodEditorState(GodWorshipModel.RunState run,
                                        GodWorshipModel.PersistentState persistent) {
        if (run.gods == null || persistent.preferences == null)
            throw new IllegalArgumentException("god maps are required");

        for (GodWorshipModel.GodId god : GodWorshipModel.GodId.values()) {
            GodWorshipModel.State state = run.gods.get(god);
            if (state == null) {
                state = new GodWorshipModel.State();
                run.gods.put(god, state);
            }
            if (state.passiveChargeRemainder == null || state.passiveChargeRemainder.signum() < 0)
                state.passiveChargeRemainder = BigDecimal.ZERO;
            if (state.chargeProgress < 0L) state.chargeProgress = 0L;
            if (state.buffActiveUntilMillis < 0L) state.buffActiveUntilMillis = 0L;
            if (state.cooldownUntilMillis < 0L) state.cooldownUntilMillis = 0L;
            if (state.offeringSettledUntilMillis < 0L) state.offeringSettledUntilMillis = 0L;

            if (!state.unlocked) {
                state.level = 0L;
                state.phase = GodWorshipModel.Phase.LOCKED;
                state.chargeProgress = 0L;
                state.buffActiveUntilMillis = 0L;
                state.cooldownUntilMillis = 0L;
                state.offeringSettledUntilMillis = 0L;
                state.passiveChargeRemainder = BigDecimal.ZERO;
            } else {
                if (state.level < 1L) state.level = 1L;
                if (state.phase == null || state.phase == GodWorshipModel.Phase.LOCKED)
                    state.phase = GodWorshipModel.Phase.CHARGE;
            }

            GodWorshipModel.Preference preference = persistent.preferences.get(god);
            if (preference == null) {
                preference = new GodWorshipModel.Preference();
                persistent.preferences.put(god, preference);
            }
            if (preference.activationMode == null)
                preference.activationMode = GodWorshipModel.ActivationMode.MANUAL;
            if (preference.activationMode != GodWorshipModel.ActivationMode.LINKED_TO_GOD)
                preference.linkedGodId = null;
            if (preference.linkedGodId == god) {
                preference.activationMode = GodWorshipModel.ActivationMode.MANUAL;
                preference.linkedGodId = null;
            }
            preference.validateFor(god);
        }

        if (persistent.offerings == null || persistent.offerings.signum() < 0)
            persistent.offerings = BigDecimal.ZERO;
        if (persistent.permanentDurationLevel < 0L)
            persistent.permanentDurationLevel = 0L;
    }

    @SuppressWarnings("unchecked")
    private static <T> T repository(Object service, Class<T> expectedType) throws Exception {
        Field field = service.getClass().getDeclaredField("repository");
        field.setAccessible(true);
        Object value = field.get(service);
        if (!expectedType.isInstance(value)) throw new IllegalStateException("Unexpected mechanics repository type");
        return (T) value;
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
