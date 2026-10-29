package com.example.revolution.rune.definition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RuneCollectionDefinitionTest {
    @Test
    void canonicalIdsAreStableAndComplete() {
        assertEquals(7, RuneCollectionId.values().length);
        assertEquals("collection_01", RuneCollectionId.COLLECTION_01.id());
        assertEquals("collection_07", RuneCollectionId.COLLECTION_07.id());
        assertEquals(RuneCollectionId.COLLECTION_01, RuneCollectionId.fromLegacyIndex(0));
        assertEquals(RuneCollectionId.COLLECTION_07, RuneCollectionId.fromLegacyIndex(6));
        assertEquals("slot_01", RuneSlotId.fromLegacyIndex(0).id());
        assertEquals("slot_26", RuneSlotId.fromLegacyIndex(25).id());
    }

    @Test
    void validCollectionRequiresAllTwentySixPhysicalSlots() {
        RuneCollectionDefinition definition = new RuneCollectionDefinition(
                RuneCollectionId.COLLECTION_02,
                "Collection 2",
                completeRunes(RuneCollectionId.COLLECTION_02));

        assertEquals(26, definition.runes.size());
        assertEquals("collection_02/slot_17", definition.rune(RuneSlotId.ofOrdinal(17)).identity());
        assertEquals(1, definition.rune(RuneSlotId.ofOrdinal(17)).effects.size());
    }

    @Test
    void duplicateOrMissingSlotsCannotBecomeProductionDefinitions() {
        List<RuneDefinition> runes = completeRunes(RuneCollectionId.COLLECTION_03);
        runes.set(25, runes.get(0));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new RuneCollectionDefinition(RuneCollectionId.COLLECTION_03, "Collection 3", runes));
        assertTrue(error.getMessage().contains("duplicate rune slot"));
    }

    @Test
    void hybridRuneCanCarryMultipleGenericEffects() {
        List<RuneEffectDefinition> effects = new ArrayList<>();
        effects.add(RuneEffectDefinition.damage(RuneEffectType.DAMAGE_MULTIPLIER, RuneDamageChannel.BURNING, 1.2D));
        effects.add(RuneEffectDefinition.damage(RuneEffectType.DAMAGE_MULTIPLIER, RuneDamageChannel.CLICK, 1.1D));

        RuneDefinition rune = new RuneDefinition(
                RuneCollectionId.COLLECTION_05,
                RuneSlotId.ofOrdinal(1),
                "burning_hybrid_01",
                "Burning Hybrid Rune 01",
                effects);

        assertEquals(2, rune.effects.size());
        assertEquals(RuneDamageChannel.BURNING, rune.effects.get(0).damageChannel);
        assertEquals(RuneDamageChannel.CLICK, rune.effects.get(1).damageChannel);
    }

    private static List<RuneDefinition> completeRunes(RuneCollectionId collectionId) {
        List<RuneDefinition> runes = new ArrayList<>();
        for (int ordinal = 1; ordinal <= RuneLayoutDefinition.SLOTS_PER_COLLECTION; ordinal++) {
            String suffix = ordinal < 10 ? "0" + ordinal : String.valueOf(ordinal);
            runes.add(new RuneDefinition(
                    collectionId,
                    RuneSlotId.ofOrdinal(ordinal),
                    collectionId.id() + "_slot_" + suffix,
                    "Rune " + suffix,
                    Collections.singletonList(RuneEffectDefinition.scalar(
                            RuneEffectType.MONSTER_RESOURCE_MULTIPLIER, 1.0D))));
        }
        return runes;
    }
}
