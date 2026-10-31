package com.example.revolution.client.network;

import com.example.revolution.client.AuraGuiScreen;
import com.example.revolution.network.AuraSnapshotPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Applies server Aura snapshots and refreshes the functional screen when requested/open. */
public final class AuraSnapshotPacketHandler implements IMessageHandler<AuraSnapshotPacket, IMessage> {
    @Override public IMessage onMessage(AuraSnapshotPacket message, MessageContext ctx) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.addScheduledTask(() -> {
            AuraClientState.update(message);
            boolean alreadyOpen = minecraft.currentScreen instanceof AuraGuiScreen;
            boolean requested = AuraClientState.consumeOpenRequested();
            if (!alreadyOpen && !requested) return;
            minecraft.displayGuiScreen(new AuraGuiScreen());
        });
        return null;
    }
}
