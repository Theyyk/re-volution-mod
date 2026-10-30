package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Additive v2.3.0 metadata for the fixed rune collection chain. */
public final class RuneCollectionMetadataPacket implements IMessage {
    private final List<String> collectionIds = new ArrayList<>();
    private final List<String> displayNames = new ArrayList<>();
    private final List<Integer> improvedOrHigherProgress = new ArrayList<>();
    private int unlockedMask;
    private int activeCollection;
    private int unlockThreshold;

    public RuneCollectionMetadataPacket() {}

    public RuneCollectionMetadataPacket(
            List<String> collectionIds,
            List<String> displayNames,
            int unlockedMask,
            int activeCollection,
            List<Integer> improvedOrHigherProgress,
            int unlockThreshold) {
        this.collectionIds.addAll(collectionIds);
        this.displayNames.addAll(displayNames);
        this.unlockedMask = unlockedMask;
        this.activeCollection = activeCollection;
        this.improvedOrHigherProgress.addAll(improvedOrHigherProgress);
        this.unlockThreshold = unlockThreshold;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        collectionIds.clear();
        displayNames.clear();
        improvedOrHigherProgress.clear();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            collectionIds.add(ByteBufUtils.readUTF8String(buf));
            displayNames.add(ByteBufUtils.readUTF8String(buf));
            improvedOrHigherProgress.add(buf.readInt());
        }
        unlockedMask = buf.readInt();
        activeCollection = buf.readInt();
        unlockThreshold = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        int count = Math.min(collectionIds.size(), Math.min(displayNames.size(), improvedOrHigherProgress.size()));
        buf.writeInt(count);
        for (int i = 0; i < count; i++) {
            ByteBufUtils.writeUTF8String(buf, collectionIds.get(i) == null ? "" : collectionIds.get(i));
            ByteBufUtils.writeUTF8String(buf, displayNames.get(i) == null ? "" : displayNames.get(i));
            buf.writeInt(Math.max(0, improvedOrHigherProgress.get(i)));
        }
        buf.writeInt(unlockedMask);
        buf.writeInt(activeCollection);
        buf.writeInt(unlockThreshold);
    }

    public List<String> getCollectionIds() { return new ArrayList<>(collectionIds); }
    public List<String> getDisplayNames() { return new ArrayList<>(displayNames); }
    public List<Integer> getImprovedOrHigherProgress() { return new ArrayList<>(improvedOrHigherProgress); }
    public int getUnlockedMask() { return unlockedMask; }
    public int getActiveCollection() { return activeCollection; }
    public int getUnlockThreshold() { return unlockThreshold; }

    public boolean isUnlocked(int collectionIndex) {
        return collectionIndex >= 0 && collectionIndex < Integer.SIZE
                && (unlockedMask & (1 << collectionIndex)) != 0;
    }
}
