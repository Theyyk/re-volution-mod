package com.example.revolution.rune.service;

import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.definition.RuneLayoutDefinition;
import com.example.revolution.rune.definition.RuneRankUpRule;
import com.example.revolution.rune.rules.RuneProgression;
import java.math.BigDecimal;

/** Rune-domain adapter over Balance Core for forward rank-up rules. */
public final class RuneRankUpBalanceService {
    private final BalanceService balance;

    public RuneRankUpBalanceService(BalanceService balance) {
        if (balance == null) throw new IllegalArgumentException("balance service is required");
        this.balance = balance;
    }

    /**
     * Resolves the rank-up rule at the factual nested progression point.
     * higherRuneCount is the number of slots already above sourceRank before this rank-up.
     */
    public RuneRankUpRule rule(int statusId, int sourceRank, int higherRuneCount) {
        if (!RuneStatusConfig.isRuneStatus(statusId)) throw new IllegalArgumentException("status id must be positive");
        if (higherRuneCount < 0 || higherRuneCount >= RuneLayoutDefinition.SLOTS_PER_COLLECTION) {
            throw new IllegalArgumentException("higherRuneCount must be within 0..25");
        }
        int normalizedSource = RuneProgression.clampRank(sourceRank);
        if (RuneProgression.isMaximumRank(normalizedSource)) {
            throw new IllegalArgumentException("maximum rank has no forward transition");
        }

        String prefix = "runes." + RuneStatusConfig.statusKey(statusId) + ".rankup.rank." + normalizedSource + ".";
        int targetRank = positiveIntValue(prefix + "target", "target rank");
        int requiredSlots = positiveIntValue(prefix + "required_slots", "required slots");
        String resource = canonicalResource(balance.getActiveValue(prefix + "resource"));
        int cost = positiveInt(balance.evaluateActiveCurve(prefix + "cost", higherRuneCount), "rank-up cost");
        RuneRankUpRule.ClearMode clearMode = clearMode(balance.getActiveValue(prefix + "clear_mode"));
        boolean preserveEnhancements = booleanValue(prefix + "preserve_enhancements");

        if (targetRank <= normalizedSource || targetRank > RuneLayoutDefinition.MAX_GAMEPLAY_RANK) {
            throw new IllegalStateException("rank-up target must be above source and within the supported gameplay rank range");
        }
        if (requiredSlots > RuneLayoutDefinition.SLOTS_PER_COLLECTION) {
            throw new IllegalStateException("required slots exceed rune layout size: " + requiredSlots);
        }
        if (!preserveEnhancements) {
            throw new IllegalStateException("rank-up requires preserve_enhancements=true");
        }

        return new RuneRankUpRule(
                normalizedSource,
                targetRank,
                requiredSlots,
                resource,
                cost,
                clearMode,
                true);
    }

    /** Backward-compatible first-rank-up lookup. */
    public RuneRankUpRule rule(int statusId, int sourceRank) {
        return rule(statusId, sourceRank, 0);
    }

    private int positiveIntValue(String key, String label) {
        String value = balance.getActiveValue(key);
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) throw new NumberFormatException("not positive");
            return parsed;
        } catch (RuntimeException e) {
            throw new IllegalStateException(label + " must be a positive integer: " + value, e);
        }
    }

    private boolean booleanValue(String key) {
        String value = balance.getActiveValue(key);
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new IllegalStateException(key + " must be true or false: " + value);
    }

    private String canonicalResource(String configured) {
        String canonical = ResourceDefinitions.findCanonicalName(configured);
        if (canonical == null || canonical.trim().isEmpty()) {
            throw new IllegalStateException("rank-up resource is not registered: " + configured);
        }
        return canonical;
    }

    private RuneRankUpRule.ClearMode clearMode(String configured) {
        try {
            return RuneRankUpRule.ClearMode.valueOf(configured.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (RuntimeException e) {
            throw new IllegalStateException("unsupported rank-up clear mode: " + configured, e);
        }
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
