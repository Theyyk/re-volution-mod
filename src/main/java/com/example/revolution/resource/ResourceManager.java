package com.example.revolution.resource;


import com.example.revolution.BrandingProperties;
import com.example.revolution.number.LargeNumbers;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

import java.math.BigDecimal;
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
        client = MongoClients.create(BrandingProperties.get("revolution.mongo.uri", "mongodb://localhost:27017"));
        database = client.getDatabase(BrandingProperties.get("revolution.mongo.database", "MyProject_build"));
        players = database.getCollection("players");
        mobs = database.getCollection("custom_mobs");
    }

    public static void purgeDeprecatedLightnings() {
        ensureConnected();
        players.updateMany(new Document(RETIRED_LIGHTNINGS, new Document("$exists", true)),
                new Document("$unset", new Document(RETIRED_LIGHTNINGS, "")));
        mobs.updateMany(new Document("resource", RETIRED_LIGHTNINGS),
                new Document("$unset", new Document("resource", "").append("amount", "")));
        database.getCollection("custom_resources").deleteMany(new Document("name", RETIRED_LIGHTNINGS));
    }

    public static void purgeDeprecatedLightnings(UUID uuid) {
        ensureConnected();
        players.updateOne(new Document("_id", uuid.toString()),
                new Document("$unset", new Document(RETIRED_LIGHTNINGS, "")));
    }

    public static boolean isRetiredResource(String name) { return RETIRED_LIGHTNINGS.equals(normalize(name).trim()); }
    public static String normalize(String name) { return name == null ? "" : name.toLowerCase(Locale.ROOT); }

    public static String getDisplayName(String resource) {
        String normalized = normalize(resource);
        if (normalized.equals("coins")) return "Монеты";
        if (normalized.equals("crystals")) return "Кристаллы";
        if (normalized.equals(RUNE_DUST)) return "Пыль рун";
        if (normalized.equals(RUNE_SHARDS)) return "Осколки рун";
        if (normalized.equals(RUNE_CORES)) return "Ядра рун";
        return resource;
    }

    public static List<String> getResourceNames() { return new ArrayList<>(BUILT_IN); }

    public static String findCanonicalName(String input) {
        if (input == null || isRetiredResource(input)) return null;
        for (String name : BUILT_IN) if (name.equalsIgnoreCase(input)) return name;
        return null;
    }

    public static BigDecimal getPlayerResourceValue(UUID uuid, String resource) {
        ensureConnected();
        String canonical = findCanonicalName(resource);
        if (canonical == null) return BigDecimal.ZERO;
        Document doc = players.find(new Document("_id", uuid.toString())).first();
        if (doc == null) return BigDecimal.ZERO;
        String normalized = normalize(canonical);
        if (normalized.equals("coins")) return getDecimal(doc, "balance");
        if (normalized.equals("crystals")) return getDecimal(doc, "crystals");
        Document custom = doc.get("resources", Document.class);
        return custom == null ? BigDecimal.ZERO : getDecimal(custom, normalized);
    }

    public static int getPlayerResource(UUID uuid, String resource) {
        BigDecimal value = getPlayerResourceValue(uuid, resource);
        if (value.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) >= 0) return Integer.MAX_VALUE;
        return Math.max(0, value.intValue());
    }

    public static long getPlayerResourceLong(UUID uuid, String resource) {
        BigDecimal value = getPlayerResourceValue(uuid, resource);
        if (value.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) >= 0) return Long.MAX_VALUE;
        return Math.max(0L, value.longValue());
    }

    public static synchronized boolean grantPlayerResource(UUID uuid, String resource, BigDecimal amount) {
        ensureConnected();
        String canonical = findCanonicalName(resource);
        if (canonical == null || amount == null || amount.signum() < 0) return false;
        if (amount.signum() == 0) return true;
        BigDecimal next = LargeNumbers.normalize(getPlayerResourceValue(uuid, canonical).add(amount));
        writePlayerResource(uuid, canonical, next);
        return true;
    }

    public static boolean grantPlayerResource(UUID uuid, String resource, int amount) {
        return grantPlayerResource(uuid, resource, BigDecimal.valueOf(amount));
    }

    public static synchronized boolean spendPlayerResource(UUID uuid, String resource, int amount) {
        if (amount < 0) return false;
        String canonical = findCanonicalName(resource);
        if (canonical == null) return false;
        BigDecimal current = getPlayerResourceValue(uuid, canonical);
        BigDecimal cost = BigDecimal.valueOf(amount);
        if (current.compareTo(cost) < 0) return false;
        writePlayerResource(uuid, canonical, current.subtract(cost));
        return true;
    }

    public static boolean canAfford(UUID uuid, String resource, int amount) {
        return amount >= 0 && getPlayerResourceValue(uuid, resource).compareTo(BigDecimal.valueOf(amount)) >= 0;
    }

    public static int addPlayerResource(UUID uuid, String resource, int amount) {
        if (amount > 0) grantPlayerResource(uuid, resource, amount);
        else if (amount < 0) spendPlayerResource(uuid, resource, (int) Math.min(Integer.MAX_VALUE, -(long) amount));
        return getPlayerResource(uuid, resource);
    }

    public static int setPlayerResource(UUID uuid, String resource, int value) {
        String canonical = findCanonicalName(resource);
        if (canonical == null) return 0;
        writePlayerResource(uuid, canonical, BigDecimal.valueOf(Math.max(0, value)));
        return getPlayerResource(uuid, canonical);
    }

    private static void writePlayerResource(UUID uuid, String canonical, BigDecimal value) {
        ensureConnected();
        String field = playerResourceField(canonical);
        players.updateOne(new Document("_id", uuid.toString()),
                new Document("$set", new Document(field, LargeNumbers.canonical(value.max(BigDecimal.ZERO)))),
                new UpdateOptions().upsert(true));
    }

    public static void setMobReward(String mobName, String resource, BigDecimal amount) {
        ensureConnected();
        String canonical = findCanonicalName(resource);
        if (canonical == null || amount == null || amount.signum() <= 0) return;
        mobs.updateOne(new Document("name", mobName),
                new Document("$set", new Document("resource", canonical)
                        .append("amount", LargeNumbers.canonical(amount))));
    }

    public static void setMobReward(String mobName, String resource, int amount) {
        setMobReward(mobName, resource, BigDecimal.valueOf(amount));
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

    public static BigDecimal getMobAmountValue(String mobName) {
        ensureConnected();
        Document doc = mobs.find(new Document("name", mobName)).first();
        if (doc == null) return BigDecimal.ZERO;
        BigDecimal amount = getDecimal(doc, "amount");
        if (amount.signum() > 0) return amount;
        return getDecimal(doc, "money");
    }

    public static int getMobAmount(String mobName) {
        BigDecimal value = getMobAmountValue(mobName);
        if (value.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) >= 0) return Integer.MAX_VALUE;
        return Math.max(0, value.intValue());
    }

    private static String playerResourceField(String canonical) {
        String normalized = normalize(canonical);
        if (normalized.equals("coins")) return "balance";
        if (normalized.equals("crystals")) return "crystals";
        return "resources." + normalized;
    }

    private static BigDecimal getDecimal(Document doc, String field) {
        if (doc == null) return BigDecimal.ZERO;
        Object raw = doc.get(field);
        if (raw == null) return BigDecimal.ZERO;
        try {
            if (raw instanceof Number) return LargeNumbers.normalize(new BigDecimal(raw.toString()));
            return LargeNumbers.parseExact(String.valueOf(raw));
        } catch (RuntimeException ignored) {
            return BigDecimal.ZERO;
        }
    }
}
