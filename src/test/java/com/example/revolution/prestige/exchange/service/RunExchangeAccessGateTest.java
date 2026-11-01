package com.example.revolution.prestige.exchange.service;

import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.pet.definition.PetCatalog;
import com.example.revolution.pet.repository.PetRepository;
import com.example.revolution.pet.service.PetBalanceService;
import com.example.revolution.pet.service.PetService;
import com.example.revolution.pet.state.PetPlayerState;
import com.example.revolution.prestige.PrestigeBranch;
import com.example.revolution.prestige.PrestigeBranchRegistry;
import com.example.revolution.prestige.PrestigeRepository;
import com.example.revolution.prestige.PrestigeService;
import com.example.revolution.prestige.PrestigeState;
import com.example.revolution.resource.repository.ResourceRepository;
import com.example.revolution.resource.service.ResourceService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RunExchangeAccessGateTest {
    @Test void lockedExchangeRejectsPreviewAndMutationWithoutDestroyingRunState() {
        UUID player = UUID.randomUUID();
        MemoryPrestigeRepository prestigeRepository = new MemoryPrestigeRepository();
        MemoryResourceRepository resourceRepository = new MemoryResourceRepository();
        BalanceService balance = BalanceBootstrap.activateBundledDefaults();
        PrestigeService prestige = new PrestigeService(prestigeRepository, new PrestigeBranchRegistry());
        ResourceService resources = new ResourceService(resourceRepository);
        PetService pets = new PetService(new MemoryPetRepository(), new PetCatalog(), new PetBalanceService(balance), resources, prestige);
        PrestigeState.RunState run = prestige.startRun(player, PrestigeBranch.CLICK);
        RunExchangeService exchange = new RunExchangeService(prestige, resources, pets, balance, RunExchangeCatalogFactory.fromBalance(balance));

        exchange.setAccessGate(uuid -> false);
        assertNull(exchange.preview(player, run.runId, "coins_fragments"));
        assertEquals(RunExchangeService.Result.LOCKED, exchange.exchange(player, run.runId, "coins_fragments"));
        assertTrue(prestige.activeRun(player).runExchangePurchaseCounts.isEmpty());
        assertEquals(run.runId, prestige.activeRun(player).runId);

        exchange.setAccessGate(uuid -> true);
        assertNotNull(exchange.preview(player, run.runId, "coins_fragments"));
    }

    private static final class MemoryPrestigeRepository implements PrestigeRepository {
        private final Map<UUID, PrestigeState> states = new HashMap<>();
        @Override public PrestigeState load(UUID playerUuid) {
            PrestigeState state = states.get(playerUuid);
            return state == null ? new PrestigeState() : state.copy();
        }
        @Override public void save(UUID playerUuid, PrestigeState state) { states.put(playerUuid, state.copy()); }
        @Override public ResetCommitResult commitReset(UUID playerUuid, String expectedRunId, PrestigeState state) {
            states.put(playerUuid, state.copy());
            return ResetCommitResult.APPLIED;
        }
    }

    private static final class MemoryResourceRepository implements ResourceRepository {
        private final Map<UUID, Map<String, BigDecimal>> values = new HashMap<>();
        @Override public void purgeDeprecatedLightnings() {}
        @Override public void purgeDeprecatedLightnings(UUID uuid) {}
        @Override public BigDecimal getPlayerResourceValue(UUID uuid, String resource) {
            Map<String, BigDecimal> player = values.get(uuid);
            if (player == null) return BigDecimal.ZERO;
            BigDecimal value = player.get(resource);
            return value == null ? BigDecimal.ZERO : value;
        }
        @Override public void writePlayerResource(UUID uuid, String canonical, BigDecimal value) {
            values.computeIfAbsent(uuid, ignored -> new HashMap<>()).put(canonical, value);
        }
    }

    private static final class MemoryPetRepository implements PetRepository {
        private final Map<UUID, PetPlayerState> states = new HashMap<>();
        @Override public PetPlayerState load(UUID playerUuid) {
            PetPlayerState state = states.get(playerUuid);
            return state == null ? new PetPlayerState() : state.copy();
        }
        @Override public void save(UUID playerUuid, PetPlayerState state) { states.put(playerUuid, state.copy()); }
    }
}
