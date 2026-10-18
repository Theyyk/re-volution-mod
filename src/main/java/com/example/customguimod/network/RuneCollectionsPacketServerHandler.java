package com.example.customguimod.network;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class RuneCollectionsPacketServerHandler implements IMessageHandler<RuneCollectionsPacket, IMessage> {
    @Override
    public IMessage onMessage(RuneCollectionsPacket message, MessageContext ctx) {
        return null;
    }
}
