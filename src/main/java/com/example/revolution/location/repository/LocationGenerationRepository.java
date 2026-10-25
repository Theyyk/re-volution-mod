package com.example.revolution.location.repository;

import com.example.revolution.location.generation.LocationGenerationPlan;

/** Generation provenance; storage must be available before any world mutation. */
public interface LocationGenerationRepository {
    void requireAvailable();
    void save(LocationGenerationPlan plan);
    void delete(int internalLocationId);
}
