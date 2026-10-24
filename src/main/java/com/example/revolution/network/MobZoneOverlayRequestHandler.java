package com.example.revolution.network;

import com.example.revolution.lifecycle.ServerLifecycle;
import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.service.StageTransitionTriggerService.OverlayGeometry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class MobZoneOverlayRequestHandler implements IMessageHandler<MobZoneOverlayRequestPacket, IMessage> {
    @Override
    public IMessage onMessage(MobZoneOverlayRequestPacket message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        player.getServer().addScheduledTask(() -> {
            List<MobZoneOverlayPacket.Zone> zones = new ArrayList<>();
            for (LocationDefinition zone : ServerLifecycle.services().mobZoneOverlay.snapshot()) {
                OverlayGeometry trigger = null;
                if (MobZoneIds.isMobZoneId(zone.getLocationId())) {
                    trigger = ServerLifecycle.services().stageTransitionTriggers.overlayGeometry(
                            MobZoneIds.worldLocationId(zone.getLocationId()),
                            MobZoneIds.mobSlotId(zone.getLocationId()));
                }
                zones.add(trigger == null
                        ? new MobZoneOverlayPacket.Zone(
                                zone.getLocationId(), zone.getCenterX(), zone.getCenterZ(), zone.getRadius())
                        : new MobZoneOverlayPacket.Zone(
                                zone.getLocationId(), zone.getCenterX(), zone.getCenterZ(), zone.getRadius(),
                                true,
                                trigger.getInnerRadius(), trigger.getOuterRadius(),
                                trigger.getForwardX(), trigger.getForwardZ()));
            }
            NetworkHandler.sendToClient(new MobZoneOverlayPacket(zones), player);
        });
        return null;
    }
}
