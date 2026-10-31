package com.example.revolution.network;

import com.example.revolution.aura.AuraModel;
import com.example.revolution.lifecycle.ServerLifecycle;
import com.example.revolution.lifecycle.ServerServices;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server adapter for Aura client intents. Generated values are never accepted from the client. */
public final class AuraActionPacketHandler implements IMessageHandler<AuraActionPacket, IMessage> {
    @Override public IMessage onMessage(AuraActionPacket message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        if (player == null || message == null) return null;
        player.getServerWorld().addScheduledTask(() -> handle(player, message));
        return null;
    }

    private static void handle(EntityPlayerMP player, AuraActionPacket request) {
        ServerServices services = ServerLifecycle.services(); String action = request.getAction();
        if ("sync".equals(action)) { sendSnapshot(player, services); return; }
        if (!AuraRequestLimiter.acceptMutation(player.getUniqueID(), request.getRequestNonce())) return;
        try {
            if ("open".equals(action)) {
                services.auras.open(player.getUniqueID(), Integer.parseInt(request.getValue()), System.currentTimeMillis());
            } else if ("equip".equals(action)) {
                services.auras.equip(player.getUniqueID(), UUID.fromString(request.getValue()));
            } else if ("unequip".equals(action)) {
                services.auras.unequip(player.getUniqueID());
            } else if ("pin".equals(action)) {
                services.auras.setPinned(player.getUniqueID(), UUID.fromString(request.getValue()), true);
            } else if ("unpin".equals(action)) {
                services.auras.setPinned(player.getUniqueID(), UUID.fromString(request.getValue()), false);
            } else if ("skip_animation".equals(action)) {
                services.auras.setSkipOpeningAnimation(player.getUniqueID(), Boolean.parseBoolean(request.getValue()));
            } else if ("craft".equals(action)) {
                AuraModel.Rarity rarity = AuraModel.Rarity.valueOf(request.getValue());
                services.auras.craft(player.getUniqueID(), rarity, parseIds(request.getIds()), System.currentTimeMillis());
            } else if ("dismantle".equals(action)) {
                services.auras.dismantle(player.getUniqueID(), UUID.fromString(request.getValue()), System.currentTimeMillis());
            } else return;
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            // Invalid/stale client intent fails closed; authoritative snapshot below repairs UI state.
        }
        sendSnapshot(player, services);
    }

    public static void sendSnapshot(EntityPlayerMP player, ServerServices services) {
        AuraModel.PlayerState state = services.auras.state(player.getUniqueID()); List<AuraSnapshotPacket.Entry> entries = new ArrayList<>();
        for (AuraModel.AuraInstance aura : state.ownedAuras) {
            entries.add(new AuraSnapshotPacket.Entry(aura.instanceId.toString(), aura.rarity.name(), aura.origin.name(), aura.generationSeed,
                    rawSummary(aura), aggregateSummary(aura), aura.pinned, aura.instanceId.equals(state.activeAuraId)));
        }
        NetworkHandler.sendToClient(new AuraSnapshotPacket(entries,
                services.resources.getPlayerResource(player.getUniqueID(), "aura_key"),
                services.resources.getPlayerResource(player.getUniqueID(), "crystals"),
                services.auraPolicy.keyCrystalPrice(), state.totalChestOpens, state.totalChestAuraDrops,
                state.skipOpeningAnimation, services.auraPolicy.featureEnabled()), player);
    }

    private static List<UUID> parseIds(String raw) {
        List<UUID> result = new ArrayList<>(); if (raw == null || raw.trim().isEmpty()) return result;
        for (String token : raw.split(",")) result.add(UUID.fromString(token.trim()));
        return result;
    }

    private static String rawSummary(AuraModel.AuraInstance aura) {
        StringBuilder out = new StringBuilder();
        for (AuraModel.Roll roll : aura.rawRolls) { if (out.length() > 0) out.append('|'); out.append(roll.stat.name()).append('=').append(exact(roll.additiveMultiplier)); }
        return out.toString();
    }

    private static String aggregateSummary(AuraModel.AuraInstance aura) {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<com.example.revolution.effect.EffectResolver.Stat, BigDecimal> entry : aura.aggregatedEffects.entrySet()) {
            if (out.length() > 0) out.append('|'); out.append(entry.getKey().name()).append('=').append(exact(entry.getValue()));
        }
        return out.toString();
    }

    private static String exact(BigDecimal value) { return value == null ? "0" : value.stripTrailingZeros().toPlainString(); }
}
