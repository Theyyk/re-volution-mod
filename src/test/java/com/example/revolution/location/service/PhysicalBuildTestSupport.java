package com.example.revolution.location.service;

import com.example.revolution.location.generation.BuildTemplateCatalog;
import com.example.revolution.location.generation.LocationGenerationPlan;
import com.example.revolution.location.generation.PhysicalStageBuilder;
import com.example.revolution.location.repository.LocationBuildTemplateRepository;
import com.example.revolution.location.repository.LocationGenerationRepository;
import java.util.HashMap;
import java.util.Map;

/** In-memory construction/storage boundary used by geometry regression tests. */
final class PhysicalBuildTestSupport implements LocationBuildTemplateRepository, LocationGenerationRepository,
        BuildTemplateCatalog, PhysicalStageBuilder, PhysicalStageBuilder.PreparedBuild {
    final Map<Integer, String> templates = new HashMap<>();
    final Map<Integer, LocationGenerationPlan> saved = new HashMap<>();
    boolean valid = true;
    boolean failPrepare;
    boolean failApply;
    boolean failSave;
    boolean failDelete;
    boolean available = true;
    boolean changed;
    int templateWrites;
    int prepares;
    int applies;
    int rollbacks;
    LocationGenerationPlan prepared;

    static PhysicalBuildTestSupport configured() {
        PhysicalBuildTestSupport support = new PhysicalBuildTestSupport();
        for (int id = 0; id <= 9; id++) support.templates.put(id, "test_template");
        return support;
    }

    @Override public String get(int id) { return templates.get(id); }
    @Override public void upsert(int id, String name) { templateWrites++; templates.put(id, name); }
    @Override public void validate(String name) {
        if (!valid || !"test_template".equals(name)) throw new IllegalArgumentException("Unknown or invalid REBuilder template: " + name);
    }
    @Override public void requireAvailable() { if (!available) throw new IllegalStateException("Storage unavailable"); }
    @Override public void save(LocationGenerationPlan plan) {
        saved.put(plan.getInternalLocationId(), plan);
        if (failSave) throw new IllegalStateException("metadata write failed after server accepted it");
    }
    @Override public void delete(int id) {
        if (failDelete) throw new IllegalStateException("metadata compensation failed");
        saved.remove(id);
    }
    @Override public PreparedBuild prepare(LocationGenerationPlan plan) {
        prepares++;
        prepared = plan;
        if (failPrepare) throw new IllegalStateException("World validation failed");
        return this;
    }
    @Override public void apply() {
        applies++;
        changed = true;
        if (failApply) throw new IllegalStateException("Build interrupted after partial change");
    }
    @Override public void rollback() { rollbacks++; changed = false; }
}
