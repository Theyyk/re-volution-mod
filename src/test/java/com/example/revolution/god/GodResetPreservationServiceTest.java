package com.example.revolution.god;

import com.example.revolution.god.GodWorshipModel.ActivationMode;
import com.example.revolution.god.GodWorshipModel.GodId;
import com.example.revolution.god.GodWorshipModel.PersistentState;
import com.example.revolution.god.GodWorshipModel.Phase;
import com.example.revolution.god.GodWorshipModel.RunState;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GodResetPreservationServiceTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000702");

    @Test
    void requiresEnabledFeatureAndCompletedMechanicUnlock() {
        for (boolean feature : new boolean[]{false, true}) {
            for (boolean unlocked : new boolean[]{false, true}) {
                MemoryRepository repository = new MemoryRepository();
                seed(repository);
                AtomicBoolean featureFlag = new AtomicBoolean(feature);
                AtomicBoolean mechanicUnlock = new AtomicBoolean(unlocked);
                GodResetPreservationService service = new GodResetPreservationService(
                        repository, featureFlag::get, uuid -> mechanicUnlock.get() && PLAYER.equals(uuid));

                service.afterAppliedReset(PLAYER);

                if (feature && unlocked) {
                    assertEquals(0, repository.resetCalls);
                    assertTrue(repository.run.gods.get(GodId.GOD_1).unlocked);
                    assertEquals(7L, repository.run.gods.get(GodId.GOD_1).level);
                    assertEquals(Phase.BUFF_ACTIVE, repository.run.gods.get(GodId.GOD_1).phase);
                    assertEquals(95_000L, repository.run.gods.get(GodId.GOD_1).buffActiveUntilMillis);
                } else {
                    assertEquals(1, repository.resetCalls);
                    assertFalse(repository.run.gods.get(GodId.GOD_1).unlocked);
                    assertEquals(0L, repository.run.gods.get(GodId.GOD_1).level);
                }

                // Persistent wallet, meta and automation choices are never part of reset.
                assertEquals(new BigDecimal("42.50"), repository.persistent.offerings);
                assertEquals(3L, repository.persistent.permanentDurationLevel);
                assertTrue(repository.persistent.preferences.get(GodId.GOD_1).autoWorshipEnabled);
                assertEquals(ActivationMode.LINKED_TO_GOD,
                        repository.persistent.preferences.get(GodId.GOD_1).activationMode);
                assertEquals(GodId.GOD_2, repository.persistent.preferences.get(GodId.GOD_1).linkedGodId);
            }
        }
    }

    @Test
    void nullPlayerDoesNotResetOrLookupUnlock() {
        MemoryRepository repository = new MemoryRepository();
        seed(repository);
        GodResetPreservationService service = new GodResetPreservationService(
                repository, () -> false, uuid -> { throw new AssertionError("should not lookup"); });
        service.afterAppliedReset(null);
        assertEquals(0, repository.resetCalls);
        assertTrue(repository.run.gods.get(GodId.GOD_1).unlocked);
    }

    private static void seed(MemoryRepository repository) {
        repository.run.gods.get(GodId.GOD_1).unlocked = true;
        repository.run.gods.get(GodId.GOD_1).level = 7L;
        repository.run.gods.get(GodId.GOD_1).phase = Phase.BUFF_ACTIVE;
        repository.run.gods.get(GodId.GOD_1).buffActiveUntilMillis = 95_000L;
        repository.persistent.offerings = new BigDecimal("42.50");
        repository.persistent.permanentDurationLevel = 3L;
        repository.persistent.preferences.get(GodId.GOD_1).autoWorshipEnabled = true;
        repository.persistent.preferences.get(GodId.GOD_1).activationMode = ActivationMode.LINKED_TO_GOD;
        repository.persistent.preferences.get(GodId.GOD_1).linkedGodId = GodId.GOD_2;
    }

    private static final class MemoryRepository implements GodWorshipRepository {
        RunState run = new RunState();
        PersistentState persistent = new PersistentState();
        int resetCalls;

        @Override public RunState loadRunState(UUID uuid) { return run; }
        @Override public PersistentState loadPersistentState(UUID uuid) { return persistent; }
        @Override public void saveRunState(UUID uuid, RunState value) { run = value; }
        @Override public void savePersistentState(UUID uuid, PersistentState value) { persistent = value; }
        @Override public void resetRunState(UUID uuid) {
            resetCalls++;
            run = new RunState();
        }
    }
}
