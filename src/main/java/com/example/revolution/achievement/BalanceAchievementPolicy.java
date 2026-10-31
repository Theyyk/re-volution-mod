package com.example.revolution.achievement;

import com.example.revolution.balance.service.BalanceService;
import java.math.BigDecimal;

/** Achievement meta progression is derived from points and Balance Core curves, never stored as a second source of truth. */
public final class BalanceAchievementPolicy {
    private final BalanceService balance;

    public BalanceAchievementPolicy(BalanceService balance) {
        if (balance == null) throw new IllegalArgumentException("balance is required");
        this.balance = balance;
    }

    public int level(long achievementPoints) {
        BigDecimal points = BigDecimal.valueOf(Math.max(0L, achievementPoints));
        int level = 1;
        while (level < Integer.MAX_VALUE) {
            BigDecimal threshold = balance.evaluateActiveCurve("achievement.level.points_threshold", level - 1);
            if (threshold.signum() < 0) throw new IllegalStateException("Achievement level threshold must be non-negative");
            if (points.compareTo(threshold) < 0) break;
            level++;
        }
        return level;
    }

    public BigDecimal bonusPercent(long achievementPoints) {
        int level = level(achievementPoints);
        BigDecimal percent = balance.evaluateActiveCurve("achievement.level.economy_bonus_percent", level - 1);
        if (percent.signum() < 0) throw new IllegalStateException("Achievement economy bonus must be non-negative");
        return percent;
    }

    public BigDecimal rewardMultiplier(long achievementPoints) {
        return BigDecimal.ONE.add(bonusPercent(achievementPoints).movePointLeft(2));
    }

    public BigDecimal nextLevelThreshold(long achievementPoints) {
        int level = level(achievementPoints);
        return balance.evaluateActiveCurve("achievement.level.points_threshold", level - 1);
    }
}
