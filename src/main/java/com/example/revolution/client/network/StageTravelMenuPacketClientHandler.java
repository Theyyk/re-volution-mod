package com.example.revolution.client.network;

import com.example.revolution.client.StageTravelGuiScreen;
import com.example.revolution.network.StageTravelMenuPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Opens the L travel menu from the server-authoritative stage snapshot. */
public final class StageTravelMenuPacketClientHandler implements IMessageHandler<StageTravelMenuPacket, IMessage> {
    @Override
    public IMessage onMessage(StageTravelMenuPacket message, MessageContext ctx) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.addScheduledTask(() -> minecraft.displayGuiScreen(new StageTravelGuiScreen(message.getEntries())));
        return null;
    }
}
