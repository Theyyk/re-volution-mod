package com.example.revolution.location.repository;

import com.example.revolution.location.generation.LocationGenerationPlan;
import com.example.revolution.location.service.StageMobBootstrapService;

/** Decorates generation persistence so a committed new stage immediately receives its canonical mob population. */
public final class BootstrappingLocationGenerationRepository implements LocationGenerationRepository {
    private final LocationGenerationRepository delegate;
    private final StageMobBootstrapService stageMobs;

    public BootstrappingLocationGenerationRepository(
            LocationGenerationRepository delegate,
            StageMobBootstrapService stageMobs) {
        if (delegate == null) throw new IllegalArgumentException("delegate is required");
        if (stageMobs == null) throw new IllegalArgumentException("stageMobs is required");
        this.delegate = delegate;
        this.stageMobs = stageMobs;
    }

    @Override public void requireAvailable() { delegate.requireAvailable(); }

    @Override
    public void save(LocationGenerationPlan plan) {
        delegate.save(plan);
        stageMobs.bootstrapNewStage(
                plan.getWorldLocationId(),
                plan.getMobSlotId(),
                plan.getCenterY());
    }

    @Override public void delete(int internalLocationId) { delegate.delete(internalLocationId); }

    @Override public Double getAnchorY(int internalLocationId) { return delegate.getAnchorY(internalLocationId); }
}
