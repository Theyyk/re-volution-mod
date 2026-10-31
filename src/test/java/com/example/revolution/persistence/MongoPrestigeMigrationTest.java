package com.example.revolution.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.revolution.prestige.PrestigeBranch;
import com.example.revolution.prestige.PrestigeState;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.bson.Document;
import org.junit.jupiter.api.Test;

/** Legacy Mastery documents must load with safe defaults and rewrite without destructive migration. */
public final class MongoPrestigeMigrationTest {
    @Test
    public void legacyMasteryStateLoadsAndRewritesAsSchemaThree() throws Exception {
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000601");
        Document click = new Document("bestSavedProgress", "600")
                .append("rankMetaLevels", new Document("CLICK_I", 5).append("CLICK_II", 4).append("CLICK_III", 3))
                .append("rankMetaLevelCap", 5)
                .append("masteryTier", 0)
                .append("masteryQuestState", "AVAILABLE");
        Document storedPrestige = new Document("schemaVersion", 2)
                .append("branches", new Document("CLICK", click));
        Harness harness = harness(player, storedPrestige);

        MongoPrestigeRepository repository = new MongoPrestigeRepository(harness.connection, true);
        PrestigeState loaded = repository.load(player);
        PrestigeState.BranchState branch = loaded.branch(PrestigeBranch.CLICK);
        assertEquals(0, branch.masteryTier);
        assertEquals(5, branch.rankMetaLevelCap);
        assertEquals(5, branch.rankMetaLevels.get("CLICK_I").intValue());
        assertEquals(4, branch.rankMetaLevels.get("CLICK_II").intValue());
        assertEquals(3, branch.rankMetaLevels.get("CLICK_III").intValue());

        repository.save(player, loaded);
        Document update = harness.writtenUpdate.get();
        assertNotNull(update);
        Document rewritten = update.get("$set", Document.class).get("prestige", Document.class);
        assertEquals(3, rewritten.getInteger("schemaVersion").intValue());
        Document rewrittenClick = rewritten.get("branches", Document.class).get("CLICK", Document.class);
        assertFalse(rewrittenClick.containsKey("masteryQuestState"));
    }

    @Test
    public void v25ActiveRunDefaultsExchangeCountersToZeroAndPersistsThemOnExistingSchema() throws Exception {
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000602");
        Document activeRun = new Document("runId", "legacy-v25-run")
                .append("branch", "CLICK")
                .append("startedAt", 123L)
                .append("progress", "42")
                .append("level", 2)
                .append("reachedRanks", java.util.Arrays.asList("CLICK_I"))
                .append("runBranchCurrency", "9")
                .append("masteryPendingFragments", "1")
                .append("petPendingFragments", "2")
                .append("runModifiers", new Document());
        Document storedPrestige = new Document("schemaVersion", 3)
                .append("activeRun", activeRun)
                .append("branches", new Document());
        Harness harness = harness(player, storedPrestige);
        MongoPrestigeRepository repository = new MongoPrestigeRepository(harness.connection, true);

        PrestigeState loaded = repository.load(player);
        assertNotNull(loaded.activeRun);
        assertEquals("legacy-v25-run", loaded.activeRun.runId);
        assertTrue(loaded.activeRun.runExchangePurchaseCounts.isEmpty());
        loaded.activeRun.runExchangePurchaseCounts.put("coins_fragments", 2);

        repository.save(player, loaded);
        Document rewritten = harness.writtenUpdate.get().get("$set", Document.class).get("prestige", Document.class);
        assertEquals(3, rewritten.getInteger("schemaVersion").intValue());
        Document rewrittenRun = rewritten.get("activeRun", Document.class);
        Document purchases = rewrittenRun.get("runExchange", Document.class).get("purchases", Document.class);
        assertEquals(2, purchases.getInteger("coins_fragments").intValue());
        assertEquals("9", rewrittenRun.getString("runBranchCurrency"));
    }

    private static Harness harness(UUID player, Document storedPrestige) throws Exception {
        Document playerDocument = new Document("_id", player.toString()).append("prestige", storedPrestige);
        AtomicReference<Document> writtenUpdate = new AtomicReference<>();
        MongoConnection connection = new MongoConnection(LogManager.getLogger("MongoPrestigeMigrationTest"));
        Field database = MongoConnection.class.getDeclaredField("database");
        database.setAccessible(true);
        database.set(connection, proxy(MongoDatabase.class, (object, method, values) -> {
            if (!method.getName().equals("getCollection")) throw new AssertionError(method);
            return proxy(MongoCollection.class, (target, operation, arguments) -> {
                if (operation.getName().equals("find")) {
                    return proxy(FindIterable.class, (iterator, query, ignored) -> {
                        if (query.getName().equals("first")) return playerDocument;
                        throw new AssertionError(query);
                    });
                }
                if (operation.getName().equals("updateOne")) {
                    writtenUpdate.set((Document) arguments[1]);
                    return null;
                }
                throw new AssertionError(operation);
            });
        }));
        return new Harness(connection, writtenUpdate);
    }

    private static final class Harness {
        final MongoConnection connection;
        final AtomicReference<Document> writtenUpdate;
        Harness(MongoConnection connection, AtomicReference<Document> writtenUpdate) {
            this.connection = connection;
            this.writtenUpdate = writtenUpdate;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
