package com.example.revolution.unique;

import com.example.revolution.balance.service.BalanceBootstrap;
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

    @Test void crystalEnhancementCurvesAreRuneSpecificAndUnlimitedByDefault() {
        BalanceUniqueRunePolicy policy = new BalanceUniqueRunePolicy(BalanceBootstrap.activateBundledDefaults());
        assertNotEquals(policy.enhancementPearlCost("crystal_01", 4), policy.enhancementPearlCost("crystal_04", 4));
        assertTrue(policy.enhancementPearlCost("crystal_26", 100).signum() > 0);
    }
}
