package com.example.customguimod;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MongoManager {

    private static MongoClient client;
    private static MongoDatabase database;
    private static MongoCollection<Document> players;

    public static void connect() {
        try {
            client = MongoClients.create(System.getProperty("customguimod.mongo.uri", "mongodb://localhost:27017"));
            database = client.getDatabase(System.getProperty("customguimod.mongo.database", "MyProject_build"));
            players = database.getCollection("players");
            CustomGuiMod.logger.info("MongoDB connected!");
        } catch (Exception e) {
            CustomGuiMod.logger.error("MongoDB error: " + e.getMessage());
        }
    }

    // === БАЛАНС ===
    public static int getBalance(UUID uuid) {
        if (players == null) return 0;
        Document doc = players.find(new Document("_id", uuid.toString())).first();
        if (doc == null) return 0;
        Integer b = doc.getInteger("balance", 0);
        return b == null ? 0 : b;
    }

    public static void setBalance(UUID uuid, int balance) {
        if (players == null) return;
        players.updateOne(
                new Document("_id", uuid.toString()),
                new Document("$set", new Document("balance", balance)),
                new UpdateOptions().upsert(true)
        );
    }

    // === КРИСТАЛЛЫ ===
    public static int getCrystals(UUID uuid) {
        if (players == null) return 0;
        Document doc = players.find(new Document("_id", uuid.toString())).first();
        if (doc == null) return 0;
        Integer c = doc.getInteger("crystals", 0);
        return c == null ? 0 : c;
    }

    public static void setCrystals(UUID uuid, int crystals) {
        if (players == null) return;
        players.updateOne(
                new Document("_id", uuid.toString()),
                new Document("$set", new Document("crystals", crystals)),
                new UpdateOptions().upsert(true)
        );
    }

    // === КОЛОДЫ ===
    public static class SavedCard {
        public int slot, cardIndex, layer, level;

        public SavedCard(int slot, int cardIndex, int layer, int level) {
            this.slot = slot;
            this.cardIndex = cardIndex;
            this.layer = layer;
            this.level = level;
        }
    }

    @SuppressWarnings("unchecked")
    public static List<SavedCard> getCardsInDeck(UUID uuid, int deckIndex) {
        List<SavedCard> result = new ArrayList<>();
        if (players == null) return result;

        Document doc = players.find(new Document("_id", uuid.toString())).first();
        if (doc == null) return result;

        List<Document> decks = (List<Document>) doc.get("decks");
        if (decks == null || deckIndex < 0 || deckIndex >= decks.size()) {
            CustomGuiMod.logger.warn("getCardsInDeck: deck " + deckIndex + " not found");
            return result;
        }

        Document deck = decks.get(deckIndex);
        List<Document> cards = (List<Document>) deck.get("cards");
        if (cards == null) {
            CustomGuiMod.logger.info("getCardsInDeck: deck " + deckIndex + " has no cards field");
            return result;
        }

        for (Document c : cards) {
            result.add(new SavedCard(
                    c.getInteger("slot", 0),
                    c.getInteger("cardIndex", 0),
                    c.getInteger("layer", 1),
                    c.getInteger("level", 1)
            ));
        }

        CustomGuiMod.logger.info("getCardsInDeck: loaded " + result.size() + " cards from deck " + deckIndex);
        return result;
    }

    public static int getActiveDeck(UUID uuid) {
        if (players == null) return 0;
        Document doc = players.find(new Document("_id", uuid.toString())).first();
        if (doc == null) return 0;
        Integer d = doc.getInteger("activeDeck", 0);
        return d == null ? 0 : d;
    }

    public static void setActiveDeck(UUID uuid, int deckIndex) {
        if (players == null) return;
        players.updateOne(
                new Document("_id", uuid.toString()),
                new Document("$set", new Document("activeDeck", deckIndex)),
                new UpdateOptions().upsert(true)
        );
    }

    @SuppressWarnings("unchecked")
    public static List<String> getDeckNames(UUID uuid) {
        List<String> result = new ArrayList<>();
        if (players == null) return result;
        Document doc = players.find(new Document("_id", uuid.toString())).first();
        if (doc == null) return result;
        List<Document> decks = (List<Document>) doc.get("decks");
        if (decks == null) return result;
        for (Document d : decks) {
            result.add(d.getString("name"));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static void addDeck(UUID uuid, String name) {
        if (players == null) return;
        Document doc = players.find(new Document("_id", uuid.toString())).first();
        if (doc == null) {
            List<Document> decks = new ArrayList<>();
            decks.add(new Document("name", name).append("cards", new ArrayList<Document>()));
            players.insertOne(new Document("_id", uuid.toString())
                    .append("balance", 0)
                    .append("crystals", 0)
                    .append("activeDeck", 0)
                    .append("decks", decks));
            CustomGuiMod.logger.info("addDeck: created player doc + deck '" + name + "'");
            return;
        }
        List<Document> decks = (List<Document>) doc.get("decks");
        if (decks == null) decks = new ArrayList<>();
        decks.add(new Document("name", name).append("cards", new ArrayList<Document>()));
        players.updateOne(
                new Document("_id", uuid.toString()),
                new Document("$set", new Document("decks", decks)),
                new UpdateOptions().upsert(true)
        );
        CustomGuiMod.logger.info("addDeck: added deck '" + name + "', total decks: " + decks.size());
    }

    @SuppressWarnings("unchecked")
    public static void deleteDeck(UUID uuid, int deckIndex) {
        if (players == null) return;
        Document doc = players.find(new Document("_id", uuid.toString())).first();
        if (doc == null) return;
        List<Document> decks = (List<Document>) doc.get("decks");
        if (decks == null || deckIndex >= decks.size()) return;
        decks.remove(deckIndex);
        int active = getActiveDeck(uuid);
        if (active >= decks.size()) active = 0;
        players.updateOne(
                new Document("_id", uuid.toString()),
                new Document("$set", new Document("decks", decks).append("activeDeck", active)),
                new UpdateOptions().upsert(true)
        );
    }

    @SuppressWarnings("unchecked")
    public static void addCardToDeck(UUID uuid, int deckIndex, int slot, int cardIndex, int layer, int level) {
        if (players == null) {
            CustomGuiMod.logger.warn("addCardToDeck: players == null");
            return;
        }

        Document doc = players.find(new Document("_id", uuid.toString())).first();
        if (doc == null) {
            CustomGuiMod.logger.warn("addCardToDeck: doc not found for " + uuid);
            return;
        }

        List<Document> decks = (List<Document>) doc.get("decks");
        if (decks == null) {
            CustomGuiMod.logger.warn("addCardToDeck: decks == null");
            return;
        }
        if (deckIndex < 0 || deckIndex >= decks.size()) {
            CustomGuiMod.logger.warn("addCardToDeck: deck " + deckIndex + " not found (size: " + decks.size() + ")");
            return;
        }

        Document deck = decks.get(deckIndex);
        List<Document> cards = (List<Document>) deck.get("cards");
        if (cards == null) {
            cards = new ArrayList<>();
        }

        cards.add(new Document("slot", slot)
                .append("cardIndex", cardIndex)
                .append("layer", layer)
                .append("level", level));

        deck.put("cards", cards);
        decks.set(deckIndex, deck);

        players.updateOne(
                new Document("_id", uuid.toString()),
                new Document("$set", new Document("decks", decks)),
                new UpdateOptions().upsert(true)
        );

        CustomGuiMod.logger.info("addCardToDeck: added card to deck " + deckIndex
                + " (total in deck: " + cards.size() + ", slot=" + slot + ")");
    }

    @SuppressWarnings("unchecked")
    public static boolean setRuneRank(UUID uuid, int deckIndex, int cardIndex, int layer) {
        if (players == null) return false;
        Document doc = players.find(new Document("_id", uuid.toString())).first();
        if (doc == null) return false;

        List<Document> decks = (List<Document>) doc.get("decks");
        if (decks == null || deckIndex < 0 || deckIndex >= decks.size()) return false;

        Document deck = decks.get(deckIndex);
        List<Document> cards = (List<Document>) deck.get("cards");
        if (cards == null) return false;

        Document target = null;
        for (Document card : cards) {
            if (card.getInteger("cardIndex", -1) != cardIndex) continue;
            if (target == null || card.getInteger("layer", 1) > target.getInteger("layer", 1)) {
                target = card;
            }
        }
        if (target == null) return false;

        target.put("slot", cardIndex);
        target.put("cardIndex", cardIndex);
        target.put("layer", Math.max(RuneInventory.NORMAL_RANK, Math.min(RuneInventory.ADVANCED_RANK, layer)));
        if (!target.containsKey("level")) target.put("level", 1);

        deck.put("cards", cards);
        decks.set(deckIndex, deck);
        players.updateOne(
                new Document("_id", uuid.toString()),
                new Document("$set", new Document("decks", decks)),
                new UpdateOptions().upsert(true)
        );

        CustomGuiMod.logger.info("setRuneRank: deck=" + deckIndex + ", rune=" + cardIndex + ", rank=" + layer);
        return true;
    }

    @SuppressWarnings("unchecked")
    public static void clearDeck(UUID uuid, int deckIndex) {
        if (players == null) return;
        Document doc = players.find(new Document("_id", uuid.toString())).first();
        if (doc == null) return;
        List<Document> decks = (List<Document>) doc.get("decks");
        if (decks == null || deckIndex >= decks.size()) return;
        decks.get(deckIndex).put("cards", new ArrayList<Document>());
        players.updateOne(
                new Document("_id", uuid.toString()),
                new Document("$set", new Document("decks", decks)),
                new UpdateOptions().upsert(true)
        );
    }

    public static List<Integer> getFreeSlots(UUID uuid, int deckIndex) {
        List<Integer> occupied = new ArrayList<>();
        for (SavedCard card : getCardsInDeck(uuid, deckIndex)) {
            occupied.add(card.slot);
        }
        return RuneInventory.freePurchaseSlots(occupied);
    }

    // === МОБЫ ===
    public static class CustomMob {
        public String name;
        public String type;
        public int hp;
        public int money;
        public double x, y, z;
        public boolean enabled;

        public CustomMob(String name, String type, int hp, int money, double x, double y, double z, boolean enabled) {
            this.name = name;
            this.type = type;
            this.hp = hp;
            this.money = money;
            this.x = x;
            this.y = y;
            this.z = z;
            this.enabled = enabled;
        }
    }

    public static MongoCollection<Document> getMobsCollection() {
        return database.getCollection("custom_mobs");
    }

    public static void createMob(String name, String type, int hp, int money) {
        if (players == null) return;
        getMobsCollection().insertOne(new Document("name", name)
                .append("type", type)
                .append("hp", hp)
                .append("money", money)
                .append("x", 0.0)
                .append("y", 0.0)
                .append("z", 0.0)
                .append("enabled", false));
        CustomGuiMod.logger.info("Mob created: " + name);
    }

    public static void setMobSpawn(String name, double x, double y, double z) {
        if (players == null) return;
        getMobsCollection().updateOne(
                new Document("name", name),
                new Document("$set", new Document("x", x).append("y", y).append("z", z).append("enabled", true))
        );
    }

    public static void unspawnMob(String name) {
        if (players == null) return;
        getMobsCollection().updateOne(
                new Document("name", name),
                new Document("$set", new Document("enabled", false))
        );
    }

    public static void deleteMob(String name) {
        if (players == null) return;
        getMobsCollection().deleteMany(new Document("name", name));
    }

    public static List<CustomMob> getAllMobs() {
        List<CustomMob> result = new ArrayList<>();
        if (players == null) return result;
        for (Document doc : getMobsCollection().find()) {
            result.add(new CustomMob(
                    doc.getString("name"),
                    doc.getString("type"),
                    doc.getInteger("hp", 10),
                    doc.getInteger("money", 50),
                    doc.getDouble("x"),
                    doc.getDouble("y"),
                    doc.getDouble("z"),
                    doc.getBoolean("enabled", false)
            ));
        }
        return result;
    }

    public static CustomMob getMob(String name) {
        if (players == null) return null;
        Document doc = getMobsCollection().find(new Document("name", name)).first();
        if (doc == null) return null;
        return new CustomMob(
                doc.getString("name"),
                doc.getString("type"),
                doc.getInteger("hp", 10),
                doc.getInteger("money", 50),
                doc.getDouble("x"),
                doc.getDouble("y"),
                doc.getDouble("z"),
                doc.getBoolean("enabled", false)
        );
    }
}
