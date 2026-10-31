package com.example.revolution.prestige;

/** Data-driven Mastery tier policy boundary. Core reset logic must not derive tier values itself. */
public interface MasteryTierPolicy {
    TierDefinition tier(int masteryTier);

    /** Compatibility policy for isolated tests and legacy constructors. Production wires Balance Core. */
    static MasteryTierPolicy compatibility() {
        return masteryTier -> new TierDefinition(Math.max(0, masteryTier), 5, 1,
                new PremiumPreservePolicy(false, 0, false, false));
    }

    final class PremiumPreservePolicy {
        public final boolean enabled;
        public final int crystalCost;
        public final boolean preserveRunes;
        public final boolean preservePets;

        public PremiumPreservePolicy(boolean enabled, int crystalCost, boolean preserveRunes, boolean preservePets) {
            if (crystalCost < 0) throw new IllegalArgumentException("premium preserve Crystal cost must be >= 0");
            this.enabled = enabled;
            this.crystalCost = crystalCost;
            this.preserveRunes = preserveRunes;
            this.preservePets = preservePets;
        }
    }

    final class TierDefinition {
        public final int tier;
        public final int rankMetaLevelCap;
        public final int metaLevelGainPerRun;
        public final PremiumPreservePolicy premiumPreserve;

        public TierDefinition(int tier, int rankMetaLevelCap, int metaLevelGainPerRun, PremiumPreservePolicy premiumPreserve) {
            if (tier < 0) throw new IllegalArgumentException("tier must be >= 0");
            if (rankMetaLevelCap < 0) throw new IllegalArgumentException("rank meta-level cap must be >= 0");
            if (metaLevelGainPerRun <= 0) throw new IllegalArgumentException("meta-level gain per run must be > 0");
            if (premiumPreserve == null) throw new IllegalArgumentException("premium preserve policy is required");
            this.tier = tier;
            this.rankMetaLevelCap = rankMetaLevelCap;
            this.metaLevelGainPerRun = metaLevelGainPerRun;
            this.premiumPreserve = premiumPreserve;
        }

        /** Compatibility accessor for existing preview/UI code. */
        public int premiumPreserveCrystalCost() { return premiumPreserve.crystalCost; }
    }
}
