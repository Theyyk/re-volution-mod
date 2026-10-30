package com.example.revolution.prestige.exchange.service;

import com.example.revolution.prestige.PrestigeService;
import com.example.revolution.prestige.PrestigeState;
import com.example.revolution.prestige.exchange.definition.RunExchangeCatalog;
import com.example.revolution.prestige.exchange.definition.RunExchangeOffer;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.resource.service.ResourceService;
import java.util.UUID;

/**
 * Server-authoritative foundation for spending run-scoped Fragments of Knowledge on game resources.
 * v2.4.0 intentionally registers no concrete offers until rates/products receive a separate product decision.
 */
public final class RunExchangeService {
    public enum Result { APPLIED, UNKNOWN_OFFER, NO_ACTIVE_RUN, STALE_RUN, INSUFFICIENT_FRAGMENTS, INVALID_TARGET }

    private final PrestigeService prestige;
    private final ResourceService resources;
    private final RunExchangeCatalog catalog;

    public RunExchangeService(PrestigeService prestige, ResourceService resources, RunExchangeCatalog catalog) {
        if (prestige == null || resources == null || catalog == null) throw new IllegalArgumentException("Run Exchange dependencies are required");
        this.prestige = prestige;
        this.resources = resources;
        this.catalog = catalog;
    }

    public synchronized Result exchange(UUID playerUuid, String expectedRunId, String offerId) {
        RunExchangeOffer offer = catalog.get(offerId);
        if (offer == null) return Result.UNKNOWN_OFFER;
        String target = ResourceDefinitions.findCanonicalName(offer.targetResource);
        if (target == null) return Result.INVALID_TARGET;
        PrestigeState.RunState run = prestige.activeRun(playerUuid);
        if (run == null) return Result.NO_ACTIVE_RUN;
        if (expectedRunId == null || !expectedRunId.equals(run.runId)) return Result.STALE_RUN;
        if (run.runBranchCurrency.compareTo(offer.fragmentCost) < 0) return Result.INSUFFICIENT_FRAGMENTS;
        if (!prestige.spendRunBranchCurrency(playerUuid, expectedRunId, offer.fragmentCost)) return Result.STALE_RUN;
        if (!resources.grantPlayerResource(playerUuid, target, offer.targetAmount)) {
            throw new IllegalStateException("Validated Run Exchange target could not be granted");
        }
        return Result.APPLIED;
    }

    public RunExchangeCatalog catalog() { return catalog; }
}
