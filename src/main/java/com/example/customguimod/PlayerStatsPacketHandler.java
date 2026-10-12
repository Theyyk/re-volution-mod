package com.example.customguimod;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PlayerStatsPacketHandler implements IMessageHandler<PlayerStatsPacket, IMessage> {

    @Override
    public IMessage onMessage(PlayerStatsPacket message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            ClientPlayerStats.update(
                    message.getCoins(),
                    message.getCrystals(),
                    message.getRuneResources(),
                    message.getTotalDamage()
            );
            CustomGuiMod.logger.info("Player stats updated on client: coins=" + message.getCoins()
                    + ", crystals=" + message.getCrystals()
                    + ", runeResources=" + message.getRuneResources()
                    + ", damage=" + message.getTotalDamage());
        });
        return null;
    }
}
