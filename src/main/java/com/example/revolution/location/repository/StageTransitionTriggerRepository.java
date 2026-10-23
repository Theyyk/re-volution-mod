package com.example.revolution.location.repository;

import com.example.revolution.location.definition.StageTransitionTriggerDefinition;
import java.util.List;

/** Persistence port for independently configurable stage-entry trigger zones. */
public interface StageTransitionTriggerRepository {
    List<StageTransitionTriggerDefinition> getAll();
    StageTransitionTriggerDefinition get(int worldLocationId, int mobSlotId);
    void upsert(StageTransitionTriggerDefinition trigger);
    void delete(int worldLocationId, int mobSlotId);
}
