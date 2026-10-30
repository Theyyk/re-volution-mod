package com.example.revolution.rune.service;

import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.definition.RuneCollectionId;

/** Single compatibility mapping between legacy status balance keys and fixed v2.3.0 collections. */
final class RuneBalanceNamespace {
    private RuneBalanceNamespace() {}

    static String legacyStatus(int statusId) {
        if (!RuneStatusConfig.isRuneStatus(statusId)) {
            throw new IllegalArgumentException("status id must be positive");
        }
        return RuneStatusConfig.statusKey(statusId);
    }

    static String collection(RuneCollectionId collectionId) {
        if (collectionId == null) throw new IllegalArgumentException("collection id is required");
        return collectionId == RuneCollectionId.COLLECTION_01 ? "poison" : collectionId.id();
    }

    static String collection(int collectionIndex) {
        RuneCollectionId id = RuneCollectionId.fromLegacyIndex(collectionIndex);
        if (id == null) throw new IllegalArgumentException("fixed rune collection index must be within 0..6");
        return collection(id);
    }

    static String valueKey(String namespace, String suffix) {
        return "runes." + namespace + "." + suffix;
    }

    static String curveKey(String namespace, String action, int rank) {
        return "runes." + namespace + "." + action + ".rank." + rank;
    }
}
