package com.example.revolution.prestige;

/** Data-driven Mastery tier policy boundary. Core reset logic must not derive tier values itself. */
public interface MasteryTierPolicy {
    TierDefinition tier(int masteryTier);

    /** Compatibility policy for isolated tests and legacy constructors. Production wires Balance Core. */
    static MasteryTierPolicy compatibility() {
        return masteryTier -> new TierDefinition(Math.max(0, masteryTier), 5, 1, 0);
    }

    final class TierDefinition {
        public final int tier;
        public final int rankMetaLevelCap;
        public final int metaLevelGainPerRun;
        public final int premiumPreserveCrystalCost;

        public TierDefinition(int tier, int rankMetaLevelCap, int metaLevelGainPerRun, int premiumPreserveCrystalCost) {
            if (tier < 0) throw new IllegalArgumentException("tier must be >= 0");
            if (rankMetaLevelCap < 0) throw new IllegalArgumentException("rank meta-level cap must be >= 0");
            if (metaLevelGainPerRun <= 0) throw new IllegalArgumentException("meta-level gain per run must be > 0");
            if (premiumPreserveCrystalCost < 0) throw new IllegalArgumentException("premium preserve Crystal cost must be >= 0");
            this.tier = tier;
            this.rankMetaLevelCap = rankMetaLevelCap;
            this.metaLevelGainPerRun = metaLevelGainPerRun;
            this.premiumPreserveCrystalCost = premiumPreserveCrystalCost;
        }
    }
}
