package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Player request to travel to one Location + mobSlot selected in the L menu. */
public final class StageTravelTeleportRequestPacket implements IMessage {
    private int worldLocationId;
    private int mobSlotId;

    public StageTravelTeleportRequestPacket() {}

    public StageTravelTeleportRequestPacket(int worldLocationId, int mobSlotId) {
        this.worldLocationId = worldLocationId;
        this.mobSlotId = mobSlotId;
    }

    public int getWorldLocationId() { return worldLocationId; }
    public int getMobSlotId() { return mobSlotId; }

    @Override public void fromBytes(ByteBuf buf) {
        worldLocationId = buf.readInt();
        mobSlotId = buf.readInt();
    }

    @Override public void toBytes(ByteBuf buf) {
        buf.writeInt(worldLocationId);
        buf.writeInt(mobSlotId);
    }
}
