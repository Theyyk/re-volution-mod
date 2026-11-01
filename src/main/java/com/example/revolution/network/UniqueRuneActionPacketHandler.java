package com.example.revolution.network;

import com.example.revolution.lifecycle.ServerLifecycle;
import com.example.revolution.lifecycle.ServerServices;
import com.example.revolution.unique.UniqueRuneModel;
import com.example.revolution.unique.UniqueRuneService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
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
        String action = request.getAction();
        if ("sync".equals(action)) { sendSnapshot(player, services); return; }
        UniqueRuneModel.CollectionId collection = collection(request.getCollectionId());
        if ("purchase_crystal".equals(action)) services.uniqueRunes.purchaseCrystal(player.getUniqueID(), request.getRequestNonce());
        else if ("purchase_offerings".equals(action)) services.uniqueRunes.purchaseOfferings(player.getUniqueID(), request.getRequestNonce());
        else if ("daily".equals(action)) services.uniqueRunes.claimDaily(player.getUniqueID(), System.currentTimeMillis() / 86400000L, request.getRequestNonce());
        else if ("enhance".equals(action)) services.uniqueRunes.enhance(player.getUniqueID(), request.getRuneId(), request.getRequestNonce());
        else if ("awaken".equals(action) && collection != null) services.uniqueRunes.awaken(player.getUniqueID(), collection, request.getRuneId(), request.getRequestNonce());
        else if ("rollback".equals(action) && collection != null) services.uniqueRunes.rollback(player.getUniqueID(), collection, request.isConfirmed(), request.getRequestNonce());
        else if ("share".equals(action) && collection != null) { share(player, services, collection, request.getRuneId()); return; }
        else return;
        sendSnapshot(player, services);
    }

    public static void sendSnapshot(EntityPlayerMP player, ServerServices services) {
        UniqueRuneModel.PlayerState state = services.uniqueRunes.snapshot(player.getUniqueID());
        List<UniqueRuneSnapshotPacket.Collection> collections = new ArrayList<>();
        List<UniqueRuneSnapshotPacket.Entry> entries = new ArrayList<>();
        for (UniqueRuneModel.CollectionId id : UniqueRuneModel.CollectionId.values()) {
            UniqueRuneModel.CollectionState collection = state.collection(id);
            UniqueRuneSnapshotPacket.Collection out = new UniqueRuneSnapshotPacket.Collection();
            out.collectionId = id.name(); out.rarity = collection.currentRarity.name(); out.progress = collection.progress();
            out.unlocked = services.uniqueRunes.isUnlocked(player.getUniqueID(), id);
            out.canAwaken = collection.progress() == UniqueRuneModel.RUNES_PER_COLLECTION && collection.currentRarity != UniqueRuneModel.Rarity.MYTHIC;
            out.canRollback = collection.rollbackRarity != null; collections.add(out);
            UniqueRuneModel.CollectionDefinition definition = services.uniqueRunePolicy.definition(id);
            for (Map.Entry<String, UniqueRuneModel.Rarity> owned : collection.owned.entrySet()) {
                UniqueRuneModel.RuneDefinition rune = definition.rune(owned.getKey()); if (rune == null) continue;
                UniqueRuneService.SharePayload payload = services.uniqueRunes.share(player.getUniqueID(), id, rune.runeId); if (payload == null) continue;
                UniqueRuneSnapshotPacket.Entry entry = new UniqueRuneSnapshotPacket.Entry(); entry.collectionId = id.name(); entry.runeId = rune.runeId;
                entry.name = rune.displayName; entry.rarity = owned.getValue().name(); entry.enhancement = payload.enhancement; entry.effects = effectText(payload.effects); entries.add(entry);
            }
        }
        NetworkHandler.sendToClient(new UniqueRuneSnapshotPacket(entries, collections,
                exact(services.uniqueRunes.pearlBalance(player.getUniqueID())), exact(services.uniqueRunes.crystalBalance(player.getUniqueID())),
                exact(services.uniqueRunes.offeringsBalance(player.getUniqueID())), exact(services.uniqueRunePolicy.crystalPurchaseCost()),
                exact(services.uniqueRunePolicy.offeringsPurchaseCost()), services.uniqueRunePolicy.zbtStatus()), player);
    }

    private static void share(EntityPlayerMP player, ServerServices services, UniqueRuneModel.CollectionId collection, String runeId) {
        UniqueRuneService.SharePayload payload = services.uniqueRunes.share(player.getUniqueID(), collection, runeId); if (payload == null) return;
        UniqueRuneModel.RuneDefinition definition = services.uniqueRunePolicy.definition(collection).rune(runeId); if (definition == null) return;
        TextComponentString tag = new TextComponentString("[Unique Rune: " + definition.displayName + "]");
        String tooltipText = definition.displayName + "\nCollection: " + collection.name() + "\nRarity: " + payload.rarity.name()
                + "\nEnhancement: +" + payload.enhancement + "\nEffects: " + effectText(payload.effects);
        tag.setStyle(new Style().setColor(TextFormatting.AQUA).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(tooltipText))));
        TextComponentString message = new TextComponentString(player.getName() + " "); message.appendSibling(tag);
        if (player.getServer() != null) player.getServer().getPlayerList().sendMessage(message);
    }

    private static String effectText(Map<String, String> effects) {
        if (effects == null || effects.isEmpty()) return "none"; StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> entry : effects.entrySet()) { if (out.length() > 0) out.append(", "); out.append(entry.getKey()).append(" +").append(entry.getValue()); }
        return out.toString();
    }
    private static UniqueRuneModel.CollectionId collection(String raw) { try { return UniqueRuneModel.CollectionId.valueOf(raw); } catch (RuntimeException ignored) { return null; } }
    private static String exact(BigDecimal value) { return value == null ? "0" : value.stripTrailingZeros().toPlainString(); }
}
