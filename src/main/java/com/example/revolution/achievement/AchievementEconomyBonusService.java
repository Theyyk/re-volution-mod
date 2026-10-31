package com.example.revolution.achievement;

import com.example.revolution.resource.definition.ResourceDefinitions;
import java.math.BigDecimal;
import java.util.UUID;

/** Same permanent Achievement percentage for Coins, Dust, Shards and Cores. */
public final class AchievementEconomyBonusService {
    private final AchievementService achievements;
    private final BalanceAchievementPolicy policy;

    public AchievementEconomyBonusService(AchievementService achievements, BalanceAchievementPolicy policy) {
        if (achievements == null || policy == null) throw new IllegalArgumentException("Achievement bonus dependencies are required");
        this.achievements = achievements;
        this.policy = policy;
    }

    public BigDecimal multiplier(UUID playerUuid, String resource) {
        String normalized = ResourceDefinitions.normalize(resource);
        if (!"coins".equals(normalized)
                && !ResourceDefinitions.RUNE_DUST.equals(normalized)
                && !ResourceDefinitions.RUNE_SHARDS.equals(normalized)
                && !ResourceDefinitions.RUNE_CORES.equals(normalized)) return BigDecimal.ONE;
        return policy.rewardMultiplier(achievements.snapshot(playerUuid).achievementPoints);
    }
}
