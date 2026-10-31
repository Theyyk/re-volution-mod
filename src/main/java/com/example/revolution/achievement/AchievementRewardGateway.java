package com.example.revolution.achievement;

import java.util.UUID;

/**
 * Canonical reward boundary. Implementations must grant the complete stage reward atomically/idempotently
 * for the supplied claim key, or return false without a partial grant.
 */
public interface AchievementRewardGateway {
    boolean grantStageRewards(UUID playerUuid, String claimKey, AchievementStage stage);
}
