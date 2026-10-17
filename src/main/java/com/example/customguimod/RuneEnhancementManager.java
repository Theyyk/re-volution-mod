package com.example.customguimod;

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
final class RuneEnhancementManager {
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

    static int getLevel(UUID uuid, int deckIndex, int runeIndex) {
        if (uuid == null || deckIndex < 0 || runeIndex < 0 || runeIndex >= RuneInventory.BOOST_TYPE_COUNT) return 0;
        ensureConnected();
        Document player = players.find(new Document("_id", uuid.toString())).first();
        if (player == null) return 0;

        Object rawDecks = player.get("decks");
        if (!(rawDecks instanceof List)) return 0;
        List<?> decks = (List<?>) rawDecks;
        if (deckIndex >= decks.size() || !(decks.get(deckIndex) instanceof Document)) return 0;

        Document deck = (Document) decks.get(deckIndex);
        Document enhancements = deck.get("runeEnhancements", Document.class);
        if (enhancements == null) return 0;
        Number level = enhancements.get(runeKey(runeIndex), Number.class);
        return level == null ? 0 : Math.max(0, level.intValue());
    }

    static int[] getLevels(UUID uuid, int deckIndex) {
        int[] result = new int[RuneInventory.BOOST_TYPE_COUNT];
        if (uuid == null || deckIndex < 0) return result;
        ensureConnected();
        Document player = players.find(new Document("_id", uuid.toString())).first();
        if (player == null) return result;

        Object rawDecks = player.get("decks");
        if (!(rawDecks instanceof List)) return result;
        List<?> decks = (List<?>) rawDecks;
        if (deckIndex >= decks.size() || !(decks.get(deckIndex) instanceof Document)) return result;

        Document enhancements = ((Document) decks.get(deckIndex)).get("runeEnhancements", Document.class);
        if (enhancements == null) return result;
        for (int rune = 0; rune < result.length; rune++) {
            Number level = enhancements.get(runeKey(rune), Number.class);
            if (level != null) result[rune] = Math.max(0, level.intValue());
        }
        return result;
    }

    static int addLevels(UUID uuid, int deckIndex, int runeIndex, int amount) {
        if (uuid == null || deckIndex < 0 || runeIndex < 0 || runeIndex >= RuneInventory.BOOST_TYPE_COUNT || amount <= 0) {
            return getLevel(uuid, deckIndex, runeIndex);
        }
        ensureConnected();
        int current = getLevel(uuid, deckIndex, runeIndex);
        long target = (long) current + amount;
        int safeAmount = target > Integer.MAX_VALUE ? Integer.MAX_VALUE - current : amount;
        if (safeAmount <= 0) return current;

        String field = "decks." + deckIndex + ".runeEnhancements." + runeKey(runeIndex);
        players.updateOne(
                new Document("_id", uuid.toString()),
                new Document("$inc", new Document(field, safeAmount))
        );
        return getLevel(uuid, deckIndex, runeIndex);
    }

    /** Resets every fixed-slot enhancement in one deck without touching owned runes or ranks. */
    static void resetDeck(UUID uuid, int deckIndex) {
        if (uuid == null || deckIndex < 0) return;
        ensureConnected();
        String field = "decks." + deckIndex + ".runeEnhancements";
        players.updateOne(
                new Document("_id", uuid.toString()),
                new Document("$unset", new Document(field, ""))
        );
    }

    private static String runeKey(int runeIndex) {
        return Integer.toString(runeIndex);
    }
}
