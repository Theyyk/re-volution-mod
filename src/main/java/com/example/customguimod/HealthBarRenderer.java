package com.example.customguimod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.math.BigDecimal;
import java.math.RoundingMode;

@SideOnly(Side.CLIENT)
public class HealthBarRenderer {

    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Post event) {
        EntityLivingBase entity = event.getEntity();
        if (!(entity instanceof EntityLiving)) return;
        if (entity == Minecraft.getMinecraft().player) return;
        if (!entity.hasCustomName()) return;
        if (!(entity instanceof EntityZombie) && !(entity instanceof EntitySkeleton)) return;

        String rawName = entity.getCustomNameTag();
        if (!rawName.startsWith("§c") || !rawName.contains("|")) return;
        String[] parts = rawName.substring(2).split("\\|");
        if (parts.length < 5) return;

        String mobName = parts[0];
        String resource = parts[3];
        String resourceAmount = parts[4];
        BigDecimal currentHp = parseHealth(parts[1], BigDecimal.valueOf(entity.getHealth()));
        BigDecimal maxHp = parseHealth(parts[2], BigDecimal.valueOf(entity.getMaxHealth()));

        if (maxHp.signum() <= 0 || currentHp.signum() <= 0) return;
        if (entity.getDistance(Minecraft.getMinecraft().player) > 32.0F) return;

        double ratio = currentHp.divide(maxHp, 6, RoundingMode.HALF_UP).doubleValue();
        ratio = Math.max(0.0D, Math.min(1.0D, ratio));

        double x = event.getX();
        double y = event.getY() + entity.height + 0.5;
        double z = event.getZ();

        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-Minecraft.getMinecraft().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(-0.025F, -0.025F, 0.025F);
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        int barWidth = 60, barHeight = 6, halfWidth = barWidth / 2;
        drawRect(-halfWidth - 1, -1, halfWidth + 1, barHeight + 1, 0xFF000000);
        drawRect(-halfWidth, 0, halfWidth, barHeight, 0xFF333333);
        int fillWidth = (int) (barWidth * ratio);
        int color = ratio > 0.6D ? 0xFF00FF00 : ratio > 0.3D ? 0xFFFFFF00 : 0xFFFF0000;
        drawRect(-halfWidth, 0, -halfWidth + fillWidth, barHeight, color);

        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();

        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String hpText = LargeNumberFormatter.format(currentHp) + " / " + LargeNumberFormatter.format(maxHp);
        int hpWidth = font.getStringWidth(hpText);
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.5f, 0.5f, 0.5f);
        font.drawString(hpText, -(hpWidth / 2), 3, 0xFFFFFF);
        GlStateManager.popMatrix();

        int nameWidth = font.getStringWidth(mobName);
        font.drawString("§c" + mobName, -nameWidth / 2, -10, 0xFFFFFF);
        String resourceText = getDisplayResource(resource) + " x" + compactResourceAmount(resourceAmount);
        int resWidth = font.getStringWidth(resourceText);
        font.drawString("§e" + resourceText, -resWidth / 2, barHeight + 1, 0xFFFFFF);

        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }

    private BigDecimal parseHealth(String rawValue, BigDecimal fallback) {
        try {
            return LargeNumberValue.parseExact(rawValue);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private String compactResourceAmount(String rawAmount) {
        if (rawAmount == null || rawAmount.trim().isEmpty()) return "0";
        try {
            return LargeNumberFormatter.format(new BigDecimal(rawAmount.trim()));
        } catch (NumberFormatException ignored) {
            return rawAmount;
        }
    }

    private String getDisplayResource(String resource) {
        if (resource.equalsIgnoreCase("coins")) return "Монеты";
        if (resource.equalsIgnoreCase("crystals")) return "Кристаллы";
        if (resource.equalsIgnoreCase("lightnings")) return "Молнии";
        return resource;
    }

    private void drawRect(int left, int top, int right, int bottom, int color) {
        if (left < right) { int i = left; left = right; right = i; }
        if (top < bottom) { int j = top; top = bottom; bottom = j; }
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.color(r, g, b, a);
        buffer.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION);
        buffer.pos(left, bottom, 0.0D).endVertex();
        buffer.pos(right, bottom, 0.0D).endVertex();
        buffer.pos(right, top, 0.0D).endVertex();
        buffer.pos(left, top, 0.0D).endVertex();
        tessellator.draw();
    }
}
