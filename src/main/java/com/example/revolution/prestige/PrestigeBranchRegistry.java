package com.example.revolution.prestige;

import com.example.revolution.effect.EffectResolver;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Data-driven boundary for Prestige Foundation. Numeric values are technical ZBT placeholders,
 * deliberately centralized here so Balance Core can replace them without changing the engine.
 */
public final class PrestigeBranchRegistry {
    private final Map<PrestigeBranch, BranchDefinition> definitions = new EnumMap<>(PrestigeBranch.class);

    public PrestigeBranchRegistry() {
        register(click());
        register(placeholder(PrestigeBranch.POISON));
        register(placeholder(PrestigeBranch.COINS));
        register(placeholder(PrestigeBranch.RUNE_RESOURCES));
        register(placeholder(PrestigeBranch.ALL_DAMAGE));
    }

    public BranchDefinition get(PrestigeBranch branch) {
        BranchDefinition definition = definitions.get(branch);
        if (definition == null) throw new IllegalArgumentException("Unknown Prestige branch: " + branch);
        return definition;
    }

    public List<PrestigeBranch> branches() {
        return Collections.unmodifiableList(new ArrayList<>(definitions.keySet()));
    }

    private void register(BranchDefinition definition) { definitions.put(definition.branch, definition); }

    private static BranchDefinition click() {
        List<RankDefinition> ranks = new ArrayList<>();
        ranks.add(new RankDefinition("CLICK_I", new BigDecimal("100"), EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER, new BigDecimal("0.05")));
        ranks.add(new RankDefinition("CLICK_II", new BigDecimal("500"), EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER, new BigDecimal("0.10")));
        ranks.add(new RankDefinition("CLICK_III", new BigDecimal("1000"), EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER, new BigDecimal("0.15")));
        return new BranchDefinition(PrestigeBranch.CLICK, new BigDecimal("250"), BigDecimal.ONE,
                new BigDecimal("0.01"), ranks);
    }

    private static BranchDefinition placeholder(PrestigeBranch branch) {
        return new BranchDefinition(branch, new BigDecimal("250"), BigDecimal.ONE, BigDecimal.ZERO, Collections.emptyList());
    }

    public static final class BranchDefinition {
        public final PrestigeBranch branch;
        public final BigDecimal progressPerLevel;
        public final BigDecimal defaultSaveMultiplier;
        public final BigDecimal permanentBonusPerLevel;
        public final List<RankDefinition> ranks;

        private BranchDefinition(PrestigeBranch branch, BigDecimal progressPerLevel, BigDecimal defaultSaveMultiplier,
                                 BigDecimal permanentBonusPerLevel, List<RankDefinition> ranks) {
            this.branch = branch;
            this.progressPerLevel = progressPerLevel;
            this.defaultSaveMultiplier = defaultSaveMultiplier;
            this.permanentBonusPerLevel = permanentBonusPerLevel;
            this.ranks = Collections.unmodifiableList(new ArrayList<>(ranks));
        }

        public int levelFor(BigDecimal progress) {
            if (progress == null || progress.signum() <= 0) return 0;
            return progress.divideToIntegralValue(progressPerLevel).intValue();
        }
    }

    public static final class RankDefinition {
        public final String rankId;
        public final BigDecimal unlockProgress;
        public final EffectResolver.Stat effectStat;
        public final BigDecimal baseEffect;

        private RankDefinition(String rankId, BigDecimal unlockProgress, EffectResolver.Stat effectStat, BigDecimal baseEffect) {
            this.rankId = rankId;
            this.unlockProgress = unlockProgress;
            this.effectStat = effectStat;
            this.baseEffect = baseEffect;
        }

        /** Technical placeholder meta scaling; Balance Core may replace this policy later. */
        public BigDecimal effectAtMetaLevel(int metaLevel) {
            return baseEffect.multiply(BigDecimal.ONE.add(new BigDecimal("0.10").multiply(BigDecimal.valueOf(Math.max(0, metaLevel)))));
        }
    }
}
