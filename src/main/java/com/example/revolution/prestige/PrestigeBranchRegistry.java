package com.example.revolution.prestige;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.resource.definition.ResourceDefinitions;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data-driven boundary for Prestige. Numeric values remain technical ZBT placeholders so Balance Core
 * can replace them without changing progression or gameplay event routing.
 */
public final class PrestigeBranchRegistry {
    private final Map<PrestigeBranch, BranchDefinition> definitions = new EnumMap<>(PrestigeBranch.class);

    public PrestigeBranchRegistry() {
        register(click());
        register(damageBranch(PrestigeBranch.POISON, GameplayDamageSource.POISON,
                EffectResolver.Stat.POISON_DAMAGE_MULTIPLIER, "POISON"));
        register(resourceBranch(PrestigeBranch.COINS, singletonWeight("coins", BigDecimal.ONE),
                EffectResolver.Stat.COIN_REWARD_MULTIPLIER, "COINS"));
        Map<String, BigDecimal> runeWeights = new LinkedHashMap<>();
        runeWeights.put(ResourceDefinitions.RUNE_DUST, BigDecimal.ONE);
        runeWeights.put(ResourceDefinitions.RUNE_SHARDS, BigDecimal.ONE);
        runeWeights.put(ResourceDefinitions.RUNE_CORES, BigDecimal.ONE);
        register(resourceBranch(PrestigeBranch.RUNE_RESOURCES, runeWeights,
                EffectResolver.Stat.RUNE_RESOURCE_REWARD_MULTIPLIER, "RUNE_RESOURCES"));
        register(allDamage());
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
        return new BranchDefinition(PrestigeBranch.CLICK, ProgressSource.damage(GameplayDamageSource.CLICK),
                new BigDecimal("250"), BigDecimal.ONE, EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER,
                new BigDecimal("0.01"), ranks);
    }

    private static BranchDefinition damageBranch(PrestigeBranch branch, GameplayDamageSource source,
                                                  EffectResolver.Stat effectStat, String rankPrefix) {
        return standardBranch(branch, ProgressSource.damage(source), effectStat, rankPrefix);
    }

    private static BranchDefinition resourceBranch(PrestigeBranch branch, Map<String, BigDecimal> weights,
                                                    EffectResolver.Stat effectStat, String rankPrefix) {
        return standardBranch(branch, ProgressSource.resources(weights), effectStat, rankPrefix);
    }

    private static BranchDefinition allDamage() {
        return standardBranch(PrestigeBranch.ALL_DAMAGE, ProgressSource.allDamage(),
                EffectResolver.Stat.ALL_DAMAGE_MULTIPLIER, "ALL_DAMAGE");
    }

    private static BranchDefinition standardBranch(PrestigeBranch branch, ProgressSource progressSource,
                                                   EffectResolver.Stat effectStat, String rankPrefix) {
        List<RankDefinition> ranks = new ArrayList<>();
        ranks.add(new RankDefinition(rankPrefix + "_I", new BigDecimal("100"), effectStat, new BigDecimal("0.05")));
        ranks.add(new RankDefinition(rankPrefix + "_II", new BigDecimal("500"), effectStat, new BigDecimal("0.10")));
        ranks.add(new RankDefinition(rankPrefix + "_III", new BigDecimal("1000"), effectStat, new BigDecimal("0.15")));
        return new BranchDefinition(branch, progressSource, new BigDecimal("250"), BigDecimal.ONE,
                effectStat, new BigDecimal("0.01"), ranks);
    }

    private static Map<String, BigDecimal> singletonWeight(String resource, BigDecimal weight) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        result.put(ResourceDefinitions.normalize(resource), weight);
        return result;
    }

    public static final class ProgressSource {
        public enum Kind { DAMAGE_SOURCE, ALL_DAMAGE, RESOURCE }

        public final Kind kind;
        public final GameplayDamageSource damageSource;
        public final Map<String, BigDecimal> resourceWeights;

        private ProgressSource(Kind kind, GameplayDamageSource damageSource, Map<String, BigDecimal> resourceWeights) {
            this.kind = kind;
            this.damageSource = damageSource;
            Map<String, BigDecimal> normalized = new LinkedHashMap<>();
            if (resourceWeights != null) {
                for (Map.Entry<String, BigDecimal> item : resourceWeights.entrySet()) {
                    String name = ResourceDefinitions.normalize(item.getKey());
                    BigDecimal weight = item.getValue();
                    if (!name.isEmpty() && weight != null && weight.signum() > 0) normalized.put(name, weight);
                }
            }
            this.resourceWeights = Collections.unmodifiableMap(normalized);
        }

        public static ProgressSource damage(GameplayDamageSource source) {
            if (source == null) throw new IllegalArgumentException("damage source is required");
            return new ProgressSource(Kind.DAMAGE_SOURCE, source, Collections.emptyMap());
        }

        public static ProgressSource allDamage() {
            return new ProgressSource(Kind.ALL_DAMAGE, null, Collections.emptyMap());
        }

        public static ProgressSource resources(Map<String, BigDecimal> weights) {
            return new ProgressSource(Kind.RESOURCE, null, weights);
        }

        public BigDecimal fromDamage(GameplayDamageSource source, BigDecimal factualAmount) {
            if (factualAmount == null || factualAmount.signum() <= 0 || source == null) return BigDecimal.ZERO;
            if (kind == Kind.ALL_DAMAGE) return factualAmount;
            if (kind == Kind.DAMAGE_SOURCE && damageSource == source) return factualAmount;
            return BigDecimal.ZERO;
        }

        public BigDecimal fromResource(String resource, BigDecimal factualAmount) {
            if (kind != Kind.RESOURCE || factualAmount == null || factualAmount.signum() <= 0) return BigDecimal.ZERO;
            BigDecimal weight = resourceWeights.get(ResourceDefinitions.normalize(resource));
            return weight == null ? BigDecimal.ZERO : factualAmount.multiply(weight);
        }

        @Override public String toString() {
            if (kind == Kind.DAMAGE_SOURCE) return "DAMAGE:" + damageSource;
            if (kind == Kind.ALL_DAMAGE) return "DAMAGE:ALL";
            return "RESOURCE:" + resourceWeights;
        }
    }

    public static final class BranchDefinition {
        public final PrestigeBranch branch;
        public final ProgressSource progressSource;
        public final BigDecimal progressPerLevel;
        public final BigDecimal defaultSaveMultiplier;
        public final EffectResolver.Stat permanentEffectStat;
        public final BigDecimal permanentBonusPerLevel;
        public final List<RankDefinition> ranks;

        private BranchDefinition(PrestigeBranch branch, ProgressSource progressSource, BigDecimal progressPerLevel,
                                 BigDecimal defaultSaveMultiplier, EffectResolver.Stat permanentEffectStat,
                                 BigDecimal permanentBonusPerLevel, List<RankDefinition> ranks) {
            this.branch = branch;
            this.progressSource = progressSource;
            this.progressPerLevel = progressPerLevel;
            this.defaultSaveMultiplier = defaultSaveMultiplier;
            this.permanentEffectStat = permanentEffectStat;
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
