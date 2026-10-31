package com.example.revolution.network;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Dedicated-server placeholder; the physical client registers the real Aura snapshot handler. */
public final class AuraSnapshotPacketServerHandler implements IMessageHandler<AuraSnapshotPacket, IMessage> {
    @Override public IMessage onMessage(AuraSnapshotPacket message, MessageContext ctx) { return null; }
}
