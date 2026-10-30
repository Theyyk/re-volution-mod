package com.example.revolution.rune.service;

import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.definition.RuneStatType;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class RuneV230BundledBalanceTest {
    @Test
    void bundledSnapshotIncludesMythicAndAllFixedCollectionTemplates() {
        BalanceService balance = BalanceBootstrap.activateBundledDefaults();
        assertEquals("4", balance.getActiveValue("runes.poison.rankup.rank.3.target"));
        assertEquals("rune_cores", balance.getActiveValue("runes.poison.rankup.rank.3.resource"));

        String[] channels = {"POISON", "DECAY", "WITHER", "BLEEDING", "BURNING", "CURSE"};
        for (int index = 0; index < channels.length; index++) {
            String namespace = String.format(java.util.Locale.ROOT, "runes.collection_%02d", index + 2);
            assertEquals("technical_status", balance.getActiveValue(namespace + ".effect_template"));
            assertEquals(channels[index], balance.getActiveValue(namespace + ".primary_channel"));
            assertNotNull(balance.getActiveSnapshot().getConfig().getCurve(namespace + ".purchase.rank.4"));
            assertNotNull(balance.getActiveSnapshot().getConfig().getCurve(namespace + ".rankup.rank.3.cost"));
        }
    }

    @Test
    void sharedTemplateSubstitutesEachCollectionDamageChannel() {
        RuneEffectBalanceService effects = new RuneEffectBalanceService(BalanceBootstrap.activateBundledDefaults());
        RuneCollectionId[] ids = {
                RuneCollectionId.COLLECTION_02,
                RuneCollectionId.COLLECTION_03,
                RuneCollectionId.COLLECTION_04,
                RuneCollectionId.COLLECTION_05,
                RuneCollectionId.COLLECTION_06,
                RuneCollectionId.COLLECTION_07
        };
        RuneStatType[] expected = {
                RuneStatType.POISON_DAMAGE_FLAT,
                RuneStatType.DECAY_DAMAGE_FLAT,
                RuneStatType.WITHER_DAMAGE_FLAT,
                RuneStatType.BLEEDING_DAMAGE_FLAT,
                RuneStatType.BURNING_DAMAGE_FLAT,
                RuneStatType.CURSE_DAMAGE_FLAT
        };

        for (int i = 0; i < ids.length; i++) {
            List<RuneEffectBalanceService.ResolvedEffect> resolved = effects.effects(ids[i], 0, 4, 0);
            assertEquals(1, resolved.size(), ids[i].id());
            assertEquals(expected[i], resolved.get(0).statType, ids[i].id());
            assertEquals(90.0D, resolved.get(0).primaryValue, 0.000001D, ids[i].id());
        }
    }

    @Test
    void templateKeepsPetXpAsDistinctSafeChannelAndHybridClickAsSecondEffect() {
        RuneEffectBalanceService effects = new RuneEffectBalanceService(BalanceBootstrap.activateBundledDefaults());

        List<RuneEffectBalanceService.ResolvedEffect> pet = effects.effects(RuneCollectionId.COLLECTION_06, 18, 3, 0);
        assertEquals(1, pet.size());
        assertEquals(RuneStatType.PET_XP_MULTIPLIER, pet.get(0).statType);

        List<RuneEffectBalanceService.ResolvedEffect> hybrid = effects.effects(RuneCollectionId.COLLECTION_06, 23, 3, 0);
        assertEquals(2, hybrid.size());
        assertEquals(RuneStatType.BURNING_DAMAGE_MULTIPLIER, hybrid.get(0).statType);
        assertEquals(RuneStatType.CLICK_DAMAGE_MULTIPLIER, hybrid.get(1).statType);
    }

    @Test
    void technicalSlotsUseGenericTimedAndEffectivenessSemantics() {
        RuneEffectBalanceService effects = new RuneEffectBalanceService(BalanceBootstrap.activateBundledDefaults());

        RuneEffectBalanceService.ResolvedEffect delayed = effects.effects(
                RuneCollectionId.COLLECTION_06, 19, 2, 0).get(0);
        assertEquals(RuneEffectBalanceService.EffectType.DELAYED_DAMAGE_BONUS, delayed.type);
        assertEquals("BURNING", delayed.targetSelector);
        assertEquals("HIT_MOB", delayed.trigger);
        assertEquals("TARGET_ALIVE", delayed.condition);
        assertEquals(2.0D, delayed.delaySeconds, 0.000001D);
        assertEquals(1.08D, delayed.primaryValue, 0.000001D);

        RuneEffectBalanceService.ResolvedEffect effectiveness = effects.effects(
                RuneCollectionId.COLLECTION_03, 20, 3, 0).get(0);
        assertEquals(RuneEffectBalanceService.EffectType.EFFECTIVENESS, effectiveness.type);
        assertEquals("DECAY", effectiveness.targetSelector);
        assertEquals(1.15D, effectiveness.primaryValue, 0.000001D);

        RuneEffectBalanceService.ResolvedEffect duration = effects.effects(
                RuneCollectionId.COLLECTION_07, 21, 1, 0).get(0);
        assertEquals(RuneEffectBalanceService.EffectType.TEMPORARY_EFFECT_DURATION, duration.type);
        assertEquals("CURSE", duration.targetSelector);
        assertEquals(1.03D, duration.primaryValue, 0.000001D);

        RuneEffectBalanceService.ResolvedEffect enhancement = effects.effects(
                RuneCollectionId.COLLECTION_05, 22, 4, 0).get(0);
        assertEquals(RuneEffectBalanceService.EffectType.ENHANCEMENT_EFFECTIVENESS, enhancement.type);
        assertEquals("BLEEDING", enhancement.targetSelector);
        assertEquals(1.15D, enhancement.primaryValue, 0.000001D);
    }
}
