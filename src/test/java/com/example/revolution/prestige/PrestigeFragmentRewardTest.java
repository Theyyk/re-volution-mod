package com.example.revolution.prestige;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PrestigeFragmentRewardTest {
    @Test public void fragmentRewardProviderControlsFactualLevelRewards() {
        UUID id = UUID.randomUUID();
        MemoryRepo repo = new MemoryRepo();
        PrestigeService service = new PrestigeService(repo, new PrestigeBranchRegistry());
        service.setFragmentRewardProvider(branch -> new BigDecimal("2.5"));
        service.startRun(id, PrestigeBranch.CLICK);

        service.recordProgress(id, PrestigeBranch.CLICK, new BigDecimal("1000000000"));
        PrestigeState.RunState run = service.activeRun(id);
        assertNotNull(run);
        assertTrue(run.branchLevel > 0);
        BigDecimal expected = new BigDecimal("2.5").multiply(BigDecimal.valueOf(run.branchLevel));
        assertEquals(0, expected.compareTo(run.runBranchCurrency));
    }

    private static final class MemoryRepo implements PrestigeRepository {
        private final Map<UUID, PrestigeState> states = new HashMap<>();
        public PrestigeState load(UUID id) {
            PrestigeState state = states.get(id);
            return state == null ? new PrestigeState() : state.copy();
        }
        public void save(UUID id, PrestigeState state) { states.put(id, state.copy()); }
        public ResetCommitResult commitReset(UUID id, String expectedRunId, PrestigeState state) {
            states.put(id, state.copy());
            return ResetCommitResult.APPLIED;
        }
    }
}
