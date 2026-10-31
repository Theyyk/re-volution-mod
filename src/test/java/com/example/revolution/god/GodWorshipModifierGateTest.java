package com.example.revolution.god;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.god.GodWorshipModel.FeatureGate;
import com.example.revolution.god.GodWorshipModel.GodId;
import com.example.revolution.god.GodWorshipModel.PersistentState;
import com.example.revolution.god.GodWorshipModel.Phase;
import com.example.revolution.god.GodWorshipModel.RunState;
import com.example.revolution.god.GodWorshipModel.State;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GodWorshipModifierGateTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000032");

    @Test
    void secondaryChannelsAreIgnoredWhileGateIsDisabled() {
        FakeRepository repository = activeClickSecondaryGods();
        FakePolicy policy = new FakePolicy(false);
        GodWorshipModifierService service = new GodWorshipModifierService(repository, policy);

        assertEquals(0, service.apply(PLAYER, GameplayDamageSource.CLICK, new BigDecimal("100"), 5_000L)
                .compareTo(new BigDecimal("100")));
    }

    @Test
    void sameChannelSecondaryGodBuffsStackAdditivelyWithSameX() {
        FakeRepository repository = activeClickSecondaryGods();
        FakePolicy policy = new FakePolicy(true);
        GodWorshipModifierService service = new GodWorshipModifierService(repository, policy);

        assertEquals(0, service.apply(PLAYER, GameplayDamageSource.CLICK, new BigDecimal("100"), 5_000L)
                .compareTo(new BigDecimal("120.0")));
    }

    @Test
    void effectivenessExtensionScalesSpecificGodChannelWithoutChangingBasePolicy() {
        FakeRepository repository = new FakeRepository();
        activate(repository.run.gods.get(GodId.GOD_1));
        FakePolicy policy = new FakePolicy(false);
        GodWorshipModifierService service = new GodWorshipModifierService(repository, policy,
                (playerUuid, god, channel) -> god == GodId.GOD_1
                        && channel == EffectResolver.Stat.POISON_DAMAGE_MULTIPLIER
                        ? new BigDecimal("2") : BigDecimal.ONE);

        assertEquals(0, service.apply(PLAYER, GameplayDamageSource.POISON, new BigDecimal("100"), 5_000L)
                .compareTo(new BigDecimal("120.0")));
    }

    private static FakeRepository activeClickSecondaryGods() {
        FakeRepository repository = new FakeRepository();
        activate(repository.run.gods.get(GodId.GOD_1));
        activate(repository.run.gods.get(GodId.GOD_5));
        return repository;
    }

    private static void activate(State state) {
        state.unlocked = true;
        state.level = 1L;
        state.phase = Phase.BUFF_ACTIVE;
        state.buffActiveUntilMillis = 10_000L;
    }

    private static final class FakePolicy implements GodWorshipModifierService.Policy {
        private final boolean secondary;
        FakePolicy(boolean secondary) { this.secondary = secondary; }
        @Override public boolean enabled(GodId god) { return true; }
        @Override public boolean featureEnabled(FeatureGate gate) {
            return gate == FeatureGate.GOD_SECONDARY_BUFFS && secondary;
        }
        @Override public EffectResolver.Stat primaryChannel(GodId god) {
            if (god == GodId.GOD_1) return EffectResolver.Stat.POISON_DAMAGE_MULTIPLIER;
            if (god == GodId.GOD_5) return EffectResolver.Stat.BLEEDING_DAMAGE_MULTIPLIER;
            return EffectResolver.Stat.ALL_DAMAGE_MULTIPLIER;
        }
        @Override public EffectResolver.Stat secondaryChannel(GodId god) {
            return god == GodId.GOD_1 || god == GodId.GOD_5
                    ? EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER : null;
        }
        @Override public BigDecimal buffAdditiveMultiplier(GodId god, long level) { return new BigDecimal("0.10"); }
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
}
