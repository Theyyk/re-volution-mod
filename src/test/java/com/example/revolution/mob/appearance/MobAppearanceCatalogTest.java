package com.example.revolution.mob.appearance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class MobAppearanceCatalogTest {
    @Test
    void locationOneHasFortyMobsAndOneBossInStableSlots() {
        List<MobAppearanceDefinition> definitions = MobAppearanceCatalog.location1();
        assertEquals(41, definitions.size());

        Set<String> mobIds = new HashSet<>();
        Set<String> appearanceIds = new HashSet<>();
        for (int slot = 1; slot <= 41; slot++) {
            MobAppearanceDefinition definition = MobAppearanceCatalog.bySlot(1, slot);
            assertNotNull(definition, "missing Location 1 slot " + slot);
            assertEquals(slot, definition.getMobSlotId());
            assertEquals(1, definition.getLocationId());
            assertTrue(mobIds.add(definition.getMobId()), "duplicate mobId " + definition.getMobId());
            assertTrue(appearanceIds.add(definition.getAppearanceId()), "duplicate appearanceId " + definition.getAppearanceId());
            assertEquals(definition, MobAppearanceCatalog.byMobId(definition.getMobId()));
        }

        MobAppearanceDefinition boss = MobAppearanceCatalog.bySlot(1, 41);
        assertEquals("forest_ancient_guardian", boss.getMobId());
        assertEquals("Хранитель Древнего Леса", boss.getDisplayName());
        assertEquals("boss", boss.getFamilyId());
        assertEquals("forest_guardian_boss", boss.getModelId());
        assertEquals("forest_guardian_boss", boss.getAnimationSetId());
    }

    @Test
    void catalogContainsOnlyVisualContentNotBalanceValues() {
        MobAppearanceDefinition first = MobAppearanceCatalog.bySlot(1, 1);
        assertNotNull(first);
        assertEquals("Моховая черепаха", first.getDisplayName());
        assertEquals("forest_turtle", first.getModelId());
        assertEquals("moss_turtle", first.getTextureId());
        assertEquals(0.80D, first.getScale(), 0.00001D);
    }

    @Test
    void animationContractContainsOnlyAgreedVisualStates() {
        MobAnimationState[] states = MobAnimationState.values();
        assertEquals(5, states.length);
        assertEquals("idle", states[0].getId());
        assertEquals("walk", states[1].getId());
        assertEquals("spawn", states[2].getId());
        assertEquals("hurt", states[3].getId());
        assertEquals("death", states[4].getId());
    }
}
