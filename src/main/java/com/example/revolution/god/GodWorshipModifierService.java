package com.example.revolution.god;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.god.GodWorshipModel.FeatureGate;
import com.example.revolution.god.GodWorshipModel.GodId;
import com.example.revolution.god.GodWorshipModel.Phase;
import com.example.revolution.god.GodWorshipModel.RunState;
import com.example.revolution.god.GodWorshipModel.State;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Resolves the isolated late God layer without registering it as a normal EffectResolver source. */
public final class GodWorshipModifierService {
    private final GodWorshipRepository repository;
    private final BalanceGodWorshipPolicy policy;

    public GodWorshipModifierService(GodWorshipRepository repository, BalanceGodWorshipPolicy policy) {
        if (repository == null || policy == null) throw new IllegalArgumentException("God modifier dependencies are required");
        this.repository = repository;
        this.policy = policy;
    }

    public BigDecimal apply(UUID playerUuid, GameplayDamageSource source, BigDecimal preGodDamage, long nowMillis) {
        if (playerUuid == null || preGodDamage == null || preGodDamage.signum() <= 0) return preGodDamage == null ? BigDecimal.ZERO : preGodDamage;
        EffectResolver.Stat channel = statFor(source);
        RunState run = repository.loadRunState(playerUuid);
        boolean secondary = policy.featureEnabled(FeatureGate.GOD_SECONDARY_BUFFS);
        List<BigDecimal> channelDeltas = new ArrayList<>();
        List<BigDecimal> allDeltas = new ArrayList<>();

        for (GodId god : GodId.values()) {
            State state = run.gods.get(god);
            if (state == null || !state.unlocked || state.level <= 0L || state.phase != Phase.BUFF_ACTIVE
                    || nowMillis >= state.buffActiveUntilMillis || !policy.enabled(god)) continue;
            BigDecimal delta = policy.buffAdditiveMultiplier(god, state.level);
            if (delta.signum() <= 0) continue;
            if (GodWorshipRules.affects(god, EffectResolver.Stat.ALL_DAMAGE_MULTIPLIER, secondary)) allDeltas.add(delta);
            if (channel != null && GodWorshipRules.affects(god, channel, secondary)) channelDeltas.add(delta);
        }

        return GodWorshipRules.applyGodLayer(
                preGodDamage,
                GodWorshipRules.godLayerMultiplier(channelDeltas),
                GodWorshipRules.godLayerMultiplier(allDeltas));
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
