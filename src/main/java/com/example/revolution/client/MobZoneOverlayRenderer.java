package com.example.revolution.client;

import com.example.revolution.network.MobZoneOverlayPacket;
import com.example.revolution.network.MobZoneOverlayRequestPacket;
import com.example.revolution.network.MobZoneOverlayState;
import com.example.revolution.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

/** Admin-only lightweight world overlay for configured circular mob-zone boundaries. */
public final class MobZoneOverlayRenderer {
    private static final int SEGMENTS = 96;
    private static final int REQUEST_INTERVAL_TICKS = 40;
    private static final double VIEW_DISTANCE = 64.0D;
    private static final double HEIGHT_OFFSET = 0.15D;
    private static final float RED = 0.31F;
    private static final float GREEN = 0.86F;
    private static final float BLUE = 0.47F;
    private static final float ALPHA = 0.28F;
    private int requestTicks;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) {
            requestTicks = 0;
            MobZoneOverlayState.clear();
            return;
        }
        if (++requestTicks >= REQUEST_INTERVAL_TICKS) {
            requestTicks = 0;
            NetworkHandler.INSTANCE.sendToServer(new MobZoneOverlayRequestPacket());
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;
        if (MobZoneOverlayState.get().isEmpty()) return;

        double cameraX = mc.getRenderManager().viewerPosX;
        double cameraY = mc.getRenderManager().viewerPosY;
        double cameraZ = mc.getRenderManager().viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.depthMask(false);
        GL11.glLineWidth(2.0F);

        for (MobZoneOverlayPacket.Zone zone : MobZoneOverlayState.get()) {
            if (!isNearBoundary(mc, zone)) continue;
            drawZone(mc, zone, cameraX, cameraY, cameraZ);
        }

        GL11.glLineWidth(1.0F);
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private boolean isNearBoundary(Minecraft mc, MobZoneOverlayPacket.Zone zone) {
        double dx = mc.player.posX - zone.getCenterX();
        double dz = mc.player.posZ - zone.getCenterZ();
        double centerDistance = Math.sqrt(dx * dx + dz * dz);
        return Math.abs(centerDistance - zone.getRadius()) <= VIEW_DISTANCE
                || centerDistance <= zone.getRadius() + VIEW_DISTANCE;
    }

    private void drawZone(Minecraft mc, MobZoneOverlayPacket.Zone zone, double cameraX, double cameraY, double cameraZ) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = Math.PI * 2.0D * i / SEGMENTS;
            double worldX = zone.getCenterX() + Math.cos(angle) * zone.getRadius();
            double worldZ = zone.getCenterZ() + Math.sin(angle) * zone.getRadius();
            BlockPos probe = new BlockPos(worldX, mc.player.posY, worldZ);
            double worldY = mc.player.posY + HEIGHT_OFFSET;
            if (mc.world.isBlockLoaded(probe)) {
                worldY = mc.world.getHeight(new BlockPos(worldX, 0, worldZ)).getY() + HEIGHT_OFFSET;
            }
            buffer.pos(worldX - cameraX, worldY - cameraY, worldZ - cameraZ)
                    .color(RED, GREEN, BLUE, ALPHA)
                    .endVertex();
        }
        tessellator.draw();
    }
}
