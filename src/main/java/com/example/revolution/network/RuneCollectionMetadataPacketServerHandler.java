package com.example.revolution.network;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Dedicated-server registration stub; client logic is registered by ClientNetworkRegistration. */
public final class RuneCollectionMetadataPacketServerHandler implements IMessageHandler<RuneCollectionMetadataPacket, IMessage> {
    @Override
    public IMessage onMessage(RuneCollectionMetadataPacket message, MessageContext ctx) {
        return null;
    }
}
