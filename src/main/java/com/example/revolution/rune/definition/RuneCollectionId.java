package com.example.revolution.rune.definition;

/** Stable collection identity independent from GUI position or display name. */
public enum RuneCollectionId {
    COLLECTION_01(0, "collection_01"),
    COLLECTION_02(1, "collection_02"),
    COLLECTION_03(2, "collection_03"),
    COLLECTION_04(3, "collection_04"),
    COLLECTION_05(4, "collection_05"),
    COLLECTION_06(5, "collection_06"),
    COLLECTION_07(6, "collection_07");

    private final int legacyIndex;
    private final String id;

    RuneCollectionId(int legacyIndex, String id) {
        this.legacyIndex = legacyIndex;
        this.id = id;
    }

    public int legacyIndex() {
        return legacyIndex;
    }

    public String id() {
        return id;
    }

    public static RuneCollectionId fromLegacyIndex(int index) {
        for (RuneCollectionId collection : values()) {
            if (collection.legacyIndex == index) return collection;
        }
        return null;
    }

    public static RuneCollectionId fromId(String id) {
        if (id == null) return null;
        for (RuneCollectionId collection : values()) {
            if (collection.id.equals(id)) return collection;
        }
        return null;
    }
}
