package com.example.revolution.client.network;

import com.example.revolution.network.PetActionPacket;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client registration stub; PetActionPacket is client -> server only. */
public final class PetActionPacketClientHandler implements IMessageHandler<PetActionPacket, IMessage> {
    @Override public IMessage onMessage(PetActionPacket message, MessageContext ctx) { return null; }
}
