package com.example.revolution.network;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Dedicated-server placeholder; physical clients register the real snapshot handler. */
public final class UniqueRuneSnapshotPacketServerHandler implements IMessageHandler<UniqueRuneSnapshotPacket, IMessage> {
    @Override public IMessage onMessage(UniqueRuneSnapshotPacket message, MessageContext ctx) { return null; }
}
