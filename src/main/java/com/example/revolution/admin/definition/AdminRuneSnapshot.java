package com.example.revolution.admin.definition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Read-only regular Rune collection projection for the private Admin API. */
public final class AdminRuneSnapshot {
    public final int schemaVersion;
    public final String playerUuid;
    public final int activeCollection;
    public final List<Collection> collections;

    public AdminRuneSnapshot(String playerUuid, int activeCollection, List<Collection> collections) {
        this.schemaVersion = 1;
        this.playerUuid = playerUuid;
        this.activeCollection = activeCollection;
        this.collections = Collections.unmodifiableList(new ArrayList<>(collections));
    }

    public static final class Collection {
        public final int index;
        public final String name;
        public final boolean active;
        public final List<Rune> runes;

        public Collection(int index, String name, boolean active, List<Rune> runes) {
            this.index = index;
            this.name = name == null ? "" : name;
            this.active = active;
            this.runes = Collections.unmodifiableList(new ArrayList<>(runes));
        }
    }

    public static final class Rune {
        public final int slot;
        public final int runeIndex;
        public final int layer;
        public final int level;

        public Rune(int slot, int runeIndex, int layer, int level) {
            this.slot = slot;
            this.runeIndex = runeIndex;
            this.layer = layer;
            this.level = level;
        }
    }
}
