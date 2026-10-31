package com.example.revolution.client.network;

import com.example.revolution.network.GodWorshipSnapshotPacket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Physical-client cache populated only from authoritative God snapshots. */
public final class GodWorshipClientState {
    private static Snapshot current = Snapshot.empty();
    private GodWorshipClientState() {}

    public static synchronized void update(GodWorshipSnapshotPacket packet) {
        if (packet == null) return;
        current = new Snapshot(
                new ArrayList<>(packet.getEntries()),
                packet.getOfferings(),
                packet.getPermanentDurationLevel(),
                packet.getPermanentDurationBonusSeconds(),
                packet.getDurationUpgradeCost(),
                packet.isAutoActivationEnabled(),
                packet.isDurationUpgradeEnabled(),
                packet.isSecondaryBuffsEnabled());
    }

    public static synchronized Snapshot snapshot() { return current; }
    public static synchronized void clear() { current = Snapshot.empty(); }

    public static final class Snapshot {
        public final List<GodWorshipSnapshotPacket.Entry> entries;
        public final String offerings;
        public final long permanentDurationLevel;
        public final long permanentDurationBonusSeconds;
        public final String durationUpgradeCost;
        public final boolean autoActivationEnabled;
        public final boolean durationUpgradeEnabled;
        public final boolean secondaryBuffsEnabled;

        private Snapshot(List<GodWorshipSnapshotPacket.Entry> entries, String offerings, long permanentDurationLevel,
                         long permanentDurationBonusSeconds, String durationUpgradeCost,
                         boolean autoActivationEnabled, boolean durationUpgradeEnabled, boolean secondaryBuffsEnabled) {
            this.entries = Collections.unmodifiableList(entries);
            this.offerings = offerings == null ? "0" : offerings;
            this.permanentDurationLevel = Math.max(0L, permanentDurationLevel);
            this.permanentDurationBonusSeconds = Math.max(0L, permanentDurationBonusSeconds);
            this.durationUpgradeCost = durationUpgradeCost == null ? "0" : durationUpgradeCost;
            this.autoActivationEnabled = autoActivationEnabled;
            this.durationUpgradeEnabled = durationUpgradeEnabled;
            this.secondaryBuffsEnabled = secondaryBuffsEnabled;
        }

        private static Snapshot empty() {
            return new Snapshot(Collections.emptyList(), "0", 0L, 0L, "0", false, false, false);
        }
    }
}
