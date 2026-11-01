package com.example.revolution.prestige;

import com.example.revolution.effect.EffectResolver;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PrestigeEffectAccessGateTest {
    @Test void lockedArtsSuppressesPassiveEffectsWithoutDeletingMasteryState() {
        UUID player = UUID.randomUUID();
        MemoryRepository repository = new MemoryRepository();
        PrestigeService service = new PrestigeService(repository, new PrestigeBranchRegistry());

        PrestigeState.RunState run = service.startRun(player, PrestigeBranch.CLICK);
        service.recordProgress(player, PrestigeBranch.CLICK, new BigDecimal("100"));
        assertFalse(service.contributions(player, EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER).isEmpty());

        service.setEffectAccessGate(uuid -> false);
        assertTrue(service.contributions(player, EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER).isEmpty());
        PrestigeState.RunState lockedState = service.activeRun(player);
        assertFalse(lockedState.reachedRanks.isEmpty());
        assertTrue(run.runId.equals(lockedState.runId));

        service.setEffectAccessGate(uuid -> true);
        assertFalse(service.contributions(player, EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER).isEmpty());
    }

    private static final class MemoryRepository implements PrestigeRepository {
        private final Map<UUID, PrestigeState> states = new HashMap<>();

        @Override public PrestigeState load(UUID playerUuid) {
            PrestigeState state = states.get(playerUuid);
            return state == null ? new PrestigeState() : state.copy();
        }

        @Override public void save(UUID playerUuid, PrestigeState state) {
            states.put(playerUuid, state.copy());
        }

        @Override public ResetCommitResult commitReset(UUID playerUuid, String expectedRunId, PrestigeState state) {
            states.put(playerUuid, state.copy());
            return ResetCommitResult.APPLIED;
        }
    }
}
