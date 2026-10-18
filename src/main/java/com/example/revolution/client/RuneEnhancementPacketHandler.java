package com.example.revolution.client;

import com.example.revolution.network.RuneEnhancementPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RuneEnhancementPacketHandler implements IMessageHandler<RuneEnhancementPacket, IMessage> {
    @Override
    public IMessage onMessage(RuneEnhancementPacket message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> ClientRuneEnhancements.accept(message));
        return null;
    }
}
