package com.example.revolution.client.network;
import com.example.revolution.network.MechanicUnlockActionPacket;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
public final class MechanicUnlockActionPacketClientHandler implements IMessageHandler<MechanicUnlockActionPacket, IMessage> { @Override public IMessage onMessage(MechanicUnlockActionPacket message, MessageContext ctx) { return null; } }
