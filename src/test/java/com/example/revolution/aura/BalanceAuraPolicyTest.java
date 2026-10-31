package com.example.revolution.aura;

import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.effect.EffectResolver;
import java.math.BigDecimal;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class BalanceAuraPolicyTest {
    @Test void bundledV29OverlayLoadsAndExposesAllRarities() {
        BalanceService balance = BalanceBootstrap.activateBundledDefaults();
        BalanceAuraPolicy policy = new BalanceAuraPolicy(balance);
        assertTrue(policy.featureEnabled());
        assertEquals(Arrays.asList(1, 3, 5, 10), policy.allowedOpenQuantities());
        assertTrue(policy.keyCrystalPrice() > 0);
        for (AuraModel.Rarity rarity : AuraModel.Rarity.values()) {
            assertTrue(policy.rarityEnabled(rarity));
            assertTrue(policy.dropWeight(rarity).signum() > 0);
            assertTrue(policy.minRolls(rarity) > 0);
            assertTrue(policy.maxRolls(rarity) >= policy.minRolls(rarity));
            assertTrue(policy.minEffect(rarity, EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER).signum() >= 0);
            assertTrue(policy.maxEffect(rarity, EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER)
                    .compareTo(policy.minEffect(rarity, EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER)) >= 0);
        }
        assertEquals(0, policy.craftToNextCost(AuraModel.Rarity.MYTHIC));
        assertEquals(0, policy.dismantleToPreviousCount(AuraModel.Rarity.COMMON));
    }

    @Test void strongerGeneralEffectCanHaveLowerWeightThanSpecificChannel() {
        BalanceAuraPolicy policy = new BalanceAuraPolicy(BalanceBootstrap.activateBundledDefaults());
        BigDecimal allDamage = policy.effectWeight(AuraModel.Rarity.EPIC, EffectResolver.Stat.ALL_DAMAGE_MULTIPLIER);
        BigDecimal click = policy.effectWeight(AuraModel.Rarity.EPIC, EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER);
        assertTrue(allDamage.compareTo(click) < 0);
    }

    @Test void petAndUnrelatedProgressionStatsAreNotInAuraPool() {
        assertFalse(BalanceAuraPolicy.SUPPORTED_EFFECTS.contains(EffectResolver.Stat.PET_XP_MULTIPLIER));
        assertFalse(BalanceAuraPolicy.SUPPORTED_EFFECTS.contains(EffectResolver.Stat.FORGE_RESOURCE_MULTIPLIER));
        assertFalse(BalanceAuraPolicy.SUPPORTED_EFFECTS.contains(EffectResolver.Stat.FORGE_SPEED_MULTIPLIER));
    }
}
