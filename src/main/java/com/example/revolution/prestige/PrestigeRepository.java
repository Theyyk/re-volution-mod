package com.example.revolution.prestige;

import java.util.UUID;

public interface PrestigeRepository {
    PrestigeState load(UUID playerUuid);
    void save(UUID playerUuid, PrestigeState state);
    ResetCommitResult commitReset(UUID playerUuid, String expectedRunId, PrestigeState state);

    default ResetCommitResult commitReset(UUID playerUuid, String expectedRunId, PrestigeState state, ResetCommitPolicy policy) {
        if (policy == null || policy.isNormal()) return commitReset(playerUuid, expectedRunId, state);
        return ResetCommitResult.POLICY_UNAVAILABLE;
    }

    final class ResetCommitPolicy {
        public final int crystalCost;
        public final boolean preserveRunes;
        public final boolean preservePets;

        public ResetCommitPolicy(int crystalCost, boolean preserveRunes, boolean preservePets) {
            if (crystalCost < 0) throw new IllegalArgumentException("Crystal cost must be >= 0");
            this.crystalCost = crystalCost;
            this.preserveRunes = preserveRunes;
            this.preservePets = preservePets;
        }

        public static ResetCommitPolicy normal() { return new ResetCommitPolicy(0, false, false); }
        public boolean isNormal() { return crystalCost == 0 && !preserveRunes && !preservePets; }
    }

    enum ResetCommitResult { APPLIED, DUPLICATE, STALE, INSUFFICIENT_CRYSTALS, POLICY_UNAVAILABLE }
}
