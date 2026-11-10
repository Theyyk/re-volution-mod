package com.example.revolution.player.service;

import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.definition.RuneRankUpRule;
import com.example.revolution.rune.rules.RuneProgression;
import com.example.revolution.rune.service.RuneRankUpBalanceService;
import com.example.revolution.resource.definition.ResourceDefinitions;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class PlayerStatsAwakeningRuleTest {
    private final RuneRankUpBalanceService rules = new RuneRankUpBalanceService(
            BalanceBootstrap.activateBundledDefaults());

    @Test void completeNormalCohortUsesLiveRankUpCurrencyAndCost() {
        int[] ranks = new int[26];
        Arrays.fill(ranks, RuneProgression.NORMAL_RANK);
        RuneRankUpRule rule = PlayerStatsSyncService.currentAwakeningRule(
                rules, RuneCollectionId.COLLECTION_01, ranks);
        assertNotNull(rule);
        assertEquals(ResourceDefinitions.RUNE_SHARDS, rule.resource);
        assertEquals(RuneProgression.IMPROVED_RANK, rule.targetRank);
    }

    @Test void nextCohortUsesLowestRankAndFactualHigherCount() {
        int[] ranks = new int[26];
        Arrays.fill(ranks, RuneProgression.NORMAL_RANK);
        ranks[5] = RuneProgression.IMPROVED_RANK;
        RuneRankUpRule rule = PlayerStatsSyncService.currentAwakeningRule(
                rules, RuneCollectionId.COLLECTION_01, ranks);
        assertNotNull(rule);
        assertEquals(RuneProgression.NORMAL_RANK, rule.sourceRank);
        assertEquals(rules.rule(RuneCollectionId.COLLECTION_01, RuneProgression.NORMAL_RANK, 1).cost, rule.cost);
        Arrays.fill(ranks, RuneProgression.IMPROVED_RANK);
        assertEquals(RuneProgression.ADVANCED_RANK, PlayerStatsSyncService.currentAwakeningRule(
                rules, RuneCollectionId.COLLECTION_01, ranks).targetRank);
    }

    @Test void incompleteAndMaxRankDoNotAdvertiseAwakening() {
        int[] ranks = new int[26];
        ranks[0] = RuneProgression.NORMAL_RANK;
        assertNull(PlayerStatsSyncService.currentAwakeningRule(
                rules, RuneCollectionId.COLLECTION_01, ranks));
        Arrays.fill(ranks, RuneProgression.MYTHIC_RANK);
        assertNull(PlayerStatsSyncService.currentAwakeningRule(
                rules, RuneCollectionId.COLLECTION_01, ranks));
    }
}
