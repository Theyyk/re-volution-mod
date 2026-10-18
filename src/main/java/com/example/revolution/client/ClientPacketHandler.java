package com.example.revolution.client;

import com.example.revolution.RevolutionMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientPacketHandler {

    @SubscribeEvent
    public void onClientPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
        RevolutionMod.logger.info("Клиент получил пакет, канал: " + event.getPacket().channel());

        if (!event.getPacket().channel().equals("revolution:main")) return;

        ByteBuf buf = event.getPacket().payload();
        String data = ByteBufUtils.readUTF8String(buf);
        RevolutionMod.logger.info("Клиент получил: [" + data + "]");

        // Обработка данных от сервера
        if (data.startsWith("balance:")) {
            int balance = Integer.parseInt(data.substring("balance:".length()));
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (Minecraft.getMinecraft().currentScreen instanceof RunesGuiScreen) {
                    ((RunesGuiScreen) Minecraft.getMinecraft().currentScreen).setBalance(balance);
                }
            });
        }
    }
}
