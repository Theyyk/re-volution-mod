package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Server-authored Pet UI snapshot. */
public final class PetSnapshotPacket implements IMessage {
    public static final class Entry {
        public String petId = "";
        public String rank = "ORDINARY";
        public String level = "1";
        public String xp = "0";
        public boolean owned;
        public boolean selected;
        public boolean premiumSlot;
        public long cooldownUntil;
        public String purchaseCost = "0";
        public String effects = "";

        public Entry() {}
        public Entry(String petId, String rank, String level, String xp, boolean owned, boolean selected,
                     boolean premiumSlot, long cooldownUntil, String purchaseCost, String effects) {
            this.petId = petId; this.rank = rank; this.level = level; this.xp = xp; this.owned = owned;
            this.selected = selected; this.premiumSlot = premiumSlot; this.cooldownUntil = cooldownUntil;
            this.purchaseCost = purchaseCost; this.effects = effects;
        }
    }

    private int normalSlots;
    private int premiumSlots;
    private String nextNormalSlotCost = "0";
    private final List<Entry> entries = new ArrayList<>();

    public PetSnapshotPacket() {}
    public PetSnapshotPacket(int normalSlots, int premiumSlots, String nextNormalSlotCost, List<Entry> entries) {
        this.normalSlots = normalSlots; this.premiumSlots = premiumSlots;
        this.nextNormalSlotCost = nextNormalSlotCost == null ? "0" : nextNormalSlotCost;
        if (entries != null) this.entries.addAll(entries);
    }
    public int getNormalSlots() { return normalSlots; }
    public int getPremiumSlots() { return premiumSlots; }
    public String getNextNormalSlotCost() { return nextNormalSlotCost; }
    public List<Entry> getEntries() { return Collections.unmodifiableList(entries); }

    @Override public void fromBytes(ByteBuf buf) {
        normalSlots = buf.readInt(); premiumSlots = buf.readInt();
        nextNormalSlotCost = ByteBufUtils.readUTF8String(buf); entries.clear();
        int count = Math.max(0, Math.min(256, buf.readInt()));
        for (int i = 0; i < count; i++) {
            Entry e = new Entry();
            e.petId = ByteBufUtils.readUTF8String(buf); e.rank = ByteBufUtils.readUTF8String(buf);
            e.level = ByteBufUtils.readUTF8String(buf); e.xp = ByteBufUtils.readUTF8String(buf);
            e.owned = buf.readBoolean(); e.selected = buf.readBoolean(); e.premiumSlot = buf.readBoolean();
            e.cooldownUntil = buf.readLong(); e.purchaseCost = ByteBufUtils.readUTF8String(buf);
            e.effects = ByteBufUtils.readUTF8String(buf); entries.add(e);
        }
    }
    @Override public void toBytes(ByteBuf buf) {
        buf.writeInt(normalSlots); buf.writeInt(premiumSlots); ByteBufUtils.writeUTF8String(buf, nextNormalSlotCost);
        buf.writeInt(entries.size());
        for (Entry e : entries) {
            ByteBufUtils.writeUTF8String(buf, e.petId); ByteBufUtils.writeUTF8String(buf, e.rank);
            ByteBufUtils.writeUTF8String(buf, e.level); ByteBufUtils.writeUTF8String(buf, e.xp);
            buf.writeBoolean(e.owned); buf.writeBoolean(e.selected); buf.writeBoolean(e.premiumSlot);
            buf.writeLong(e.cooldownUntil); ByteBufUtils.writeUTF8String(buf, e.purchaseCost);
            ByteBufUtils.writeUTF8String(buf, e.effects);
        }
    }
}
