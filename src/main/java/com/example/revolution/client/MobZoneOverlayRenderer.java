package com.example.revolution.client;

import com.example.revolution.network.MobZoneOverlayPacket;
import com.example.revolution.network.MobZoneOverlayRequestPacket;
import com.example.revolution.network.MobZoneOverlayState;
import com.example.revolution.network.NetworkHandler;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.state.IBlockState;
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

/** Lightweight world overlay for configured mob-zone boundaries and stage trigger rings. */
public final class MobZoneOverlayRenderer {
    private static final int SEGMENTS = 96;
    private static final int HALF_SEGMENTS = SEGMENTS / 2;
    private static final int REQUEST_INTERVAL_TICKS = 40;
    private static final double VIEW_DISTANCE = 64.0D;
    private static final double HEIGHT_OFFSET = 0.12D;
    private static final float ZONE_RED = 0.31F;
    private static final float ZONE_GREEN = 0.86F;
    private static final float ZONE_BLUE = 0.47F;
    private static final float FORWARD_RED = 0.20F;
    private static final float FORWARD_GREEN = 0.72F;
    private static final float FORWARD_BLUE = 1.00F;
    private static final float BACKWARD_RED = 1.00F;
    private static final float BACKWARD_GREEN = 0.38F;
    private static final float BACKWARD_BLUE = 0.24F;
    private static final float NEUTRAL_RED = 0.90F;
    private static final float NEUTRAL_GREEN = 0.75F;
    private static final float NEUTRAL_BLUE = 0.25F;
    private static final float ALPHA = 0.42F;
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
            drawCircle(mc, zone.getCenterX(), zone.getCenterZ(), zone.getRadius(),
                    cameraX, cameraY, cameraZ, ZONE_RED, ZONE_GREEN, ZONE_BLUE);
            if (zone.hasTrigger()) drawTrigger(mc, zone, cameraX, cameraY, cameraZ);
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
        double visibleRadius = zone.hasTrigger() ? zone.getTriggerOuterRadius() : zone.getRadius();
        return Math.abs(centerDistance - visibleRadius) <= VIEW_DISTANCE
                || centerDistance <= visibleRadius + VIEW_DISTANCE;
    }

    private void drawTrigger(Minecraft mc, MobZoneOverlayPacket.Zone zone,
            double cameraX, double cameraY, double cameraZ) {
        double axisLength = Math.sqrt(zone.getForwardX() * zone.getForwardX()
                + zone.getForwardZ() * zone.getForwardZ());
        if (axisLength <= 1.0E-12D) {
            drawCircle(mc, zone.getCenterX(), zone.getCenterZ(), zone.getTriggerInnerRadius(),
                    cameraX, cameraY, cameraZ, NEUTRAL_RED, NEUTRAL_GREEN, NEUTRAL_BLUE);
            drawCircle(mc, zone.getCenterX(), zone.getCenterZ(), zone.getTriggerOuterRadius(),
                    cameraX, cameraY, cameraZ, NEUTRAL_RED, NEUTRAL_GREEN, NEUTRAL_BLUE);
            return;
        }

        double forwardAngle = Math.atan2(zone.getForwardZ(), zone.getForwardX());
        double forwardStart = forwardAngle - Math.PI / 2.0D;
        double backwardStart = forwardAngle + Math.PI / 2.0D;

        drawHalfRing(mc, zone, forwardStart, cameraX, cameraY, cameraZ,
                FORWARD_RED, FORWARD_GREEN, FORWARD_BLUE);
        drawHalfRing(mc, zone, backwardStart, cameraX, cameraY, cameraZ,
                BACKWARD_RED, BACKWARD_GREEN, BACKWARD_BLUE);

        drawSeparator(mc, zone, forwardStart, cameraX, cameraY, cameraZ);
        drawSeparator(mc, zone, forwardStart + Math.PI, cameraX, cameraY, cameraZ);
    }

    private void drawHalfRing(Minecraft mc, MobZoneOverlayPacket.Zone zone, double startAngle,
            double cameraX, double cameraY, double cameraZ, float red, float green, float blue) {
        drawArc(mc, zone.getCenterX(), zone.getCenterZ(), zone.getTriggerInnerRadius(), startAngle,
                cameraX, cameraY, cameraZ, red, green, blue);
        drawArc(mc, zone.getCenterX(), zone.getCenterZ(), zone.getTriggerOuterRadius(), startAngle,
                cameraX, cameraY, cameraZ, red, green, blue);
    }

    private void drawArc(Minecraft mc, double centerX, double centerZ, double radius, double startAngle,
            double cameraX, double cameraY, double cameraZ, float red, float green, float blue) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= HALF_SEGMENTS; i++) {
            double angle = startAngle + Math.PI * i / HALF_SEGMENTS;
            double worldX = centerX + Math.cos(angle) * radius;
            double worldZ = centerZ + Math.sin(angle) * radius;
            double worldY = terrainY(mc, worldX, worldZ);
            buffer.pos(worldX - cameraX, worldY - cameraY, worldZ - cameraZ)
                    .color(red, green, blue, ALPHA).endVertex();
        }
        tessellator.draw();
    }

    private void drawSeparator(Minecraft mc, MobZoneOverlayPacket.Zone zone, double angle,
            double cameraX, double cameraY, double cameraZ) {
        double innerX = zone.getCenterX() + Math.cos(angle) * zone.getTriggerInnerRadius();
        double innerZ = zone.getCenterZ() + Math.sin(angle) * zone.getTriggerInnerRadius();
        double outerX = zone.getCenterX() + Math.cos(angle) * zone.getTriggerOuterRadius();
        double outerZ = zone.getCenterZ() + Math.sin(angle) * zone.getTriggerOuterRadius();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(innerX - cameraX, terrainY(mc, innerX, innerZ) - cameraY, innerZ - cameraZ)
                .color(1.0F, 1.0F, 1.0F, ALPHA).endVertex();
        buffer.pos(outerX - cameraX, terrainY(mc, outerX, outerZ) - cameraY, outerZ - cameraZ)
                .color(1.0F, 1.0F, 1.0F, ALPHA).endVertex();
        tessellator.draw();
    }

    private void drawCircle(Minecraft mc, double centerX, double centerZ, double radius,
            double cameraX, double cameraY, double cameraZ, float red, float green, float blue) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = Math.PI * 2.0D * i / SEGMENTS;
            double worldX = centerX + Math.cos(angle) * radius;
            double worldZ = centerZ + Math.sin(angle) * radius;
            double worldY = terrainY(mc, worldX, worldZ);
            buffer.pos(worldX - cameraX, worldY - cameraY, worldZ - cameraZ)
                    .color(red, green, blue, ALPHA).endVertex();
        }
        tessellator.draw();
    }

    private double terrainY(Minecraft mc, double worldX, double worldZ) {
        int x = (int) Math.floor(worldX);
        int z = (int) Math.floor(worldZ);
        BlockPos column = new BlockPos(x, 0, z);
        if (!mc.world.isBlockLoaded(column)) return mc.player.posY + HEIGHT_OFFSET;

        int top = Math.min(mc.world.getHeight(column).getY(), mc.world.getActualHeight() - 1);
        for (int y = top; y >= 1; y--) {
            BlockPos groundPos = new BlockPos(x, y - 1, z);
            IBlockState ground = mc.world.getBlockState(groundPos);
            if (ground.getBlock() instanceof BlockLeaves || ground.getBlock() instanceof BlockLog) continue;
            if (!ground.getMaterial().blocksMovement() || ground.getMaterial().isLiquid()) continue;
            return y + HEIGHT_OFFSET;
        }
        return mc.player.posY + HEIGHT_OFFSET;
    }
}
