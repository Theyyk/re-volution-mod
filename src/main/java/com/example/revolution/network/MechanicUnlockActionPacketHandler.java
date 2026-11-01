package com.example.revolution.network;

import com.example.revolution.lifecycle.ServerLifecycle;
import com.example.revolution.lifecycle.ServerServices;
import com.example.revolution.mechanic.MechanicUnlockService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server-authoritative manual unlock transaction with monotonic replay guard. */
public final class MechanicUnlockActionPacketHandler implements IMessageHandler<MechanicUnlockActionPacket, IMessage> {
    private static final ConcurrentMap<UUID, Long> LAST_NONCE = new ConcurrentHashMap<>();
    @Override public IMessage onMessage(MechanicUnlockActionPacket message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        if (player == null || message == null) return null;
        player.getServerWorld().addScheduledTask(() -> handle(player, message));
        return null;
    }
    private static void handle(EntityPlayerMP player, MechanicUnlockActionPacket request) {
        ServerServices services = ServerLifecycle.services();
        if ("sync".equals(request.getAction())) { sendSnapshot(player, services); return; }
        if (!"unlock".equals(request.getAction())) return;
        if (!accept(player.getUniqueID(), request.getRequestNonce())) return;
        services.mechanicUnlocks.unlock(player.getUniqueID(), request.getUnlockId());
        sendSnapshot(player, services);
    }
    private static boolean accept(UUID uuid, long nonce) {
        if (nonce <= 0L) return false;
        for (;;) {
            Long previous = LAST_NONCE.get(uuid);
            if (previous != null && nonce <= previous) return false;
            if (previous == null ? LAST_NONCE.putIfAbsent(uuid, nonce) == null : LAST_NONCE.replace(uuid, previous, nonce)) return true;
        }
    }
    public static void sendSnapshot(EntityPlayerMP player, ServerServices services) {
        List<MechanicUnlockSnapshotPacket.Entry> entries = new ArrayList<>();
        for (MechanicUnlockService.EntrySnapshot snapshot : services.mechanicUnlocks.snapshot(player.getUniqueID())) {
            MechanicUnlockSnapshotPacket.Entry e = new MechanicUnlockSnapshotPacket.Entry();
            e.unlockId = snapshot.definition.unlockId; e.displayName = snapshot.definition.displayName;
            e.rarity = snapshot.definition.rarity.name(); e.parentUnlockId = snapshot.definition.parentUnlockId;
            e.target = snapshot.definition.target; e.progress = snapshot.progress; e.eligible = snapshot.eligible; e.unlocked = snapshot.unlocked;
            entries.add(e);
        }
        boolean ready = false; for (MechanicUnlockSnapshotPacket.Entry e : entries) if (e.eligible && !e.unlocked) { ready = true; break; }
        NetworkHandler.sendToClient(new MechanicUnlockSnapshotPacket(entries, ready, services.mechanicUnlocks.policy().zbtStatus()), player);
    }
    public static void clear() { LAST_NONCE.clear(); }
}
