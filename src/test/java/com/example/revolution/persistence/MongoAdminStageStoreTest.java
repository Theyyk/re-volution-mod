package com.example.revolution.persistence;

import java.util.Arrays;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MongoAdminStageStoreTest {
    @Test void populationPreservesExistingPointsAndDisabledSlotsAndAddsIndependentIdentities() {
        Document active = new Document("_id", "original").append("spawnId", 1).append("enabled", true)
                .append("x", 4.0).append("y", 64.0).append("z", 8.0).append("spawnMode", "FIXED_POINT");
        Document disabled = new Document("_id", "disabled").append("spawnId", 10).append("enabled", false);
        List<Document> changed = MongoAdminStageStore.population(Arrays.asList(active, disabled), 3);
        assertEquals(4, changed.size()); assertEquals(disabled, changed.get(0)); assertEquals(active, changed.get(1));
        assertEquals(11, changed.get(2).getInteger("spawnId")); assertEquals(12, changed.get(3).getInteger("spawnId"));
        assertFalse(changed.get(2).containsKey("_id")); assertEquals("RANDOM_AREA", changed.get(2).getString("spawnMode"));
        assertEquals(64.0, changed.get(2).getDouble("y")); assertEquals("FIXED_POINT", active.getString("spawnMode"));
    }
}
