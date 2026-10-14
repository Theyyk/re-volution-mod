package com.example.customguimod;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class RuneEnhancementPacketServerHandler implements IMessageHandler<RuneEnhancementPacket, IMessage> {
    @Override
    public IMessage onMessage(RuneEnhancementPacket message, MessageContext ctx) {
        return null;
    }
}
