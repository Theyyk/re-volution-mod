package com.example.revolution.rune;
/** Keeps server records intact and groups them into the visible rune catalog. */
public final class RuneInventory {
    // Read compatibility only: old purchases may use slots 26-49.
    public static final int LEGACY_RECORD_CAPACITY = 50;
    public static final int BOOST_TYPE_COUNT = 26;
    public static final int NORMAL_RANK = RuneProgression.NORMAL_RANK;
    public static final int IMPROVED_RANK = RuneProgression.IMPROVED_RANK;
    public static final int ADVANCED_RANK = RuneProgression.ADVANCED_RANK;

    public static final class Entry {
        public final int runeIndex;
        public final int layer;

        Entry(int runeIndex, int layer) {
            this.runeIndex = runeIndex;
            this.layer = layer;
        }
    }

    private final Entry[] slots = new Entry[LEGACY_RECORD_CAPACITY];

    public void set(int slot, int runeIndex, int layer) {
        if (slot < 0 || slot >= LEGACY_RECORD_CAPACITY || runeIndex < 0 || runeIndex >= BOOST_TYPE_COUNT) return;
        slots[slot] = new Entry(runeIndex, RuneProgression.clampRank(layer));
    }

    public Entry get(int slot) {
        return slot < 0 || slot >= LEGACY_RECORD_CAPACITY ? null : slots[slot];
    }

    public Entry getBoost(int runeIndex) {
        if (runeIndex < 0 || runeIndex >= BOOST_TYPE_COUNT) return null;
        Entry best = null;
        for (Entry entry : slots) {
            if (entry == null || entry.runeIndex != runeIndex) continue;
            if (best == null || entry.layer > best.layer) best = entry;
        }
        return best;
    }

    public int occupiedTypeCount() {
        return occupiedTypeCount(BOOST_TYPE_COUNT);
    }

    public int occupiedTypeCount(int availableTypes) {
        int limit = Math.max(0, Math.min(BOOST_TYPE_COUNT, availableTypes));
        boolean[] present = new boolean[BOOST_TYPE_COUNT];
        int count = 0;
        for (Entry entry : slots) {
            if (entry != null && entry.runeIndex < limit && !present[entry.runeIndex]) {
                present[entry.runeIndex] = true;
                count++;
            }
        }
        return count;
    }

    public int occupiedCount() {
        int count = 0;
        for (Entry entry : slots) if (entry != null) count++;
        return count;
    }

    public int ownedCatalogSlotCount() {
        return occupiedTypeCount(BOOST_TYPE_COUNT);
    }

    public boolean hasAllCatalogRunes() {
        return ownedCatalogSlotCount() == BOOST_TYPE_COUNT;
    }

    public RuneAwakeningDecision awakeningDecision(int runeIndex) {
        return RuneAwakeningDecision.evaluate(bestRanks(), runeIndex);
    }

    public boolean canAwaken(int runeIndex) {
        return awakeningDecision(runeIndex).isAllowed();
    }

    public int[] bestRanks() {
        int[] ranks = new int[BOOST_TYPE_COUNT];
        for (int rune = 0; rune < BOOST_TYPE_COUNT; rune++) {
            Entry best = getBoost(rune);
            ranks[rune] = best == null ? 0 : best.layer;
        }
        return ranks;
    }

    /** Purchase attempts stop once all 26 fixed runes of the element are collected. */
    public boolean isPurchaseLimitReached() {
        return hasAllCatalogRunes();
    }

    /** Legacy helper for old non-element collections that still use fixed storage slots. */
    public static java.util.List<Integer> freePurchaseSlots(java.util.List<Integer> occupiedSlots) {
        java.util.List<Integer> free = new java.util.ArrayList<>();
        for (int slot = 0; slot < BOOST_TYPE_COUNT; slot++) {
            if (!occupiedSlots.contains(slot)) free.add(slot);
        }
        return free;
    }

    public void clear() {
        java.util.Arrays.fill(slots, null);
    }
}
