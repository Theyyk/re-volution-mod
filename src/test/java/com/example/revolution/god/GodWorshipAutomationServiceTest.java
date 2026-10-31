package com.example.revolution.god;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.god.GodWorshipModel.ActivationMode;
import com.example.revolution.god.GodWorshipModel.FeatureGate;
import com.example.revolution.god.GodWorshipModel.GodId;
import com.example.revolution.god.GodWorshipModel.PersistentState;
import com.example.revolution.god.GodWorshipModel.Phase;
import com.example.revolution.god.GodWorshipModel.RunState;
import com.example.revolution.god.GodWorshipModel.State;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GodWorshipAutomationServiceTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000028");

    @Test
    void offlineOfferingSettlementStopsAtFactualBuffExpiryAndIsIdempotent() {
        FakeRepository repository = new FakeRepository();
        FakePolicy policy = new FakePolicy();
        policy.offeringRate = new BigDecimal("2");
        State state = repository.run.gods.get(GodId.GOD_1);
        state.unlocked = true;
        state.level = 1;
        state.phase = Phase.BUFF_ACTIVE;
        state.buffActiveUntilMillis = 10_000L;
        state.offeringSettledUntilMillis = 4_000L;

        GodWorshipAutomationService service = new GodWorshipAutomationService(repository, policy);
        GodWorshipAutomationService.TickResult first = service.tick(PLAYER, 20_000L);
        GodWorshipAutomationService.TickResult second = service.tick(PLAYER, 30_000L);

        assertEquals(0, first.offeringsEarned.compareTo(new BigDecimal("12.000000000000")));
        assertEquals(0, repository.persistent.offerings.compareTo(new BigDecimal("12.000000000000")));
        assertEquals(0, second.offeringsEarned.signum());
        assertEquals(Phase.COOLDOWN, repository.run.gods.get(GodId.GOD_1).phase);
        assertEquals(15_000L, repository.run.gods.get(GodId.GOD_1).cooldownUntilMillis);
    }

    @Test
    void manualModeAutoWorshipCannotPerformFinalManualChargeStep() {
        FakeRepository repository = new FakeRepository();
        FakePolicy policy = new FakePolicy();
        repository.persistent.offerings = BigDecimal.TEN;
        State state = repository.run.gods.get(GodId.GOD_1);
        state.unlocked = true;
        state.level = 1;
        state.phase = Phase.CHARGE;
        state.chargeProgress = 8L;
        repository.persistent.preferences.get(GodId.GOD_1).autoWorshipEnabled = true;
        repository.persistent.preferences.get(GodId.GOD_1).activationMode = ActivationMode.MANUAL;

        GodWorshipAutomationService service = new GodWorshipAutomationService(repository, policy);
        service.tick(PLAYER, 1_000L);
        service.tick(PLAYER, 2_000L);

        assertEquals(9L, state.chargeProgress);
        assertEquals(Phase.CHARGE, state.phase);
        assertEquals(0, repository.persistent.offerings.compareTo(new BigDecimal("9")));
    }

    @Test
    void passiveChargeExistsOnlyWhileGodPanelIsOpenAndRetainsFraction() {
        FakeRepository repository = new FakeRepository();
        FakePolicy policy = new FakePolicy();
        policy.passiveRate = new BigDecimal("0.5");
        State state = repository.run.gods.get(GodId.GOD_1);
        state.unlocked = true;
        state.level = 1;
        state.phase = Phase.CHARGE;
        repository.persistent.preferences.get(GodId.GOD_1).activationMode = ActivationMode.MANUAL;
        GodWorshipAutomationService service = new GodWorshipAutomationService(repository, policy);

        service.tick(PLAYER, 1_000L);
        service.tick(PLAYER, 2_000L);
        assertEquals(0L, state.chargeProgress);
        assertEquals(0, state.passiveChargeRemainder.signum());

        service.setMenuOpen(PLAYER, true, 2_000L);
        service.tick(PLAYER, 3_000L);
        assertEquals(0L, state.chargeProgress);
        assertEquals(0, state.passiveChargeRemainder.compareTo(new BigDecimal("0.500000000000")));
        service.tick(PLAYER, 4_000L);
        assertEquals(1L, state.chargeProgress);
        assertEquals(0, state.passiveChargeRemainder.signum());

        service.setMenuOpen(PLAYER, false, 4_000L);
        service.tick(PLAYER, 100_000L);
        assertEquals(1L, state.chargeProgress);
    }

    @Test
    void autoActivationPropagatesThroughLinkedGodsOnce() {
        FakeRepository repository = new FakeRepository();
        FakePolicy policy = new FakePolicy();
        policy.gates.add(FeatureGate.GOD_AUTO_ACTIVATION);
        for (GodId god : new GodId[]{GodId.GOD_1, GodId.GOD_2, GodId.GOD_3}) {
            State state = repository.run.gods.get(god);
            state.unlocked = true;
            state.level = 1;
            state.phase = Phase.CHARGE;
            state.chargeProgress = 10L;
        }
        repository.persistent.preferences.get(GodId.GOD_1).activationMode = ActivationMode.AUTO_WHEN_READY;
        repository.persistent.preferences.get(GodId.GOD_2).activationMode = ActivationMode.LINKED_TO_GOD;
        repository.persistent.preferences.get(GodId.GOD_2).linkedGodId = GodId.GOD_1;
        repository.persistent.preferences.get(GodId.GOD_3).activationMode = ActivationMode.LINKED_TO_GOD;
        repository.persistent.preferences.get(GodId.GOD_3).linkedGodId = GodId.GOD_2;

        GodWorshipAutomationService service = new GodWorshipAutomationService(repository, policy);
        GodWorshipAutomationService.TickResult result = service.tick(PLAYER, 50_000L);

        assertEquals(3, result.activations);
        assertEquals(Phase.BUFF_ACTIVE, repository.run.gods.get(GodId.GOD_1).phase);
        assertEquals(Phase.BUFF_ACTIVE, repository.run.gods.get(GodId.GOD_2).phase);
        assertEquals(Phase.BUFF_ACTIVE, repository.run.gods.get(GodId.GOD_3).phase);
        assertEquals(50_000L, repository.run.gods.get(GodId.GOD_3).offeringSettledUntilMillis);
    }

    @Test
    void invalidSelfLinkDoesNotOverwriteExistingPreference() {
        FakeRepository repository = new FakeRepository();
        FakePolicy policy = new FakePolicy();
        policy.gates.add(FeatureGate.GOD_AUTO_ACTIVATION);
        GodWorshipAutomationService service = new GodWorshipAutomationService(repository, policy);

        assertFalse(service.setActivationMode(PLAYER, GodId.GOD_1, ActivationMode.LINKED_TO_GOD, GodId.GOD_1));
        assertEquals(ActivationMode.MANUAL, repository.persistent.preferences.get(GodId.GOD_1).activationMode);
        assertTrue(service.setActivationMode(PLAYER, GodId.GOD_1, ActivationMode.LINKED_TO_GOD, GodId.GOD_2));
        assertEquals(GodId.GOD_2, repository.persistent.preferences.get(GodId.GOD_1).linkedGodId);
    }

    private static final class FakeRepository implements GodWorshipRepository {
        private RunState run = new RunState();
        private PersistentState persistent = new PersistentState();

        @Override public RunState loadRunState(UUID playerUuid) { return run; }
        @Override public PersistentState loadPersistentState(UUID playerUuid) { return persistent; }
        @Override public void saveRunState(UUID playerUuid, RunState state) { run = state; }
        @Override public void savePersistentState(UUID playerUuid, PersistentState state) { persistent = state; }
        @Override public void resetRunState(UUID playerUuid) { run = new RunState(); }
    }

    private static final class FakePolicy implements GodWorshipAutomationService.Policy {
        private final Set<FeatureGate> gates = EnumSet.noneOf(FeatureGate.class);
        private BigDecimal offeringRate = BigDecimal.ONE;
        private BigDecimal passiveRate = BigDecimal.ZERO;

        @Override public boolean enabled(GodId god) { return true; }
        @Override public String unlockResource(GodId god) { return "coins"; }
        @Override public String upgradeResource(GodId god) { return "coins"; }
        @Override public EffectResolver.Stat primaryChannel(GodId god) { return EffectResolver.Stat.ALL_DAMAGE_MULTIPLIER; }
        @Override public EffectResolver.Stat secondaryChannel(GodId god) { return null; }
        @Override public BigDecimal unlockCost(GodId god) { return BigDecimal.ONE; }
        @Override public BigDecimal upgradeCost(GodId god, long currentLevel) { return BigDecimal.ONE; }
        @Override public long chargeRequired(GodId god) { return 10L; }
        @Override public long chargePerClick(GodId god) { return 1L; }
        @Override public BigDecimal passiveChargePerSecond(GodId god) { return passiveRate; }
        @Override public long buffDurationMillis(GodId god) { return 10_000L; }
        @Override public long cooldownDurationMillis(GodId god) { return 5_000L; }
        @Override public long cooldownReductionPerClickMillis(GodId god) { return 1_000L; }
        @Override public int clickRateLimitPerSecond(GodId god) { return 25; }
        @Override public long permanentDurationMillis(long permanentDurationLevel) { return permanentDurationLevel * 1_000L; }
        @Override public boolean featureEnabled(FeatureGate gate) { return gates.contains(gate); }
        @Override public BigDecimal offeringGenerationPerSecond() { return offeringRate; }
        @Override public long autoWorshipIntervalMillis() { return 1_000L; }
        @Override public BigDecimal autoWorshipCooldownCost() { return BigDecimal.ONE; }
        @Override public long autoWorshipCooldownReductionMillis() { return 1_000L; }
        @Override public BigDecimal autoWorshipChargeCost() { return BigDecimal.ONE; }
        @Override public long autoWorshipChargeGain() { return 1L; }
        @Override public BigDecimal durationUpgradeCost(long currentLevel) { return BigDecimal.TEN; }
    }
}
