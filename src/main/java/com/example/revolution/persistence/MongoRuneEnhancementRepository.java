package com.example.revolution.persistence;

import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.state.RuneInventory;
import java.util.List;
import java.util.UUID;
import org.bson.Document;

public final class MongoRuneEnhancementRepository implements RuneEnhancementRepository {
    private final MongoConnection connection;

    public MongoRuneEnhancementRepository(MongoConnection connection) { this.connection = connection; }

    @Override
    public int getLevel(UUID uuid, int collectionIndex, int runeIndex) {
        if (uuid == null || collectionIndex < 0 || runeIndex < 0 || runeIndex >= RuneInventory.BOOST_TYPE_COUNT) return 0;

        Document player = connection.collection("players").find(new Document("_id", uuid.toString())).first();
        if (player == null) return 0;

        Object rawCollections = player.get("decks");
        if (!(rawCollections instanceof List)) return 0;
        List<?> collections = (List<?>) rawCollections;
        if (collectionIndex >= collections.size() || !(collections.get(collectionIndex) instanceof Document)) return 0;

        Document collection = (Document) collections.get(collectionIndex);
        Document enhancements = collection.get("runeEnhancements", Document.class);
        if (enhancements == null) return 0;
        Number level = enhancements.get(runeKey(runeIndex), Number.class);
        return level == null ? 0 : Math.max(0, level.intValue());
    }

    @Override
    public int[] getLevels(UUID uuid, int collectionIndex) {
        int[] result = new int[RuneInventory.BOOST_TYPE_COUNT];
        if (uuid == null || collectionIndex < 0) return result;

        Document player = connection.collection("players").find(new Document("_id", uuid.toString())).first();
        if (player == null) return result;

        Object rawCollections = player.get("decks");
        if (!(rawCollections instanceof List)) return result;
        List<?> collections = (List<?>) rawCollections;
        if (collectionIndex >= collections.size() || !(collections.get(collectionIndex) instanceof Document)) return result;

        Document enhancements = ((Document) collections.get(collectionIndex)).get("runeEnhancements", Document.class);
        if (enhancements == null) return result;
        for (int rune = 0; rune < result.length; rune++) {
            Number level = enhancements.get(runeKey(rune), Number.class);
            if (level != null) result[rune] = Math.max(0, level.intValue());
        }
        return result;
    }

    @Override
    public int addLevels(UUID uuid, int collectionIndex, int runeIndex, int amount) {
        if (uuid == null || collectionIndex < 0 || runeIndex < 0 || runeIndex >= RuneInventory.BOOST_TYPE_COUNT || amount <= 0) {
            return getLevel(uuid, collectionIndex, runeIndex);
        }

        int current = getLevel(uuid, collectionIndex, runeIndex);
        long target = (long) current + amount;
        int safeAmount = target > Integer.MAX_VALUE ? Integer.MAX_VALUE - current : amount;
        if (safeAmount <= 0) return current;

        String field = "decks." + collectionIndex + ".runeEnhancements." + runeKey(runeIndex);
        connection.collection("players").updateOne(
                new Document("_id", uuid.toString()),
                new Document("$inc", new Document(field, safeAmount))
        );
        return getLevel(uuid, collectionIndex, runeIndex);
    }

    @Override
    public void resetCollection(UUID uuid, int collectionIndex) {
        if (uuid == null || collectionIndex < 0) return;

        String field = "decks." + collectionIndex + ".runeEnhancements";
        connection.collection("players").updateOne(
                new Document("_id", uuid.toString()),
                new Document("$unset", new Document(field, ""))
        );
    }

    private String runeKey(int runeIndex) {
        return Integer.toString(runeIndex);
    }
}
