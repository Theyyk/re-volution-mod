package com.example.revolution.network;

import com.example.revolution.lifecycle.ServerLifecycle;
import com.example.revolution.lifecycle.ServerServices;
import com.example.revolution.mechanic.BalanceMechanicUnlockPolicy;
import com.example.revolution.prestige.PrestigeState;
import com.example.revolution.prestige.exchange.definition.RunExchangeOffer;
import com.example.revolution.prestige.exchange.service.RunExchangeService;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Authoritative Run Exchange adapter. Never accept a client cost, reward or target amount. */
public final class RunExchangeActionPacketHandler implements IMessageHandler<RunExchangeActionPacket, IMessage> {
    private static final ConcurrentMap<UUID, Long> LAST_PURCHASE_ID = new ConcurrentHashMap<>();

    @Override public IMessage onMessage(RunExchangeActionPacket request, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        if (player != null && request != null)
            player.getServerWorld().addScheduledTask(() -> handle(player, request));
        return null;
    }

    private static void handle(EntityPlayerMP player, RunExchangeActionPacket request) {
        ServerServices services = ServerLifecycle.services();
        UUID id = player.getUniqueID();
        String response = "";
        if ("sync".equals(request.getAction())) {
            // Read-only server snapshot.
        } else if (!"purchase".equals(request.getAction())) {
            response = "UNKNOWN_ACTION";
        } else if (!services.mechanicUnlocks.isUnlocked(id, BalanceMechanicUnlockPolicy.RUN_EXCHANGE)) {
            response = "LOCKED";
        } else if (!acceptPurchase(id, request.getRequestId())) {
            response = "REPLAY_REJECTED";
        } else {
            try {
                response = services.runExchange.exchange(id, request.getExpectedRunId(), request.getOfferId()).name();
            } catch (IllegalArgumentException invalid) {
                response = "INVALID_REQUEST";
            } catch (IllegalStateException invalid) {
                response = "SERVER_ERROR";
            }
        }
        sendSnapshot(player, services, request.getRequestId(), response);
    }

    /** Monotonic per-player action IDs forbid replaying the same charged operation. */
    static boolean acceptPurchase(UUID id, long requestId) {
        if (id == null || requestId <= 0L) return false;
        for (;;) {
            Long last = LAST_PURCHASE_ID.get(id);
            if (last != null && requestId <= last) return false;
            if (last == null ? LAST_PURCHASE_ID.putIfAbsent(id, requestId) == null
                    : LAST_PURCHASE_ID.replace(id, last, requestId)) return true;
        }
    }

    public static void clear() { LAST_PURCHASE_ID.clear(); }

    static void sendSnapshot(EntityPlayerMP player, ServerServices services, long requestId, String response) {
        UUID id = player.getUniqueID();
        RunExchangeSnapshotPacket packet = new RunExchangeSnapshotPacket();
        packet.requestId = requestId;
        packet.response = response;
        packet.unlocked = services.mechanicUnlocks.isUnlocked(id, BalanceMechanicUnlockPolicy.RUN_EXCHANGE);
        PrestigeState.RunState run = services.prestige.activeRun(id);
        packet.runId = run == null ? "" : run.runId;
        packet.fragments = exact(run == null ? BigDecimal.ZERO : run.runBranchCurrency);
        packet.crystals = exact(services.resources.getPlayerResourceValue(id, "crystals"));

        for (RunExchangeOffer offer : services.runExchange.catalog().all()) {
            RunExchangeService.Preview preview = null;
            if (packet.unlocked && run != null && offer.enabled) {
                try { preview = services.runExchange.preview(id, run.runId, offer.offerId); }
                catch (IllegalStateException invalid) { /* Disabled balance entry remains unavailable. */ }
            }
            String reason = "";
            boolean available = false;
            if (!packet.unlocked) reason = "Разблокируйте Run Exchange";
            else if (run == null) reason = "Требуется активный забег";
            else if (!offer.enabled) reason = "Предложение отключено";
            else if (preview == null) reason = "Предложение недоступно";
            else {
                boolean afford = offer.paymentCurrency == RunExchangeOffer.PaymentCurrency.FRAGMENTS
                        ? run.runBranchCurrency.compareTo(preview.currentCost) >= 0
                        : services.resources.canAfford(id, "crystals", preview.currentCost);
                available = afford;
                if (!afford) reason = "Недостаточно ресурсов";
            }
            String target = offer.isPetSlotOffer() ? "Дополнительный слот питомца" : offer.targetResource;
            String reward = preview == null ? "—" : (offer.isPetSlotOffer()
                    ? "1 слот" : "+" + exact(preview.expectedReward) + " (" + exact(preview.rewardPercent) + "%)");
            packet.addOffer(new RunExchangeSnapshotPacket.OfferEntry(offer.offerId,
                    offer.paymentCurrency.name(), target,
                    preview == null ? "—" : exact(preview.currentCost), reward,
                    preview == null ? 0 : preview.purchaseCount, available, reason));
        }
        NetworkHandler.sendToClient(packet, player);
    }

    private static String exact(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
