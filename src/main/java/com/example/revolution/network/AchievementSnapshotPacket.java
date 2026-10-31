package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Complete server-authored functional Achievement UI state. */
public final class AchievementSnapshotPacket implements IMessage {
    public static final class Entry {
        public String achievementId = "";
        public String name = "";
        public String category = "";
        public int stageIndex;
        public String stageRoman = "";
        public String progress = "0";
        public String target = "0";
        public String crystals = "0";
        public String offerings = "0";
        public String auraKeys = "0";
        public long achievementPoints;
        public int highestCompletedStage;
        public int highestClaimedStage;
        public boolean completed;
        public boolean claimed;
        public boolean finalStage;
        public boolean hidden;
        public boolean revealed;

        public Entry() {}
    }

    private final List<Entry> entries = new ArrayList<>();
    private long totalAchievementPoints;
    private int achievementLevel;
    private String economyBonusPercent = "0";
    private String nextLevelThreshold = "0";

    public AchievementSnapshotPacket() {}

    public AchievementSnapshotPacket(List<Entry> entries, long totalAchievementPoints, int achievementLevel,
                                     String economyBonusPercent, String nextLevelThreshold) {
        if (entries != null) this.entries.addAll(entries);
        this.totalAchievementPoints = Math.max(0L, totalAchievementPoints);
        this.achievementLevel = Math.max(1, achievementLevel);
        this.economyBonusPercent = safe(economyBonusPercent);
        this.nextLevelThreshold = safe(nextLevelThreshold);
    }

    public List<Entry> getEntries() { return Collections.unmodifiableList(entries); }
    public long getTotalAchievementPoints() { return totalAchievementPoints; }
    public int getAchievementLevel() { return achievementLevel; }
    public String getEconomyBonusPercent() { return economyBonusPercent; }
    public String getNextLevelThreshold() { return nextLevelThreshold; }

    @Override public void fromBytes(ByteBuf buf) {
        entries.clear();
        int count = Math.max(0, Math.min(10000, buf.readInt()));
        for (int i = 0; i < count; i++) {
            Entry e = new Entry();
            e.achievementId = ByteBufUtils.readUTF8String(buf);
            e.name = ByteBufUtils.readUTF8String(buf);
            e.category = ByteBufUtils.readUTF8String(buf);
            e.stageIndex = Math.max(0, buf.readInt());
            e.stageRoman = ByteBufUtils.readUTF8String(buf);
            e.progress = ByteBufUtils.readUTF8String(buf);
            e.target = ByteBufUtils.readUTF8String(buf);
            e.crystals = ByteBufUtils.readUTF8String(buf);
            e.offerings = ByteBufUtils.readUTF8String(buf);
            e.auraKeys = ByteBufUtils.readUTF8String(buf);
            e.achievementPoints = Math.max(0L, buf.readLong());
            e.highestCompletedStage = Math.max(0, buf.readInt());
            e.highestClaimedStage = Math.max(0, buf.readInt());
            e.completed = buf.readBoolean();
            e.claimed = buf.readBoolean();
            e.finalStage = buf.readBoolean();
            e.hidden = buf.readBoolean();
            e.revealed = buf.readBoolean();
            entries.add(e);
        }
        totalAchievementPoints = Math.max(0L, buf.readLong());
        achievementLevel = Math.max(1, buf.readInt());
        economyBonusPercent = ByteBufUtils.readUTF8String(buf);
        nextLevelThreshold = ByteBufUtils.readUTF8String(buf);
    }

    @Override public void toBytes(ByteBuf buf) {
        int count = Math.min(10000, entries.size());
        buf.writeInt(count);
        for (int i = 0; i < count; i++) {
            Entry e = entries.get(i);
            ByteBufUtils.writeUTF8String(buf, safe(e.achievementId));
            ByteBufUtils.writeUTF8String(buf, safe(e.name));
            ByteBufUtils.writeUTF8String(buf, safe(e.category));
            buf.writeInt(Math.max(0, e.stageIndex));
            ByteBufUtils.writeUTF8String(buf, safe(e.stageRoman));
            ByteBufUtils.writeUTF8String(buf, safe(e.progress));
            ByteBufUtils.writeUTF8String(buf, safe(e.target));
            ByteBufUtils.writeUTF8String(buf, safe(e.crystals));
            ByteBufUtils.writeUTF8String(buf, safe(e.offerings));
            ByteBufUtils.writeUTF8String(buf, safe(e.auraKeys));
            buf.writeLong(Math.max(0L, e.achievementPoints));
            buf.writeInt(Math.max(0, e.highestCompletedStage));
            buf.writeInt(Math.max(0, e.highestClaimedStage));
            buf.writeBoolean(e.completed);
            buf.writeBoolean(e.claimed);
            buf.writeBoolean(e.finalStage);
            buf.writeBoolean(e.hidden);
            buf.writeBoolean(e.revealed);
        }
        buf.writeLong(totalAchievementPoints);
        buf.writeInt(achievementLevel);
        ByteBufUtils.writeUTF8String(buf, safe(economyBonusPercent));
        ByteBufUtils.writeUTF8String(buf, safe(nextLevelThreshold));
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
