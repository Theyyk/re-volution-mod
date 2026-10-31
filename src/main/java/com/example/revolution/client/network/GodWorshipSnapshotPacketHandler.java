package com.example.revolution.client.network;

import com.example.revolution.network.GodWorshipSnapshotPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Applies God snapshots on the physical client thread. */
public final class GodWorshipSnapshotPacketHandler implements IMessageHandler<GodWorshipSnapshotPacket, IMessage> {
    @Override public IMessage onMessage(GodWorshipSnapshotPacket message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> GodWorshipClientState.update(message));
        return null;
    }
}
