package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Complete factual state for the functional Unique Rune UI. Unknown rune identities are omitted. */
public final class UniqueRuneSnapshotPacket implements IMessage {
    public static final class Entry {
        public String collectionId = "";
        public String runeId = "";
        public String name = "";
        public String rarity = "";
        public int enhancement;
        public String effects = "";
    }
    public static final class Collection {
        public String collectionId = "";
        public String rarity = "COMMON";
        public int progress;
        public boolean unlocked;
        public boolean canAwaken;
        public boolean canRollback;
    }

    private final List<Entry> entries = new ArrayList<>();
    private final List<Collection> collections = new ArrayList<>();
    private String pearl = "0";
    private String crystals = "0";
    private String offerings = "0";
    private String crystalPurchaseCost = "0";
    private String offeringsPurchaseCost = "0";
    private String zbtStatus = "draft";

    public UniqueRuneSnapshotPacket() {}
    public UniqueRuneSnapshotPacket(List<Entry> entries, List<Collection> collections, String pearl, String crystals,
                                    String offerings, String crystalPurchaseCost, String offeringsPurchaseCost, String zbtStatus) {
        if (entries != null) this.entries.addAll(entries); if (collections != null) this.collections.addAll(collections);
        this.pearl = safe(pearl); this.crystals = safe(crystals); this.offerings = safe(offerings);
        this.crystalPurchaseCost = safe(crystalPurchaseCost); this.offeringsPurchaseCost = safe(offeringsPurchaseCost); this.zbtStatus = safe(zbtStatus);
    }
    public List<Entry> getEntries() { return Collections.unmodifiableList(entries); }
    public List<Collection> getCollections() { return Collections.unmodifiableList(collections); }
    public String getPearl() { return pearl; }
    public String getCrystals() { return crystals; }
    public String getOfferings() { return offerings; }
    public String getCrystalPurchaseCost() { return crystalPurchaseCost; }
    public String getOfferingsPurchaseCost() { return offeringsPurchaseCost; }
    public String getZbtStatus() { return zbtStatus; }

    @Override public void fromBytes(ByteBuf buf) {
        entries.clear(); collections.clear();
        int collectionCount = Math.max(0, Math.min(3, buf.readInt()));
        for (int i = 0; i < collectionCount; i++) { Collection c = new Collection(); c.collectionId = ByteBufUtils.readUTF8String(buf); c.rarity = ByteBufUtils.readUTF8String(buf); c.progress = Math.max(0, Math.min(26, buf.readInt())); c.unlocked = buf.readBoolean(); c.canAwaken = buf.readBoolean(); c.canRollback = buf.readBoolean(); collections.add(c); }
        int count = Math.max(0, Math.min(78, buf.readInt()));
        for (int i = 0; i < count; i++) { Entry e = new Entry(); e.collectionId = ByteBufUtils.readUTF8String(buf); e.runeId = ByteBufUtils.readUTF8String(buf); e.name = ByteBufUtils.readUTF8String(buf); e.rarity = ByteBufUtils.readUTF8String(buf); e.enhancement = Math.max(0, buf.readInt()); e.effects = ByteBufUtils.readUTF8String(buf); entries.add(e); }
        pearl = ByteBufUtils.readUTF8String(buf); crystals = ByteBufUtils.readUTF8String(buf); offerings = ByteBufUtils.readUTF8String(buf);
        crystalPurchaseCost = ByteBufUtils.readUTF8String(buf); offeringsPurchaseCost = ByteBufUtils.readUTF8String(buf); zbtStatus = ByteBufUtils.readUTF8String(buf);
    }
    @Override public void toBytes(ByteBuf buf) {
        int collectionCount = Math.min(3, collections.size()); buf.writeInt(collectionCount);
        for (int i = 0; i < collectionCount; i++) { Collection c = collections.get(i); ByteBufUtils.writeUTF8String(buf, safe(c.collectionId)); ByteBufUtils.writeUTF8String(buf, safe(c.rarity)); buf.writeInt(Math.max(0, Math.min(26, c.progress))); buf.writeBoolean(c.unlocked); buf.writeBoolean(c.canAwaken); buf.writeBoolean(c.canRollback); }
        int count = Math.min(78, entries.size()); buf.writeInt(count);
        for (int i = 0; i < count; i++) { Entry e = entries.get(i); ByteBufUtils.writeUTF8String(buf, safe(e.collectionId)); ByteBufUtils.writeUTF8String(buf, safe(e.runeId)); ByteBufUtils.writeUTF8String(buf, safe(e.name)); ByteBufUtils.writeUTF8String(buf, safe(e.rarity)); buf.writeInt(Math.max(0, e.enhancement)); ByteBufUtils.writeUTF8String(buf, safe(e.effects)); }
        ByteBufUtils.writeUTF8String(buf, safe(pearl)); ByteBufUtils.writeUTF8String(buf, safe(crystals)); ByteBufUtils.writeUTF8String(buf, safe(offerings));
        ByteBufUtils.writeUTF8String(buf, safe(crystalPurchaseCost)); ByteBufUtils.writeUTF8String(buf, safe(offeringsPurchaseCost)); ByteBufUtils.writeUTF8String(buf, safe(zbtStatus));
    }
    private static String safe(String value) { return value == null ? "" : value; }
}
