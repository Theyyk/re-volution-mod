package com.example.customguimod.network;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PongPacketServerHandler implements IMessageHandler<PongPacket, IMessage> {

    @Override
    public IMessage onMessage(PongPacket message, MessageContext ctx) {
        // Сервер не принимает PongPacket — он его только отправляет.
        // Этот класс нужен только для регистрации discriminator.
        return null;
    }
}
