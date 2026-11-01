package com.example.revolution.unique;

import com.example.revolution.effect.EffectResolver;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Stable v2.11.0 domain contract for permanent Unique Rune Collections. */
public final class UniqueRuneModel {
    private UniqueRuneModel() {}

    public static final int RUNES_PER_COLLECTION = 26;

    public enum CollectionId { CRYSTAL_COLLECTION, DAILY_COLLECTION, OFFERINGS_COLLECTION }
    public enum AcquisitionType { CRYSTALS, DAILY_REWARD, OFFERINGS }
    public enum DuplicatePolicy { PEARL, EXCLUDE, NONE }
    public enum Rarity {
        COMMON(1, true), IMPROVED(2, true), ADVANCED(3, true), MYTHIC(4, true), ANCIENT(5, false);
        public final int persistentId;
        public final boolean gameplayEnabled;
        Rarity(int persistentId, boolean gameplayEnabled) { this.persistentId = persistentId; this.gameplayEnabled = gameplayEnabled; }
        public Rarity nextGameplay() {
            switch (this) {
                case COMMON: return IMPROVED;
                case IMPROVED: return ADVANCED;
                case ADVANCED: return MYTHIC;
                default: return this;
            }
        }
        public Rarity previous() {
            switch (this) {
                case IMPROVED: return COMMON;
                case ADVANCED: return IMPROVED;
                case MYTHIC: return ADVANCED;
                case ANCIENT: return MYTHIC;
                default: return this;
            }
        }
    }

    public enum MechanicalEffectType { CHANCE_PROC, CONDITIONAL_BONUS, RESOURCE_MODIFIER }

    public static final class Effect {
        public final EffectResolver.Stat stat;
        public final BigDecimal baseAdditive;
        public final BigDecimal rarityScale;
        public final BigDecimal enhancementStep;
        public final MechanicalEffectType mechanicalType;
        public final String mechanicalId;
        public final BigDecimal chance;

        public Effect(EffectResolver.Stat stat, BigDecimal baseAdditive, BigDecimal rarityScale, BigDecimal enhancementStep) {
            this(stat, baseAdditive, rarityScale, enhancementStep, null, "", BigDecimal.ZERO);
        }

        public Effect(EffectResolver.Stat stat, BigDecimal baseAdditive, BigDecimal rarityScale, BigDecimal enhancementStep,
                      MechanicalEffectType mechanicalType, String mechanicalId, BigDecimal chance) {
            if (stat == null && mechanicalType == null) throw new IllegalArgumentException("Unique Rune effect requires a stat or mechanical type");
            this.stat = stat;
            this.baseAdditive = nonNegative(baseAdditive);
            this.rarityScale = nonNegative(rarityScale);
            this.enhancementStep = nonNegative(enhancementStep);
            this.mechanicalType = mechanicalType;
            this.mechanicalId = mechanicalId == null ? "" : mechanicalId;
            this.chance = nonNegative(chance).min(BigDecimal.ONE);
        }

        public BigDecimal value(Rarity rarity, int enhancement) {
            int rankSteps = Math.max(0, rarity == null ? 0 : rarity.persistentId - 1);
            BigDecimal rarityMultiplier = BigDecimal.ONE.add(rarityScale.multiply(BigDecimal.valueOf(rankSteps)));
            BigDecimal enhancementMultiplier = BigDecimal.ONE.add(enhancementStep.multiply(BigDecimal.valueOf(Math.max(0, enhancement))));
            return baseAdditive.multiply(rarityMultiplier).multiply(enhancementMultiplier);
        }
    }

    public static final class RuneDefinition {
        public final String runeId;
        public final CollectionId collectionId;
        public final String displayName;
        public final List<Effect> effects;
        public RuneDefinition(String runeId, CollectionId collectionId, String displayName, List<Effect> effects) {
            if (runeId == null || runeId.trim().isEmpty() || collectionId == null || effects == null || effects.isEmpty())
                throw new IllegalArgumentException("Unique Rune definition is incomplete");
            this.runeId = runeId;
            this.collectionId = collectionId;
            this.displayName = displayName == null ? runeId : displayName;
            this.effects = Collections.unmodifiableList(new ArrayList<>(effects));
        }
    }

    public static final class CollectionDefinition {
        public final CollectionId id;
        public final AcquisitionType acquisitionType;
        public final DuplicatePolicy duplicatePolicy;
        public final List<RuneDefinition> runes;
        public CollectionDefinition(CollectionId id, AcquisitionType acquisitionType, DuplicatePolicy duplicatePolicy, List<RuneDefinition> runes) {
            if (id == null || acquisitionType == null || duplicatePolicy == null || runes == null || runes.size() != RUNES_PER_COLLECTION)
                throw new IllegalArgumentException("Unique collection must contain exactly 26 runes");
            Set<String> ids = new LinkedHashSet<>();
            for (RuneDefinition rune : runes) {
                if (rune == null || rune.collectionId != id || !ids.add(rune.runeId)) throw new IllegalArgumentException("Invalid Unique Rune collection content");
            }
            this.id = id; this.acquisitionType = acquisitionType; this.duplicatePolicy = duplicatePolicy;
            this.runes = Collections.unmodifiableList(new ArrayList<>(runes));
        }
        public RuneDefinition rune(String runeId) {
            for (RuneDefinition rune : runes) if (rune.runeId.equals(runeId)) return rune;
            return null;
        }
    }

    public static final class CollectionState {
        public Rarity currentRarity = Rarity.COMMON;
        /** Rune id -> factual owned rarity in the current cycle. */
        public final Map<String, Rarity> owned = new LinkedHashMap<>();
        /** Enhancement survives awaken/rollback and is meaningful only for Crystal collection. */
        public final Map<String, Integer> enhancement = new LinkedHashMap<>();
        public Rarity rollbackRarity;
        public final Map<String, Rarity> rollbackOwned = new LinkedHashMap<>();

        public int progress() {
            int result = 0;
            for (Rarity rarity : owned.values()) if (rarity == currentRarity) result++;
            return result;
        }
        public boolean ownsAtCurrent(String runeId) { return currentRarity == owned.get(runeId); }
        public int enhancement(String runeId) { Integer value = enhancement.get(runeId); return value == null ? 0 : Math.max(0, value); }
        public CollectionState copy() {
            CollectionState copy = new CollectionState(); copy.currentRarity = currentRarity;
            copy.owned.putAll(owned); copy.enhancement.putAll(enhancement); copy.rollbackRarity = rollbackRarity; copy.rollbackOwned.putAll(rollbackOwned); return copy;
        }
    }

    public static final class PlayerState {
        public final Map<CollectionId, CollectionState> collections = new EnumMap<>(CollectionId.class);
        public long lastDailyClaimDay = Long.MIN_VALUE;
        public PlayerState() { for (CollectionId id : CollectionId.values()) collections.put(id, new CollectionState()); }
        public CollectionState collection(CollectionId id) { return collections.get(id); }
        public PlayerState copy() {
            PlayerState copy = new PlayerState(); copy.collections.clear();
            for (Map.Entry<CollectionId, CollectionState> entry : collections.entrySet()) copy.collections.put(entry.getKey(), entry.getValue().copy());
            copy.lastDailyClaimDay = lastDailyClaimDay; return copy;
        }
    }

    private static BigDecimal nonNegative(BigDecimal value) { return value == null || value.signum() < 0 ? BigDecimal.ZERO : value; }
}
