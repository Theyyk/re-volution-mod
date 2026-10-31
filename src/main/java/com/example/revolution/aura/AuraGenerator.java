package com.example.revolution.aura;

import com.example.revolution.effect.EffectResolver;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** Server-side generator. Seed plus active Balance snapshot deterministically reproduce rarity and raw rolls. */
public final class AuraGenerator {
    private final AuraPolicy policy;
    public AuraGenerator(AuraPolicy policy) { if (policy == null) throw new IllegalArgumentException("policy is required"); this.policy = policy; }

    public AuraModel.AuraInstance generateFromChest(long seed, long nowMillis) {
        Random random = new Random(seed);
        AuraModel.Rarity rarity = weightedRarity(random);
        return generate(rarity, AuraModel.Origin.CHEST, seed, nowMillis);
    }

    public AuraModel.AuraInstance generate(AuraModel.Rarity rarity, AuraModel.Origin origin, long seed, long nowMillis) {
        if (rarity == null || origin == null || !policy.rarityEnabled(rarity)) throw new IllegalArgumentException("enabled rarity is required");
        Random random = new Random(seed ^ (rarity.ordinal() * 0x9E3779B97F4A7C15L));
        int min = policy.minRolls(rarity), max = policy.maxRolls(rarity);
        if (min <= 0 || max < min) throw new IllegalStateException("invalid Aura roll bounds for " + rarity);
        int count = min + (max == min ? 0 : random.nextInt(max - min + 1));
        List<AuraModel.Roll> rolls = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            EffectResolver.Stat stat = weightedEffect(rarity, random);
            BigDecimal low = policy.minEffect(rarity, stat), high = policy.maxEffect(rarity, stat);
            if (low.signum() < 0 || high.compareTo(low) < 0) throw new IllegalStateException("invalid Aura effect range");
            BigDecimal value = low.add(high.subtract(low).multiply(BigDecimal.valueOf(random.nextDouble()))).setScale(4, RoundingMode.HALF_UP);
            rolls.add(new AuraModel.Roll(stat, value));
        }
        return new AuraModel.AuraInstance(new UUID(random.nextLong(), random.nextLong()), rarity, origin, seed, rolls, false, nowMillis);
    }

    private AuraModel.Rarity weightedRarity(Random random) {
        BigDecimal total = BigDecimal.ZERO;
        for (AuraModel.Rarity rarity : AuraModel.Rarity.values()) if (policy.rarityEnabled(rarity)) total = total.add(policy.dropWeight(rarity).max(BigDecimal.ZERO));
        if (total.signum() <= 0) throw new IllegalStateException("Aura rarity pool is empty");
        BigDecimal needle = total.multiply(BigDecimal.valueOf(random.nextDouble()));
        BigDecimal cursor = BigDecimal.ZERO;
        for (AuraModel.Rarity rarity : AuraModel.Rarity.values()) {
            if (!policy.rarityEnabled(rarity)) continue;
            cursor = cursor.add(policy.dropWeight(rarity).max(BigDecimal.ZERO));
            if (needle.compareTo(cursor) < 0) return rarity;
        }
        return AuraModel.Rarity.MYTHIC;
    }

    private EffectResolver.Stat weightedEffect(AuraModel.Rarity rarity, Random random) {
        BigDecimal total = BigDecimal.ZERO;
        for (EffectResolver.Stat stat : BalanceAuraPolicy.SUPPORTED_EFFECTS) if (policy.effectEnabled(rarity, stat)) total = total.add(policy.effectWeight(rarity, stat).max(BigDecimal.ZERO));
        if (total.signum() <= 0) throw new IllegalStateException("Aura effect pool is empty for " + rarity);
        BigDecimal needle = total.multiply(BigDecimal.valueOf(random.nextDouble()));
        BigDecimal cursor = BigDecimal.ZERO;
        EffectResolver.Stat fallback = null;
        for (EffectResolver.Stat stat : BalanceAuraPolicy.SUPPORTED_EFFECTS) {
            if (!policy.effectEnabled(rarity, stat)) continue;
            fallback = stat; cursor = cursor.add(policy.effectWeight(rarity, stat).max(BigDecimal.ZERO));
            if (needle.compareTo(cursor) < 0) return stat;
        }
        if (fallback == null) throw new IllegalStateException("Aura effect pool is empty");
        return fallback;
    }
}
