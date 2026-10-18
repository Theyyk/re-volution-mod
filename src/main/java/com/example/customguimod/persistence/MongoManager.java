package com.example.customguimod.persistence;

import com.example.customguimod.CustomGuiMod;
import com.example.customguimod.rune.RuneStorageRules;
import com.example.customguimod.number.LargeNumbers;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

import java.math.BigDecimal;
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

    public static int getBalance(UUID uuid) {
        if (players == null) return 0;
        Document doc = players.find(new Document("_id", uuid.toString())).first();
        if (doc == null) return 0;
        Integer b = doc.getInteger("balance", 0);
        return b == null ? 0 : b;
    }

    public static void setBalance(UUID uuid, int balance) {
        if (players == null) return;
        players.updateOne(new Document("_id", uuid.toString()),
                new Document("$set", new Document("balance", balance)), new UpdateOptions().upsert(true));
    }

    public static int getCrystals(UUID uuid) {
        if (players == null) return 0;
        Document doc = players.find(new Document("_id", uuid.toString())).first();
        if (doc == null) return 0;
        Integer c = doc.getInteger("crystals", 0);
        return c == null ? 0 : c;
    }

    public static void setCrystals(UUID uuid, int crystals) {
        if (players == null) return;
        players.updateOne(new Document("_id", uuid.toString()),
                new Document("$set", new Document("crystals", crystals)), new UpdateOptions().upsert(true));
    }

    public static class SavedRune {
        public int slot, runeIndex, layer, level;
        public SavedRune(int slot, int runeIndex, int layer, int level) {
            this.slot = slot; this.runeIndex = runeIndex; this.layer = layer; this.level = level;
        }
    }

    @SuppressWarnings("unchecked")
    public static List<SavedRune> getRunesInCollection(UUID uuid, int collectionIndex) {
        List<SavedRune> result = new ArrayList<>();
        if (players == null) return result;
        Document doc = players.find(new Document("_id", uuid.toString())).first();
        if (doc == null) return result;
        List<Document> collections = (List<Document>) doc.get("decks");
        if (collections == null || collectionIndex < 0 || collectionIndex >= collections.size()) {
            CustomGuiMod.logger.warn("getCardsInDeck: deck " + collectionIndex + " not found");
            return result;
        }
        Document collection = collections.get(collectionIndex);
        List<Document> runes = (List<Document>) collection.get("cards");
        if (runes == null) {
            CustomGuiMod.logger.info("getCardsInDeck: deck " + collectionIndex + " has no cards field");
            return result;
        }
        for (Document c : runes) {
            result.add(new SavedRune(c.getInteger("slot", 0), c.getInteger("cardIndex", 0),
                    c.getInteger("layer", 1), c.getInteger("level", 1)));
        }
        return result;
    }

    public static int getActiveCollection(UUID uuid) {
        if (players == null) return 0;
        Document doc = players.find(new Document("_id", uuid.toString())).first();
        if (doc == null) return 0;
        Integer d = doc.getInteger("activeDeck", 0);
        return d == null ? 0 : d;
    }

    public static void setActiveCollection(UUID uuid, int collectionIndex) {
        if (players == null) return;
        players.updateOne(new Document("_id", uuid.toString()),
                new Document("$set", new Document("activeDeck", collectionIndex)), new UpdateOptions().upsert(true));
    }

    @SuppressWarnings("unchecked")
    public static List<String> getCollectionNames(UUID uuid) {
        List<String> result = new ArrayList<>();
        if (players == null) return result;
        Document doc = players.find(new Document("_id", uuid.toString())).first();
        if (doc == null) return result;
        List<Document> collections = (List<Document>) doc.get("decks");
        if (collections == null) return result;
        for (Document d : collections) result.add(d.getString("name"));
        return result;
    }

    @SuppressWarnings("unchecked")
    public static void addCollection(UUID uuid, String name) {
        if (players == null) return;
        Document doc = players.find(new Document("_id", uuid.toString())).first();
        if (doc == null) {
            List<Document> collections = new ArrayList<>();
            collections.add(new Document("name", name).append("cards", new ArrayList<Document>()));
            players.insertOne(new Document("_id", uuid.toString()).append("balance", 0).append("crystals", 0)
                    .append("activeDeck", 0).append("decks", collections));
            CustomGuiMod.logger.info("addDeck: created player doc + deck '" + name + "'");
            return;
        }
        List<Document> collections = (List<Document>) doc.get("decks");
        if (collections == null) collections = new ArrayList<>();
        collections.add(new Document("name", name).append("cards", new ArrayList<Document>()));
        players.updateOne(new Document("_id", uuid.toString()),
                new Document("$set", new Document("decks", collections)), new UpdateOptions().upsert(true));
        CustomGuiMod.logger.info("addDeck: added deck '" + name + "', total decks: " + collections.size());
    }

    @SuppressWarnings("unchecked")
    public static void deleteCollection(UUID uuid, int collectionIndex) {
        if (players == null) return;
        Document doc = players.find(new Document("_id", uuid.toString())).first();
        if (doc == null) return;
        List<Document> collections = (List<Document>) doc.get("decks");
        if (collections == null || collectionIndex >= collections.size()) return;
        collections.remove(collectionIndex);
        int active = getActiveCollection(uuid);
        if (active >= collections.size()) active = 0;
        players.updateOne(new Document("_id", uuid.toString()),
                new Document("$set", new Document("decks", collections).append("activeDeck", active)), new UpdateOptions().upsert(true));
    }

    @SuppressWarnings("unchecked")
    public static void addRuneToCollection(UUID uuid, int collectionIndex, int slot, int runeIndex, int layer, int level) {
        if (players == null) { CustomGuiMod.logger.warn("addCardToDeck: players == null"); return; }
        Document doc = players.find(new Document("_id", uuid.toString())).first();
        if (doc == null) { CustomGuiMod.logger.warn("addCardToDeck: doc not found for " + uuid); return; }
        List<Document> collections = (List<Document>) doc.get("decks");
        if (collections == null) { CustomGuiMod.logger.warn("addCardToDeck: decks == null"); return; }
        if (collectionIndex < 0 || collectionIndex >= collections.size()) {
            CustomGuiMod.logger.warn("addCardToDeck: deck " + collectionIndex + " not found (size: " + collections.size() + ")"); return;
        }
        Document collection = collections.get(collectionIndex);
        List<Document> runes = (List<Document>) collection.get("cards");
        if (runes == null) runes = new ArrayList<>();
        runes.add(new Document("slot", slot).append("cardIndex", runeIndex).append("layer", layer).append("level", level));
        collection.put("cards", runes); collections.set(collectionIndex, collection);
        players.updateOne(new Document("_id", uuid.toString()),
                new Document("$set", new Document("decks", collections)), new UpdateOptions().upsert(true));
        CustomGuiMod.logger.info("addCardToDeck: added card to deck " + collectionIndex + " (total in deck: " + runes.size() + ", slot=" + slot + ")");
    }

    @SuppressWarnings("unchecked")
    public static boolean setRuneRank(UUID uuid, int collectionIndex, int runeIndex, int layer) {
        if (players == null) return false;
        Document doc = players.find(new Document("_id", uuid.toString())).first();
        if (doc == null) return false;
        List<Document> collections = (List<Document>) doc.get("decks");
        if (collections == null || collectionIndex < 0 || collectionIndex >= collections.size()) return false;
        Document collection = collections.get(collectionIndex);
        List<Document> runes = (List<Document>) collection.get("cards");
        if (runes == null) return false;
        Document target = null;
        for (Document rune : runes) {
            if (rune.getInteger("cardIndex", -1) != runeIndex) continue;
            if (target == null || rune.getInteger("layer", 1) > target.getInteger("layer", 1)) target = rune;
        }
        if (target == null) return false;
        target.put("slot", runeIndex); target.put("cardIndex", runeIndex);
        target.put("layer", Math.max(RuneStorageRules.NORMAL_RANK, Math.min(RuneStorageRules.ADVANCED_RANK, layer)));
        if (!target.containsKey("level")) target.put("level", 1);
        collection.put("cards", runes); collections.set(collectionIndex, collection);
        players.updateOne(new Document("_id", uuid.toString()),
                new Document("$set", new Document("decks", collections)), new UpdateOptions().upsert(true));
        CustomGuiMod.logger.info("setRuneRank: deck=" + collectionIndex + ", rune=" + runeIndex + ", rank=" + layer);
        return true;
    }

    @SuppressWarnings("unchecked")
    public static void clearCollection(UUID uuid, int collectionIndex) {
        if (players == null) return;
        Document doc = players.find(new Document("_id", uuid.toString())).first();
        if (doc == null) return;
        List<Document> collections = (List<Document>) doc.get("decks");
        if (collections == null || collectionIndex >= collections.size()) return;
        collections.get(collectionIndex).put("cards", new ArrayList<Document>());
        players.updateOne(new Document("_id", uuid.toString()),
                new Document("$set", new Document("decks", collections)), new UpdateOptions().upsert(true));
    }

    public static List<Integer> getFreeSlots(UUID uuid, int collectionIndex) {
        List<Integer> occupied = new ArrayList<>();
        for (SavedRune rune : getRunesInCollection(uuid, collectionIndex)) occupied.add(rune.slot);
        return RuneStorageRules.freePurchaseSlots(occupied);
    }

    public static class CustomMob {
        public String name;
        public String type;
        public BigDecimal hp;
        public int money;
        public double x, y, z;
        public boolean enabled;

        public CustomMob(String name, String type, BigDecimal hp, int money, double x, double y, double z, boolean enabled) {
            this.name = name; this.type = type; this.hp = hp; this.money = money;
            this.x = x; this.y = y; this.z = z; this.enabled = enabled;
        }
    }

    public static MongoCollection<Document> getMobsCollection() { return database.getCollection("custom_mobs"); }

    public static void createMob(String name, String type, BigDecimal hp, int money) {
        if (players == null) return;
        getMobsCollection().insertOne(new Document("name", name).append("type", type)
                .append("hp", LargeNumbers.canonical(hp)).append("money", money)
                .append("x", 0.0).append("y", 0.0).append("z", 0.0).append("enabled", false));
        CustomGuiMod.logger.info("Mob created: " + name);
    }

    public static void setMobSpawn(String name, double x, double y, double z) {
        if (players == null) return;
        getMobsCollection().updateOne(new Document("name", name),
                new Document("$set", new Document("x", x).append("y", y).append("z", z).append("enabled", true)));
    }

    public static void unspawnMob(String name) {
        if (players == null) return;
        getMobsCollection().updateOne(new Document("name", name), new Document("$set", new Document("enabled", false)));
    }

    public static void deleteMob(String name) {
        if (players == null) return;
        getMobsCollection().deleteMany(new Document("name", name));
    }

    public static List<CustomMob> getAllMobs() {
        List<CustomMob> result = new ArrayList<>();
        if (players == null) return result;
        for (Document doc : getMobsCollection().find()) {
            result.add(new CustomMob(doc.getString("name"), doc.getString("type"), getBigDecimal(doc, "hp", BigDecimal.TEN),
                    doc.getInteger("money", 50), doc.getDouble("x"), doc.getDouble("y"), doc.getDouble("z"),
                    doc.getBoolean("enabled", false)));
        }
        return result;
    }

    public static CustomMob getMob(String name) {
        if (players == null) return null;
        Document doc = getMobsCollection().find(new Document("name", name)).first();
        if (doc == null) return null;
        return new CustomMob(doc.getString("name"), doc.getString("type"), getBigDecimal(doc, "hp", BigDecimal.TEN),
                doc.getInteger("money", 50), doc.getDouble("x"), doc.getDouble("y"), doc.getDouble("z"),
                doc.getBoolean("enabled", false));
    }

    private static BigDecimal getBigDecimal(Document doc, String field, BigDecimal fallback) {
        if (doc == null) return fallback;
        Object raw = doc.get(field);
        if (raw == null) return fallback;
        try {
            if (raw instanceof Number) return LargeNumbers.normalize(new BigDecimal(raw.toString()));
            return LargeNumbers.parseExact(String.valueOf(raw));
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
