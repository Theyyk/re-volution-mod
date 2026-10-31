package com.example.revolution.client;

import com.example.revolution.network.AchievementSnapshotPacket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Read-only client mirror of the latest server-authored Achievement snapshot. */
public final class ClientAchievements {
    private static final List<AchievementSnapshotPacket.Entry> ENTRIES = new ArrayList<>();
    private static long points;
    private static int level = 1;
    private static String bonusPercent = "0";
    private static String nextLevelThreshold = "0";

    private ClientAchievements() {}

    public static synchronized void accept(AchievementSnapshotPacket packet) {
        ENTRIES.clear();
        ENTRIES.addAll(packet.getEntries());
        points = packet.getTotalAchievementPoints();
        level = packet.getAchievementLevel();
        bonusPercent = packet.getEconomyBonusPercent();
        nextLevelThreshold = packet.getNextLevelThreshold();
    }

    public static synchronized List<AchievementSnapshotPacket.Entry> entries() {
        return Collections.unmodifiableList(new ArrayList<>(ENTRIES));
    }
    public static synchronized long points() { return points; }
    public static synchronized int level() { return level; }
    public static synchronized String bonusPercent() { return bonusPercent; }
    public static synchronized String nextLevelThreshold() { return nextLevelThreshold; }
}
