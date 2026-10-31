package com.example.revolution.achievement;

import java.util.UUID;

/**
 * Optional production boundary for one-document claim commits. Implementations atomically persist
 * the claimed stage, Achievement Points and all configured stage rewards, or persist nothing.
 */
public interface AchievementAtomicClaimStore {
    boolean commit(UUID playerUuid, String achievementId, int expectedPriorClaimedStage,
                   AchievementStage stage, AchievementPlayerState nextState);
}
