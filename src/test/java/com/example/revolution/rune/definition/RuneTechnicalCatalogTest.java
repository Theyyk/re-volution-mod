package com.example.revolution.rune.definition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RuneTechnicalCatalogTest {
    @Test
    void everyTechnicalCollectionHasTwentySixStructuralEffectSlots() {
        for (RuneCollectionId collectionId : RuneCollectionId.values()) {
            if (collectionId == RuneCollectionId.COLLECTION_01) continue;
            for (int slot = 0; slot < 26; slot++) {
                assertFalse(RuneTechnicalCatalog.effects(collectionId, slot).isEmpty(),
                        collectionId.id() + "/slot_" + slot);
            }
        }
    }

    @Test
    void fixedCollectionChannelsMatchV230MappingAndPetXpIsNoOpOnly() {
        assertEquals(RuneDamageChannel.POISON, RuneTechnicalCatalog.primaryChannel(RuneCollectionId.COLLECTION_02));
        assertEquals(RuneDamageChannel.DECAY, RuneTechnicalCatalog.primaryChannel(RuneCollectionId.COLLECTION_03));
        assertEquals(RuneDamageChannel.WITHER, RuneTechnicalCatalog.primaryChannel(RuneCollectionId.COLLECTION_04));
        assertEquals(RuneDamageChannel.BLEEDING, RuneTechnicalCatalog.primaryChannel(RuneCollectionId.COLLECTION_05));
        assertEquals(RuneDamageChannel.BURNING, RuneTechnicalCatalog.primaryChannel(RuneCollectionId.COLLECTION_06));
        assertEquals(RuneDamageChannel.CURSE, RuneTechnicalCatalog.primaryChannel(RuneCollectionId.COLLECTION_07));

        RuneTechnicalCatalog.EffectLabel pet = RuneTechnicalCatalog.effects(
                RuneCollectionId.COLLECTION_06, 18).get(0);
        assertEquals("PET_XP_MULTIPLIER", pet.type);
        assertTrue(pet.runtimeNoOp);
        assertEquals(null, pet.channel);
    }
}
