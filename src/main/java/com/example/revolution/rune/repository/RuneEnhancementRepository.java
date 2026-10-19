package com.example.revolution.rune.repository;

import java.util.UUID;

/** Persistence port. Implementations preserve the existing document contract. */
public interface RuneEnhancementRepository {
    int getLevel(UUID uuid, int collectionIndex, int runeIndex);
    int[] getLevels(UUID uuid, int collectionIndex);
    int addLevels(UUID uuid, int collectionIndex, int runeIndex, int amount);
    void resetCollection(UUID uuid, int collectionIndex);
}
