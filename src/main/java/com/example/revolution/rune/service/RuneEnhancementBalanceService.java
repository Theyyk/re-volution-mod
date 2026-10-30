package com.example.revolution.rune.service;

import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.definition.RuneEnhancementTargeting;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Rune-domain adapter over Balance Core for enhancement economics. */
public final class RuneEnhancementBalanceService {
    private final BalanceService balance;

    public RuneEnhancementBalanceService(BalanceService balance) {
        if (balance == null) throw new IllegalArgumentException("balance service is required");
        this.balance = balance;
    }

    public String resource(int statusId) {
        return resourceNamespace(RuneBalanceNamespace.legacyStatus(statusId));
    }

    public String resource(RuneCollectionId collectionId) {
        return resourceNamespace(RuneBalanceNamespace.collection(collectionId));
    }

    public RuneEnhancementTargeting targeting(int statusId) {
        return targetingNamespace(RuneBalanceNamespace.legacyStatus(statusId));
    }

    public RuneEnhancementTargeting targeting(RuneCollectionId collectionId) {
        return targetingNamespace(RuneBalanceNamespace.collection(collectionId));
    }

    /** Cost of one global enhancement step at the current factual total collection enhancement level. */
    public BigDecimal nextCost(int statusId, int totalEnhancementLevel) {
        return nextCostNamespace(RuneBalanceNamespace.legacyStatus(statusId), totalEnhancementLevel);
    }

    public BigDecimal nextCost(RuneCollectionId collectionId, int totalEnhancementLevel) {
        return nextCostNamespace(RuneBalanceNamespace.collection(collectionId), totalEnhancementLevel);
    }

    /** Sequential bulk cost from the collection-wide total enhancement level. */
    public BigDecimal bulkCost(int statusId, int totalEnhancementLevel, int amount) {
        return bulkCostNamespace(RuneBalanceNamespace.legacyStatus(statusId), totalEnhancementLevel, amount);
    }

    public BigDecimal bulkCost(RuneCollectionId collectionId, int totalEnhancementLevel, int amount) {
        return bulkCostNamespace(RuneBalanceNamespace.collection(collectionId), totalEnhancementLevel, amount);
    }

    public BigDecimal[] bulkCosts(int statusId, int totalEnhancementLevel, int[] amounts) {
        return bulkCostsNamespace(RuneBalanceNamespace.legacyStatus(statusId), totalEnhancementLevel, amounts);
    }

    public BigDecimal[] bulkCosts(RuneCollectionId collectionId, int totalEnhancementLevel, int[] amounts) {
        return bulkCostsNamespace(RuneBalanceNamespace.collection(collectionId), totalEnhancementLevel, amounts);
    }

    /** Sum of all persisted slot levels; this is the only progression index used for enhancement price. */
    public int totalEnhancementLevel(int[] levels) {
        if (levels == null) return 0;
        long total = 0L;
        for (int level : levels) {
            total += Math.max(0, level);
            if (total > Integer.MAX_VALUE) throw new IllegalStateException("total enhancement level exceeds supported curve index");
        }
        return (int) total;
    }

    private String resourceNamespace(String namespace) {
        String configured = requiredValue(namespace, "enhancement.resource");
        String canonical = ResourceDefinitions.findCanonicalName(configured);
        if (canonical == null || canonical.trim().isEmpty()) {
            throw new IllegalStateException("enhancement resource is not registered: " + configured);
        }
        return canonical;
    }

    private RuneEnhancementTargeting targetingNamespace(String namespace) {
        String configured = requiredValue(namespace, "enhancement.targeting");
        try {
            return RuneEnhancementTargeting.valueOf(configured.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("unsupported enhancement targeting: " + configured, e);
        }
    }

    private BigDecimal nextCostNamespace(String namespace, int totalEnhancementLevel) {
        if (totalEnhancementLevel < 0) throw new IllegalArgumentException("total enhancement level must be >= 0");
        BigDecimal value = balance.evaluateActiveCurve(RuneBalanceNamespace.valueKey(namespace, "enhancement.cost"), totalEnhancementLevel);
        if (value == null || value.signum() <= 0) throw new IllegalStateException("enhancement cost must be > 0");
        return value.setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal bulkCostNamespace(String namespace, int totalEnhancementLevel, int amount) {
        if (amount <= 0) throw new IllegalArgumentException("enhancement amount must be positive");
        if (totalEnhancementLevel < 0) throw new IllegalArgumentException("total enhancement level must be >= 0");
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < amount; i++) {
            long index = (long) totalEnhancementLevel + i;
            if (index > Integer.MAX_VALUE) throw new IllegalStateException("total enhancement level exceeds supported curve index");
            total = total.add(nextCostNamespace(namespace, (int) index));
        }
        return total;
    }

    private BigDecimal[] bulkCostsNamespace(String namespace, int totalEnhancementLevel, int[] amounts) {
        if (amounts == null) throw new IllegalArgumentException("bulk amounts are required");
        BigDecimal[] result = new BigDecimal[amounts.length];
        for (int i = 0; i < amounts.length; i++) {
            if (amounts[i] <= 0) throw new IllegalArgumentException("enhancement amount must be positive");
            result[i] = bulkCostNamespace(namespace, totalEnhancementLevel, amounts[i]);
        }
        return result;
    }

    private String requiredValue(String namespace, String suffix) {
        String value = balance.getActiveSnapshot().getConfig().getValue(RuneBalanceNamespace.valueKey(namespace, suffix));
        if (value == null) throw new IllegalStateException("missing enhancement balance value: " + suffix);
        return value;
    }
}
