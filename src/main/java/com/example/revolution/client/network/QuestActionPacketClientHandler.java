package com.example.revolution.client.network;

import com.example.revolution.network.QuestActionPacket;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client registration stub; QuestActionPacket is client -> server only. */
public final class QuestActionPacketClientHandler implements IMessageHandler<QuestActionPacket, IMessage> {
    @Override public IMessage onMessage(QuestActionPacket message, MessageContext ctx) { return null; }
}
