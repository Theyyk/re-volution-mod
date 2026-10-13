package com.example.customguimod;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ResourceManager {

    public static final String RUNE_DUST = "rune_dust";
    public static final String RUNE_SHARDS = "rune_shards";
    public static final String RUNE_CORES = "rune_cores";
    public static final String RETIRED_LIGHTNINGS = "lightnings";

    private static final List<String> BUILT_IN = Arrays.asList(
            "coins", "crystals", RUNE_DUST, RUNE_SHARDS, RUNE_CORES
    );

    private static MongoClient client;
    private static MongoDatabase database;
    private static MongoCollection<Document> players;
    private static MongoCollection<Document> resources;
    private static MongoCollection<Document> mobs;

    private ResourceManager() {}

    private static synchronized void ensureConnected() {
        if (client != null) return;

        client = MongoClients.create(System.getProperty("customguimod.mongo.uri", "mongodb://localhost:27017"));
        database = client.getDatabase(System.getProperty("customguimod.mongo.database", "MyProject_build"));
        players = database.getCollection("players");
        resources = database.getCollection("custom_resources");
        mobs = database.getCollection("custom_mobs");
    }

    public static void purgeDeprecatedLightnings() {
        ensureConnected();

        players.updateMany(
                new Document(RETIRED_LIGHTNINGS, new Document("$exists", true)),
                new Document("$unset", new Document(RETIRED_LIGHTNINGS, ""))
        );

        mobs.updateMany(
                new Document("resource", RETIRED_LIGHTNINGS),
                new Document("$unset", new Document("resource", "").append("amount", ""))
        );

        resources.deleteMany(new Document("name", RETIRED_LIGHTNINGS));
    }

    public static void purgeDeprecatedLightnings(UUID uuid) {
        ensureConnected();
        players.updateOne(
                new Document("_id", uuid.toString()),
                new Document("$unset", new Document(RETIRED_LIGHTNINGS, ""))
        );
    }

    public static boolean isRetiredResource(String name) {
        return RETIRED_LIGHTNINGS.equals(normalize(name).trim());
    }

    public static boolean isValidName(String name) {
        return name != null && name.matches("[\\p{L}\\p{N}_-]+");
    }

    public static String normalize(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    public static String getDisplayName(String resource) {
        String normalized = normalize(resource);
        if (normalized.equals("coins")) return "Монеты";
        if (normalized.equals("crystals")) return "Кристаллы";
        if (normalized.equals(RUNE_DUST)) return "Пыль рун";
        if (normalized.equals(RUNE_SHARDS)) return "Осколки рун";
        if (normalized.equals(RUNE_CORES)) return "Ядра рун";
        return resource;
    }

    public static List<String> getResourceNames() {
        ensureConnected();
        List<String> result = new ArrayList<>(BUILT_IN);

        for (Document doc : resources.find()) {
            String name = doc.getString("name");
            if (name != null && !isRetiredResource(name) && !containsIgnoreCase(result, name)) {
                result.add(name);
            }
        }

        return result;
    }

    public static String findCanonicalName(String input) {
        if (input == null || isRetiredResource(input)) return null;
        for (String name : getResourceNames()) {
            if (name.equalsIgnoreCase(input)) return name;
        }
        return null;
    }

    public static boolean resourceExists(String name) {
        return findCanonicalName(name) != null;
    }

    public static boolean createResource(String name) {
        ensureConnected();
        if (!isValidName(name) || isRetiredResource(name) || resourceExists(name)) return false;

        resources.insertOne(new Document("name", normalize(name)));
        return true;
    }

    public static boolean deleteResource(String name) {
        ensureConnected();
        String canonical = findCanonicalName(name);
        if (canonical == null || BUILT_IN.contains(normalize(canonical))) return false;

        return resources.deleteMany(new Document("name", canonical)).getDeletedCount() > 0;
    }

    public static int getPlayerResource(UUID uuid, String resource) {
        ensureConnected();
        String canonical = findCanonicalName(resource);
        if (canonical == null) return 0;

        Document doc = players.find(new Document("_id", uuid.toString())).first();
        if (doc == null) return 0;

        String normalized = normalize(canonical);
        if (normalized.equals("coins")) return getInt(doc, "balance");
        if (normalized.equals("crystals")) return getInt(doc, "crystals");

        Document custom = doc.get("resources", Document.class);
        if (custom == null) return 0;
        return getInt(custom, normalized);
    }

    /**
     * Generic signed adjustment kept for admin/migration compatibility.
     * Gameplay actions should use grantPlayerResource/spendPlayerResource instead.
     */
    public static int addPlayerResource(UUID uuid, String resource, int amount) {
        ensureConnected();
        String canonical = findCanonicalName(resource);
        if (canonical == null) return 0;

        String field = playerResourceField(canonical);
        players.updateOne(
                new Document("_id", uuid.toString()),
                new Document("$inc", new Document(field, amount)),
                new UpdateOptions().upsert(true)
        );

        return getPlayerResource(uuid, canonical);
    }

    public static boolean canAfford(UUID uuid, String resource, int amount) {
        if (amount < 0) return false;
        String canonical = findCanonicalName(resource);
        return canonical != null && getPlayerResource(uuid, canonical) >= amount;
    }

    /** Grant a non-negative amount of a known, non-retired resource. */
    public static boolean grantPlayerResource(UUID uuid, String resource, int amount) {
        if (amount < 0) return false;
        String canonical = findCanonicalName(resource);
        if (canonical == null) return false;
        if (amount == 0) return true;

        addPlayerResource(uuid, canonical, amount);
        return true;
    }

    /**
     * Atomically spends a non-negative amount and never lets the persisted balance cross below zero.
     */
    public static boolean spendPlayerResource(UUID uuid, String resource, int amount) {
        ensureConnected();
        if (amount < 0) return false;

        String canonical = findCanonicalName(resource);
        if (canonical == null) return false;
        if (amount == 0) return true;

        String field = playerResourceField(canonical);
        Document filter = new Document("_id", uuid.toString())
                .append(field, new Document("$gte", amount));
        UpdateResult result = players.updateOne(
                filter,
                new Document("$inc", new Document(field, -amount))
        );
        return result.getModifiedCount() > 0;
    }

    public static int setPlayerResource(UUID uuid, String resource, int value) {
        ensureConnected();
        String canonical = findCanonicalName(resource);
        if (canonical == null) return 0;

        String field = playerResourceField(canonical);
        players.updateOne(
                new Document("_id", uuid.toString()),
                new Document("$set", new Document(field, value)),
                new UpdateOptions().upsert(true)
        );
        return getPlayerResource(uuid, canonical);
    }

    public static void setMobReward(String mobName, String resource, int amount) {
        ensureConnected();
        String canonical = findCanonicalName(resource);
        if (canonical == null) return;

        mobs.updateOne(
                new Document("name", mobName),
                new Document("$set", new Document("resource", canonical).append("amount", amount))
        );
    }

    public static String getMobResource(String mobName) {
        ensureConnected();
        Document doc = mobs.find(new Document("name", mobName)).first();
        if (doc == null) return "coins";

        String resource = doc.getString("resource");
        if (resource == null || resource.isEmpty()) return "coins";

        String canonical = findCanonicalName(resource);
        return canonical == null ? "coins" : canonical;
    }

    public static int getMobAmount(String mobName) {
        ensureConnected();
        Document doc = mobs.find(new Document("name", mobName)).first();
        if (doc == null) return 0;

        Integer amount = doc.getInteger("amount");
        if (amount != null) return amount;

        Integer oldMoney = doc.getInteger("money");
        return oldMoney == null ? 0 : oldMoney;
    }

    private static String playerResourceField(String canonical) {
        String normalized = normalize(canonical);
        if (normalized.equals("coins")) return "balance";
        if (normalized.equals("crystals")) return "crystals";
        return "resources." + normalized;
    }

    private static int getInt(Document doc, String field) {
        Number number = doc.get(field, Number.class);
        return number == null ? 0 : number.intValue();
    }

    private static boolean containsIgnoreCase(List<String> values, String value) {
        for (String current : values) {
            if (current.equalsIgnoreCase(value)) return true;
        }
        return false;
    }
}
