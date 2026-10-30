package com.example.revolution.rune.definition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RuneCollectionCatalogTest {
    @Test
    void productionCatalogContainsSevenCollectionsWithTwentySixStableSlotsEach() {
        List<RuneCollectionDefinition> collections = RuneCollectionCatalog.all();
        assertEquals(RuneLayoutDefinition.RUNE_COLLECTION_COUNT, collections.size());

        for (RuneCollectionDefinition collection : collections) {
            assertEquals(RuneLayoutDefinition.SLOTS_PER_COLLECTION, collection.runes.size(), collection.id.id());
            Set<RuneSlotId> slots = new HashSet<>();
            for (RuneDefinition rune : collection.runes) {
                assertEquals(collection.id, rune.collectionId);
                assertTrue(slots.add(rune.slotId), "duplicate slot in " + collection.id.id());
                assertTrue(!rune.effects.isEmpty(), "missing effects for " + rune.identity());
            }
            assertEquals(RuneLayoutDefinition.SLOTS_PER_COLLECTION, slots.size());
        }
    }

    @Test
    void collectionOneKeepsExistingPrimaryContentNames() {
        RuneCollectionDefinition primary = RuneCollectionCatalog.get(RuneCollectionId.COLLECTION_01);
        assertNotNull(primary);
        assertEquals("Кинжал убийцы", primary.rune(RuneSlotId.ofOrdinal(1)).displayName);
        assertEquals("Реликвия охотника", primary.rune(RuneSlotId.ofOrdinal(26)).displayName);
    }

    @Test
    void technicalCollectionsFollowAgreedReferenceChannelMapping() {
        RuneReferenceTheme[] themes = RuneReferenceTheme.values();
        RuneCollectionId[] ids = RuneCollectionId.values();
        for (int index = 1; index < ids.length; index++) {
            RuneCollectionDefinition collection = RuneCollectionCatalog.get(ids[index]);
            RuneDamageChannel expected = themes[index].channel();
            RuneEffectDefinition first = collection.rune(RuneSlotId.ofOrdinal(1)).effects.get(0);
            assertEquals(RuneEffectType.DAMAGE_FLAT, first.type);
            assertEquals(expected, first.damageChannel, ids[index].id());
        }
    }

    @Test
    void technicalCatalogMatchesProductionTemplateSlotSemantics() {
        RuneCollectionDefinition burning = RuneCollectionCatalog.get(RuneCollectionId.COLLECTION_06);

        assertEquals(RuneEffectType.DAMAGE_FLAT,
                burning.rune(RuneSlotId.ofOrdinal(7)).effects.get(0).type);
        assertEquals(RuneEffectType.DAMAGE_MULTIPLIER,
                burning.rune(RuneSlotId.ofOrdinal(8)).effects.get(0).type);
        assertEquals(RuneEffectType.ALL_DAMAGE_MULTIPLIER,
                burning.rune(RuneSlotId.ofOrdinal(12)).effects.get(0).type);

        RuneDefinition proc = burning.rune(RuneSlotId.ofOrdinal(14));
        assertEquals(2, proc.effects.size());
        assertEquals(RuneEffectType.PROC_CHANCE, proc.effects.get(0).type);
        assertEquals(RuneEffectType.PROC_DAMAGE_MULTIPLIER, proc.effects.get(1).type);

        RuneEffectDefinition temporary = burning.rune(RuneSlotId.ofOrdinal(15)).effects.get(0);
        assertEquals(RuneEffectType.TEMPORARY_DAMAGE_MULTIPLIER, temporary.type);
        assertEquals("ALL_DAMAGE", temporary.targetSelector);

        assertEquals(RuneEffectType.DELAYED_DAMAGE_BONUS,
                burning.rune(RuneSlotId.ofOrdinal(20)).effects.get(0).type);
        assertEquals(RuneEffectType.EFFECTIVENESS,
                burning.rune(RuneSlotId.ofOrdinal(21)).effects.get(0).type);
        assertEquals(RuneEffectType.TEMPORARY_EFFECT_DURATION,
                burning.rune(RuneSlotId.ofOrdinal(22)).effects.get(0).type);
        assertEquals(RuneEffectType.ENHANCEMENT_EFFECTIVENESS,
                burning.rune(RuneSlotId.ofOrdinal(23)).effects.get(0).type);

        RuneDefinition hybrid = burning.rune(RuneSlotId.ofOrdinal(24));
        assertEquals(2, hybrid.effects.size());
        assertEquals(RuneDamageChannel.BURNING, hybrid.effects.get(0).damageChannel);
        assertEquals(RuneDamageChannel.CLICK, hybrid.effects.get(1).damageChannel);

        RuneDefinition petXp = burning.rune(RuneSlotId.ofOrdinal(19));
        assertEquals(RuneEffectType.PET_XP_MULTIPLIER, petXp.effects.get(0).type);

        Set<RuneEffectType> types = new HashSet<>();
        for (RuneCollectionDefinition collection : RuneCollectionCatalog.all()) {
            for (RuneDefinition rune : collection.runes) {
                for (RuneEffectDefinition effect : rune.effects) types.add(effect.type);
            }
        }
        for (RuneEffectType type : RuneEffectType.values()) {
            assertTrue(types.contains(type), "catalog misses generic effect type " + type);
        }
    }
}
