package com.example.customguimod.network;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PlayerStatsPacketServerHandler implements IMessageHandler<PlayerStatsPacket, IMessage> {

    @Override
    public IMessage onMessage(PlayerStatsPacket message, MessageContext ctx) {
        // Сервер не принимает PlayerStatsPacket — он его только отправляет.
        return null;
    }
}
