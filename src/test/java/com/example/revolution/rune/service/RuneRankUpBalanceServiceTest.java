package com.example.revolution.rune.service;

import com.example.revolution.balance.config.BalanceConfigCodec;
import com.example.revolution.balance.config.BalanceConfigParser;
import com.example.revolution.balance.curve.BalanceCurveEngine;
import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.balance.validation.BalanceConfigValidator;
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

        RuneRankUpRule normal = service.rule(RuneStatusConfig.STATUS_POISON, RuneProgression.NORMAL_RANK, 0);
        assertEquals(RuneProgression.IMPROVED_RANK, normal.targetRank);
        assertEquals(26, normal.requiredSlots);
        assertEquals(ResourceDefinitions.RUNE_SHARDS, normal.resource);
        assertEquals(1, normal.cost);
        assertEquals(159, service.rule(RuneStatusConfig.STATUS_POISON, RuneProgression.NORMAL_RANK, 25).cost);
        assertEquals(RuneRankUpRule.ClearMode.AT_OR_BELOW_SOURCE, normal.clearMode);
        assertTrue(normal.preserveEnhancements);

        RuneRankUpRule improved = service.rule(RuneStatusConfig.STATUS_POISON, RuneProgression.IMPROVED_RANK, 0);
        assertEquals(RuneProgression.ADVANCED_RANK, improved.targetRank);
        assertEquals(26, improved.requiredSlots);
        assertEquals(ResourceDefinitions.RUNE_SHARDS, improved.resource);
        assertEquals(1, improved.cost);
        assertEquals(159, service.rule(RuneStatusConfig.STATUS_POISON, RuneProgression.IMPROVED_RANK, 25).cost);
    }

    @Test
    void rankUpCostUsesCountAlreadyAboveSourceRank() {
        BalanceService balance = new BalanceService(
                new BalanceConfigParser(), new BalanceConfigValidator(),
                new BalanceConfigCodec(), new BalanceCurveEngine());
        balance.activate("{\"balanceSchemaVersion\":1,\"values\":{"
                + "\"runes.poison.rankup.rank.1.target\":\"2\","
                + "\"runes.poison.rankup.rank.1.required_slots\":\"26\","
                + "\"runes.poison.rankup.rank.1.resource\":\"rune_shards\","
                + "\"runes.poison.rankup.rank.1.clear_mode\":\"AT_OR_BELOW_SOURCE\","
                + "\"runes.poison.rankup.rank.1.preserve_enhancements\":\"true\"},\"curves\":{"
                + "\"runes.poison.rankup.rank.1.cost\":{\"mode\":\"TABLE\",\"values\":[\"1\",\"2\",\"3\",\"5\"]}}}");
        RuneRankUpBalanceService service = new RuneRankUpBalanceService(balance);

        assertEquals(1, service.rule(RuneStatusConfig.STATUS_POISON, RuneProgression.NORMAL_RANK, 0).cost);
        assertEquals(2, service.rule(RuneStatusConfig.STATUS_POISON, RuneProgression.NORMAL_RANK, 1).cost);
        assertEquals(3, service.rule(RuneStatusConfig.STATUS_POISON, RuneProgression.NORMAL_RANK, 2).cost);
        assertEquals(5, service.rule(RuneStatusConfig.STATUS_POISON, RuneProgression.NORMAL_RANK, 3).cost);
        assertEquals(ResourceDefinitions.RUNE_SHARDS,
                service.rule(RuneStatusConfig.STATUS_POISON, RuneProgression.NORMAL_RANK, 3).resource);
    }

    @Test
    void maximumRankAndInvalidIndexAreRejected() {
        RuneRankUpBalanceService service = new RuneRankUpBalanceService(BalanceBootstrap.activateBundledDefaults());

        assertThrows(IllegalArgumentException.class,
                () -> service.rule(RuneStatusConfig.STATUS_POISON, RuneProgression.ADVANCED_RANK, 0));
        assertThrows(IllegalArgumentException.class,
                () -> service.rule(RuneStatusConfig.STATUS_POISON, RuneProgression.NORMAL_RANK, -1));
        assertThrows(IllegalArgumentException.class,
                () -> service.rule(RuneStatusConfig.STATUS_POISON, RuneProgression.NORMAL_RANK, 26));
    }
}
