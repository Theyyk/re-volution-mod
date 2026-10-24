package com.example.revolution.network;

import com.example.revolution.lifecycle.ServerLifecycle;
import com.example.revolution.lifecycle.ServerServices;
import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.MobZoneIds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Builds the player-visible travel menu only from configured progression stages. */
public final class StageTravelMenuRequestHandler implements IMessageHandler<StageTravelMenuRequestPacket, IMessage> {
    @Override
    public IMessage onMessage(StageTravelMenuRequestPacket message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        player.getServer().addScheduledTask(() -> {
            ServerServices services = ServerLifecycle.services();
            List<StageTravelMenuPacket.Entry> entries = new ArrayList<>();
            for (LocationDefinition zone : services.mobZoneOverlay.snapshot()) {
                int internalId = zone.getLocationId();
                if (!MobZoneIds.isMobZoneId(internalId)) continue;
                int worldLocationId = MobZoneIds.worldLocationId(internalId);
                int mobSlotId = MobZoneIds.mobSlotId(internalId);
                if (!services.mobProgression.configuredSlots(worldLocationId).contains(mobSlotId)) continue;
                entries.add(new StageTravelMenuPacket.Entry(
                        worldLocationId,
                        mobSlotId,
                        services.mobProgression.isUnlocked(player.getUniqueID(), internalId),
                        zone.hasTransitionPoint(),
                        zone.isBossZone()));
            }
            NetworkHandler.sendToClient(new StageTravelMenuPacket(
                    entries, services.fastAuto.isEnabled(player.getUniqueID())), player);
        });
        return null;
    }
}
