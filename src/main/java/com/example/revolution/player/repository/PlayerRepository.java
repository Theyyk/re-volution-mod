package com.example.revolution.player.repository;

import com.example.revolution.rune.repository.RuneRecord;
import java.util.List;
import java.util.UUID;

/** Persistence port. Implementations preserve the existing document contract. */
public interface PlayerRepository {
    int getBalance(UUID uuid);
    void setBalance(UUID uuid, int balance);
    int getCrystals(UUID uuid);
    void setCrystals(UUID uuid, int crystals);
    List<RuneRecord> getRunesInCollection(UUID uuid, int collectionIndex);
    int getActiveCollection(UUID uuid);
    void setActiveCollection(UUID uuid, int collectionIndex);
    List<String> getCollectionNames(UUID uuid);
    void addCollection(UUID uuid, String name);
    void deleteCollection(UUID uuid, int collectionIndex);
    void addRuneToCollection(UUID uuid, int collectionIndex, int slot, int runeIndex, int layer, int level);
    boolean setRuneRank(UUID uuid, int collectionIndex, int runeIndex, int layer);
    void clearCollection(UUID uuid, int collectionIndex);
}
