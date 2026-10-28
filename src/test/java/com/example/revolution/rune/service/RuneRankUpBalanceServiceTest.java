package com.example.revolution.rune.service;

import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.definition.RuneRankUpRule;
import com.example.revolution.rune.rules.RuneProgression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RuneRankUpBalanceServiceTest {
    @Test
    void bundledDefaultsDefineNormalAndImprovedTransitions() {
        RuneRankUpBalanceService service = new RuneRankUpBalanceService(BalanceBootstrap.activateBundledDefaults());

        RuneRankUpRule normal = service.rule(RuneStatusConfig.STATUS_POISON, RuneProgression.NORMAL_RANK);
        assertEquals(RuneProgression.IMPROVED_RANK, normal.targetRank);
        assertEquals(26, normal.requiredSlots);
        assertEquals(ResourceDefinitions.RUNE_DUST, normal.resource);
        assertEquals(100, normal.cost);
        assertEquals(RuneRankUpRule.ClearMode.AT_OR_BELOW_SOURCE, normal.clearMode);
        assertTrue(normal.preserveEnhancements);

        RuneRankUpRule improved = service.rule(RuneStatusConfig.STATUS_POISON, RuneProgression.IMPROVED_RANK);
        assertEquals(RuneProgression.ADVANCED_RANK, improved.targetRank);
        assertEquals(26, improved.requiredSlots);
        assertEquals(ResourceDefinitions.RUNE_DUST, improved.resource);
        assertEquals(100, improved.cost);
    }

    @Test
    void maximumRankHasNoForwardTransition() {
        RuneRankUpBalanceService service = new RuneRankUpBalanceService(BalanceBootstrap.activateBundledDefaults());

        assertThrows(IllegalArgumentException.class,
                () -> service.rule(RuneStatusConfig.STATUS_POISON, RuneProgression.ADVANCED_RANK));
    }
}
