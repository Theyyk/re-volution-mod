package com.example.revolution.client;

/** Confirmation must be repeated on the exact same unique-rune tile in the same collection. */
final class UniqueRollbackConfirmation {
    private String collectionId;
    private String runeId;

    boolean isArmed() {
        return collectionId != null && runeId != null;
    }

    void cancel() {
        collectionId = null;
        runeId = null;
    }

    /** First gesture arms; only a second identical target confirms. */
    boolean confirmOrArm(String collection, String rune) {
        if (collection == null || collection.isEmpty() || rune == null || rune.isEmpty()) {
            cancel();
            return false;
        }
        if (collection.equals(collectionId) && rune.equals(runeId)) {
            cancel();
            return true;
        }
        collectionId = collection;
        runeId = rune;
        return false;
    }
}
