package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Server-authoritative snapshot used to render the L stage-travel menu. */
public final class StageTravelMenuPacket implements IMessage {
    public static final class Entry {
        private final int worldLocationId;
        private final int mobSlotId;
        private final boolean unlocked;
        private final boolean hasTransitionPoint;

        public Entry(int worldLocationId, int mobSlotId, boolean unlocked, boolean hasTransitionPoint) {
            this.worldLocationId = worldLocationId;
            this.mobSlotId = mobSlotId;
            this.unlocked = unlocked;
            this.hasTransitionPoint = hasTransitionPoint;
        }

        public int getWorldLocationId() { return worldLocationId; }
        public int getMobSlotId() { return mobSlotId; }
        public boolean isUnlocked() { return unlocked; }
        public boolean hasTransitionPoint() { return hasTransitionPoint; }
    }

    private final List<Entry> entries = new ArrayList<>();
    private boolean fastAutoEnabled;

    public StageTravelMenuPacket() {}

    public StageTravelMenuPacket(List<Entry> entries, boolean fastAutoEnabled) {
        if (entries != null) this.entries.addAll(entries);
        this.fastAutoEnabled = fastAutoEnabled;
    }

    public List<Entry> getEntries() { return Collections.unmodifiableList(entries); }
    public boolean isFastAutoEnabled() { return fastAutoEnabled; }

    @Override
    public void fromBytes(ByteBuf buf) {
        entries.clear();
        fastAutoEnabled = buf.readBoolean();
        int count = Math.max(0, Math.min(buf.readInt(), 4096));
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(buf.readInt(), buf.readInt(), buf.readBoolean(), buf.readBoolean()));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(fastAutoEnabled);
        buf.writeInt(entries.size());
        for (Entry entry : entries) {
            buf.writeInt(entry.worldLocationId);
            buf.writeInt(entry.mobSlotId);
            buf.writeBoolean(entry.unlocked);
            buf.writeBoolean(entry.hasTransitionPoint);
        }
    }
}
