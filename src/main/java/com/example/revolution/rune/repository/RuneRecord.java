package com.example.revolution.rune.repository;

/** Immutable stored rune values; legacy Mongo field names stay in the mapper. */
public final class RuneRecord {
    public final int slot, runeIndex, layer, level;
    public RuneRecord(int slot, int runeIndex, int layer, int level) {
        this.slot = slot; this.runeIndex = runeIndex; this.layer = layer; this.level = level;
    }
}
