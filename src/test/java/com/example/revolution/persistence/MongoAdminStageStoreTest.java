package com.example.revolution.persistence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MongoAdminStageStoreTest {
    @Test void populationBalancesFixedAndRandomWithRandomTakingOddRemainder() {
        Document active = spawn("original", 1, true, "FIXED_POINT");
        Document disabled = spawn("disabled", 10, false, "FIXED_POINT");

        assertSplit(MongoAdminStageStore.population(Arrays.asList(active, disabled), 1), 0, 1);
        assertSplit(MongoAdminStageStore.population(Arrays.asList(active, disabled), 2), 1, 1);
        assertSplit(MongoAdminStageStore.population(Arrays.asList(active, disabled), 3), 1, 2);
        assertSplit(MongoAdminStageStore.population(Arrays.asList(active, disabled), 4), 2, 2);
        assertSplit(MongoAdminStageStore.population(Arrays.asList(active, disabled), 5), 2, 3);
        assertSplit(MongoAdminStageStore.population(Arrays.asList(active, disabled), 6), 3, 3);
    }

    @Test void populationRebalancesIncreaseAndDecreaseCases() {
        List<Document> one = Arrays.asList(spawn("one", 1, true, "FIXED_POINT"));
        List<Document> five = MongoAdminStageStore.population(one, 5);
        assertSplit(five, 2, 3); // 1 -> 5

        List<Document> two = MongoAdminStageStore.population(five, 2);
        assertSplit(two, 1, 1); // 5 -> 2

        List<Document> backToOne = MongoAdminStageStore.population(two, 1);
        assertSplit(backToOne, 0, 1); // 2 -> 1
    }

    @Test void populationPreservesDisabledSlotsAndAddsIndependentIdentities() {
        Document active = spawn("original", 1, true, "FIXED_POINT");
        active.append("x", 4.0).append("y", 64.0).append("z", 8.0);
        Document disabled = spawn("disabled", 10, false, "FIXED_POINT");

        List<Document> changed = MongoAdminStageStore.population(Arrays.asList(active, disabled), 3);
        assertEquals(4, changed.size());
        assertEquals(disabled, changed.get(0));
        assertEquals(11, changed.get(2).getInteger("spawnId"));
        assertEquals(12, changed.get(3).getInteger("spawnId"));
        assertFalse(changed.get(2).containsKey("_id"));
        assertEquals(64.0, changed.get(2).getDouble("y"));
        assertEquals("FIXED_POINT", active.getString("spawnMode")); // input is not mutated
    }

    private static Document spawn(String id, int spawnId, boolean enabled, String mode) {
        return new Document("_id", id).append("spawnId", spawnId).append("enabled", enabled).append("spawnMode", mode);
    }

    private static void assertSplit(List<Document> rows, int expectedFixed, int expectedRandom) {
        List<Document> enabled = new ArrayList<>();
        for (Document row : rows) if (row.getBoolean("enabled", false)) enabled.add(row);
        int fixed = 0, random = 0;
        for (Document row : enabled) {
            if ("FIXED_POINT".equals(row.getString("spawnMode"))) fixed++;
            if ("RANDOM_AREA".equals(row.getString("spawnMode"))) random++;
        }
        assertEquals(expectedFixed + expectedRandom, enabled.size());
        assertEquals(expectedFixed, fixed);
        assertEquals(expectedRandom, random);
    }
}
