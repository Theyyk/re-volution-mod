package com.example.revolution.rune.service;

import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.rune.definition.RuneCollectionId;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Rune-domain adapter over the generic Balance Core for purchase and duplicate economics. */
public final class RunePurchaseBalanceService {
    private static final BigDecimal DEFAULT_HIGHER_RANK_MULTIPLIER = BigDecimal.ONE;
    private final BalanceService balance;

    public RunePurchaseBalanceService(BalanceService balance) {
        if (balance == null) throw new IllegalArgumentException("balance service is required");
        this.balance = balance;
    }

    RuneRankRevertBalanceService revertBalanceRules() {
        return new RuneRankRevertBalanceService(balance);
    }

    public String purchaseResource(int statusId) {
        return purchaseResourceNamespace(RuneBalanceNamespace.legacyStatus(statusId));
    }

    public String purchaseResource(RuneCollectionId collectionId) {
        return purchaseResourceNamespace(RuneBalanceNamespace.collection(collectionId));
    }

    public String duplicateResource(int statusId) {
        return duplicateResourceNamespace(RuneBalanceNamespace.legacyStatus(statusId));
    }

    public String duplicateResource(RuneCollectionId collectionId) {
        return duplicateResourceNamespace(RuneBalanceNamespace.collection(collectionId));
    }

    public int purchaseCost(int statusId, int rank, int lowerProgress, int higherRuneCount) {
        return purchaseCostNamespace(RuneBalanceNamespace.legacyStatus(statusId), rank, lowerProgress, higherRuneCount);
    }

    public int purchaseCost(RuneCollectionId collectionId, int rank, int lowerProgress, int higherRuneCount) {
        return purchaseCostNamespace(RuneBalanceNamespace.collection(collectionId), rank, lowerProgress, higherRuneCount);
    }

    public int purchaseCost(int statusId, int rank, int lowerProgress) {
        return purchaseCost(statusId, rank, lowerProgress, 0);
    }

    public int purchaseCost(RuneCollectionId collectionId, int rank, int lowerProgress) {
        return purchaseCost(collectionId, rank, lowerProgress, 0);
    }

    public int duplicateReward(int statusId, int rank, int lowerProgress, int higherRuneCount) {
        return duplicateRewardNamespace(RuneBalanceNamespace.legacyStatus(statusId), rank, lowerProgress, higherRuneCount);
    }

    public int duplicateReward(RuneCollectionId collectionId, int rank, int lowerProgress, int higherRuneCount) {
        return duplicateRewardNamespace(RuneBalanceNamespace.collection(collectionId), rank, lowerProgress, higherRuneCount);
    }

    public int duplicateReward(int statusId, int rank, int lowerProgress) {
        return duplicateReward(statusId, rank, lowerProgress, 0);
    }

    public int duplicateReward(RuneCollectionId collectionId, int rank, int lowerProgress) {
        return duplicateReward(collectionId, rank, lowerProgress, 0);
    }

    private String purchaseResourceNamespace(String namespace) {
        return canonicalResource(requiredValue(namespace, "purchase.resource"), "purchase");
    }

    private String duplicateResourceNamespace(String namespace) {
        return canonicalResource(requiredValue(namespace, "duplicate.resource"), "duplicate");
    }

    private int purchaseCostNamespace(String namespace, int rank, int lowerProgress, int higherRuneCount) {
        if (higherRuneCount < 0) throw new IllegalArgumentException("higherRuneCount must be >= 0");
        BigDecimal base = requiredCurve(namespace, "purchase", rank, lowerProgress);
        BigDecimal multiplier = optionalDecimalValue(
                namespace, "purchase.higher_rank_multiplier", DEFAULT_HIGHER_RANK_MULTIPLIER);
        if (multiplier.signum() <= 0) throw new IllegalStateException("purchase higher-rank multiplier must be > 0");
        BigDecimal resolved = base.multiply(multiplier.pow(higherRuneCount));
        return positiveRoundedInt(resolved, "purchase cost");
    }

    private int duplicateRewardNamespace(String namespace, int rank, int lowerProgress, int higherRuneCount) {
        String rateRaw = optionalValue(namespace, "duplicate.rate");
        if (rateRaw == null) {
            return nonNegativeInt(requiredCurve(namespace, "duplicate", rank, lowerProgress), "duplicate reward");
        }
        BigDecimal rate = parseNonNegativeDecimal(rateRaw, "duplicate rate");
        BigDecimal purchaseCost = BigDecimal.valueOf(
                purchaseCostNamespace(namespace, rank, lowerProgress, higherRuneCount));
        int rounded = nonNegativeRoundedInt(purchaseCost.multiply(rate), "duplicate reward");
        return Math.max(1, rounded);
    }

    private BigDecimal requiredCurve(String namespace, String action, int rank, int index) {
        if (rank <= 0) throw new IllegalArgumentException("rank must be positive");
        if (index < 0) throw new IllegalArgumentException("curve index must be >= 0");
        return balance.evaluateActiveCurve(RuneBalanceNamespace.curveKey(namespace, action, rank), index);
    }

    private String requiredValue(String namespace, String suffix) {
        return balance.getActiveValue(RuneBalanceNamespace.valueKey(namespace, suffix));
    }

    private String optionalValue(String namespace, String suffix) {
        return balance.getActiveSnapshot().getConfig().getValue(
                RuneBalanceNamespace.valueKey(namespace, suffix));
    }

    private BigDecimal optionalDecimalValue(String namespace, String suffix, BigDecimal fallback) {
        String raw = optionalValue(namespace, suffix);
        return raw == null ? fallback : parseNonNegativeDecimal(raw, suffix);
    }

    private String canonicalResource(String configured, String action) {
        String canonical = ResourceDefinitions.findCanonicalName(configured);
        if (canonical == null || canonical.trim().isEmpty()) {
            throw new IllegalStateException(action + " resource is not registered: " + configured);
        }
        return canonical;
    }

    private BigDecimal parseNonNegativeDecimal(String raw, String label) {
        try {
            BigDecimal value = new BigDecimal(raw);
            if (value.signum() < 0) throw new IllegalStateException(label + " must be >= 0");
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalStateException(label + " must be a decimal: " + raw, e);
        }
    }

    private int positiveRoundedInt(BigDecimal value, String label) {
        int amount = nonNegativeRoundedInt(value, label);
        if (amount <= 0) throw new IllegalStateException(label + " must be > 0");
        return amount;
    }

    private int nonNegativeRoundedInt(BigDecimal value, String label) {
        if (value == null || value.signum() < 0) throw new IllegalStateException(label + " must be >= 0");
        try {
            return value.setScale(0, RoundingMode.HALF_UP).intValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalStateException(label + " must fit ResourceService integer range: " + value, e);
        }
    }

    private int nonNegativeInt(BigDecimal value, String label) {
        if (value == null || value.signum() < 0) throw new IllegalStateException(label + " must be >= 0");
        try {
            return value.stripTrailingZeros().intValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalStateException(label + " must be an integer within ResourceService range: " + value, e);
        }
    }
}
