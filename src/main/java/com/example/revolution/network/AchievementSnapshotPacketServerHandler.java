package com.example.revolution.network;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Dedicated-server placeholder; physical clients register the real snapshot handler. */
public final class AchievementSnapshotPacketServerHandler implements IMessageHandler<AchievementSnapshotPacket, IMessage> {
    @Override public IMessage onMessage(AchievementSnapshotPacket message, MessageContext ctx) { return null; }
}
