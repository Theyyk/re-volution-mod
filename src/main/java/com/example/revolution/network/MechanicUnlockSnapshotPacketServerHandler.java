package com.example.revolution.network;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
/** Dedicated-server safe no-op for clientbound packet registration. */
public final class MechanicUnlockSnapshotPacketServerHandler implements IMessageHandler<MechanicUnlockSnapshotPacket, IMessage> {
    @Override public IMessage onMessage(MechanicUnlockSnapshotPacket message, MessageContext ctx) { return null; }
}
