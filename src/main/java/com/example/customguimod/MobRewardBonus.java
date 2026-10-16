package com.example.customguimod;

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

    static int apply(int baseAmount, double bonusPercent) {
        if (baseAmount <= 0) return Math.max(0, baseAmount);
        if (!Double.isFinite(bonusPercent) || bonusPercent <= 0.0D) return baseAmount;

        double boosted = baseAmount * (1.0D + bonusPercent / 100.0D);
        if (!Double.isFinite(boosted) || boosted >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return Math.max(baseAmount, (int) Math.floor(boosted + 0.0000001D));
    }
}
