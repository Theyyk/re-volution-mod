package com.example.revolution.client.network;

import com.example.revolution.client.ClientPets;
import com.example.revolution.network.PetSnapshotPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class PetSnapshotPacketHandler implements IMessageHandler<PetSnapshotPacket, IMessage> {
    @Override public IMessage onMessage(PetSnapshotPacket message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> ClientPets.accept(message));
        return null;
    }
}
