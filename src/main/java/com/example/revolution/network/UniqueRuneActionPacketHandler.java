package com.example.revolution.network;

import com.example.revolution.lifecycle.ServerLifecycle;
import com.example.revolution.lifecycle.ServerServices;
import com.example.revolution.mechanic.BalanceMechanicUnlockPolicy;
import com.example.revolution.unique.UniqueRuneModel;
import com.example.revolution.unique.UniqueRuneService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server adapter. Every mutation and every shared field is derived from authoritative state. */
public final class UniqueRuneActionPacketHandler implements IMessageHandler<UniqueRuneActionPacket, IMessage> {
    @Override public IMessage onMessage(UniqueRuneActionPacket message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        if (player == null || message == null) return null;
        player.getServerWorld().addScheduledTask(() -> handle(player, message));
        return null;
    }

    private static void handle(EntityPlayerMP player, UniqueRuneActionPacket request) {
        ServerServices services = ServerLifecycle.services();
        if (!services.mechanicUnlocks.isUnlocked(player.getUniqueID(), BalanceMechanicUnlockPolicy.UNIQUE_RUNE_COLLECTIONS)) return;
        String action = request.getAction();
        if ("sync".equals(action)) { sendSnapshot(player, services); return; }
        UniqueRuneModel.CollectionId collection = collection(request.getCollectionId());
        UniqueRuneService.Result crystalRoll = null;
        if ("purchase_crystal".equals(action)) crystalRoll = services.uniqueRunes.purchaseCrystal(player.getUniqueID(), request.getRequestNonce());
        else if ("purchase_offerings".equals(action)) services.uniqueRunes.purchaseOfferings(player.getUniqueID(), request.getRequestNonce());
        else if ("daily".equals(action)) services.uniqueRunes.claimDaily(player.getUniqueID(), System.currentTimeMillis() / 86400000L, request.getRequestNonce());
        else if ("enhance".equals(action)) services.uniqueRunes.enhance(player.getUniqueID(), request.getRequestNonce());
        else if ("awaken".equals(action) && collection != null) services.uniqueRunes.awaken(player.getUniqueID(), collection, request.getRuneId(), request.getRequestNonce());
        else if ("rollback".equals(action) && collection != null) services.uniqueRunes.rollback(player.getUniqueID(), collection, request.getRuneId(), request.isConfirmed(), request.getRequestNonce());
        else if ("share".equals(action) && collection != null) { share(player, services, collection, request.getRuneId()); return; }
        else return;
        sendSnapshot(player, services, crystalRoll);
    }

    public static void sendSnapshot(EntityPlayerMP player, ServerServices services) {
        sendSnapshot(player, services, null);
    }

    private static void sendSnapshot(EntityPlayerMP player, ServerServices services, UniqueRuneService.Result crystalRoll) {
        if (!services.mechanicUnlocks.isUnlocked(player.getUniqueID(), BalanceMechanicUnlockPolicy.UNIQUE_RUNE_COLLECTIONS)) return;
        UniqueRuneModel.PlayerState state = services.uniqueRunes.snapshot(player.getUniqueID());
        List<UniqueRuneSnapshotPacket.Collection> collections = new ArrayList<>();
        List<UniqueRuneSnapshotPacket.Entry> entries = new ArrayList<>();
        for (UniqueRuneModel.CollectionId id : UniqueRuneModel.CollectionId.values()) {
            UniqueRuneModel.CollectionState collection = state.collection(id);
            UniqueRuneSnapshotPacket.Collection out = new UniqueRuneSnapshotPacket.Collection();
            out.collectionId = id.name(); out.rarity = collection.currentRarity.name(); out.progress = collection.progress();
            out.unlocked = services.uniqueRunes.isUnlocked(player.getUniqueID(), id);
            out.canAwaken = false; out.canRollback = false;
            for (String ownedId : collection.owned.keySet()) {
                out.canAwaken |= collection.canAwaken(ownedId);
                out.canRollback |= collection.canRollback(ownedId);
            }
            collections.add(out);
            UniqueRuneModel.CollectionDefinition definition = services.uniqueRunePolicy.definition(id);
            for (Map.Entry<String, UniqueRuneModel.Rarity> owned : collection.owned.entrySet()) {
                UniqueRuneModel.RuneDefinition rune = definition.rune(owned.getKey()); if (rune == null) continue;
                UniqueRuneService.SharePayload payload = services.uniqueRunes.share(player.getUniqueID(), id, rune.runeId); if (payload == null) continue;
                UniqueRuneSnapshotPacket.Entry entry = new UniqueRuneSnapshotPacket.Entry(); entry.collectionId = id.name(); entry.runeId = rune.runeId;
                entry.name = rune.displayName; entry.rarity = owned.getValue().name(); entry.enhancement = payload.enhancement;
                entry.canAwaken = collection.canAwaken(rune.runeId) && out.unlocked;
                entry.canRollback = collection.canRollback(rune.runeId) && out.unlocked;
                entry.enhancementPearlCost = id == UniqueRuneModel.CollectionId.CRYSTAL_COLLECTION
                        ? exact(services.uniqueRunePolicy.enhancementPearlCost(rune.runeId, payload.enhancement)) : "0";
                entry.effects = effectText(payload.effects); entries.add(entry);
            }
        }
        UniqueRuneSnapshotPacket snapshot = new UniqueRuneSnapshotPacket(entries, collections,
                exact(services.uniqueRunes.pearlBalance(player.getUniqueID())), exact(services.uniqueRunes.crystalBalance(player.getUniqueID())),
                exact(services.uniqueRunes.offeringsBalance(player.getUniqueID())), exact(services.uniqueRunePolicy.crystalPurchaseCost()),
                exact(services.uniqueRunePolicy.offeringsPurchaseCost()), services.uniqueRunePolicy.zbtStatus(),
                state.lastDailyClaimDay >= System.currentTimeMillis() / 86400000L
                        ? (state.lastDailyClaimDay + 1L) * 86400000L : 0L);
        snapshot.setDailyExtraReward(services.uniqueRunePolicy.dailyExtraResource(),
                exact(services.uniqueRunePolicy.dailyExtraAmount()));
        snapshot.setCrystalEnhancementCost(exact(services.uniqueRunePolicy.enhancementPearlCost("", 0)));
        if (crystalRoll != null && crystalRoll.ok())
            snapshot.setLastCrystalRoll(crystalRoll.runeId, crystalRoll.duplicate, exact(crystalRoll.compensation));
        NetworkHandler.sendToClient(snapshot, player);
    }

    private static void share(EntityPlayerMP player, ServerServices services, UniqueRuneModel.CollectionId collection, String runeId) {
        UniqueRuneService.SharePayload payload = services.uniqueRunes.share(player.getUniqueID(), collection, runeId); if (payload == null) return;
        UniqueRuneModel.RuneDefinition definition = services.uniqueRunePolicy.definition(collection).rune(runeId); if (definition == null) return;
        String tooltipText = definition.displayName + "\nCollection: " + collection.name() + "\nRarity: " + payload.rarity.name()
                + "\nEnhancement: +" + payload.enhancement + "\nEffects: " + effectText(payload.effects);
        ShareChatDelivery.send(player, "Unique Rune: " + definition.displayName, tooltipText);
    }

    private static String effectText(Map<String, String> effects) { if (effects == null || effects.isEmpty()) return "none"; StringBuilder out = new StringBuilder(); for (Map.Entry<String, String> entry : effects.entrySet()) { if (out.length() > 0) out.append(", "); out.append(entry.getKey()).append(" +").append(entry.getValue()); } return out.toString(); }
    private static UniqueRuneModel.CollectionId collection(String raw) { try { return UniqueRuneModel.CollectionId.valueOf(raw); } catch (RuntimeException ignored) { return null; } }
    private static String exact(BigDecimal value) { return value == null ? "0" : value.stripTrailingZeros().toPlainString(); }
}
