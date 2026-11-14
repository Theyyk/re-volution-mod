package com.example.revolution.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.bson.Document;
import org.junit.jupiter.api.Test;

/** Historical player progression and permanent/admin grants are never destructively mixed. */
public final class MongoMobProgressionLegacyVisibilityTest {
    @Test public void provenanceDistinguishesPermanentUnmaskedAndMaskedOldGrants() {
        Document row = new Document("unlockedSlots", Arrays.asList(2, 5, 9))
                .append("legacyRunSlots", Arrays.asList(2, 5))
                .append("permanentSlots", Collections.singletonList(5));
        assertEquals("LEGACY_MASKED", MongoMobProgressionRepository.rawGrantProvenance(row, 2));
        assertEquals("PERMANENT", MongoMobProgressionRepository.rawGrantProvenance(row, 5));
        assertEquals("LEGACY_UNMASKED", MongoMobProgressionRepository.rawGrantProvenance(row, 9));
        assertEquals("NONE", MongoMobProgressionRepository.rawGrantProvenance(row, 10));
        assertEquals("NONE", MongoMobProgressionRepository.rawGrantProvenance(null, 5));
        assertTrue(MongoMobProgressionRepository.visibleSlots(row).contains(5));
        assertTrue(MongoMobProgressionRepository.visibleSlots(row).contains(9));
        assertFalse(MongoMobProgressionRepository.visibleSlots(row).contains(2));
    }

    @Test public void originalLegacyRecordsRemainVisibleBeforeExplicitClassification() {
        Document doc = new Document("unlockedSlots", Arrays.asList(2, 5, 9));
        assertEquals(3, MongoMobProgressionRepository.visibleSlots(doc).size());
        assertTrue(MongoMobProgressionRepository.visibleSlots(doc).contains(9));
    }

    @Test public void classificationMasksOnlyHistoricalRunSlotsAndKeepsOriginalBson() {
        Document doc = new Document("unlockedSlots", Arrays.asList(2, 5, 9))
                .append("legacyRunSlots", Arrays.asList(2, 5, 9));
        assertEquals(Collections.emptySet(), MongoMobProgressionRepository.visibleSlots(doc));
        assertEquals(Arrays.asList(2, 5, 9), doc.get("unlockedSlots"));
        doc.append("permanentSlots", Collections.singletonList(5));
        assertEquals(Collections.singleton(5), MongoMobProgressionRepository.visibleSlots(doc));
    }

    @Test public void addingPermanentSlotPreservesHistoricalMaskVisibility() {
        Document doc = new Document("unlockedSlots", Arrays.asList(2, 5, 9))
                .append("legacyRunSlots", Arrays.asList(2, 5, 9))
                .append("permanentSlots", Arrays.asList(2, 5));
        assertTrue(MongoMobProgressionRepository.visibleSlots(doc).contains(5));
        assertFalse(MongoMobProgressionRepository.visibleSlots(doc).contains(9));
        assertEquals(Arrays.asList(2, 5, 9), doc.get("legacyRunSlots"));
    }

    @Test public void legacyAdminGrantsForOtherUnclassifiedPlayersRemainVisible() {
        Document adminLegacy = new Document("unlockedSlots", Arrays.asList(2, 5));
        assertTrue(MongoMobProgressionRepository.visibleSlots(adminLegacy).contains(5));
        assertFalse(MongoMobProgressionRepository.visibleSlots(new Document("unlockedSlots",
                Arrays.asList(2, 5)).append("legacyRunSlots", Arrays.asList(2, 5))).contains(5));
    }
}
