package com.example.revolution.aura;

import com.example.revolution.effect.EffectResolver;
import java.math.BigDecimal;
import java.util.List;

public interface AuraPolicy {
    boolean featureEnabled();
    boolean rarityEnabled(AuraModel.Rarity rarity);
    BigDecimal dropWeight(AuraModel.Rarity rarity);
    int minRolls(AuraModel.Rarity rarity);
    int maxRolls(AuraModel.Rarity rarity);
    int craftToNextCost(AuraModel.Rarity rarity);
    int dismantleToPreviousCount(AuraModel.Rarity rarity);
    boolean effectEnabled(AuraModel.Rarity rarity, EffectResolver.Stat stat);
    BigDecimal effectWeight(AuraModel.Rarity rarity, EffectResolver.Stat stat);
    BigDecimal minEffect(AuraModel.Rarity rarity, EffectResolver.Stat stat);
    BigDecimal maxEffect(AuraModel.Rarity rarity, EffectResolver.Stat stat);
    int keyCrystalPrice();
    List<Integer> allowedOpenQuantities();
}
