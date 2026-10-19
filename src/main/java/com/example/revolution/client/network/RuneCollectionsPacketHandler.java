package com.example.revolution.client.network;

import com.example.revolution.client.ClientRuneEnhancements;
import com.example.revolution.client.RuneGuiPreloader;
import com.example.revolution.client.RunesGuiScreen;
import com.example.revolution.network.RuneCollectionsPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RuneCollectionsPacketHandler implements IMessageHandler<RuneCollectionsPacket, IMessage> {

    @Override
    public IMessage onMessage(RuneCollectionsPacket message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            ClientRuneEnhancements.suppressNextPulse();
            RuneGuiPreloader.acceptCollections(message.getCollectionNames(), message.getActiveCollection());

            if (Minecraft.getMinecraft().currentScreen instanceof RunesGuiScreen) {
                RunesGuiScreen screen = (RunesGuiScreen) Minecraft.getMinecraft().currentScreen;
                screen.setCollections(message.getCollectionNames(), message.getActiveCollection());
            }
        });
        return null;
    }
}
