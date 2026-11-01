package com.example.revolution.client.network;

import com.example.revolution.client.ClientUniqueRunes;
import com.example.revolution.network.UniqueRuneSnapshotPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class UniqueRuneSnapshotPacketHandler implements IMessageHandler<UniqueRuneSnapshotPacket, IMessage> {
    @Override public IMessage onMessage(UniqueRuneSnapshotPacket message, MessageContext ctx) {
        if (message == null) return null;
        Minecraft.getMinecraft().addScheduledTask(() -> ClientUniqueRunes.accept(message));
        return null;
    }
}
