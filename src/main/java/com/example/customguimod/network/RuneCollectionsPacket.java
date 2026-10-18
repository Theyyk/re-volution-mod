package com.example.customguimod.network;

import com.example.customguimod.rune.RuneStatusRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.ArrayList;
import java.util.List;

public class RuneCollectionsPacket implements IMessage {

    private List<String> collectionNames = new ArrayList<>();
    private int activeCollection;

    public RuneCollectionsPacket() {}

    public RuneCollectionsPacket(List<String> collectionNames, int activeCollection) {
        this.collectionNames = new ArrayList<>();
        for (int i = 0; i < collectionNames.size(); i++) {
            this.collectionNames.add(RuneStatusRegistry.displayNameForCollection(i, collectionNames.get(i)));
        }
        this.activeCollection = activeCollection;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        collectionNames.clear();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            collectionNames.add(ByteBufUtils.readUTF8String(buf));
        }
        activeCollection = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(collectionNames.size());
        for (String collectionName : collectionNames) {
            ByteBufUtils.writeUTF8String(buf, collectionName == null ? "" : collectionName);
        }
        buf.writeInt(activeCollection);
    }

    public List<String> getCollectionNames() {
        return collectionNames;
    }

    public int getActiveCollection() {
        return activeCollection;
    }
}
