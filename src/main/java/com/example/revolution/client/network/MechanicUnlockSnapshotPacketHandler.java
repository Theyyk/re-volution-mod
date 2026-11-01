package com.example.revolution.client.network;

import com.example.revolution.client.mechanic.ClientMechanicUnlocks;
import com.example.revolution.client.mechanic.MechanicUnlockGuiScreen;
import com.example.revolution.network.MechanicUnlockSnapshotPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class MechanicUnlockSnapshotPacketHandler implements IMessageHandler<MechanicUnlockSnapshotPacket, IMessage> {
    @Override public IMessage onMessage(MechanicUnlockSnapshotPacket message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            ClientMechanicUnlocks.apply(message);
            if (Minecraft.getMinecraft().currentScreen instanceof MechanicUnlockGuiScreen) ((MechanicUnlockGuiScreen) Minecraft.getMinecraft().currentScreen).refreshButtons();
        });
        return null;
    }
}
