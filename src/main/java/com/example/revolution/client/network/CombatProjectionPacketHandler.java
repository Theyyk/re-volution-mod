package com.example.revolution.client.network;

import com.example.revolution.client.ClientPlayerStats;
import com.example.revolution.network.CombatProjectionPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class CombatProjectionPacketHandler implements IMessageHandler<CombatProjectionPacket, IMessage> {
    @Override public IMessage onMessage(CombatProjectionPacket message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> ClientPlayerStats.setDamageProjection(
                message.getClick(), message.getPoison(), message.getClickDetails(), message.getPoisonDetails()));
        return null;
    }
}
