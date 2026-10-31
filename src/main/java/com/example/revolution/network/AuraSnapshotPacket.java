package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Server-authored Aura collection snapshot used by the functional client menus. */
public final class AuraSnapshotPacket implements IMessage {
    public static final class Entry {
        public String instanceId = "";
        public String rarity = "";
        public String origin = "";
        public long generationSeed;
        public String rawRolls = "";
        public String aggregatedEffects = "";
        public boolean pinned;
        public boolean active;

        public Entry() {}
        public Entry(String instanceId, String rarity, String origin, long generationSeed,
                     String rawRolls, String aggregatedEffects, boolean pinned, boolean active) {
            this.instanceId = safe(instanceId); this.rarity = safe(rarity); this.origin = safe(origin);
            this.generationSeed = generationSeed; this.rawRolls = safe(rawRolls);
            this.aggregatedEffects = safe(aggregatedEffects); this.pinned = pinned; this.active = active;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private int auraKeys;
    private int crystals;
    private int keyCrystalPrice;
    private long totalChestOpens;
    private long totalChestAuraDrops;
    private boolean skipOpeningAnimation;
    private boolean featureEnabled;
    private String rarityRules = "";

    public AuraSnapshotPacket() {}
    public AuraSnapshotPacket(List<Entry> entries, int auraKeys, int crystals, int keyCrystalPrice,
                              long totalChestOpens, long totalChestAuraDrops,
                              boolean skipOpeningAnimation, boolean featureEnabled, String rarityRules) {
        if (entries != null) this.entries.addAll(entries);
        this.auraKeys = Math.max(0, auraKeys); this.crystals = Math.max(0, crystals);
        this.keyCrystalPrice = Math.max(0, keyCrystalPrice); this.totalChestOpens = Math.max(0L, totalChestOpens);
        this.totalChestAuraDrops = Math.max(0L, totalChestAuraDrops);
        this.skipOpeningAnimation = skipOpeningAnimation; this.featureEnabled = featureEnabled;
        this.rarityRules = safe(rarityRules);
    }

    public List<Entry> getEntries() { return Collections.unmodifiableList(entries); }
    public int getAuraKeys() { return auraKeys; }
    public int getCrystals() { return crystals; }
    public int getKeyCrystalPrice() { return keyCrystalPrice; }
    public long getTotalChestOpens() { return totalChestOpens; }
    public long getTotalChestAuraDrops() { return totalChestAuraDrops; }
    public boolean isSkipOpeningAnimation() { return skipOpeningAnimation; }
    public boolean isFeatureEnabled() { return featureEnabled; }
    public String getRarityRules() { return rarityRules; }

    @Override public void fromBytes(ByteBuf buf) {
        entries.clear(); int count = Math.max(0, Math.min(10000, buf.readInt()));
        for (int i = 0; i < count; i++) {
            Entry e = new Entry(); e.instanceId = ByteBufUtils.readUTF8String(buf); e.rarity = ByteBufUtils.readUTF8String(buf);
            e.origin = ByteBufUtils.readUTF8String(buf); e.generationSeed = buf.readLong();
            e.rawRolls = ByteBufUtils.readUTF8String(buf); e.aggregatedEffects = ByteBufUtils.readUTF8String(buf);
            e.pinned = buf.readBoolean(); e.active = buf.readBoolean(); entries.add(e);
        }
        auraKeys = Math.max(0, buf.readInt()); crystals = Math.max(0, buf.readInt()); keyCrystalPrice = Math.max(0, buf.readInt());
        totalChestOpens = Math.max(0L, buf.readLong()); totalChestAuraDrops = Math.max(0L, buf.readLong());
        skipOpeningAnimation = buf.readBoolean(); featureEnabled = buf.readBoolean(); rarityRules = ByteBufUtils.readUTF8String(buf);
    }

    @Override public void toBytes(ByteBuf buf) {
        int count = Math.min(10000, entries.size()); buf.writeInt(count);
        for (int i = 0; i < count; i++) {
            Entry e = entries.get(i); ByteBufUtils.writeUTF8String(buf, safe(e.instanceId)); ByteBufUtils.writeUTF8String(buf, safe(e.rarity));
            ByteBufUtils.writeUTF8String(buf, safe(e.origin)); buf.writeLong(e.generationSeed);
            ByteBufUtils.writeUTF8String(buf, safe(e.rawRolls)); ByteBufUtils.writeUTF8String(buf, safe(e.aggregatedEffects));
            buf.writeBoolean(e.pinned); buf.writeBoolean(e.active);
        }
        buf.writeInt(auraKeys); buf.writeInt(crystals); buf.writeInt(keyCrystalPrice);
        buf.writeLong(totalChestOpens); buf.writeLong(totalChestAuraDrops); buf.writeBoolean(skipOpeningAnimation); buf.writeBoolean(featureEnabled);
        ByteBufUtils.writeUTF8String(buf, rarityRules);
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
