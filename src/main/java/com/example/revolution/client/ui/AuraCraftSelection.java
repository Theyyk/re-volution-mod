package com.example.revolution.client.ui;

import com.example.revolution.network.AuraSnapshotPacket;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Only explicit, distinct, non-active and non-pinned Aura instances can be submitted. */
public final class AuraCraftSelection {
    private AuraCraftSelection() {}
    public static boolean eligible(AuraSnapshotPacket.Entry entry, String rarity) {
        return entry != null && rarity != null && rarity.equals(entry.rarity)
                && !entry.active && !entry.pinned && entry.instanceId != null && !entry.instanceId.isEmpty();
    }
    public static boolean valid(List<AuraSnapshotPacket.Entry> entries, String rarity, List<String> ids, int required) {
        if (entries == null || ids == null || required <= 0 || ids.size() != required
                || new HashSet<>(ids).size() != required) return false;
        for (String id : ids) {
            boolean found = false;
            for (AuraSnapshotPacket.Entry entry : entries)
                if (eligible(entry, rarity) && entry.instanceId.equals(id)) { found = true; break; }
            if (!found) return false;
        }
        return true;
    }
}
