package com.example.revolution.client.network;

import com.example.revolution.client.MasteryGuiScreen;
import com.example.revolution.network.MasterySnapshotPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** No GUI is opened from an unrelated response or while typing in another screen. */
public final class MasterySnapshotPacketHandler implements IMessageHandler<MasterySnapshotPacket, IMessage> {
    @Override public IMessage onMessage(MasterySnapshotPacket message, MessageContext ctx) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.addScheduledTask(() -> {
            if (minecraft.player == null || minecraft.getConnection() == null) return;
            if (!MasteryClientState.apply(message)) return;
            if (minecraft.currentScreen instanceof MasteryGuiScreen) {
                ((MasteryGuiScreen) minecraft.currentScreen).refreshButtons();
            } else if (MasteryClientState.consumeOpenRequested() && minecraft.currentScreen == null) {
                minecraft.displayGuiScreen(new MasteryGuiScreen());
            }
        });
        return null;
    }
}
