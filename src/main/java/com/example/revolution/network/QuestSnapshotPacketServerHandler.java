package com.example.revolution.network;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Dedicated-server placeholder; the client registers its real Quest snapshot handler. */
public final class QuestSnapshotPacketServerHandler implements IMessageHandler<QuestSnapshotPacket, IMessage> {
    @Override public IMessage onMessage(QuestSnapshotPacket message, MessageContext ctx) { return null; }
}
