package com.example.revolution.network;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Dedicated server placeholder for the client-only Run Exchange snapshot. */
public final class RunExchangeSnapshotPacketServerHandler implements IMessageHandler<RunExchangeSnapshotPacket, IMessage> {
    @Override public IMessage onMessage(RunExchangeSnapshotPacket packet, MessageContext ctx) { return null; }
}
