package com.example.customguimod;

/** Keeps server records intact and groups them into the visible rune catalog. */
final class RuneInventory {
    // Read compatibility only: old purchases may use slots 26-49.
    static final int LEGACY_RECORD_CAPACITY = 50;
    static final int BOOST_TYPE_COUNT = 26;
    static final int NORMAL_RANK = RuneProgression.NORMAL_RANK;
    static final int IMPROVED_RANK = RuneProgression.IMPROVED_RANK;
    static final int ADVANCED_RANK = RuneProgression.ADVANCED_RANK;

    static final class Entry {
        final int cardIndex;
        final int layer;

        Entry(int cardIndex, int layer) {
            this.cardIndex = cardIndex;
            this.layer = layer;
        }
    }

    private final Entry[] slots = new Entry[LEGACY_RECORD_CAPACITY];

    void set(int slot, int cardIndex, int layer) {
        if (slot < 0 || slot >= LEGACY_RECORD_CAPACITY || cardIndex < 0 || cardIndex >= BOOST_TYPE_COUNT) return;
        slots[slot] = new Entry(cardIndex, RuneProgression.clampRank(layer));
    }

    Entry get(int slot) {
        return slot < 0 || slot >= LEGACY_RECORD_CAPACITY ? null : slots[slot];
    }

    Entry getBoost(int cardIndex) {
        if (cardIndex < 0 || cardIndex >= BOOST_TYPE_COUNT) return null;
        Entry best = null;
        for (Entry entry : slots) {
            if (entry == null || entry.cardIndex != cardIndex) continue;
            if (best == null || entry.layer > best.layer) best = entry;
        }
        return best;
    }

    int occupiedTypeCount() {
        return occupiedTypeCount(BOOST_TYPE_COUNT);
    }

    int occupiedTypeCount(int availableTypes) {
        int limit = Math.max(0, Math.min(BOOST_TYPE_COUNT, availableTypes));
        boolean[] present = new boolean[BOOST_TYPE_COUNT];
        int count = 0;
        for (Entry entry : slots) {
            if (entry != null && entry.cardIndex < limit && !present[entry.cardIndex]) {
                present[entry.cardIndex] = true;
                count++;
            }
        }
        return count;
    }

    int occupiedCount() {
        int count = 0;
        for (Entry entry : slots) if (entry != null) count++;
        return count;
    }

    int ownedCatalogSlotCount() {
        return occupiedTypeCount(BOOST_TYPE_COUNT);
    }

    boolean hasAllCatalogRunes() {
        return ownedCatalogSlotCount() == BOOST_TYPE_COUNT;
    }

    RuneAwakeningDecision awakeningDecision(int cardIndex) {
        return RuneAwakeningDecision.evaluate(bestRanks(), cardIndex);
    }

    boolean canAwaken(int cardIndex) {
        return awakeningDecision(cardIndex).isAllowed();
    }

    int[] bestRanks() {
        int[] ranks = new int[BOOST_TYPE_COUNT];
        for (int rune = 0; rune < BOOST_TYPE_COUNT; rune++) {
            Entry best = getBoost(rune);
            ranks[rune] = best == null ? 0 : best.layer;
        }
        return ranks;
    }

    /** Purchase attempts stop once all 26 fixed runes of the element are collected. */
    boolean isPurchaseLimitReached() {
        return hasAllCatalogRunes();
    }

    /** Legacy helper for old non-element collections that still use fixed storage slots. */
    static java.util.List<Integer> freePurchaseSlots(java.util.List<Integer> occupiedSlots) {
        java.util.List<Integer> free = new java.util.ArrayList<>();
        for (int slot = 0; slot < BOOST_TYPE_COUNT; slot++) {
            if (!occupiedSlots.contains(slot)) free.add(slot);
        }
        return free;
    }

    void clear() {
        java.util.Arrays.fill(slots, null);
    }
}
