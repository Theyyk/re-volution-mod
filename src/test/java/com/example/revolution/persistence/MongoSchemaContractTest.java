package com.example.revolution.persistence;

import com.example.revolution.RevolutionMod;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.repository.RuneRecord;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.bson.Document;

/** Exercises real repository mapping against captured driver calls; never opens a Mongo socket. */
public final class MongoSchemaContractTest {
    private static Document current;
    private static Document filter;
    private static Document update;
    private static String collection;

    public static void main(String[] args) throws Exception {
        RevolutionMod.logger = LogManager.getLogger("MongoSchemaContractTest");
        MongoConnection connection = new MongoConnection(RevolutionMod.logger);
        // Seed the driver boundary without adding a test-only constructor to production.
        Field database = MongoConnection.class.getDeclaredField("database");
        database.setAccessible(true);
        database.set(connection, proxy(MongoDatabase.class, (object, method, values) -> {
            if (method.getName().equals("getCollection")) {
                collection = (String) values[0];
                return proxy(MongoCollection.class, (target, operation, arguments) -> {
                    if (operation.getName().equals("find")) {
                        if (arguments != null && arguments.length > 0) filter = (Document) arguments[0];
                        return proxy(FindIterable.class, (iterator, query, ignored) -> {
                            if (query.getName().equals("first")) return current;
                            throw new AssertionError(query);
                        });
                    }
                    if (operation.getName().equals("updateOne")) {
                        filter = (Document) arguments[0];
                        update = (Document) arguments[1];
                        return null;
                    }
                    if (operation.getName().equals("insertOne")) {
                        update = (Document) arguments[0];
                        return null;
                    }
                    throw new AssertionError(operation);
                });
            }
            throw new AssertionError(method);
        }));

        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Document storedRune = new Document("slot", 28).append("cardIndex", 4).append("layer", 2).append("level", 7);
        Document collectionDoc = new Document("name", "Legacy").append("cards", new ArrayList<>(Arrays.asList(storedRune)))
                .append("runeEnhancements", new Document("4", 12));
        Document fixedCollectionDeck = new Document("name", "Collection 02")
                .append("cards", new ArrayList<Document>())
                .append("runeEnhancements", new Document("0", 9));
        Document fixedMythic = new Document("slot", 0).append("cardIndex", 0).append("layer", 4).append("level", 1);
        Document runeRoot = new Document("unlockedCollections", new ArrayList<>(Arrays.asList("collection_02")))
                .append("collections", new Document("collection_02",
                        new Document("slots", new ArrayList<>(Arrays.asList(fixedMythic)))));
        current = new Document("_id", player.toString()).append("balance", 42).append("crystals", 8)
                .append("activeDeck", 0)
                .append("decks", new ArrayList<>(Arrays.asList(collectionDoc, fixedCollectionDeck)))
                .append("runeCollections", runeRoot)
                .append("resources", new Document("rune_dust", "12345678901234567890.5"));

        check(new MongoPlayerRepository(connection, false).getBalance(player) == 0,
                "physical-client core player repository remains inactive");
        check(new MongoMobRepository(connection, false).getAllMobs().isEmpty(),
                "physical-client core mob definitions remain inactive");
        MongoPlayerRepository players = new MongoPlayerRepository(connection);
        RuneRecord rune = players.getRunesInCollection(player, 0).get(0);
        check(rune.slot == 28 && rune.runeIndex == 4 && rune.layer == 2 && rune.level == 7, "legacy rune record mapping");
        RuneRecord mythic = players.getRunesInCollection(player, 1).get(0);
        check(mythic.slot == 0 && mythic.runeIndex == 0 && mythic.layer == 4,
                "v2.3 fixed collection Mythic rank survives repository restart mapping");
        Set<RuneCollectionId> unlocked = players.getUnlockedCollections(player);
        check(unlocked.contains(RuneCollectionId.COLLECTION_01) && unlocked.contains(RuneCollectionId.COLLECTION_02),
                "permanent unlock metadata restores default C1 plus stored C2");
        check(players.getBalance(player) == 42 && players.getCrystals(player) == 8, "legacy integer balances");
        check(players.getCollectionNames(player).equals(Arrays.asList("Legacy", "Collection 02")), "stored collection names");
        players.setActiveCollection(player, 2);
        check(update.get("$set", Document.class).getInteger("activeDeck") == 2, "activeDeck key");
        check(filter.getString("_id").equals(player.toString()) && collection.equals("players"), "UUID filter and collection");
        players.addRuneToCollection(player, 0, 5, 5, 3, 1);
        Document inserted = ((java.util.List<Document>) collectionDoc.get("cards")).get(1);
        check(inserted.equals(new Document("slot", 5).append("cardIndex", 5).append("layer", 3).append("level", 1)), "legacy cardIndex/cards schema");
        players.addRuneToCollection(player, 1, 1, 1, 4, 1);
        Document pushed = update.get("$push", Document.class).get("runeCollections.collections.collection_02.slots", Document.class);
        check(pushed != null && pushed.getInteger("layer") == 4,
                "new fixed collections persist Mythic through additive rune root");
        check(players.unlockCollection(player, RuneCollectionId.COLLECTION_03), "new permanent collection can be unlocked");
        check("collection_03".equals(update.get("$addToSet", Document.class)
                        .getString("runeCollections.unlockedCollections")),
                "unlock persistence uses additive v2.3 metadata path");

        MongoRuneEnhancementRepository enhancements = new MongoRuneEnhancementRepository(connection);
        check(enhancements.getLevels(player, 0)[4] == 12, "fixed-slot enhancement reads");
        check(enhancements.getLevels(player, 1)[0] == 9,
                "v2.3 fixed collection enhancement survives restart through compatibility deck storage");
        enhancements.addLevels(player, 0, 4, 3);
        check(update.get("$inc", Document.class).getInteger("decks.0.runeEnhancements.4") == 3, "enhancement dotted update path");
        enhancements.addLevels(player, 1, 0, 2);
        check(update.get("$inc", Document.class).getInteger("decks.1.runeEnhancements.0") == 2,
                "fixed collection enhancement keeps compatibility dotted path");
        enhancements.resetCollection(player, 0);
        check(update.get("$unset", Document.class).containsKey("decks.0.runeEnhancements"), "enhancement reset only");

        MongoResourceRepository resources = new MongoResourceRepository(connection);
        check(resources.getPlayerResourceValue(player, "rune_dust").equals(new BigDecimal("12345678901234567890.5")), "exact string resources");
        resources.writePlayerResource(player, "coins", new BigDecimal("900000000000000000000"));
        check(update.get("$set", Document.class).get("balance").equals("900000000000000000000"), "exact canonical balance encoding");
        resources.writePlayerResource(player, "rune_dust", new BigDecimal("1.25"));
        check(update.get("$set", Document.class).getString("resources.rune_dust").equals("1.25"), "resource dotted update path");

        MongoMobRepository mobs = new MongoMobRepository(connection);
        mobs.createMob("legacy", "skeleton", new BigDecimal("1E30"), 50);
        check(collection.equals("custom_mobs") && update.getString("hp").equals("1000000000000000000000000000000"), "mob collection and exact HP");
        check(update.getInteger("money") == 50 && !update.getBoolean("enabled"), "legacy mob defaults");
        current = new Document("name", "legacy").append("type", "skeleton").append("hp", 1200)
                .append("money", 50).append("x", 1.0).append("y", 64.0).append("z", -2.0).append("enabled", true);
        check(mobs.getMob("legacy").hp.compareTo(new BigDecimal("1200")) == 0, "numeric HP read compatibility");
        check(mobs.getMobAmountValue("legacy").compareTo(new BigDecimal("50")) == 0, "money reward fallback");
        mobs.setMobReward("legacy", "rune_cores", new BigDecimal("1E25"));
        check(update.get("$set", Document.class).getString("resource").equals("rune_cores"), "mob resource key");
        check(update.get("$set", Document.class).getString("amount").equals("10000000000000000000000000"), "exact mob reward encoding");
        connection.close();
        check(!connection.isConnected(), "connection state clears on shutdown");
        System.out.println("Mongo document names, keys, numeric encodings and v2.3 rune restart compatibility passed");
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
