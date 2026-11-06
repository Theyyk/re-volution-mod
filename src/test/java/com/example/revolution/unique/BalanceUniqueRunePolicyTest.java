package com.example.revolution.unique;

import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.balance.config.BalanceConfigCodec;
import com.example.revolution.balance.definition.BalanceConfig;
import java.util.LinkedHashMap;
import java.util.Map;
import com.example.revolution.unique.UniqueRuneModel.CollectionId;
import com.example.revolution.unique.UniqueRuneModel.Rarity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class BalanceUniqueRunePolicyTest {
    @Test void bundledV211OverlayDefinesThreeFixedCollectionsOf26() {
        BalanceUniqueRunePolicy policy = new BalanceUniqueRunePolicy(BalanceBootstrap.activateBundledDefaults());
        assertTrue(policy.enabled());
        assertEquals("draft", policy.zbtStatus());
        assertEquals(3, policy.definitions().size());
        for (CollectionId id : CollectionId.values()) {
            assertTrue(policy.collectionEnabled(id));
            assertEquals(26, policy.definition(id).runes.size());
        }
    }

    @Test void rarityProgressionStopsAtMythicAndAncientRemainsReserved() {
        assertEquals(Rarity.IMPROVED, Rarity.COMMON.nextGameplay());
        assertEquals(Rarity.ADVANCED, Rarity.IMPROVED.nextGameplay());
        assertEquals(Rarity.MYTHIC, Rarity.ADVANCED.nextGameplay());
        assertEquals(Rarity.MYTHIC, Rarity.MYTHIC.nextGameplay());
        assertFalse(Rarity.ANCIENT.gameplayEnabled);
    }

    @Test void changingActiveBalanceCorePriceAppliesToEveryCrystalRune() {
        BalanceService balance = BalanceBootstrap.activateBundledDefaults();
        BalanceUniqueRunePolicy policy = new BalanceUniqueRunePolicy(balance);
        BalanceConfig current = balance.getActiveSnapshot().getConfig();
        Map<String, String> values = new LinkedHashMap<>(current.getValues());
        values.put("unique_rune.crystal.enhancement_base_cost", "7");
        // The obsolete growth key remains in existing snapshots without affecting the price.
        values.put("unique_rune.crystal.enhancement_cost_growth",
                "100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100");
        balance.activate(new BalanceConfigCodec().canonicalJson(
                new BalanceConfig(current.getBalanceSchemaVersion(), values, current.getCurves())));
        assertEquals(0, new java.math.BigDecimal("7").compareTo(policy.enhancementPearlCost("crystal_01", 0)));
        assertEquals(0, new java.math.BigDecimal("7").compareTo(policy.enhancementPearlCost("crystal_26", 1000)));
    }

    @Test void crystalEnhancementCostIsOnePearlRegardlessOfRuneOrLevel() {
        BalanceUniqueRunePolicy policy = new BalanceUniqueRunePolicy(BalanceBootstrap.activateBundledDefaults());
        for (String id : new String[]{"crystal_01", "crystal_04", "crystal_26"}) {
            for (int level : new int[]{0, 4, 100}) {
                assertEquals(0, java.math.BigDecimal.ONE.compareTo(policy.enhancementPearlCost(id, level)));
            }
        }
    }
}
