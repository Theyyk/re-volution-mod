package com.example.customguimod.network;

import com.example.customguimod.PingPacket;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PingPacketClientHandler implements IMessageHandler<PingPacket, IMessage> {

    @Override
    public IMessage onMessage(PingPacket message, MessageContext ctx) {
        // Клиент не принимает PingPacket — он его только отправляет.
        return null;
    }
}
