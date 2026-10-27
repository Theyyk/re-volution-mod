package com.example.revolution.location.service;

import com.example.revolution.location.generation.LocationGenerationPlan;
import com.example.revolution.location.repository.LocationGenerationRepository;

/** Decorates generation persistence so a committed new stage receives canonical gameplay metadata. */
public final class BootstrappingLocationGenerationRepository implements LocationGenerationRepository {
    private final LocationGenerationRepository delegate;
    private final StageMobBootstrapService stageMobs;
    private final StageBossLayoutService bossLayout;

    public BootstrappingLocationGenerationRepository(
            LocationGenerationRepository delegate,
            StageMobBootstrapService stageMobs,
            StageBossLayoutService bossLayout) {
        if (delegate == null) throw new IllegalArgumentException("delegate is required");
        if (stageMobs == null) throw new IllegalArgumentException("stageMobs is required");
        if (bossLayout == null) throw new IllegalArgumentException("bossLayout is required");
        this.delegate = delegate;
        this.stageMobs = stageMobs;
        this.bossLayout = bossLayout;
    }

    @Override public void requireAvailable() { delegate.requireAvailable(); }

    @Override
    public void save(LocationGenerationPlan plan) {
        delegate.save(plan);
        stageMobs.bootstrapNewStage(
                plan.getWorldLocationId(),
                plan.getMobSlotId(),
                plan.getCenterY());
        bossLayout.reconcileLocation(plan.getWorldLocationId());
    }

    @Override public void delete(int internalLocationId) { delegate.delete(internalLocationId); }

    @Override public Double getAnchorY(int internalLocationId) { return delegate.getAnchorY(internalLocationId); }
}
