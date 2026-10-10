package com.example.customguimod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

/** Pixel-snapped surface drawing shared by the screen and its controls. */
final class GuiDrawing extends Gui {
    private GuiDrawing() {}

    // Fill and stroke separately: a translucent border must not darken the whole interior.
    private static final java.nio.FloatBuffer SURFACE_TRANSFORM = org.lwjgl.BufferUtils.createFloatBuffer(16);

    static void drawSurface(int x, int y, int w, int h, int background, int border) {
        if (w < 6 || h < 6) return;
        GlStateManager.pushMatrix();
        java.nio.FloatBuffer transform = SURFACE_TRANSFORM;
        transform.clear();
        org.lwjgl.opengl.GL11.glGetFloat(org.lwjgl.opengl.GL11.GL_MODELVIEW_MATRIX, transform);
        float guiScale = new net.minecraft.client.gui.ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
        float pixels = Math.max(0.01F, Math.abs(transform.get(0)) * guiScale);
        float originX = transform.get(12) * guiScale;
        float originY = transform.get(13) * guiScale;
        float snappedX = (Math.round(originX + x * pixels) - originX) / pixels;
        float snappedY = (Math.round(originY + y * pixels) - originY) / pixels;
        GlStateManager.translate(snappedX, snappedY, 0);
        GlStateManager.scale(1.0F / pixels, 1.0F / pixels, 1);
        int right = Math.round(w * pixels), bottom = Math.round(h * pixels);
        int stroke = Math.max(1, Math.round(1.25F * pixels));
        boolean itemSlot = w == h && w >= 40 && w <= 60;
        int shoulder = Math.max(stroke + 1,
                Math.round((itemSlot ? 4 : (w > 200 && h > 30) ? 4 : 3) * pixels));
        drawRect(stroke, shoulder, right - stroke, bottom - shoulder, background);
        drawRect(shoulder, stroke, right - shoulder, shoulder, background);
        drawRect(shoulder, bottom - shoulder, right - shoulder, bottom - stroke, background);
        drawRect(shoulder, 0, right - shoulder, stroke, border);
        drawRect(shoulder, bottom - stroke, right - shoulder, bottom, border);
        drawRect(0, shoulder, stroke, bottom - shoulder, border);
        drawRect(right - stroke, shoulder, right, bottom - shoulder, border);
        drawRect(shoulder - stroke, 0, shoulder, shoulder, border);
        drawRect(0, shoulder - stroke, shoulder, shoulder, border);
        drawRect(right - shoulder, 0, right - shoulder + stroke, shoulder, border);
        drawRect(right - shoulder, shoulder - stroke, right, shoulder, border);
        drawRect(0, bottom - shoulder, shoulder, bottom - shoulder + stroke, border);
        drawRect(shoulder - stroke, bottom - shoulder, shoulder, bottom, border);
        drawRect(right - shoulder, bottom - shoulder, right, bottom - shoulder + stroke, border);
        drawRect(right - shoulder, bottom - shoulder, right - shoulder + stroke, bottom, border);
        GlStateManager.popMatrix();
    }
}
