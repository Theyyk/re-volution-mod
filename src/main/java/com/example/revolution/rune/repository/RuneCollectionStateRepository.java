package com.example.revolution.rune.repository;

import com.example.revolution.rune.definition.RuneCollectionId;
import java.util.Set;
import java.util.UUID;

/** Persistent one-way unlock state for the fixed v2.3.0 rune collection chain. */
public interface RuneCollectionStateRepository {
    Set<RuneCollectionId> getUnlockedCollections(UUID uuid);

    /** Returns true only when this call persisted a new permanent unlock. */
    boolean unlockCollection(UUID uuid, RuneCollectionId collectionId);
}
