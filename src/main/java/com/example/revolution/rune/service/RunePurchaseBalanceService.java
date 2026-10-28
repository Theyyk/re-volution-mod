package com.example.revolution.rune.service;

import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.rune.config.RuneStatusConfig;
import java.math.BigDecimal;

/** Rune-domain adapter over the generic Balance Core for purchase and duplicate economics. */
public final class RunePurchaseBalanceService {
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

    /** Price of one attempt at the current collection progression point. */
    public int purchaseCost(int statusId, int rank, int openedSlots) {
        return positiveInt(requiredCurve(statusId, "purchase", rank, openedSlots), "purchase cost");
    }

    /** Reward for one duplicate at the current collection progression point. */
    public int duplicateReward(int statusId, int rank, int openedSlots) {
        return nonNegativeInt(requiredCurve(statusId, "duplicate", rank, openedSlots), "duplicate reward");
    }

    private BigDecimal requiredCurve(int statusId, String action, int rank, int openedSlots) {
        if (!RuneStatusConfig.isRuneStatus(statusId)) throw new IllegalArgumentException("status id must be positive");
        if (rank <= 0) throw new IllegalArgumentException("rank must be positive");
        if (openedSlots < 0) throw new IllegalArgumentException("openedSlots must be >= 0");
        return balance.evaluateActiveCurve(curveId(statusId, action, rank), openedSlots);
    }

    private String requiredValue(int statusId, String suffix) {
        if (!RuneStatusConfig.isRuneStatus(statusId)) throw new IllegalArgumentException("status id must be positive");
        return balance.getActiveValue("runes." + RuneStatusConfig.statusKey(statusId) + "." + suffix);
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

    private int positiveInt(BigDecimal value, String label) {
        int amount = nonNegativeInt(value, label);
        if (amount <= 0) throw new IllegalStateException(label + " must be > 0");
        return amount;
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
