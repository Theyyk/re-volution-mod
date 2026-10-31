package com.example.revolution.persistence;

import com.example.revolution.aura.AuraModel;
import com.example.revolution.effect.EffectResolver;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class MongoAuraRepositoryTest {
    @Test void missingAuraStateLoadsAsV29Defaults() {
        AuraModel.PlayerState state = MongoAuraRepository.decode(null);
        assertTrue(state.ownedAuras.isEmpty());
        assertNull(state.activeAuraId);
        assertEquals(0L, state.totalChestOpens);
        assertEquals(0L, state.totalChestAuraDrops);
        assertFalse(state.skipOpeningAnimation);
    }

    @Test void seedRawAggregatedPinnedAndActiveRoundTrip() {
        AuraModel.PlayerState state = new AuraModel.PlayerState();
        AuraModel.AuraInstance aura = new AuraModel.AuraInstance(UUID.randomUUID(), AuraModel.Rarity.LEGENDARY,
                AuraModel.Origin.CHEST, 987654321L, Arrays.asList(
                new AuraModel.Roll(EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER, new BigDecimal("0.1200")),
                new AuraModel.Roll(EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER, new BigDecimal("0.0800")),
                new AuraModel.Roll(EffectResolver.Stat.COIN_REWARD_MULTIPLIER, new BigDecimal("0.1500"))), true, 123L);
        state.ownedAuras.add(aura); state.activeAuraId = aura.instanceId; state.totalChestOpens = 4; state.totalChestAuraDrops = 4; state.skipOpeningAnimation = true;
        Document encoded = MongoAuraRepository.encode(state);
        AuraModel.PlayerState decoded = MongoAuraRepository.decode(encoded);
        AuraModel.AuraInstance restored = decoded.active();
        assertNotNull(restored); assertEquals(aura.instanceId, restored.instanceId); assertEquals(987654321L, restored.generationSeed);
        assertEquals(3, restored.rawRolls.size()); assertTrue(restored.pinned); assertEquals(AuraModel.Origin.CHEST, restored.origin);
        assertEquals(0, new BigDecimal("0.2000").compareTo(restored.aggregatedEffects.get(EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER)));
        assertEquals(4L, decoded.totalChestOpens); assertEquals(4L, decoded.totalChestAuraDrops); assertTrue(decoded.skipOpeningAnimation);
    }

    @Test void danglingActiveReferenceIsRepaired() {
        Document root = new Document("ownedAuras", Arrays.asList())
                .append("activeAuraId", UUID.randomUUID().toString())
                .append("totalChestOpens", 0L).append("totalChestAuraDrops", 0L).append("skipOpeningAnimation", false);
        assertNull(MongoAuraRepository.decode(root).activeAuraId);
    }
}
