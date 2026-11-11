package com.example.revolution.persistence;

import com.example.revolution.unique.UniqueRuneModel;
import com.example.revolution.unique.UniqueRuneModel.CollectionId;
import com.example.revolution.unique.UniqueRuneModel.Rarity;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Pure BSON fixture tests: never connect to or modify actual players data. */
final class MongoUniqueRuneMigrationTest {
    private static Document record(String id, String rarity) {
        return new Document("runeId", id).append("rarity", rarity);
    }

    private static Document v1(int count, String rank, boolean pendingRollback) {
        List<Document> owned = new ArrayList<>();
        for (int i = 1; i <= count; i++) owned.add(record(String.format("crystal_%02d", i), rank));
        Document crystal = new Document("currentRarity", rank).append("owned", owned)
                .append("enhancement", java.util.Collections.singletonList(new Document("runeId", "crystal_01").append("level", 17)))
                .append("rollbackRarity", pendingRollback ? "COMMON" : null)
                .append("rollbackOwned", pendingRollback
                        ? java.util.Arrays.asList(record("crystal_01", "COMMON"), record("crystal_26", "COMMON"))
                        : new ArrayList<Document>());
        return new Document("schemaVersion", 1)
                .append("lastDailyClaimDay", 12345L)
                .append("collections", new Document(CollectionId.CRYSTAL_COLLECTION.name(), crystal));
    }

    @Test void preservesPartialAndFullCollectionsEverySupportedRank() {
        for (String rank : new String[]{"COMMON", "IMPROVED", "ADVANCED"}) {
            for (int count : new int[]{1, 13, 25, 26}) {
                Document source = v1(count, rank, false);
                UniqueRuneModel.PlayerState state = MongoUniqueRuneRepository.decode(source);
                assertEquals(count, state.collection(CollectionId.CRYSTAL_COLLECTION).owned.size());
                assertEquals(Rarity.valueOf(rank), state.collection(CollectionId.CRYSTAL_COLLECTION).owned.get("crystal_01"));
                assertEquals(17, state.collection(CollectionId.CRYSTAL_COLLECTION).enhancement("crystal_01"));
                assertEquals(12345L, state.lastDailyClaimDay);
                Document upgraded = MongoUniqueRuneRepository.encode(state);
                assertEquals(3, upgraded.getInteger("schemaVersion"));
                UniqueRuneModel.PlayerState reloaded = MongoUniqueRuneRepository.decode(upgraded);
                assertEquals(state.collection(CollectionId.CRYSTAL_COLLECTION).owned, reloaded.collection(CollectionId.CRYSTAL_COLLECTION).owned);
                assertEquals(state.collection(CollectionId.CRYSTAL_COLLECTION).enhancement, reloaded.collection(CollectionId.CRYSTAL_COLLECTION).enhancement);
                assertEquals(12345L, reloaded.lastDailyClaimDay);
                assertEquals(source.get("collections", Document.class).get(CollectionId.CRYSTAL_COLLECTION.name(), Document.class),
                        upgraded.get("collections", Document.class).get(CollectionId.CRYSTAL_COLLECTION.name(), Document.class)
                                .get("legacyV1Snapshot", Document.class));
            }
        }
    }

    @Test void ambiguousLegacyRollbackNeverRevivesAbsentRuneOrCreatesIndividualHistory() {
        Document source = v1(1, "ADVANCED", true);
        UniqueRuneModel.PlayerState state = MongoUniqueRuneRepository.decode(source);
        assertEquals(1, state.collection(CollectionId.CRYSTAL_COLLECTION).owned.size());
        assertFalse(state.collection(CollectionId.CRYSTAL_COLLECTION).owned.containsKey("crystal_26"));
        assertTrue(state.collection(CollectionId.CRYSTAL_COLLECTION).runeRollback.isEmpty());
        Document upgraded = MongoUniqueRuneRepository.encode(state);
        UniqueRuneModel.PlayerState again = MongoUniqueRuneRepository.decode(upgraded);
        assertTrue(again.collection(CollectionId.CRYSTAL_COLLECTION).runeRollback.isEmpty());
        assertEquals(1, again.collection(CollectionId.CRYSTAL_COLLECTION).owned.size());
        assertEquals(Rarity.COMMON, again.collection(CollectionId.CRYSTAL_COLLECTION).rollbackOwned.get("crystal_26"));
        assertEquals(upgraded, MongoUniqueRuneRepository.encode(again));
    }

    @Test void individualRollbackRoundTripsWithoutAffectingOtherRune() {
        UniqueRuneModel.PlayerState state = new UniqueRuneModel.PlayerState();
        state.collection(CollectionId.DAILY_COLLECTION).owned.put("daily_01", Rarity.IMPROVED);
        state.collection(CollectionId.DAILY_COLLECTION).owned.put("daily_02", Rarity.ADVANCED);
        state.collection(CollectionId.DAILY_COLLECTION).runeRollback.put("daily_01", Rarity.COMMON);
        Document v2 = MongoUniqueRuneRepository.encode(state);
        UniqueRuneModel.PlayerState loaded = MongoUniqueRuneRepository.decode(v2);
        assertEquals(Rarity.COMMON, loaded.collection(CollectionId.DAILY_COLLECTION).runeRollback.get("daily_01"));
        assertFalse(loaded.collection(CollectionId.DAILY_COLLECTION).runeRollback.containsKey("daily_02"));
        assertEquals(Rarity.ADVANCED, loaded.collection(CollectionId.DAILY_COLLECTION).owned.get("daily_02"));
    }

    @Test void confirmedDailyTimestampRoundTripsAndLegacyDayRemainsReadable() {
        UniqueRuneModel.PlayerState state = new UniqueRuneModel.PlayerState();
        state.lastDailyClaimDay = 30001L;
        state.lastDailyClaimMillis = 30001L * 86400000L + 43200000L;
        Document encoded = MongoUniqueRuneRepository.encode(state);
        UniqueRuneModel.PlayerState restored = MongoUniqueRuneRepository.decode(encoded);
        assertEquals(state.lastDailyClaimDay, restored.lastDailyClaimDay);
        assertEquals(state.lastDailyClaimMillis, restored.lastDailyClaimMillis);
        assertEquals(encoded, MongoUniqueRuneRepository.encode(restored));

        UniqueRuneModel.PlayerState legacy = MongoUniqueRuneRepository.decode(v1(1, "COMMON", false));
        assertEquals(12345L, legacy.lastDailyClaimDay);
        assertEquals(Long.MIN_VALUE, legacy.lastDailyClaimMillis);
    }

    @Test void v2MigrationDoesNotInventCommonUnderUpgradedRune() {
        Document source = v1(26, "COMMON", false).append("schemaVersion", 2);
        List<Document> owned = source.get("collections", Document.class)
                .get(CollectionId.CRYSTAL_COLLECTION.name(), Document.class).getList("owned", Document.class);
        owned.get(0).put("rarity", "IMPROVED");
        UniqueRuneModel.PlayerState migrated = MongoUniqueRuneRepository.decode(source);
        UniqueRuneModel.CollectionState crystal = migrated.collection(CollectionId.CRYSTAL_COLLECTION);
        assertEquals(25, crystal.acquisitionCommon.size());
        assertFalse(crystal.acquisitionCommon.contains("crystal_01"));
        assertEquals(Rarity.IMPROVED, crystal.owned.get("crystal_01"));
        Document upgraded = MongoUniqueRuneRepository.encode(migrated);
        assertEquals(3, upgraded.getInteger("schemaVersion"));
        UniqueRuneModel.PlayerState again = MongoUniqueRuneRepository.decode(upgraded);
        assertEquals(crystal.acquisitionCommon, again.collection(CollectionId.CRYSTAL_COLLECTION).acquisitionCommon);
        assertEquals(upgraded, MongoUniqueRuneRepository.encode(again));
    }

    @Test void v3RoundTripsIndependentCommonAcquisitionUnderMythicRune() {
        UniqueRuneModel.PlayerState state = new UniqueRuneModel.PlayerState();
        UniqueRuneModel.CollectionState crystal = state.collection(CollectionId.CRYSTAL_COLLECTION);
        crystal.owned.put("crystal_01", Rarity.MYTHIC);
        crystal.enhancement.put("crystal_01", 7);
        crystal.runeRollback.put("crystal_01", Rarity.ADVANCED);
        crystal.acquisitionCommon.add("crystal_01");
        crystal.acquisitionCommon.add("crystal_02");
        Document encoded = MongoUniqueRuneRepository.encode(state);
        UniqueRuneModel.PlayerState decoded = MongoUniqueRuneRepository.decode(encoded);
        UniqueRuneModel.CollectionState result = decoded.collection(CollectionId.CRYSTAL_COLLECTION);
        assertEquals(Rarity.MYTHIC, result.owned.get("crystal_01"));
        assertEquals(7, result.enhancement("crystal_01"));
        assertEquals(Rarity.ADVANCED, result.runeRollback.get("crystal_01"));
        assertEquals(2, result.acquisitionCommon.size());
        assertEquals(encoded, MongoUniqueRuneRepository.encode(decoded));
        assertEquals(crystal.acquisitionCommon,
                state.copy().collection(CollectionId.CRYSTAL_COLLECTION).acquisitionCommon);
    }

    @Test void v2RanksSeedOnlyTheirOwnProvenCohorts() {
        Document source = v1(26, "COMMON", false).append("schemaVersion", 2);
        List<Document> owned = source.get("collections", Document.class)
                .get(CollectionId.CRYSTAL_COLLECTION.name(), Document.class).getList("owned", Document.class);
        owned.get(0).put("rarity", "IMPROVED");
        owned.get(1).put("rarity", "ADVANCED");
        owned.get(2).put("rarity", "MYTHIC");
        UniqueRuneModel.CollectionState result = MongoUniqueRuneRepository.decode(source)
                .collection(CollectionId.CRYSTAL_COLLECTION);
        assertEquals(23, result.acquisitionCommon.size());
        assertEquals(1, result.acquisitionImproved.size());
        assertEquals(1, result.acquisitionAdvanced.size());
        assertEquals(1, result.acquisitionMythic.size());
        assertFalse(result.acquisitionCommon.contains("crystal_03"));
        assertFalse(result.acquisitionAdvanced.contains("crystal_03"));
        assertEquals(Rarity.MYTHIC, result.owned.get("crystal_03"));
    }

    @Test void v3PersistsIndependentTierCohortsAndExactRestore() {
        UniqueRuneModel.PlayerState state = new UniqueRuneModel.PlayerState();
        UniqueRuneModel.CollectionState crystal = state.collection(CollectionId.CRYSTAL_COLLECTION);
        crystal.owned.put("crystal_01", Rarity.ADVANCED);
        crystal.enhancement.put("crystal_01", 18);
        crystal.acquisitionCommon.add("crystal_01");
        crystal.acquisitionImproved.add("crystal_01");
        crystal.acquisitionImproved.add("crystal_02");
        crystal.acquisitionAdvanced.add("crystal_01");
        crystal.acquisitionMythic.add("crystal_01");
        crystal.rollbackRestore.put("crystal_01", Rarity.MYTHIC);
        Document encoded = MongoUniqueRuneRepository.encode(state);
        UniqueRuneModel.CollectionState loaded = MongoUniqueRuneRepository.decode(encoded)
                .collection(CollectionId.CRYSTAL_COLLECTION);
        assertEquals(Rarity.ADVANCED, loaded.owned.get("crystal_01"));
        assertEquals(18, loaded.enhancement("crystal_01"));
        assertEquals(crystal.acquisitionCommon, loaded.acquisitionCommon);
        assertEquals(crystal.acquisitionImproved, loaded.acquisitionImproved);
        assertEquals(crystal.acquisitionAdvanced, loaded.acquisitionAdvanced);
        assertEquals(crystal.acquisitionMythic, loaded.acquisitionMythic);
        assertEquals(crystal.rollbackRestore, loaded.rollbackRestore);
        assertEquals(encoded, MongoUniqueRuneRepository.encode(MongoUniqueRuneRepository.decode(encoded)));
    }

    @Test void v3RejectsBadCrossCollectionAndDuplicatedAdvancedCohort() {
        Document source = MongoUniqueRuneRepository.encode(new UniqueRuneModel.PlayerState());
        Document crystal = source.get("collections", Document.class)
                .get(CollectionId.CRYSTAL_COLLECTION.name(), Document.class);
        crystal.put("acquisitionAdvanced", java.util.Arrays.asList("crystal_04", "crystal_04"));
        assertThrows(IllegalStateException.class, () -> MongoUniqueRuneRepository.decode(source));
        crystal.put("acquisitionAdvanced", java.util.Collections.singletonList("daily_04"));
        assertThrows(IllegalStateException.class, () -> MongoUniqueRuneRepository.decode(source));
    }

    @Test void malformedV3CohortFailsClosed() {
        Document source = MongoUniqueRuneRepository.encode(new UniqueRuneModel.PlayerState());
        Document crystal = source.get("collections", Document.class)
                .get(CollectionId.CRYSTAL_COLLECTION.name(), Document.class);
        crystal.put("acquisitionCommon", java.util.Arrays.asList("crystal_01", "crystal_01"));
        assertThrows(IllegalStateException.class, () -> MongoUniqueRuneRepository.decode(source));
        crystal.put("acquisitionCommon", java.util.Collections.singletonList("daily_01"));
        assertThrows(IllegalStateException.class, () -> MongoUniqueRuneRepository.decode(source));
    }

    @Test void futureSchemaAndDuplicateOwnershipFailClosed() {
        Document future = v1(1, "COMMON", false).append("schemaVersion", 999);
        assertThrows(IllegalStateException.class, () -> MongoUniqueRuneRepository.decode(future));
        Document duplicated = v1(1, "COMMON", false);
        duplicated.get("collections", Document.class).get(CollectionId.CRYSTAL_COLLECTION.name(), Document.class)
                .getList("owned", Document.class).add(record("crystal_01", "COMMON"));
        assertThrows(IllegalStateException.class, () -> MongoUniqueRuneRepository.decode(duplicated));
    }
}
