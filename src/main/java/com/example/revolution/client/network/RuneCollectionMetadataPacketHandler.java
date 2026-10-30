package com.example.revolution.client.network;

import com.example.revolution.client.ClientUiBridge;
import com.example.revolution.network.RuneCollectionMetadataPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class RuneCollectionMetadataPacketHandler implements IMessageHandler<RuneCollectionMetadataPacket, IMessage> {
    @Override
    public IMessage onMessage(RuneCollectionMetadataPacket message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> ClientUiBridge.acceptCollectionMetadata(message));
        return null;
    }
}
