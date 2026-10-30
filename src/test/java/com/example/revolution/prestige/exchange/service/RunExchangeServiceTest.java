package com.example.revolution.prestige.exchange.service;

import com.example.revolution.prestige.PrestigeBranch;
import com.example.revolution.prestige.PrestigeBranchRegistry;
import com.example.revolution.prestige.PrestigeRepository;
import com.example.revolution.prestige.PrestigeService;
import com.example.revolution.prestige.PrestigeState;
import com.example.revolution.prestige.definition.PrestigeRunResources;
import com.example.revolution.prestige.exchange.definition.RunExchangeCatalog;
import com.example.revolution.prestige.exchange.definition.RunExchangeOffer;
import com.example.revolution.resource.repository.ResourceRepository;
import com.example.revolution.resource.service.ResourceService;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RunExchangeServiceTest {
    @Test public void approvedServerOfferSpendsFragmentsAndGrantsResource() {
        UUID id = UUID.randomUUID();
        MemoryPrestigeRepo prestigeRepo = new MemoryPrestigeRepo();
        PrestigeService prestige = new PrestigeService(prestigeRepo, new PrestigeBranchRegistry());
        PrestigeState.RunState run = prestige.startRun(id, PrestigeBranch.CLICK);
        PrestigeState state = prestigeRepo.load(id);
        state.activeRun.runBranchCurrency = new BigDecimal("5");
        prestigeRepo.save(id, state);

        MemoryResourceRepo resourceRepo = new MemoryResourceRepo();
        resourceRepo.writePlayerResource(id, "coins", BigDecimal.TEN);
        RunExchangeOffer offer = new RunExchangeOffer("coins_small", new BigDecimal("2"), "coins", new BigDecimal("50"));
        RunExchangeService exchange = new RunExchangeService(
                prestige, new ResourceService(resourceRepo), new RunExchangeCatalog(Arrays.asList(offer)));

        assertEquals(RunExchangeService.Result.APPLIED, exchange.exchange(id, run.runId, "coins_small"));
        assertEquals(0, new BigDecimal("3").compareTo(prestige.activeRun(id).runBranchCurrency));
        assertEquals(0, new BigDecimal("60").compareTo(resourceRepo.getPlayerResourceValue(id, "coins")));
        assertEquals("run_branch_currency", PrestigeRunResources.FRAGMENT_OF_KNOWLEDGE_ID);
        assertEquals("Фрагменты знаний", PrestigeRunResources.FRAGMENT_OF_KNOWLEDGE_PLURAL);
    }

    @Test public void staleUnknownAndUnapprovedTargetsDoNotSpendFragments() {
        UUID id = UUID.randomUUID();
        MemoryPrestigeRepo prestigeRepo = new MemoryPrestigeRepo();
        PrestigeService prestige = new PrestigeService(prestigeRepo, new PrestigeBranchRegistry());
        PrestigeState.RunState run = prestige.startRun(id, PrestigeBranch.CLICK);
        PrestigeState state = prestigeRepo.load(id);
        state.activeRun.runBranchCurrency = new BigDecimal("5");
        prestigeRepo.save(id, state);
        MemoryResourceRepo resources = new MemoryResourceRepo();
        RunExchangeOffer invalid = new RunExchangeOffer("future", BigDecimal.ONE, "not_approved", BigDecimal.ONE);
        RunExchangeService exchange = new RunExchangeService(
                prestige, new ResourceService(resources), new RunExchangeCatalog(Arrays.asList(invalid)));

        assertEquals(RunExchangeService.Result.UNKNOWN_OFFER, exchange.exchange(id, run.runId, "missing"));
        assertEquals(RunExchangeService.Result.INVALID_TARGET, exchange.exchange(id, run.runId, "future"));
        assertEquals(RunExchangeService.Result.INVALID_TARGET, exchange.exchange(id, "stale", "future"));
        assertEquals(0, new BigDecimal("5").compareTo(prestige.activeRun(id).runBranchCurrency));
        assertTrue(RunExchangeCatalog.empty().all().isEmpty());
    }

    private static final class MemoryPrestigeRepo implements PrestigeRepository {
        private final Map<UUID, PrestigeState> states = new HashMap<>();
        public PrestigeState load(UUID id) { PrestigeState state = states.get(id); return state == null ? new PrestigeState() : state.copy(); }
        public void save(UUID id, PrestigeState state) { states.put(id, state.copy()); }
        public ResetCommitResult commitReset(UUID id, String expectedRunId, PrestigeState state) {
            states.put(id, state.copy()); return ResetCommitResult.APPLIED;
        }
    }

    private static final class MemoryResourceRepo implements ResourceRepository {
        private final Map<UUID, Map<String, BigDecimal>> values = new HashMap<>();
        public void purgeDeprecatedLightnings() {}
        public void purgeDeprecatedLightnings(UUID uuid) {}
        public BigDecimal getPlayerResourceValue(UUID uuid, String resource) {
            Map<String, BigDecimal> player = values.get(uuid);
            if (player == null) return BigDecimal.ZERO;
            BigDecimal value = player.get(resource);
            return value == null ? BigDecimal.ZERO : value;
        }
        public void writePlayerResource(UUID uuid, String canonical, BigDecimal value) {
            Map<String, BigDecimal> player = values.get(uuid);
            if (player == null) { player = new HashMap<>(); values.put(uuid, player); }
            player.put(canonical, value);
        }
    }
}
