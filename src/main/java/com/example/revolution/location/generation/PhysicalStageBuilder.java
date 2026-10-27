package com.example.revolution.location.generation;

/** World boundary: prepare is read-only; rollback restores this build's own snapshot. */
public interface PhysicalStageBuilder {
    PreparedBuild prepare(LocationGenerationPlan plan);

    interface PreparedBuild {
        void apply();
        void rollback();
    }
}
