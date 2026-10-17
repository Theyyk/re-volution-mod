package com.example.customguimod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PlayerStatsHudRenderer {

    private static final int Y = 5;

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.TEXT) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen instanceof CardsGuiScreen) return;

        draw(mc, event.getResolution().getScaledWidth());
    }

    public static void draw(Minecraft mc, int screenWidth) {
        String text = "§6Монеты: §e" + LargeNumberFormatter.format(ClientPlayerStats.getCoins())
                + " §8| §bКристаллы: §f" + LargeNumberFormatter.format(ClientPlayerStats.getCrystals())
                + " §8| §cУрон: §f+" + LargeNumberFormatter.format(ClientPlayerStats.getTotalDamage());

        int textWidth = mc.fontRenderer.getStringWidth(text);
        int x = (screenWidth - textWidth) / 2;

        Gui.drawRect(
                x - 6,
                Y - 3,
                x + textWidth + 6,
                Y + mc.fontRenderer.FONT_HEIGHT + 3,
                0x99000000
        );
        mc.fontRenderer.drawStringWithShadow(text, x, Y, 0xFFFFFF);
    }
}
