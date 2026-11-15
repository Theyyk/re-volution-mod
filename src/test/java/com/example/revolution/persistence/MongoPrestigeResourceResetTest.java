package com.example.revolution.persistence;

import static org.junit.jupiter.api.Assertions.*;

import org.bson.Document;
import org.junit.jupiter.api.Test;

/** Resource reset is part of the guarded single-document Prestige commit. */
public final class MongoPrestigeResourceResetTest {
    @Test public void resetsExactlyFourResourcesAndAdvancesEpoch() {
        Document materials = new Document("rune_dust", "12345678901234567890")
                .append("rune_shards", "70.5").append("rune_cores", "9")
                .append("aura_key", "3").append("pearl", "8");
        Document player = new Document("balance", 200)
                .append("crystals", "500")
                .append("resources", materials)
                .append("resourceResetEpoch", "older-run");
        Document filter = new Document("_id", "player").append("prestige.activeRun.runId", "run-new");
        Document set = new Document();

        MongoPrestigeRepository.appendResourceReset(player, filter, set, "run-new");

        assertEquals(200, filter.getInteger("balance"));
        assertEquals("12345678901234567890", filter.get("resources.rune_dust"));
        assertEquals("70.5", filter.get("resources.rune_shards"));
        assertEquals("9", filter.get("resources.rune_cores"));
        assertEquals("older-run", filter.getString("resourceResetEpoch"));
        for (String field : new String[] {
                "balance", "resources.rune_dust", "resources.rune_shards", "resources.rune_cores"
        }) assertEquals("0", set.get(field));
        assertEquals("run-new", set.getString("resourceResetEpoch"));
        assertFalse(set.containsKey("crystals"));
        assertFalse(set.containsKey("resources.aura_key"));
        assertFalse(set.containsKey("resources.pearl"));
    }

    @Test public void legacyMissingFieldsAreMatchedAndResetWithoutMigration() {
        Document player = new Document("crystals", 25);
        Document filter = new Document("_id", "legacy");
        Document set = new Document();
        MongoPrestigeRepository.appendResourceReset(player, filter, set, "first-reset");
        assertNull(filter.get("balance"));
        assertNull(filter.get("resources.rune_dust"));
        assertNull(filter.get("resources.rune_shards"));
        assertNull(filter.get("resources.rune_cores"));
        assertNull(filter.get("resourceResetEpoch"));
        assertEquals("0", set.getString("balance"));
        assertEquals("first-reset", set.getString("resourceResetEpoch"));
    }

    @Test public void invalidInputsCannotBuildReset() {
        assertThrows(IllegalArgumentException.class,
                () -> MongoPrestigeRepository.appendResourceReset(new Document(), new Document(), new Document(), ""));
    }
}
