package com.example.revolution.rune.service;

import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.definition.RuneEnhancementTargeting;
import com.example.revolution.rune.state.RuneInventory;
import java.math.BigDecimal;
import java.util.Random;

/** Pure planning layer: pricing uses global enhancement progress; RNG only chooses eligible targets. */
public final class RuneEnhancementPlanningService {
    private final RuneEnhancementBalanceService balance;

    public RuneEnhancementPlanningService(RuneEnhancementBalanceService balance) {
        if (balance == null) throw new IllegalArgumentException("enhancement balance is required");
        this.balance = balance;
    }

    public RuneEnhancementPlan plan(
            int statusId,
            int[] currentLevels,
            boolean[] ownedSlots,
            int requestedAmount,
            Random random) {
        return planResolved(
                balance.targeting(statusId),
                balance.resource(statusId),
                currentLevels,
                ownedSlots,
                requestedAmount,
                random,
                total -> balance.bulkCost(statusId, total, requestedAmount));
    }

    public RuneEnhancementPlan plan(
            RuneCollectionId collectionId,
            int[] currentLevels,
            boolean[] ownedSlots,
            int requestedAmount,
            Random random) {
        return planResolved(
                balance.targeting(collectionId),
                balance.resource(collectionId),
                currentLevels,
                ownedSlots,
                requestedAmount,
                random,
                total -> balance.bulkCost(collectionId, total, requestedAmount));
    }

    private RuneEnhancementPlan planResolved(
            RuneEnhancementTargeting targeting,
            String resource,
            int[] currentLevels,
            boolean[] ownedSlots,
            int requestedAmount,
            Random random,
            CostResolver costResolver) {
        if (requestedAmount <= 0) throw new IllegalArgumentException("requested amount must be positive");
        if (random == null) throw new IllegalArgumentException("random is required");

        int[] levels = normalizedLevels(currentLevels);
        int totalEnhancementLevel = balance.totalEnhancementLevel(levels);
        BigDecimal totalCost = costResolver.resolve(totalEnhancementLevel);

        int[] increments = new int[RuneInventory.BOOST_TYPE_COUNT];
        for (int attempt = 0; attempt < requestedAmount; attempt++) {
            int candidateCount = 0;
            int[] candidates = new int[RuneInventory.BOOST_TYPE_COUNT];
            for (int slot = 0; slot < RuneInventory.BOOST_TYPE_COUNT; slot++) {
                if (!isEligible(targeting, ownedSlots, slot)) continue;
                long plannedLevel = (long) levels[slot] + increments[slot];
                if (plannedLevel >= Integer.MAX_VALUE) continue;
                candidates[candidateCount++] = slot;
            }
            if (candidateCount <= 0) throw new IllegalStateException("no eligible enhancement targets remain");
            int selected = candidates[random.nextInt(candidateCount)];
            increments[selected]++;
        }

        return new RuneEnhancementPlan(resource, totalEnhancementLevel, totalCost, requestedAmount, targeting, increments);
    }

    private int[] normalizedLevels(int[] currentLevels) {
        int[] levels = new int[RuneInventory.BOOST_TYPE_COUNT];
        if (currentLevels == null) return levels;
        int limit = Math.min(levels.length, currentLevels.length);
        for (int i = 0; i < limit; i++) levels[i] = Math.max(0, currentLevels[i]);
        return levels;
    }

    private boolean isEligible(RuneEnhancementTargeting targeting, boolean[] ownedSlots, int slot) {
        if (targeting == RuneEnhancementTargeting.ALL_SLOTS) return true;
        return ownedSlots != null && slot < ownedSlots.length && ownedSlots[slot];
    }

    private interface CostResolver {
        BigDecimal resolve(int totalEnhancementLevel);
    }
}
