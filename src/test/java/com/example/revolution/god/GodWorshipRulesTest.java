package com.example.revolution.god;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.god.GodWorshipModel.ActivationMode;
import com.example.revolution.god.GodWorshipModel.GodId;
import com.example.revolution.god.GodWorshipModel.Phase;
import com.example.revolution.god.GodWorshipModel.State;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class GodWorshipRulesTest {
    @Test
    void canonicalDefinitionsMatchV280Contract() {
        assertEquals(5, GodWorshipModel.definitions().size());
        assertEquals("rune_dust", GodWorshipModel.definition(GodId.GOD_1).resource);
        assertEquals("rune_shards", GodWorshipModel.definition(GodId.GOD_2).resource);
        assertEquals("coins", GodWorshipModel.definition(GodId.GOD_3).resource);
        assertEquals("coins", GodWorshipModel.definition(GodId.GOD_4).resource);
        assertEquals("rune_cores", GodWorshipModel.definition(GodId.GOD_5).resource);
        assertTrue(GodWorshipRules.affects(GodId.GOD_1, EffectResolver.Stat.POISON_DAMAGE_MULTIPLIER, false));
        assertFalse(GodWorshipRules.affects(GodId.GOD_1, EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER, false));
        assertTrue(GodWorshipRules.affects(GodId.GOD_1, EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER, true));
    }

    @Test
    void manualAutomationCannotPerformFinalActivationStep() {
        assertEquals(9L, GodWorshipRules.automatedChargeTarget(0, 100, 10, 1,
                ActivationMode.MANUAL, false));
        assertEquals(9L, GodWorshipRules.automatedChargeTarget(0, 100, 10, 1,
                ActivationMode.MANUAL, true));
        assertEquals(10L, GodWorshipRules.automatedChargeTarget(0, 100, 10, 1,
                ActivationMode.AUTO_WHEN_READY, true));
    }

    @Test
    void cooldownStartsAtFactualBuffExpiryAndCanFinishOffline() {
        State state = unlockedCharging();
        GodWorshipRules.activate(state, 1_000L, 5_000L, 0L);
        assertEquals(Phase.BUFF_ACTIVE, state.phase);
        assertEquals(6_000L, state.buffActiveUntilMillis);

        GodWorshipRules.normalize(state, 7_000L, 10_000L);
        assertEquals(Phase.COOLDOWN, state.phase);
        assertEquals(16_000L, state.cooldownUntilMillis);

        GodWorshipRules.normalize(state, 20_000L, 10_000L);
        assertEquals(Phase.CHARGE, state.phase);
        assertEquals(0L, state.cooldownUntilMillis);
    }

    @Test
    void offeringsSettlementUsesOnlyFactualActiveInterval() {
        State state = unlockedCharging();
        GodWorshipRules.activate(state, 1_000L, 5_000L, 0L);
        assertEquals(2_000L, GodWorshipRules.factualActiveMillis(state, 4_000L, 20_000L));
        assertEquals(5_000L, GodWorshipRules.factualActiveMillis(state, 1_000L, 6_000L));
    }

    @Test
    void allDamageIsAppliedExactlyOncePerLayerApplication() {
        BigDecimal channel = GodWorshipRules.godLayerMultiplier(Arrays.asList(new BigDecimal("0.20")));
        BigDecimal all = GodWorshipRules.godLayerMultiplier(Arrays.asList(new BigDecimal("0.50")));
        assertEquals(new BigDecimal("180.0000"),
                GodWorshipRules.applyGodLayer(new BigDecimal("100"), channel, all).setScale(4));
    }

    @Test
    void cooldownClicksOnlyMutateCooldownPhase() {
        State state = unlockedCharging();
        assertEquals(0L, GodWorshipRules.reduceCooldown(state, 1_000L, 2_000L));
        state.phase = Phase.COOLDOWN;
        state.cooldownUntilMillis = 6_000L;
        assertEquals(2_000L, GodWorshipRules.reduceCooldown(state, 1_000L, 2_000L));
        assertEquals(4_000L, state.cooldownUntilMillis);
        assertEquals(3_000L, GodWorshipRules.reduceCooldown(state, 1_000L, 10_000L));
        assertEquals(Phase.CHARGE, state.phase);
    }

    @Test
    void selfLinkIsRejectedButCyclesRemainRepresentable() {
        GodWorshipModel.Preference preference = new GodWorshipModel.Preference();
        preference.activationMode = ActivationMode.LINKED_TO_GOD;
        preference.linkedGodId = GodId.GOD_1;
        assertThrows(IllegalArgumentException.class, () -> preference.validateFor(GodId.GOD_1));
        preference.linkedGodId = GodId.GOD_2;
        assertDoesNotThrow(() -> preference.validateFor(GodId.GOD_1));
    }

    private static State unlockedCharging() {
        State state = new State();
        state.unlocked = true;
        state.level = 1L;
        state.phase = Phase.CHARGE;
        return state;
    }
}
