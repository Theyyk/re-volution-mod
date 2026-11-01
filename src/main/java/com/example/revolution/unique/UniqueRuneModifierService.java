package com.example.revolution.unique;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.unique.UniqueRuneModel.CollectionDefinition;
import com.example.revolution.unique.UniqueRuneModel.CollectionId;
import com.example.revolution.unique.UniqueRuneModel.CollectionState;
import com.example.revolution.unique.UniqueRuneModel.Effect;
import com.example.revolution.unique.UniqueRuneModel.PlayerState;
import com.example.revolution.unique.UniqueRuneModel.Rarity;
import com.example.revolution.unique.UniqueRuneModel.RuneDefinition;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Permanent UNIQUE_RUNE layer, deliberately isolated between normal effects and GOD/AURA. */
public final class UniqueRuneModifierService {
    public interface MechanicalHook {
        void trigger(UUID playerUuid, String runeId, UniqueRuneModel.MechanicalEffectType type, String effectId, BigDecimal value);
    }
    public static final MechanicalHook NOOP_HOOK = (playerUuid, runeId, type, effectId, value) -> {};

    private final UniqueRuneRepository repository;
    private final BalanceUniqueRunePolicy policy;
    private final MechanicalHook mechanicalHook;

    public UniqueRuneModifierService(UniqueRuneRepository repository, BalanceUniqueRunePolicy policy) {
        this(repository, policy, NOOP_HOOK);
    }
    public UniqueRuneModifierService(UniqueRuneRepository repository, BalanceUniqueRunePolicy policy, MechanicalHook mechanicalHook) {
        if (repository == null || policy == null || mechanicalHook == null) throw new IllegalArgumentException("Unique Rune modifier dependencies are required");
        this.repository = repository; this.policy = policy; this.mechanicalHook = mechanicalHook;
    }

    public BigDecimal applyDamage(UUID playerUuid, GameplayDamageSource source, BigDecimal preUniqueDamage) {
        if (playerUuid == null || preUniqueDamage == null || preUniqueDamage.signum() <= 0) return preUniqueDamage == null ? BigDecimal.ZERO : preUniqueDamage;
        EffectResolver.Stat channel = statFor(source);
        BigDecimal additive = aggregate(playerUuid, channel)
                .add(aggregate(playerUuid, EffectResolver.Stat.ALL_DAMAGE_MULTIPLIER))
                .add(aggregate(playerUuid, EffectResolver.Stat.VULNERABILITY_MULTIPLIER))
                .add(aggregate(playerUuid, EffectResolver.Stat.EFFECTIVENESS_MULTIPLIER));
        return preUniqueDamage.multiply(BigDecimal.ONE.add(additive).max(BigDecimal.ZERO));
    }

    /** Used by online and offline resource calculations before the GOD/AURA late layers. */
    public BigDecimal resourceMultiplier(UUID playerUuid, String canonicalResource) {
        EffectResolver.Stat stat = resourceStat(canonicalResource); return stat == null ? BigDecimal.ONE : BigDecimal.ONE.add(aggregate(playerUuid, stat)).max(BigDecimal.ZERO);
    }

    /** Generic server-side mechanical hook. Chance evaluation remains authoritative at the caller boundary. */
    public List<MechanicalEffect> mechanicalEffects(UUID playerUuid) {
        List<MechanicalEffect> result = new ArrayList<>(); PlayerState state = repository.load(playerUuid);
        for (CollectionId id : CollectionId.values()) {
            CollectionDefinition definition = policy.definition(id); CollectionState collection = state.collection(id);
            for (Map.Entry<String, Rarity> owned : collection.owned.entrySet()) {
                RuneDefinition rune = definition.rune(owned.getKey()); if (rune == null) continue;
                int enhancement = id == CollectionId.CRYSTAL_COLLECTION ? collection.enhancement(rune.runeId) : 0;
                for (Effect effect : rune.effects) if (effect.mechanicalType != null)
                    result.add(new MechanicalEffect(rune.runeId, effect.mechanicalType, effect.mechanicalId, effect.chance,
                            effect.value(owned.getValue(), enhancement)));
            }
        }
        return result;
    }

    public void triggerMechanical(UUID playerUuid, MechanicalEffect effect) {
        if (playerUuid != null && effect != null) mechanicalHook.trigger(playerUuid, effect.runeId, effect.type, effect.effectId, effect.value);
    }

    private BigDecimal aggregate(UUID playerUuid, EffectResolver.Stat stat) {
        if (stat == null) return BigDecimal.ZERO; BigDecimal result = BigDecimal.ZERO; PlayerState state = repository.load(playerUuid);
        for (CollectionId id : CollectionId.values()) {
            CollectionDefinition definition = policy.definition(id); CollectionState collection = state.collection(id);
            for (Map.Entry<String, Rarity> owned : collection.owned.entrySet()) {
                RuneDefinition rune = definition.rune(owned.getKey()); if (rune == null) continue;
                int enhancement = id == CollectionId.CRYSTAL_COLLECTION ? collection.enhancement(rune.runeId) : 0;
                for (Effect effect : rune.effects) if (effect.stat == stat) result = result.add(effect.value(owned.getValue(), enhancement));
            }
        }
        return result;
    }

    public static final class MechanicalEffect {
        public final String runeId; public final UniqueRuneModel.MechanicalEffectType type; public final String effectId;
        public final BigDecimal chance; public final BigDecimal value;
        MechanicalEffect(String runeId, UniqueRuneModel.MechanicalEffectType type, String effectId, BigDecimal chance, BigDecimal value) {
            this.runeId = runeId; this.type = type; this.effectId = effectId; this.chance = chance; this.value = value;
        }
    }

    public static EffectResolver.Stat statFor(GameplayDamageSource source) {
        if (source == null) return null;
        switch (source) {
            case CLICK: return EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER;
            case POISON: return EffectResolver.Stat.POISON_DAMAGE_MULTIPLIER;
            case BURNING: return EffectResolver.Stat.BURNING_DAMAGE_MULTIPLIER;
            case BLEEDING: return EffectResolver.Stat.BLEEDING_DAMAGE_MULTIPLIER;
            case CURSE: return EffectResolver.Stat.CURSE_DAMAGE_MULTIPLIER;
            case WITHER: return EffectResolver.Stat.WITHER_DAMAGE_MULTIPLIER;
            case DECAY: return EffectResolver.Stat.DECAY_DAMAGE_MULTIPLIER;
            default: return null;
        }
    }
    private static EffectResolver.Stat resourceStat(String resource) {
        if (resource == null) return null;
        if ("coins".equalsIgnoreCase(resource)) return EffectResolver.Stat.COIN_REWARD_MULTIPLIER;
        if ("rune_dust".equalsIgnoreCase(resource)) return EffectResolver.Stat.RUNE_DUST_REWARD_MULTIPLIER;
        if ("rune_shards".equalsIgnoreCase(resource)) return EffectResolver.Stat.RUNE_SHARDS_REWARD_MULTIPLIER;
        if ("rune_cores".equalsIgnoreCase(resource)) return EffectResolver.Stat.RUNE_CORES_REWARD_MULTIPLIER;
        return null;
    }
}
