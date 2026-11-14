package com.example.revolution.client.network;

import com.example.revolution.client.MasteryGuiScreen;
import com.example.revolution.client.RunExchangeGuiScreen;
import com.example.revolution.network.RunExchangeSnapshotPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Do not reopen a closed GUI on a delayed network response. */
public final class RunExchangeSnapshotPacketHandler implements IMessageHandler<RunExchangeSnapshotPacket, IMessage> {
    @Override public IMessage onMessage(RunExchangeSnapshotPacket packet, MessageContext ctx) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.addScheduledTask(() -> {
            if (minecraft.player == null || minecraft.getConnection() == null) return;
            if (!RunExchangeClientState.apply(packet)) return;
            if (minecraft.currentScreen instanceof RunExchangeGuiScreen) {
                ((RunExchangeGuiScreen) minecraft.currentScreen).refreshButtons();
            } else {
                boolean requested = RunExchangeClientState.consumeOpenRequested();
                if (requested && minecraft.currentScreen instanceof MasteryGuiScreen)
                    minecraft.displayGuiScreen(new RunExchangeGuiScreen());
            }
        });
        return null;
    }
}
