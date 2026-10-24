package com.example.revolution.network;

import com.example.revolution.lifecycle.ServerLifecycle;
import com.example.revolution.lifecycle.ServerServices;
import com.example.revolution.location.definition.MobZoneIds;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Executes L-menu travel on the server after re-validating progression and destination safety. */
public final class StageTravelTeleportRequestHandler implements IMessageHandler<StageTravelTeleportRequestPacket, IMessage> {
    private static final String LAST_REQUEST_TICK = "revolutionStageTravelLastTick";
    private static final long REQUEST_COOLDOWN_TICKS = 10L;

    @Override
    public IMessage onMessage(StageTravelTeleportRequestPacket message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        player.getServer().addScheduledTask(() -> {
            long now = player.world.getTotalWorldTime();
            long last = player.getEntityData().getLong(LAST_REQUEST_TICK);
            if (last > 0L && now >= last && now - last < REQUEST_COOLDOWN_TICKS) return;
            player.getEntityData().setLong(LAST_REQUEST_TICK, now);

            try {
                int worldLocationId = message.getWorldLocationId();
                int mobSlotId = message.getMobSlotId();
                int targetInternalId = MobZoneIds.encode(worldLocationId, mobSlotId);
                ServerServices services = ServerLifecycle.services();
                if (!services.mobProgression.configuredSlots(worldLocationId).contains(mobSlotId)) {
                    throw new IllegalStateException("эта стадия не настроена");
                }
                services.stageTransitions.moveToStage(player, targetInternalId);
            } catch (IllegalArgumentException | IllegalStateException e) {
                player.sendMessage(new TextComponentString("Телепорт недоступен: " + e.getMessage()));
            }
        });
        return null;
    }
}
