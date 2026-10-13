package com.example.customguimod;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class DeckPacketHandler implements IMessageHandler<DeckPacket, IMessage> {

    @Override
    public IMessage onMessage(DeckPacket message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            RuneGuiPreloader.acceptDecks(message.getDeckNames(), message.getActiveDeck());

            if (Minecraft.getMinecraft().currentScreen instanceof CardsGuiScreen) {
                CardsGuiScreen screen = (CardsGuiScreen) Minecraft.getMinecraft().currentScreen;
                screen.setDecks(message.getDeckNames(), message.getActiveDeck());
            }
        });
        return null;
    }
}
