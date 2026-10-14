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
    private static MongoCollection<Document> mobs;

    private ResourceManager() {}

    private static synchronized void ensureConnected() {
        if (client != null) return;

        client = MongoClients.create(System.getProperty("customguimod.mongo.uri", "mongodb://localhost:27017"));
        database = client.getDatabase(System.getProperty("customguimod.mongo.database", "MyProject_build"));
        players = database.getCollection("players");
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

        // One-way migration cleanup for databases created before the fixed resource catalog.
        database.getCollection("custom_resources").deleteMany(new Document("name", RETIRED_LIGHTNINGS));
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

    /**
     * v1.2.8 fixed catalog. Runtime creation/deletion of currencies or rune resources is intentionally unsupported.
     */
    public static List<String> getResourceNames() {
        return new ArrayList<>(BUILT_IN);
    }

    public static String findCanonicalName(String input) {
        if (input == null || isRetiredResource(input)) return null;
        for (String name : BUILT_IN) {
            if (name.equalsIgnoreCase(input)) return name;
        }
        return null;
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
     * Gameplay code should prefer grantPlayerResource/spendPlayerResource.
     */
    public static int addPlayerResource(UUID uuid, String resource, int amount) {
        if (amount > 0) {
            grantPlayerResource(uuid, resource, amount);
        } else if (amount < 0) {
            long requested = -(long) amount;
            int spend = requested > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) requested;
            spendPlayerResource(uuid, resource, spend);
        }
        return getPlayerResource(uuid, resource);
    }

    public static boolean canAfford(UUID uuid, String resource, int amount) {
        if (amount < 0) return false;
        String canonical = findCanonicalName(resource);
        return canonical != null && getPlayerResource(uuid, canonical) >= amount;
    }

    /** Grant a non-negative amount of a known fixed resource. */
    public static boolean grantPlayerResource(UUID uuid, String resource, int amount) {
        ensureConnected();
        if (amount < 0) return false;
        String canonical = findCanonicalName(resource);
        if (canonical == null) return false;
        if (amount == 0) return true;

        String field = playerResourceField(canonical);
        players.updateOne(
                new Document("_id", uuid.toString()),
                new Document("$inc", new Document(field, amount)),
                new UpdateOptions().upsert(true)
        );
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

        int safeValue = Math.max(0, value);
        String field = playerResourceField(canonical);
        players.updateOne(
                new Document("_id", uuid.toString()),
                new Document("$set", new Document(field, safeValue)),
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
}
