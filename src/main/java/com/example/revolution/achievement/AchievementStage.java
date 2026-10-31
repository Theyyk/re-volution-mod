package com.example.revolution.achievement;

import java.math.BigDecimal;

public final class AchievementStage {
    public final int stageIndex;
    public final BigDecimal target;
    public final BigDecimal crystals;
    public final BigDecimal offerings;
    public final BigDecimal auraKeys;
    public final long achievementPoints;

    public AchievementStage(int stageIndex, BigDecimal target, BigDecimal crystals,
                            BigDecimal offerings, BigDecimal auraKeys, long achievementPoints) {
        if (stageIndex < 1) throw new IllegalArgumentException("stageIndex must be >= 1");
        if (target == null || target.signum() <= 0) throw new IllegalArgumentException("target must be positive");
        if (crystals == null || crystals.signum() < 0 || offerings == null || offerings.signum() < 0
                || auraKeys == null || auraKeys.signum() < 0 || achievementPoints < 0L) {
            throw new IllegalArgumentException("achievement rewards must be non-negative");
        }
        this.stageIndex = stageIndex;
        this.target = target;
        this.crystals = crystals;
        this.offerings = offerings;
        this.auraKeys = auraKeys;
        this.achievementPoints = achievementPoints;
    }
}
