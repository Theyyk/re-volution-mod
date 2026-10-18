package com.example.customguimod.network;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class RuneSlotPacketServerHandler implements IMessageHandler<RuneSlotPacket, IMessage> {
    @Override
    public IMessage onMessage(RuneSlotPacket message, MessageContext ctx) {
        return null;
    }
}
