package com.example.revolution.aura;

import com.example.revolution.effect.EffectResolver;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Permanent Aura domain state. Aura instances never level and are replaced only by explicit craft/dismantle. */
public final class AuraModel {
    private AuraModel() {}

    public enum Rarity { COMMON, RARE, EPIC, LEGENDARY, MYTHIC }
    public enum Origin { CHEST, CRAFT, DISMANTLE }

    public static final class Roll {
        public final EffectResolver.Stat stat;
        /** Additive percentage delta in decimal form: 0.34 means +34%. */
        public final BigDecimal additiveMultiplier;

        public Roll(EffectResolver.Stat stat, BigDecimal additiveMultiplier) {
            if (stat == null || additiveMultiplier == null || additiveMultiplier.signum() < 0)
                throw new IllegalArgumentException("Aura roll requires a non-negative stat value");
            this.stat = stat;
            this.additiveMultiplier = additiveMultiplier;
        }
    }

    public static final class AuraInstance {
        public final UUID instanceId;
        public final Rarity rarity;
        public final Origin origin;
        public final long generationSeed;
        public final List<Roll> rawRolls;
        public final Map<EffectResolver.Stat, BigDecimal> aggregatedEffects;
        public boolean pinned;
        public final long createdAtMillis;

        public AuraInstance(UUID instanceId, Rarity rarity, Origin origin, long generationSeed,
                            List<Roll> rawRolls, boolean pinned, long createdAtMillis) {
            if (instanceId == null || rarity == null || origin == null || rawRolls == null)
                throw new IllegalArgumentException("Aura instance fields are required");
            this.instanceId = instanceId;
            this.rarity = rarity;
            this.origin = origin;
            this.generationSeed = generationSeed;
            this.rawRolls = Collections.unmodifiableList(new ArrayList<>(rawRolls));
            this.aggregatedEffects = Collections.unmodifiableMap(aggregate(rawRolls));
            this.pinned = pinned;
            this.createdAtMillis = Math.max(0L, createdAtMillis);
        }

        private static Map<EffectResolver.Stat, BigDecimal> aggregate(List<Roll> rolls) {
            Map<EffectResolver.Stat, BigDecimal> result = new EnumMap<>(EffectResolver.Stat.class);
            for (Roll roll : rolls) {
                if (roll == null) continue;
                BigDecimal current = result.get(roll.stat);
                result.put(roll.stat, (current == null ? BigDecimal.ZERO : current).add(roll.additiveMultiplier));
            }
            return result;
        }
    }

    public static final class PlayerState {
        public final List<AuraInstance> ownedAuras = new ArrayList<>();
        public UUID activeAuraId;
        public long totalChestOpens;
        public long totalChestAuraDrops;
        public boolean skipOpeningAnimation;

        public AuraInstance find(UUID instanceId) {
            if (instanceId == null) return null;
            for (AuraInstance aura : ownedAuras) if (instanceId.equals(aura.instanceId)) return aura;
            return null;
        }

        public AuraInstance active() { return find(activeAuraId); }

        public void repairActiveReference() {
            if (activeAuraId != null && find(activeAuraId) == null) activeAuraId = null;
        }
    }
}
