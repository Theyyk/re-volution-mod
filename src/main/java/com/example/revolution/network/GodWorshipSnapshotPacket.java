package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Server-authored God Worship state for functional client UI. */
public final class GodWorshipSnapshotPacket implements IMessage {
    public static final class Entry {
        public String godId = "";
        public boolean unlocked;
        public long level;
        public String phase = "LOCKED";
        public long buffActiveUntil;
        public long cooldownUntil;
        public long chargeProgress;
        public long chargeRequired;
        public String unlockCost = "0";
        public String upgradeCost = "0";
        public String resource = "";
        public String primaryChannel = "";
        public String secondaryChannel = "";
        public String currentMultiplier = "1";
        public boolean autoWorshipEnabled;
        public String activationMode = "MANUAL";
        public String linkedGodId = "";

        public Entry() {}
        public Entry(String godId, boolean unlocked, long level, String phase, long buffActiveUntil,
                     long cooldownUntil, long chargeProgress, long chargeRequired, String unlockCost,
                     String upgradeCost, String resource, String primaryChannel, String secondaryChannel,
                     String currentMultiplier, boolean autoWorshipEnabled, String activationMode, String linkedGodId) {
            this.godId = safe(godId); this.unlocked = unlocked; this.level = level; this.phase = safe(phase);
            this.buffActiveUntil = buffActiveUntil; this.cooldownUntil = cooldownUntil;
            this.chargeProgress = chargeProgress; this.chargeRequired = chargeRequired;
            this.unlockCost = safe(unlockCost); this.upgradeCost = safe(upgradeCost); this.resource = safe(resource);
            this.primaryChannel = safe(primaryChannel); this.secondaryChannel = safe(secondaryChannel);
            this.currentMultiplier = safe(currentMultiplier);
            this.autoWorshipEnabled = autoWorshipEnabled; this.activationMode = safe(activationMode);
            this.linkedGodId = safe(linkedGodId);
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private String offerings = "0";
    private long permanentDurationLevel;
    private long permanentDurationBonusSeconds;
    private String durationUpgradeCost = "0";
    private boolean autoActivationEnabled;
    private boolean durationUpgradeEnabled;
    private boolean secondaryBuffsEnabled;

    public GodWorshipSnapshotPacket() {}
    public GodWorshipSnapshotPacket(List<Entry> entries, String offerings, long permanentDurationLevel,
                                    long permanentDurationBonusSeconds, String durationUpgradeCost,
                                    boolean autoActivationEnabled, boolean durationUpgradeEnabled,
                                    boolean secondaryBuffsEnabled) {
        if (entries != null) this.entries.addAll(entries);
        this.offerings = safe(offerings);
        this.permanentDurationLevel = permanentDurationLevel;
        this.permanentDurationBonusSeconds = permanentDurationBonusSeconds;
        this.durationUpgradeCost = safe(durationUpgradeCost);
        this.autoActivationEnabled = autoActivationEnabled;
        this.durationUpgradeEnabled = durationUpgradeEnabled;
        this.secondaryBuffsEnabled = secondaryBuffsEnabled;
    }

    public List<Entry> getEntries() { return Collections.unmodifiableList(entries); }
    public String getOfferings() { return offerings; }
    public long getPermanentDurationLevel() { return permanentDurationLevel; }
    public long getPermanentDurationBonusSeconds() { return permanentDurationBonusSeconds; }
    public String getDurationUpgradeCost() { return durationUpgradeCost; }
    public boolean isAutoActivationEnabled() { return autoActivationEnabled; }
    public boolean isDurationUpgradeEnabled() { return durationUpgradeEnabled; }
    public boolean isSecondaryBuffsEnabled() { return secondaryBuffsEnabled; }

    @Override public void fromBytes(ByteBuf buf) {
        entries.clear();
        int count = Math.max(0, Math.min(5, buf.readInt()));
        for (int i = 0; i < count; i++) {
            Entry e = new Entry();
            e.godId = ByteBufUtils.readUTF8String(buf); e.unlocked = buf.readBoolean(); e.level = buf.readLong();
            e.phase = ByteBufUtils.readUTF8String(buf); e.buffActiveUntil = buf.readLong(); e.cooldownUntil = buf.readLong();
            e.chargeProgress = buf.readLong(); e.chargeRequired = buf.readLong();
            e.unlockCost = ByteBufUtils.readUTF8String(buf); e.upgradeCost = ByteBufUtils.readUTF8String(buf);
            e.resource = ByteBufUtils.readUTF8String(buf); e.primaryChannel = ByteBufUtils.readUTF8String(buf);
            e.secondaryChannel = ByteBufUtils.readUTF8String(buf); e.currentMultiplier = ByteBufUtils.readUTF8String(buf);
            e.autoWorshipEnabled = buf.readBoolean(); e.activationMode = ByteBufUtils.readUTF8String(buf);
            e.linkedGodId = ByteBufUtils.readUTF8String(buf); entries.add(e);
        }
        offerings = ByteBufUtils.readUTF8String(buf); permanentDurationLevel = buf.readLong();
        permanentDurationBonusSeconds = buf.readLong(); durationUpgradeCost = ByteBufUtils.readUTF8String(buf);
        autoActivationEnabled = buf.readBoolean(); durationUpgradeEnabled = buf.readBoolean();
        secondaryBuffsEnabled = buf.readBoolean();
    }

    @Override public void toBytes(ByteBuf buf) {
        buf.writeInt(Math.min(5, entries.size()));
        for (int i = 0; i < entries.size() && i < 5; i++) {
            Entry e = entries.get(i);
            ByteBufUtils.writeUTF8String(buf, safe(e.godId)); buf.writeBoolean(e.unlocked); buf.writeLong(e.level);
            ByteBufUtils.writeUTF8String(buf, safe(e.phase)); buf.writeLong(e.buffActiveUntil); buf.writeLong(e.cooldownUntil);
            buf.writeLong(e.chargeProgress); buf.writeLong(e.chargeRequired);
            ByteBufUtils.writeUTF8String(buf, safe(e.unlockCost)); ByteBufUtils.writeUTF8String(buf, safe(e.upgradeCost));
            ByteBufUtils.writeUTF8String(buf, safe(e.resource)); ByteBufUtils.writeUTF8String(buf, safe(e.primaryChannel));
            ByteBufUtils.writeUTF8String(buf, safe(e.secondaryChannel)); ByteBufUtils.writeUTF8String(buf, safe(e.currentMultiplier));
            buf.writeBoolean(e.autoWorshipEnabled); ByteBufUtils.writeUTF8String(buf, safe(e.activationMode));
            ByteBufUtils.writeUTF8String(buf, safe(e.linkedGodId));
        }
        ByteBufUtils.writeUTF8String(buf, offerings); buf.writeLong(permanentDurationLevel);
        buf.writeLong(permanentDurationBonusSeconds); ByteBufUtils.writeUTF8String(buf, durationUpgradeCost);
        buf.writeBoolean(autoActivationEnabled); buf.writeBoolean(durationUpgradeEnabled); buf.writeBoolean(secondaryBuffsEnabled);
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
