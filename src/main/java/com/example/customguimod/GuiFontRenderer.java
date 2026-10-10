package com.example.customguimod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.io.InputStream;

/** Screen-local typography using the reference client's original Five / Seven faces. */
final class GuiFontRenderer extends FontRenderer {
    private static final int CELL = 48;
    private static final int ATLAS = 768;
    private static final float SCALE = 1.0F / 3.0F;
    private static GuiFontRenderer instance;
    private static GuiFontRenderer headingInstance;
    private final int[] glyphs = new int[65536];
    private final float[] advances = new float[65536];
    private final Font face;
    private final java.util.Map<Integer, Raster> rasters = new java.util.LinkedHashMap<>();
    private final java.nio.FloatBuffer matrix = org.lwjgl.BufferUtils.createFloatBuffer(16);

    private static final class Raster {
        final ResourceLocation texture;
        final int cell;
        Raster(ResourceLocation texture, int cell) {
            this.texture = texture;
            this.cell = cell;
        }
    }

    private Raster raster(float pixelScale) {
        int key = Math.max(1, Math.round(pixelScale * 64));
        Raster cached = rasters.get(key);
        if (cached != null) return cached;
        Font pixelFont = face.deriveFont(face.getSize2D() * SCALE * pixelScale);
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D measure = probe.createGraphics();
        measure.setFont(pixelFont);
        FontMetrics metrics = measure.getFontMetrics();
        int cell = Math.max(metrics.getHeight() + 4, metrics.getMaxAdvance() + 4);
        measure.dispose();
        BufferedImage image = new BufferedImage(cell * 16, cell * 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(java.awt.Color.WHITE);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        for (int c = 0; c < glyphs.length; c++) {
            if (glyphs[c] < 0) continue;
            graphics.setFont(face.canDisplay((char) c) ? pixelFont : new Font(Font.SANS_SERIF, Font.PLAIN, 1).deriveFont(pixelFont.getSize2D()));
            int index = glyphs[c];
            graphics.drawString(String.valueOf((char) c), index % 16 * cell, index / 16 * cell + metrics.getAscent());
        }
        graphics.dispose();
        Raster result = new Raster(Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(
                "equipment_font_pixels", new DynamicTexture(image)), cell);
        if (rasters.size() >= 8) {
            Integer oldest = rasters.keySet().iterator().next();
            Minecraft.getMinecraft().getTextureManager().deleteTexture(rasters.remove(oldest).texture);
        }
        rasters.put(key, result);
        return result;
    }

    static FontRenderer get(Minecraft mc) {
        if (instance == null) instance = new GuiFontRenderer(mc, "minecraft_seven.otf", 36, 0);
        return instance;
    }

    static FontRenderer heading(Minecraft mc) {
        if (headingInstance == null) headingInstance = new GuiFontRenderer(mc, "minecraft_five.otf", 32, 0.35F);
        return headingInstance;
    }

    private GuiFontRenderer(Minecraft mc, String file, int size, float letterSpacing) {
        super(mc.gameSettings, new ResourceLocation("textures/font/ascii.png"), mc.getTextureManager(), false);
        FONT_HEIGHT = 12;
        Arrays.fill(glyphs, -1);
        BufferedImage image = new BufferedImage(ATLAS, ATLAS, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        Font loadedFace;
        try (InputStream stream = GuiFontRenderer.class.getResourceAsStream("/assets/customguimod/fonts/" + file)) {
            if (stream == null) throw new IllegalStateException("Missing GUI font: " + file);
            loadedFace = Font.createFont(Font.TRUETYPE_FONT, stream).deriveFont((float) size);
        } catch (Exception error) {
            throw new IllegalStateException("Cannot load GUI font: " + file, error);
        }
        face = loadedFace;
        graphics.setFont(face);
        graphics.setColor(java.awt.Color.WHITE);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        FontMetrics metrics = graphics.getFontMetrics();
        StringBuilder characters = new StringBuilder();
        for (char c = 32; c <= 126; c++) characters.append(c);
        for (char c = '\u0400'; c <= '\u045f'; c++) characters.append(c);
        characters.append("–—№•");
        for (int i = 0; i < characters.length(); i++) {
            char c = characters.charAt(i);
            glyphs[c] = i;
            if (!face.canDisplay(c)) {
                graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, size));
            }
            advances[c] = (float) graphics.getFontMetrics().getStringBounds(String.valueOf(c), graphics).getWidth() * SCALE + letterSpacing;
            graphics.drawString(String.valueOf(c), i % 16 * CELL + 2, i / 16 * CELL + metrics.getAscent());
            graphics.setFont(face);
        }
        graphics.dispose();
    }

    private float advance(char c) {
        return advances[glyphs[c] < 0 ? '?' : c];
    }

    @Override
    public int getCharWidth(char c) {
        return c == '\u00a7' ? -1 : Math.round(advance(c));
    }

    @Override
    public int getStringWidth(String text) {
        if (text == null) return 0;
        float width = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\u00a7' && i + 1 < text.length()) { i++; continue; }
            width += advance(text.charAt(i));
        }
        return Math.round(width);
    }

    @Override
    public String trimStringToWidth(String text, int width, boolean reverse) {
        if (reverse) {
            int start = text.length();
            while (start > 0 && getStringWidth(text.substring(start - 1)) <= width) start--;
            return text.substring(start);
        }
        float used = 0;
        int end = 0;
        while (end < text.length()) {
            if (text.charAt(end) == '\u00a7' && end + 1 < text.length()) { end += 2; continue; }
            float next = advance(text.charAt(end));
            if (used + next > width) break;
            used += next;
            end++;
        }
        return text.substring(0, end);
    }

    @Override
    public int drawStringWithShadow(String text, float x, float y, int color) {
        return drawString(text, x, y, color, true);
    }

    @Override
    public int drawString(String text, int x, int y, int color) {
        return drawString(text, (float) x, (float) y, color, false);
    }

    @Override
    public int drawString(String text, float x, float y, int color, boolean shadow) {
        if (text == null || text.isEmpty()) return (int) x;
        // The reference uses light strokes without Minecraft's heavy drop shadow.
        Minecraft minecraft = Minecraft.getMinecraft();
        matrix.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrix);
        float modelX = matrix.get(0), modelY = matrix.get(5);
        float offsetX = matrix.get(12), offsetY = matrix.get(13);
        if (Math.abs(modelX) < 0.00001F || Math.abs(modelY) < 0.00001F) return (int) x;
        int guiScale = new net.minecraft.client.gui.ScaledResolution(minecraft).getScaleFactor();
        float pixelScale = Math.max(1, Math.round(Math.abs(modelY) * guiScale * 64)) / 64.0F;
        Raster raster = raster(pixelScale);
        minecraft.getTextureManager().bindTexture(raster.texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1, 1, 1, 1);
        int alpha = color >>> 24;
        if (alpha == 0) alpha = 255;
        int rgb = color;
        float cursor = x;
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(++i));
                if ("0123456789abcdef".indexOf(code) >= 0) rgb = getColorCode(code);
                else if (code == 'r') rgb = color;
                continue;
            }
            int glyph = glyphs[c] < 0 ? glyphs['?'] : glyphs[c];
            float u = glyph % 16 / 16.0F;
            float v = glyph / 16 / 16.0F;
            float uv = 1.0F / 16;
            float size = raster.cell / pixelScale;
            float drawX = (Math.round((cursor * modelX + offsetX) * guiScale) / (float) guiScale - offsetX) / modelX;
            float drawY = (Math.round((y * modelY + offsetY) * guiScale) / (float) guiScale - offsetY) / modelY;
            int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
            buffer.pos(drawX, drawY + size, 0).tex(u, v + uv).color(r, g, b, alpha).endVertex();
            buffer.pos(drawX + size, drawY + size, 0).tex(u + uv, v + uv).color(r, g, b, alpha).endVertex();
            buffer.pos(drawX + size, drawY, 0).tex(u + uv, v).color(r, g, b, alpha).endVertex();
            buffer.pos(drawX, drawY, 0).tex(u, v).color(r, g, b, alpha).endVertex();
            cursor += advance(c);
        }
        Tessellator.getInstance().draw();
        GlStateManager.disableBlend();
        GlStateManager.color(1, 1, 1, 1);
        return Math.round(cursor);
    }
}
