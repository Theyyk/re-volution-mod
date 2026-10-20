package com.example.revolution.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Minimal client-only Realm indicator rendered in the lower-right corner. */
@SideOnly(Side.CLIENT)
public final class RealmHudRenderer {
    private static final int MARGIN = 6;

    @SubscribeEvent
    public void onRenderText(RenderGameOverlayEvent.Text event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || minecraft.world == null || minecraft.gameSettings.hideGUI) return;

        String label = labelForDimension(minecraft.player.dimension);
        if (label == null) return;

        FontRenderer font = minecraft.fontRenderer;
        ScaledResolution resolution = event.getResolution();
        int x = resolution.getScaledWidth() - font.getStringWidth(label) - MARGIN;
        int y = resolution.getScaledHeight() - font.FONT_HEIGHT - MARGIN;
        font.drawStringWithShadow(label, x, y, 0xFFFFFF);
    }

    static String labelForDimension(int dimensionId) {
        switch (dimensionId) {
            case 0: return "Realm 0";
            case 201: return "Realm 1";
            case 202: return "Realm 2";
            case 203: return "Realm 3";
            default: return null;
        }
    }
}
