package com.example.revolution.network;

import com.example.revolution.lifecycle.ServerLifecycle;
import com.example.revolution.location.definition.LocationDefinition;
import java.util.ArrayList;
import java.util.Collections;
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
            if (!player.canUseCommand(2, "custommob")) {
                NetworkHandler.sendToClient(new MobZoneOverlayPacket(Collections.emptyList()), player);
                return;
            }
            List<MobZoneOverlayPacket.Zone> zones = new ArrayList<>();
            for (LocationDefinition zone : ServerLifecycle.services().mobZoneOverlay.snapshot()) {
                zones.add(new MobZoneOverlayPacket.Zone(zone.getLocationId(), zone.getCenterX(), zone.getCenterZ(), zone.getRadius()));
            }
            NetworkHandler.sendToClient(new MobZoneOverlayPacket(zones), player);
        });
        return null;
    }
}
