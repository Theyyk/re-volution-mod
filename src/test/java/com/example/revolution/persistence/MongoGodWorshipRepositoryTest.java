package com.example.revolution.persistence;

import com.example.revolution.god.GodWorshipModel;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

final class MongoGodWorshipRepositoryTest {
    @Test
    void absentGodDocumentsMigrateToSafeDefaults() {
        GodWorshipModel.RunState run = MongoGodWorshipRepository.decodeRun(null);
        GodWorshipModel.PersistentState persistent = MongoGodWorshipRepository.decodePersistent(null);

        for (GodWorshipModel.GodId god : GodWorshipModel.GodId.values()) {
            assertFalse(run.gods.get(god).unlocked);
            assertEquals(0L, run.gods.get(god).level);
            assertEquals(GodWorshipModel.Phase.LOCKED, run.gods.get(god).phase);
            assertEquals(GodWorshipModel.ActivationMode.MANUAL, persistent.preferences.get(god).activationMode);
            assertNull(persistent.preferences.get(god).linkedGodId);
        }
        assertEquals(0, persistent.offerings.signum());
        assertEquals(0L, persistent.permanentDurationLevel);
    }

    @Test
    void roundTripPreservesRunAndPersistentGodState() {
        GodWorshipModel.RunState run = new GodWorshipModel.RunState();
        GodWorshipModel.State god = run.gods.get(GodWorshipModel.GodId.GOD_2);
        god.unlocked = true;
        god.level = 12L;
        god.phase = GodWorshipModel.Phase.BUFF_ACTIVE;
        god.buffActiveUntilMillis = 123_456L;
        god.chargeProgress = 7L;
        god.offeringSettledUntilMillis = 120_000L;

        GodWorshipModel.PersistentState persistent = new GodWorshipModel.PersistentState();
        persistent.offerings = new BigDecimal("42.125");
        persistent.permanentDurationLevel = 9L;
        GodWorshipModel.Preference preference = persistent.preferences.get(GodWorshipModel.GodId.GOD_3);
        preference.autoWorshipEnabled = true;
        preference.activationMode = GodWorshipModel.ActivationMode.LINKED_TO_GOD;
        preference.linkedGodId = GodWorshipModel.GodId.GOD_2;

        Document encodedRun = MongoGodWorshipRepository.encodeRun(run);
        Document encodedPersistent = MongoGodWorshipRepository.encodePersistent(persistent);
        GodWorshipModel.RunState decodedRun = MongoGodWorshipRepository.decodeRun(encodedRun);
        GodWorshipModel.PersistentState decodedPersistent = MongoGodWorshipRepository.decodePersistent(encodedPersistent);

        GodWorshipModel.State decodedGod = decodedRun.gods.get(GodWorshipModel.GodId.GOD_2);
        assertEquals(12L, decodedGod.level);
        assertEquals(GodWorshipModel.Phase.BUFF_ACTIVE, decodedGod.phase);
        assertEquals(123_456L, decodedGod.buffActiveUntilMillis);
        assertEquals(120_000L, decodedGod.offeringSettledUntilMillis);
        assertEquals(0, decodedPersistent.offerings.compareTo(new BigDecimal("42.125")));
        assertEquals(9L, decodedPersistent.permanentDurationLevel);
        assertEquals(GodWorshipModel.ActivationMode.LINKED_TO_GOD,
                decodedPersistent.preferences.get(GodWorshipModel.GodId.GOD_3).activationMode);
        assertEquals(GodWorshipModel.GodId.GOD_2,
                decodedPersistent.preferences.get(GodWorshipModel.GodId.GOD_3).linkedGodId);
    }
}
