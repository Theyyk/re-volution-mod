package com.example.revolution.rune.service;

import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.rune.config.RuneStatusConfig;
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

    public String purchaseResource(int statusId) {
        return canonicalResource(requiredValue(statusId, "purchase.resource"), "purchase");
    }

    public String duplicateResource(int statusId) {
        return canonicalResource(requiredValue(statusId, "duplicate.resource"), "duplicate");
    }

    /**
     * Price of one attempt at the factual nested progression point.
     * Base curve is indexed only by lower-rank progress, then pressure is applied for every rune
     * already above the current purchase rank. Fractional prices round HALF_UP to ResourceService integers.
     */
    public int purchaseCost(int statusId, int rank, int lowerProgress, int higherRuneCount) {
        if (higherRuneCount < 0) throw new IllegalArgumentException("higherRuneCount must be >= 0");
        BigDecimal base = requiredCurve(statusId, "purchase", rank, lowerProgress);
        BigDecimal multiplier = optionalDecimalValue(
                statusId, "purchase.higher_rank_multiplier", DEFAULT_HIGHER_RANK_MULTIPLIER);
        if (multiplier.signum() <= 0) throw new IllegalStateException("purchase higher-rank multiplier must be > 0");
        BigDecimal resolved = base.multiply(multiplier.pow(higherRuneCount));
        return positiveRoundedInt(resolved, "purchase cost");
    }

    /** Backward-compatible call for callers/configs without higher-rank pressure. */
    public int purchaseCost(int statusId, int rank, int lowerProgress) {
        return purchaseCost(statusId, rank, lowerProgress, 0);
    }

    /** Duplicate compensation is always derived from the exact current purchase cost when rate is configured. */
    public int duplicateReward(int statusId, int rank, int lowerProgress, int higherRuneCount) {
        String rateRaw = optionalValue(statusId, "duplicate.rate");
        if (rateRaw == null) {
            return nonNegativeInt(requiredCurve(statusId, "duplicate", rank, lowerProgress), "duplicate reward");
        }
        BigDecimal rate = parseNonNegativeDecimal(rateRaw, "duplicate rate");
        BigDecimal purchaseCost = BigDecimal.valueOf(purchaseCost(statusId, rank, lowerProgress, higherRuneCount));
        int rounded = nonNegativeRoundedInt(purchaseCost.multiply(rate), "duplicate reward");
        return Math.max(1, rounded);
    }

    /** Backward-compatible call for legacy duplicate curves. */
    public int duplicateReward(int statusId, int rank, int lowerProgress) {
        return duplicateReward(statusId, rank, lowerProgress, 0);
    }

    private BigDecimal requiredCurve(int statusId, String action, int rank, int index) {
        if (!RuneStatusConfig.isRuneStatus(statusId)) throw new IllegalArgumentException("status id must be positive");
        if (rank <= 0) throw new IllegalArgumentException("rank must be positive");
        if (index < 0) throw new IllegalArgumentException("curve index must be >= 0");
        return balance.evaluateActiveCurve(curveId(statusId, action, rank), index);
    }

    private String requiredValue(int statusId, String suffix) {
        if (!RuneStatusConfig.isRuneStatus(statusId)) throw new IllegalArgumentException("status id must be positive");
        return balance.getActiveValue("runes." + RuneStatusConfig.statusKey(statusId) + "." + suffix);
    }

    private String optionalValue(int statusId, String suffix) {
        if (!RuneStatusConfig.isRuneStatus(statusId)) throw new IllegalArgumentException("status id must be positive");
        return balance.getActiveSnapshot().getConfig().getValue(
                "runes." + RuneStatusConfig.statusKey(statusId) + "." + suffix);
    }

    private BigDecimal optionalDecimalValue(int statusId, String suffix, BigDecimal fallback) {
        String raw = optionalValue(statusId, suffix);
        return raw == null ? fallback : parseNonNegativeDecimal(raw, suffix);
    }

    private String curveId(int statusId, String action, int rank) {
        return "runes." + RuneStatusConfig.statusKey(statusId) + "." + action + ".rank." + rank;
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
