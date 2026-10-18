package com.example.customguimod;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure mob-reward policy for rune reward stats.
 * Only custom-mob drops use this policy; compensation/admin grants do not.
 */
final class MobRewardBonus {
    private MobRewardBonus() {}

    static boolean isRuneResource(String resource) {
        String normalized = ResourceManager.normalize(resource);
        return ResourceManager.RUNE_DUST.equals(normalized)
                || ResourceManager.RUNE_SHARDS.equals(normalized)
                || ResourceManager.RUNE_CORES.equals(normalized);
    }

    static RuneStatType statTypeFor(String resource) {
        String normalized = ResourceManager.normalize(resource);
        if ("coins".equals(normalized)) return RuneStatType.COIN_REWARD_PERCENT;
        if (isRuneResource(normalized)) return RuneStatType.RESOURCE_REWARD_PERCENT;
        return null;
    }

    static BigDecimal apply(BigDecimal baseAmount, double bonusPercent) {
        if (baseAmount == null || baseAmount.signum() <= 0) return BigDecimal.ZERO;
        if (!Double.isFinite(bonusPercent) || bonusPercent <= 0.0D) return LargeNumberValue.normalize(baseAmount);
        BigDecimal multiplier = BigDecimal.ONE.add(BigDecimal.valueOf(bonusPercent).movePointLeft(2));
        BigDecimal boosted = baseAmount.multiply(multiplier).setScale(0, RoundingMode.FLOOR);
        return LargeNumberValue.normalize(boosted.max(baseAmount));
    }

    static int apply(int baseAmount, double bonusPercent) {
        BigDecimal value = apply(BigDecimal.valueOf(baseAmount), bonusPercent);
        if (value.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) >= 0) return Integer.MAX_VALUE;
        return value.intValue();
    }
}
