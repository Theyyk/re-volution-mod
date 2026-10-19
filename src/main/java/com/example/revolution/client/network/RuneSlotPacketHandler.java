package com.example.revolution.client.network;

import com.example.revolution.client.ClientUiBridge;
import com.example.revolution.network.RuneSlotPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RuneSlotPacketHandler implements IMessageHandler<RuneSlotPacket, IMessage> {
    @Override
    public IMessage onMessage(RuneSlotPacket message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> ClientUiBridge.acceptRune(message));
        return null;
    }
}
