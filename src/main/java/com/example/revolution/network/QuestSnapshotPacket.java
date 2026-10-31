package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Server-authoritative Quest board snapshot for the functional client UI. */
public final class QuestSnapshotPacket implements IMessage {
    public static final class Entry {
        public String entryId = "";
        public String templateId = "";
        public String eventType = "";
        public String progress = "0";
        public String target = "1";
        public String state = "ACTIVE";
        public int crystalReward;
        public int difficultyTier;

        public Entry() {}
        public Entry(String entryId, String templateId, String eventType, String progress, String target,
                     String state, int crystalReward, int difficultyTier) {
            this.entryId = entryId == null ? "" : entryId;
            this.templateId = templateId == null ? "" : templateId;
            this.eventType = eventType == null ? "" : eventType;
            this.progress = progress == null ? "0" : progress;
            this.target = target == null ? "1" : target;
            this.state = state == null ? "ACTIVE" : state;
            this.crystalReward = Math.max(0, crystalReward);
            this.difficultyTier = Math.max(0, difficultyTier);
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private long cooldownUntil;
    private int rerollsRemaining;
    private long completedQuestCount;

    public QuestSnapshotPacket() {}
    public QuestSnapshotPacket(List<Entry> entries, long cooldownUntil, int rerollsRemaining, long completedQuestCount) {
        if (entries != null) this.entries.addAll(entries);
        this.cooldownUntil = Math.max(0L, cooldownUntil);
        this.rerollsRemaining = Math.max(0, rerollsRemaining);
        this.completedQuestCount = Math.max(0L, completedQuestCount);
    }

    public List<Entry> getEntries() { return Collections.unmodifiableList(entries); }
    public long getCooldownUntil() { return cooldownUntil; }
    public int getRerollsRemaining() { return rerollsRemaining; }
    public long getCompletedQuestCount() { return completedQuestCount; }

    @Override public void fromBytes(ByteBuf buf) {
        entries.clear();
        int count = Math.max(0, Math.min(16, buf.readInt()));
        for (int i = 0; i < count; i++) {
            Entry entry = new Entry();
            entry.entryId = ByteBufUtils.readUTF8String(buf);
            entry.templateId = ByteBufUtils.readUTF8String(buf);
            entry.eventType = ByteBufUtils.readUTF8String(buf);
            entry.progress = ByteBufUtils.readUTF8String(buf);
            entry.target = ByteBufUtils.readUTF8String(buf);
            entry.state = ByteBufUtils.readUTF8String(buf);
            entry.crystalReward = Math.max(0, buf.readInt());
            entry.difficultyTier = Math.max(0, buf.readInt());
            entries.add(entry);
        }
        cooldownUntil = Math.max(0L, buf.readLong());
        rerollsRemaining = Math.max(0, buf.readInt());
        completedQuestCount = Math.max(0L, buf.readLong());
    }

    @Override public void toBytes(ByteBuf buf) {
        buf.writeInt(entries.size());
        for (Entry entry : entries) {
            ByteBufUtils.writeUTF8String(buf, entry.entryId);
            ByteBufUtils.writeUTF8String(buf, entry.templateId);
            ByteBufUtils.writeUTF8String(buf, entry.eventType);
            ByteBufUtils.writeUTF8String(buf, entry.progress);
            ByteBufUtils.writeUTF8String(buf, entry.target);
            ByteBufUtils.writeUTF8String(buf, entry.state);
            buf.writeInt(entry.crystalReward);
            buf.writeInt(entry.difficultyTier);
        }
        buf.writeLong(cooldownUntil);
        buf.writeInt(rerollsRemaining);
        buf.writeLong(completedQuestCount);
    }
}
