package com.example.revolution.aura;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.gameplay.GameplayDamageSource;
import java.math.BigDecimal;
import java.util.UUID;

/** Applies only the one active Aura after the normal and GOD layers. */
public final class AuraModifierService {
    private final AuraRepository repository;
    public AuraModifierService(AuraRepository repository) { if (repository == null) throw new IllegalArgumentException("repository is required"); this.repository = repository; }

    public BigDecimal applyDamage(UUID playerUuid, GameplayDamageSource source, BigDecimal preAuraDamage) {
        if (playerUuid == null || preAuraDamage == null || preAuraDamage.signum() <= 0) return preAuraDamage == null ? BigDecimal.ZERO : preAuraDamage;
        AuraModel.PlayerState state = repository.load(playerUuid); state.repairActiveReference(); AuraModel.AuraInstance active = state.active();
        if (active == null) return preAuraDamage;
        BigDecimal delta = value(active, statFor(source))
                .add(value(active, EffectResolver.Stat.ALL_DAMAGE_MULTIPLIER))
                .add(value(active, EffectResolver.Stat.VULNERABILITY_MULTIPLIER))
                .add(value(active, EffectResolver.Stat.EFFECTIVENESS_MULTIPLIER));
        return preAuraDamage.multiply(BigDecimal.ONE.add(delta).max(BigDecimal.ZERO));
    }

    public BigDecimal resourceMultiplier(UUID playerUuid, String canonicalResource) {
        AuraModel.PlayerState state = repository.load(playerUuid); state.repairActiveReference(); AuraModel.AuraInstance active = state.active();
        if (active == null || canonicalResource == null) return BigDecimal.ONE;
        EffectResolver.Stat stat = null;
        if ("coins".equalsIgnoreCase(canonicalResource)) stat = EffectResolver.Stat.COIN_REWARD_MULTIPLIER;
        else if ("rune_dust".equalsIgnoreCase(canonicalResource)) stat = EffectResolver.Stat.RUNE_DUST_REWARD_MULTIPLIER;
        else if ("rune_shards".equalsIgnoreCase(canonicalResource)) stat = EffectResolver.Stat.RUNE_SHARDS_REWARD_MULTIPLIER;
        else if ("rune_cores".equalsIgnoreCase(canonicalResource)) stat = EffectResolver.Stat.RUNE_CORES_REWARD_MULTIPLIER;
        return stat == null ? BigDecimal.ONE : BigDecimal.ONE.add(value(active, stat)).max(BigDecimal.ZERO);
    }

    private static BigDecimal value(AuraModel.AuraInstance aura, EffectResolver.Stat stat) {
        if (aura == null || stat == null) return BigDecimal.ZERO;
        BigDecimal value = aura.aggregatedEffects.get(stat); return value == null ? BigDecimal.ZERO : value;
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
}
