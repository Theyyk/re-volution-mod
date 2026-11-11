package com.example.revolution.network;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Physical dedicated-server stub. Client handler owns the projection. */
public final class MasterySnapshotPacketServerHandler implements IMessageHandler<MasterySnapshotPacket, IMessage> {
    @Override public IMessage onMessage(MasterySnapshotPacket message, MessageContext ctx) { return null; }
}
