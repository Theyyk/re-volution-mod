package com.example.customguimod.network;

import com.example.customguimod.ClientPlayerStats;
import com.example.customguimod.CustomGuiMod;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PongPacketHandler implements IMessageHandler<PongPacket, IMessage> {

    @Override
    public IMessage onMessage(PongPacket message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            ClientPlayerStats.acceptLegacyCoins(message.getBalance());
            CustomGuiMod.logger.info("Legacy coins sync received on client: " + message.getBalance());
        });
        return null;
    }
}
