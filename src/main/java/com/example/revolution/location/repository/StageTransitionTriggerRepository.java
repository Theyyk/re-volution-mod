package com.example.revolution.location.repository;

import com.example.revolution.location.definition.StageTransitionTriggerDefinition;
import com.example.revolution.location.definition.StageTransitionTriggerDefinition.Direction;
import java.util.List;

/** Persistence port for configurable stage boundary-ring triggers. */
public interface StageTransitionTriggerRepository {
    List<StageTransitionTriggerDefinition> getAll();
    StageTransitionTriggerDefinition get(int worldLocationId, int sourceMobSlotId, Direction direction);
    void upsert(StageTransitionTriggerDefinition trigger);
    void delete(int worldLocationId, int sourceMobSlotId, Direction direction);
}
