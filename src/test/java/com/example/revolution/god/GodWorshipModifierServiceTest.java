package com.example.revolution.god;

import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.god.GodWorshipModel.GodId;
import com.example.revolution.god.GodWorshipModel.PersistentState;
import com.example.revolution.god.GodWorshipModel.Phase;
import com.example.revolution.god.GodWorshipModel.RunState;
import com.example.revolution.god.GodWorshipModel.State;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GodWorshipModifierServiceTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000030");

    @Test
    void channelAndAllDamageAreAppliedOnceInLateGodLayer() {
        FakeRepository repository = new FakeRepository();
        activate(repository.run.gods.get(GodId.GOD_1), 100_000L); // poison +10%
        activate(repository.run.gods.get(GodId.GOD_3), 100_000L); // all damage +10%
        GodWorshipModifierService service = new GodWorshipModifierService(
                repository, new BalanceGodWorshipPolicy(BalanceBootstrap.activateBundledDefaults()));

        BigDecimal result = service.apply(PLAYER, GameplayDamageSource.POISON, new BigDecimal("100"), 50_000L);
        assertEquals(0, result.compareTo(new BigDecimal("121.0000")));
    }

    @Test
    void expiredBuffDoesNotAffectDamage() {
        FakeRepository repository = new FakeRepository();
        activate(repository.run.gods.get(GodId.GOD_3), 10_000L);
        GodWorshipModifierService service = new GodWorshipModifierService(
                repository, new BalanceGodWorshipPolicy(BalanceBootstrap.activateBundledDefaults()));

        BigDecimal result = service.apply(PLAYER, GameplayDamageSource.CLICK, new BigDecimal("100"), 10_001L);
        assertEquals(0, result.compareTo(new BigDecimal("100")));
    }

    private static void activate(State state, long until) {
        state.unlocked = true;
        state.level = 1L;
        state.phase = Phase.BUFF_ACTIVE;
        state.buffActiveUntilMillis = until;
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
