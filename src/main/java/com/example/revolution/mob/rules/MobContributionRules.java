package com.example.revolution.mob.rules;

import com.example.revolution.number.LargeNumbers;
import java.math.BigDecimal;
import java.math.MathContext;

/** Pure contribution/reward math for managed mobs. */
public final class MobContributionRules {
    private static final MathContext REWARD_CONTEXT = MathContext.DECIMAL128;

    private MobContributionRules() {}

    public static BigDecimal effectiveDamage(BigDecimal currentHp, double requestedDamage) {
        if (currentHp == null || currentHp.signum() <= 0) return BigDecimal.ZERO;
        if (!Double.isFinite(requestedDamage) || requestedDamage <= 0.0D) return BigDecimal.ZERO;
        BigDecimal requested = BigDecimal.valueOf(requestedDamage);
        return LargeNumbers.normalize(requested.min(currentHp));
    }

    public static BigDecimal restoreHealth(BigDecimal currentHp, BigDecimal maxHp, BigDecimal returnedContribution) {
        BigDecimal max = maxHp == null || maxHp.signum() <= 0 ? BigDecimal.ONE : maxHp;
        BigDecimal current = currentHp == null || currentHp.signum() < 0 ? BigDecimal.ZERO : currentHp;
        if (returnedContribution == null || returnedContribution.signum() <= 0) return LargeNumbers.normalize(current.min(max));
        return LargeNumbers.normalize(current.add(returnedContribution).min(max));
    }

    public static BigDecimal proportionalReward(
            BigDecimal configuredReward,
            BigDecimal contribution,
            BigDecimal totalContribution) {
        if (configuredReward == null || configuredReward.signum() <= 0) return BigDecimal.ZERO;
        if (contribution == null || contribution.signum() <= 0) return BigDecimal.ZERO;
        if (totalContribution == null || totalContribution.signum() <= 0) return BigDecimal.ZERO;
        BigDecimal cappedContribution = contribution.min(totalContribution);
        return LargeNumbers.normalize(configuredReward.multiply(cappedContribution, REWARD_CONTEXT)
                .divide(totalContribution, REWARD_CONTEXT));
    }
}
