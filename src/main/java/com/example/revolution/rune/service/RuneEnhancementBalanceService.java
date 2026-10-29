package com.example.revolution.rune.service;

import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.definition.RuneEnhancementTargeting;
import java.math.BigDecimal;
import java.util.Locale;

/** Rune-domain adapter over Balance Core for enhancement economics. */
public final class RuneEnhancementBalanceService {
    private final BalanceService balance;

    public RuneEnhancementBalanceService(BalanceService balance) {
        if (balance == null) throw new IllegalArgumentException("balance service is required");
        this.balance = balance;
    }

    public String resource(int statusId) {
        String configured = requiredValue(statusId, "enhancement.resource");
        String canonical = ResourceDefinitions.findCanonicalName(configured);
        if (canonical == null || canonical.trim().isEmpty()) {
            throw new IllegalStateException("enhancement resource is not registered: " + configured);
        }
        return canonical;
    }

    public RuneEnhancementTargeting targeting(int statusId) {
        String configured = requiredValue(statusId, "enhancement.targeting");
        try {
            return RuneEnhancementTargeting.valueOf(configured.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("unsupported enhancement targeting: " + configured, e);
        }
    }

    public int maxLevel(int statusId) {
        String raw = requiredValue(statusId, "enhancement.max_level");
        try {
            int value = Integer.parseInt(raw.trim());
            if (value <= 0) throw new NumberFormatException("not positive");
            return value;
        } catch (RuntimeException e) {
            throw new IllegalStateException("enhancement.max_level must be a positive integer: " + raw, e);
        }
    }

    /** Cost of one global enhancement step at the current total collection enhancement level. */
    public int nextCost(int statusId, int totalEnhancementLevel) {
        if (totalEnhancementLevel < 0) throw new IllegalArgumentException("total enhancement level must be >= 0");
        if (totalEnhancementLevel >= maxLevel(statusId)) {
            throw new IllegalStateException("maximum enhancement level reached");
        }
        BigDecimal value = balance.evaluateActiveCurve(curveId(statusId), totalEnhancementLevel);
        return positiveInt(value, "enhancement cost");
    }

    /** Sequential bulk cost from the collection-wide total enhancement level. */
    public int bulkCost(int statusId, int totalEnhancementLevel, int amount) {
        if (amount <= 0) throw new IllegalArgumentException("enhancement amount must be positive");
        if (totalEnhancementLevel < 0) throw new IllegalArgumentException("total enhancement level must be >= 0");
        long target = (long) totalEnhancementLevel + amount;
        if (target > maxLevel(statusId)) {
            throw new IllegalStateException("enhancement request exceeds maximum level");
        }

        long total = 0L;
        for (int i = 0; i < amount; i++) {
            long index = (long) totalEnhancementLevel + i;
            if (index > Integer.MAX_VALUE) {
                throw new IllegalStateException("total enhancement level exceeds supported curve index");
            }
            total += nextCost(statusId, (int) index);
            if (total > Integer.MAX_VALUE) {
                throw new IllegalStateException("enhancement cost exceeds ResourceService range");
            }
        }
        return (int) total;
    }

    /**
     * Packet/UI pricing. A zero entry means that bulk amount would cross the configured maximum
     * and therefore must not be submitted. Valid costs are always strictly positive.
     */
    public int[] bulkCosts(int statusId, int totalEnhancementLevel, int[] amounts) {
        if (amounts == null) throw new IllegalArgumentException("bulk amounts are required");
        int[] result = new int[amounts.length];
        int maximum = maxLevel(statusId);
        for (int i = 0; i < amounts.length; i++) {
            int amount = amounts[i];
            if (amount <= 0) throw new IllegalArgumentException("enhancement amount must be positive");
            if ((long) totalEnhancementLevel + amount > maximum) {
                result[i] = 0;
            } else {
                result[i] = bulkCost(statusId, totalEnhancementLevel, amount);
            }
        }
        return result;
    }

    /** Sum of all persisted slot levels; this is the only progression index used for enhancement price. */
    public int totalEnhancementLevel(int[] levels) {
        if (levels == null) return 0;
        long total = 0L;
        for (int level : levels) {
            total += Math.max(0, level);
            if (total > Integer.MAX_VALUE) {
                throw new IllegalStateException("total enhancement level exceeds supported curve index");
            }
        }
        return (int) total;
    }

    private String requiredValue(int statusId, String suffix) {
        if (!RuneStatusConfig.isRuneStatus(statusId)) throw new IllegalArgumentException("status id must be positive");
        return balance.getActiveValue("runes." + RuneStatusConfig.statusKey(statusId) + "." + suffix);
    }

    private String curveId(int statusId) {
        if (!RuneStatusConfig.isRuneStatus(statusId)) throw new IllegalArgumentException("status id must be positive");
        return "runes." + RuneStatusConfig.statusKey(statusId) + ".enhancement.cost";
    }

    private int positiveInt(BigDecimal value, String label) {
        if (value == null || value.signum() <= 0) throw new IllegalStateException(label + " must be > 0");
        try {
            return value.stripTrailingZeros().intValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalStateException(label + " must be an integer within ResourceService range: " + value, e);
        }
    }
}
