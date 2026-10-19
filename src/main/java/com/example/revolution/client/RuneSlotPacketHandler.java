package com.example.revolution.client;

import com.example.revolution.network.RuneSlotPacket;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.state.RuneInventory;
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
        Minecraft.getMinecraft().addScheduledTask(() -> {
            RuneGuiPreloader.acceptRune(message);
            ClientRuneRanks.accept(message);
            RuneCollectionSyncState.notePacket();

            if (message.isAnimate()
                    && RuneStatusConfig.isRuneStatus(ClientPlayerStats.getActiveRuneStatusId())
                    && message.getRuneIndex() >= 0
                    && message.getRuneIndex() < RuneInventory.BOOST_TYPE_COUNT) {
                RuneUxUiHandler.notePurchase(message.getRuneIndex());
            }

            if (Minecraft.getMinecraft().currentScreen instanceof RunesGuiScreen) {
                if (RuneGuiPreloader.shouldSuppressLiveRune(message)) return;

                RunesGuiScreen screen = (RunesGuiScreen) Minecraft.getMinecraft().currentScreen;
                if (message.getSlot() == -1) {
                    screen.clearRunes();
                } else {
                    // Purchase feedback remains a short border pulse on the destination slot.
                    screen.addRuneFromServer(message.getSlot(), message.getRuneIndex(), message.getLayer());
                }
            }
        });
        return null;
    }
}
