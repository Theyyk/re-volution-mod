package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Server-authored visible Unlock Journal snapshot. Hidden child definitions are never sent. */
public final class MechanicUnlockSnapshotPacket implements IMessage {
    public static final class Entry {
        public String unlockId = "";
        public String displayName = "";
        public String rarity = "";
        public String parentUnlockId = "";
        public int target;
        public int progress;
        public boolean eligible;
        public boolean unlocked;
    }
    private final List<Entry> entries = new ArrayList<>();
    private boolean readyHighlight;
    private String zbtStatus = "draft";
    public MechanicUnlockSnapshotPacket() {}
    public MechanicUnlockSnapshotPacket(List<Entry> entries, boolean readyHighlight, String zbtStatus) {
        if (entries != null) this.entries.addAll(entries);
        this.readyHighlight = readyHighlight;
        this.zbtStatus = zbtStatus == null ? "" : zbtStatus;
    }
    public List<Entry> getEntries() { return Collections.unmodifiableList(entries); }
    public boolean isReadyHighlight() { return readyHighlight; }
    public String getZbtStatus() { return zbtStatus; }
    @Override public void fromBytes(ByteBuf buf) {
        entries.clear();
        int count = Math.max(0, Math.min(64, buf.readInt()));
        for (int i = 0; i < count; i++) {
            Entry e = new Entry();
            e.unlockId = ByteBufUtils.readUTF8String(buf); e.displayName = ByteBufUtils.readUTF8String(buf);
            e.rarity = ByteBufUtils.readUTF8String(buf); e.parentUnlockId = ByteBufUtils.readUTF8String(buf);
            e.target = Math.max(0, buf.readInt()); e.progress = Math.max(0, buf.readInt());
            e.eligible = buf.readBoolean(); e.unlocked = buf.readBoolean(); entries.add(e);
        }
        readyHighlight = buf.readBoolean(); zbtStatus = ByteBufUtils.readUTF8String(buf);
    }
    @Override public void toBytes(ByteBuf buf) {
        int count = Math.min(64, entries.size()); buf.writeInt(count);
        for (int i = 0; i < count; i++) {
            Entry e = entries.get(i);
            ByteBufUtils.writeUTF8String(buf, safe(e.unlockId)); ByteBufUtils.writeUTF8String(buf, safe(e.displayName));
            ByteBufUtils.writeUTF8String(buf, safe(e.rarity)); ByteBufUtils.writeUTF8String(buf, safe(e.parentUnlockId));
            buf.writeInt(Math.max(0, e.target)); buf.writeInt(Math.max(0, e.progress)); buf.writeBoolean(e.eligible); buf.writeBoolean(e.unlocked);
        }
        buf.writeBoolean(readyHighlight); ByteBufUtils.writeUTF8String(buf, safe(zbtStatus));
    }
    private static String safe(String value) { return value == null ? "" : value; }
}
