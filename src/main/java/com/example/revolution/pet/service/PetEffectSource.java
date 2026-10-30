package com.example.revolution.pet.service;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.pet.definition.PetCatalog;
import com.example.revolution.pet.definition.PetDefinition;
import com.example.revolution.pet.definition.PetEffectDefinition;
import com.example.revolution.pet.definition.PetEffectType;
import com.example.revolution.pet.repository.PetRepository;
import com.example.revolution.pet.state.PetPlayerState;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Active selected Pets as a first-class source for the shared EffectResolver. */
public final class PetEffectSource implements EffectResolver.Source {
    private final PetRepository repository;
    private final PetCatalog catalog;
    private final PetBalanceService balance;

    public PetEffectSource(PetRepository repository, PetCatalog catalog, PetBalanceService balance) {
        this.repository = repository;
        this.catalog = catalog;
        this.balance = balance;
    }

    @Override public List<EffectResolver.Contribution> contributions(UUID playerUuid, EffectResolver.Stat requested) {
        PetPlayerState state = repository.load(playerUuid);
        if (state.run == null || state.run.selectedPets.isEmpty()) return Collections.emptyList();
        List<EffectResolver.Contribution> result = new ArrayList<>();
        for (PetPlayerState.SelectedPet selected : state.run.selectedPets) {
            PetDefinition pet = catalog.get(selected.petId);
            PetPlayerState.PetProgress progress = state.pets.get(selected.petId);
            if (pet == null || progress == null) continue;
            BigDecimal rankScale = balance.rankScale(progress.rank);
            for (PetEffectDefinition effect : pet.effects) {
                EffectResolver.Stat stat = stat(effect);
                if (stat == null || stat != requested) continue; // Future systems and AUTO_CLICK intentionally use separate consumers.
                BigDecimal value = balance.effectValue(effect).multiply(rankScale);
                result.add(new EffectResolver.Contribution(stat, value, "pet:" + pet.petId + ":" + effect.balanceValueId));
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** Aggregate server-controlled autoclick rate contribution from selected Pets. */
    public BigDecimal autoClickCps(UUID playerUuid) {
        PetPlayerState state = repository.load(playerUuid);
        if (state.run == null) return BigDecimal.ZERO;
        BigDecimal result = BigDecimal.ZERO;
        for (PetPlayerState.SelectedPet selected : state.run.selectedPets) {
            PetDefinition pet = catalog.get(selected.petId);
            PetPlayerState.PetProgress progress = state.pets.get(selected.petId);
            if (pet == null || progress == null) continue;
            for (PetEffectDefinition effect : pet.effects) if (effect.type == PetEffectType.AUTO_CLICK) {
                result = result.add(balance.effectValue(effect).multiply(balance.rankScale(progress.rank)));
            }
        }
        return result.max(BigDecimal.ZERO);
    }

    private static EffectResolver.Stat stat(PetEffectDefinition effect) {
        switch (effect.type) {
            case DAMAGE_MULTIPLIER: return damageStat(effect.channel);
            case ALL_DAMAGE_MULTIPLIER: return EffectResolver.Stat.ALL_DAMAGE_MULTIPLIER;
            case VULNERABILITY: return EffectResolver.Stat.VULNERABILITY_MULTIPLIER;
            case EFFECTIVENESS: return EffectResolver.Stat.EFFECTIVENESS_MULTIPLIER;
            case CLICK_CRIT_CHANCE: return EffectResolver.Stat.CLICK_CRIT_CHANCE_MULTIPLIER;
            case CLICK_CRIT_DAMAGE_MULTIPLIER: return EffectResolver.Stat.CLICK_CRIT_DAMAGE_MULTIPLIER;
            case MONSTER_GOLD_MULTIPLIER: return EffectResolver.Stat.COIN_REWARD_MULTIPLIER;
            case MONSTER_RESOURCE_MULTIPLIER: return EffectResolver.Stat.RUNE_RESOURCE_REWARD_MULTIPLIER;
            case PET_XP_MULTIPLIER: return EffectResolver.Stat.PET_XP_MULTIPLIER;
            case SPECIAL_EFFECT_MULTIPLIER: return EffectResolver.Stat.SPECIAL_EFFECT_MULTIPLIER;
            case FORGE_RESOURCE_MULTIPLIER: return EffectResolver.Stat.FORGE_RESOURCE_MULTIPLIER;
            case FORGE_SPEED_MULTIPLIER: return EffectResolver.Stat.FORGE_SPEED_MULTIPLIER;
            default: return null;
        }
    }

    private static EffectResolver.Stat damageStat(String channel) {
        if ("CLICK".equals(channel)) return EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER;
        if ("POISON".equals(channel)) return EffectResolver.Stat.POISON_DAMAGE_MULTIPLIER;
        if ("BURNING".equals(channel)) return EffectResolver.Stat.BURNING_DAMAGE_MULTIPLIER;
        if ("BLEEDING".equals(channel)) return EffectResolver.Stat.BLEEDING_DAMAGE_MULTIPLIER;
        if ("CURSE".equals(channel)) return EffectResolver.Stat.CURSE_DAMAGE_MULTIPLIER;
        if ("WITHER".equals(channel)) return EffectResolver.Stat.WITHER_DAMAGE_MULTIPLIER;
        if ("DECAY".equals(channel)) return EffectResolver.Stat.DECAY_DAMAGE_MULTIPLIER;
        return null;
    }
}
