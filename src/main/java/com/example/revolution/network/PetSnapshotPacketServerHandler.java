package com.example.revolution.network;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Dedicated-server registration stub; client logic is registered on the physical client. */
public final class PetSnapshotPacketServerHandler implements IMessageHandler<PetSnapshotPacket, IMessage> {
    @Override public IMessage onMessage(PetSnapshotPacket message, MessageContext ctx) { return null; }
}
