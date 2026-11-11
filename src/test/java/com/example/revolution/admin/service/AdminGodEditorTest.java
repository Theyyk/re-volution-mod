package com.example.revolution.admin.service;

import com.example.revolution.god.GodWorshipModel;
import com.example.revolution.god.GodWorshipRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AdminGodEditorTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000288");

    @Test void normalizesUnlockedLockedGodAndRepairsStalePreference() {
        GodWorshipModel.RunState run = new GodWorshipModel.RunState();
        GodWorshipModel.PersistentState persistent = new GodWorshipModel.PersistentState();

        GodWorshipModel.State god = run.gods.get(GodWorshipModel.GodId.GOD_1);
        god.unlocked = true;
        god.level = 0L;
        god.phase = GodWorshipModel.Phase.LOCKED;
        god.chargeProgress = -5L;
        god.buffActiveUntilMillis = -1L;
        god.cooldownUntilMillis = -2L;
        god.offeringSettledUntilMillis = -3L;
        god.passiveChargeRemainder = new BigDecimal("-0.5");

        GodWorshipModel.Preference preference = persistent.preferences.get(GodWorshipModel.GodId.GOD_1);
        preference.activationMode = GodWorshipModel.ActivationMode.MANUAL;
        preference.linkedGodId = GodWorshipModel.GodId.GOD_1;

        AdminMechanicsService.normalizeGodEditorState(run, persistent);

        assertTrue(god.unlocked);
        assertEquals(1L, god.level);
        assertEquals(GodWorshipModel.Phase.CHARGE, god.phase);
        assertEquals(0L, god.chargeProgress);
        assertEquals(0L, god.buffActiveUntilMillis);
        assertEquals(0L, god.cooldownUntilMillis);
        assertEquals(0L, god.offeringSettledUntilMillis);
        assertEquals(0, BigDecimal.ZERO.compareTo(god.passiveChargeRemainder));
        assertEquals(GodWorshipModel.ActivationMode.MANUAL, preference.activationMode);
        assertNull(preference.linkedGodId);
    }

    @Test void savesNormalizedRunStateWithoutRequiringPersistentRewrite() {
        MemoryRepository repository = new MemoryRepository();
        GodWorshipModel.RunState run = repository.loadRunState(PLAYER);
        GodWorshipModel.PersistentState persistent = repository.loadPersistentState(PLAYER);

        GodWorshipModel.State god = run.gods.get(GodWorshipModel.GodId.GOD_1);
        god.unlocked = true;
        god.level = 0L;
        god.phase = GodWorshipModel.Phase.LOCKED;
        AdminMechanicsService.normalizeGodEditorState(run, persistent);
        AdminMechanicsService.saveGodEditorState(PLAYER, repository, run, persistent);

        GodWorshipModel.State saved = repository.loadRunState(PLAYER).gods.get(GodWorshipModel.GodId.GOD_1);
        assertTrue(saved.unlocked);
        assertEquals(1L, saved.level);
        assertEquals(GodWorshipModel.Phase.CHARGE, saved.phase);
        assertEquals(1, repository.runSaveCount);
        assertEquals(0, repository.persistentSaveCount);
    }

    @Test void rollsBackPersistentStateWhenRunSaveFails() {
        MemoryRepository repository = new MemoryRepository();
        GodWorshipModel.RunState run = repository.loadRunState(PLAYER);
        GodWorshipModel.PersistentState persistent = repository.loadPersistentState(PLAYER);
        run.gods.get(GodWorshipModel.GodId.GOD_1).unlocked = true;
        run.gods.get(GodWorshipModel.GodId.GOD_1).level = 1L;
        run.gods.get(GodWorshipModel.GodId.GOD_1).phase = GodWorshipModel.Phase.CHARGE;
        persistent.offerings = new BigDecimal("25");
        repository.failRunSave = true;

        assertThrows(IllegalStateException.class,
                () -> AdminMechanicsService.saveGodEditorState(PLAYER, repository, run, persistent));

        assertEquals(0, BigDecimal.ZERO.compareTo(repository.loadPersistentState(PLAYER).offerings));
        assertEquals(2, repository.persistentSaveCount);
    }

    private static final class MemoryRepository implements GodWorshipRepository {
        private GodWorshipModel.RunState run = new GodWorshipModel.RunState();
        private GodWorshipModel.PersistentState persistent = new GodWorshipModel.PersistentState();
        private int runSaveCount;
        private int persistentSaveCount;
        private boolean failRunSave;

        @Override public GodWorshipModel.RunState loadRunState(UUID playerUuid) { return copy(run); }
        @Override public GodWorshipModel.PersistentState loadPersistentState(UUID playerUuid) { return copy(persistent); }

        @Override public void saveRunState(UUID playerUuid, GodWorshipModel.RunState state) {
            runSaveCount++;
            if (failRunSave) throw new IllegalStateException("simulated run save failure");
            run = copy(state);
        }

        @Override public void savePersistentState(UUID playerUuid, GodWorshipModel.PersistentState state) {
            persistentSaveCount++;
            persistent = copy(state);
        }

        @Override public void resetRunState(UUID playerUuid) { run = new GodWorshipModel.RunState(); }

        private static GodWorshipModel.RunState copy(GodWorshipModel.RunState source) {
            GodWorshipModel.RunState result = new GodWorshipModel.RunState();
            for (GodWorshipModel.GodId id : GodWorshipModel.GodId.values()) {
                GodWorshipModel.State target = result.gods.get(id);
                GodWorshipModel.State value = source.gods.get(id);
                if (value == null) continue;
                GodWorshipModel.State cloned = value.copy();
                target.unlocked = cloned.unlocked;
                target.level = cloned.level;
                target.phase = cloned.phase;
                target.buffActiveUntilMillis = cloned.buffActiveUntilMillis;
                target.cooldownUntilMillis = cloned.cooldownUntilMillis;
                target.chargeProgress = cloned.chargeProgress;
                target.offeringSettledUntilMillis = cloned.offeringSettledUntilMillis;
                target.passiveChargeRemainder = cloned.passiveChargeRemainder;
            }
            return result;
        }

        private static GodWorshipModel.PersistentState copy(GodWorshipModel.PersistentState source) {
            GodWorshipModel.PersistentState result = new GodWorshipModel.PersistentState();
            result.offerings = source.offerings == null ? BigDecimal.ZERO : source.offerings;
            result.permanentDurationLevel = source.permanentDurationLevel;
            for (GodWorshipModel.GodId id : GodWorshipModel.GodId.values()) {
                GodWorshipModel.Preference from = source.preferences.get(id);
                GodWorshipModel.Preference to = result.preferences.get(id);
                if (from == null) continue;
                to.autoWorshipEnabled = from.autoWorshipEnabled;
                to.activationMode = from.activationMode;
                to.linkedGodId = from.linkedGodId;
            }
            return result;
        }
    }
}
