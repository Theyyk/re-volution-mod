package com.example.customguimod.rune;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.List;
import java.util.UUID;

/**
 * Persistent v1.2.9 slot enhancement storage.
 * Enhancement belongs to a fixed rune slot inside a status/deck and intentionally survives card resets.
 */
public final class RuneEnhancementManager {
    private static MongoClient client;
    private static MongoCollection<Document> players;

    private RuneEnhancementManager() {}

    private static synchronized void ensureConnected() {
        if (client != null) return;
        client = MongoClients.create(System.getProperty("customguimod.mongo.uri", "mongodb://localhost:27017"));
        players = client
                .getDatabase(System.getProperty("customguimod.mongo.database", "MyProject_build"))
                .getCollection("players");
    }

    public static int getLevel(UUID uuid, int collectionIndex, int runeIndex) {
        if (uuid == null || collectionIndex < 0 || runeIndex < 0 || runeIndex >= RuneInventory.BOOST_TYPE_COUNT) return 0;
        ensureConnected();
        Document player = players.find(new Document("_id", uuid.toString())).first();
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

    public static int[] getLevels(UUID uuid, int collectionIndex) {
        int[] result = new int[RuneInventory.BOOST_TYPE_COUNT];
        if (uuid == null || collectionIndex < 0) return result;
        ensureConnected();
        Document player = players.find(new Document("_id", uuid.toString())).first();
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

    public static int addLevels(UUID uuid, int collectionIndex, int runeIndex, int amount) {
        if (uuid == null || collectionIndex < 0 || runeIndex < 0 || runeIndex >= RuneInventory.BOOST_TYPE_COUNT || amount <= 0) {
            return getLevel(uuid, collectionIndex, runeIndex);
        }
        ensureConnected();
        int current = getLevel(uuid, collectionIndex, runeIndex);
        long target = (long) current + amount;
        int safeAmount = target > Integer.MAX_VALUE ? Integer.MAX_VALUE - current : amount;
        if (safeAmount <= 0) return current;

        String field = "decks." + collectionIndex + ".runeEnhancements." + runeKey(runeIndex);
        players.updateOne(
                new Document("_id", uuid.toString()),
                new Document("$inc", new Document(field, safeAmount))
        );
        return getLevel(uuid, collectionIndex, runeIndex);
    }

    /** Resets every fixed-slot enhancement in one deck without touching owned runes or ranks. */
    public static void resetCollection(UUID uuid, int collectionIndex) {
        if (uuid == null || collectionIndex < 0) return;
        ensureConnected();
        String field = "decks." + collectionIndex + ".runeEnhancements";
        players.updateOne(
                new Document("_id", uuid.toString()),
                new Document("$unset", new Document(field, ""))
        );
    }

    private static String runeKey(int runeIndex) {
        return Integer.toString(runeIndex);
    }
}
