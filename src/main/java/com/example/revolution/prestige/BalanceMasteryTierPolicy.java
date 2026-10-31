package com.example.revolution.prestige;

import com.example.revolution.balance.service.BalanceService;
import java.math.BigDecimal;

/** Balance Core backed Mastery tier policy. */
public final class BalanceMasteryTierPolicy implements MasteryTierPolicy {
    public static final String CAP_CURVE = "mastery.rank_meta_level_cap";
    public static final String GAIN_CURVE = "mastery.meta_level_gain_per_run";
    public static final String PREMIUM_COST_CURVE = "mastery.premium_preserve_crystal_cost";
    public static final String PREMIUM_ENABLED = "mastery.premium_preserve.enabled";
    public static final String PREMIUM_PRESERVE_RUNES = "mastery.premium_preserve.preserve_runes";
    public static final String PREMIUM_PRESERVE_PETS = "mastery.premium_preserve.preserve_pets";

    private final BalanceService balance;

    public BalanceMasteryTierPolicy(BalanceService balance) {
        if (balance == null) throw new IllegalArgumentException("balance service is required");
        this.balance = balance;
    }

    @Override public TierDefinition tier(int masteryTier) {
        if (masteryTier < 0) throw new IllegalArgumentException("mastery tier must be >= 0");
        MasteryTierPolicy.PremiumPreservePolicy premium = new MasteryTierPolicy.PremiumPreservePolicy(
                booleanValue(PREMIUM_ENABLED),
                exactNonNegativeInt(balance.evaluateActiveCurve(PREMIUM_COST_CURVE, masteryTier), PREMIUM_COST_CURVE),
                booleanValue(PREMIUM_PRESERVE_RUNES),
                booleanValue(PREMIUM_PRESERVE_PETS));
        return new TierDefinition(
                masteryTier,
                exactNonNegativeInt(balance.evaluateActiveCurve(CAP_CURVE, masteryTier), CAP_CURVE),
                exactPositiveInt(balance.evaluateActiveCurve(GAIN_CURVE, masteryTier), GAIN_CURVE),
                premium);
    }

    private boolean booleanValue(String id) {
        String raw = balance.getActiveValue(id);
        if (raw == null) throw new IllegalArgumentException(id + " is required");
        if ("true".equalsIgnoreCase(raw.trim())) return true;
        if ("false".equalsIgnoreCase(raw.trim())) return false;
        throw new IllegalArgumentException(id + " must be true or false");
    }

    private static int exactPositiveInt(BigDecimal value, String id) {
        int result = exactNonNegativeInt(value, id);
        if (result <= 0) throw new IllegalArgumentException(id + " must resolve to > 0");
        return result;
    }

    private static int exactNonNegativeInt(BigDecimal value, String id) {
        if (value == null || value.signum() < 0) throw new IllegalArgumentException(id + " must resolve to >= 0");
        try { return value.intValueExact(); }
        catch (ArithmeticException e) { throw new IllegalArgumentException(id + " must resolve to an exact int", e); }
    }
}
