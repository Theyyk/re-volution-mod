package com.example.revolution;


import com.example.revolution.rune.RuneEnhancementManager;
import com.example.revolution.network.RuneEnhancementPacket;
import com.example.revolution.rune.RuneInventory;
import com.example.revolution.admin.AdminPanelAccess;
import com.example.revolution.admin.AdminPanelStatePacket;
import com.example.revolution.mob.MobManager;
import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.RuneSlotPacket;
import com.example.revolution.number.LargeNumbers;
import com.example.revolution.persistence.MongoManager;
import com.example.revolution.resource.ResourceManager;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class AdminPanelRequestPacket implements IMessage {
    private String action;

    public AdminPanelRequestPacket() {}
    public AdminPanelRequestPacket(String action) { this.action = action; }

    @Override public void fromBytes(ByteBuf buf) { action = ByteBufUtils.readUTF8String(buf); }
    @Override public void toBytes(ByteBuf buf) { ByteBufUtils.writeUTF8String(buf, action == null ? "" : action); }

    @SideOnly(Side.SERVER)
    public static final class Handler implements IMessageHandler<AdminPanelRequestPacket, IMessage> {
        @Override
        public IMessage onMessage(AdminPanelRequestPacket message, MessageContext ctx) {
            EntityPlayerMP admin = ctx.getServerHandler().player;
            admin.getServerWorld().addScheduledTask(() -> handle(admin, message.action == null ? "" : message.action));
            return null;
        }

        private static void handle(EntityPlayerMP admin, String action) {
            if (!AdminPanelAccess.isAllowed(admin.getUniqueID())) return;
            if (action.equals("open") || action.equals("refresh")) { sendState(admin); return; }
            String[] parts = action.split("\\t", -1);
            if (parts.length == 0) return;
            try {
                if ("runes_clear".equals(parts[0]) && parts.length >= 2) {
                    EntityPlayerMP target = target(admin, parts[1]); if (target == null) return;
                    ensureCollectionZero(target); MongoManager.clearCollection(target.getUniqueID(), 0); syncRunes(target);
                } else if ("runes_fill".equals(parts[0]) && parts.length >= 3) {
                    EntityPlayerMP target = target(admin, parts[1]); if (target == null) return;
                    int rank = Math.max(RuneInventory.NORMAL_RANK, Math.min(RuneInventory.ADVANCED_RANK, Integer.parseInt(parts[2])));
                    ensureCollectionZero(target); MongoManager.clearCollection(target.getUniqueID(), 0);
                    for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) MongoManager.addRuneToCollection(target.getUniqueID(), 0, rune, rune, rank, 1);
                    syncRunes(target);
                } else if ("runes_reset_enhancements".equals(parts[0]) && parts.length >= 2) {
                    EntityPlayerMP target = target(admin, parts[1]); if (target == null) return;
                    ensureCollectionZero(target); RuneEnhancementManager.resetCollection(target.getUniqueID(), 0); syncRunes(target);
                } else if ("resource_set".equals(parts[0]) && parts.length >= 4) {
                    EntityPlayerMP target = target(admin, parts[1]); if (target == null) return;
                    ResourceManager.setPlayerResource(target.getUniqueID(), parts[2], Integer.parseInt(parts[3])); PlayerStatsService.sendTo(target);
                } else if ("resource_add".equals(parts[0]) && parts.length >= 4) {
                    EntityPlayerMP target = target(admin, parts[1]); if (target == null) return;
                    ResourceManager.addPlayerResource(target.getUniqueID(), parts[2], Integer.parseInt(parts[3])); PlayerStatsService.sendTo(target);
                } else if ("resource_zero".equals(parts[0]) && parts.length >= 3) {
                    EntityPlayerMP target = target(admin, parts[1]); if (target == null) return;
                    ResourceManager.setPlayerResource(target.getUniqueID(), parts[2], 0); PlayerStatsService.sendTo(target);
                } else if ("resource_zero_all".equals(parts[0]) && parts.length >= 2) {
                    EntityPlayerMP target = target(admin, parts[1]); if (target == null) return;
                    for (String resource : ResourceManager.getResourceNames()) ResourceManager.setPlayerResource(target.getUniqueID(), resource, 0);
                    PlayerStatsService.sendTo(target);
                } else if ("mob_create".equals(parts[0]) && parts.length >= 7) {
                    String name = parts[1].trim();
                    String type = parts[2].trim().toLowerCase(Locale.ROOT);
                    String resource = ResourceManager.findCanonicalName(parts[3]);
                    BigDecimal amount = LargeNumbers.parseFlexible(parts[4]);
                    BigDecimal hp = LargeNumbers.parseExact(parts[5]);
                    EntityPlayerMP target = target(admin, parts[6]);
                    if (target == null) return;
                    if (name.isEmpty() || name.contains("\t") || MongoManager.getMob(name) != null) return;
                    if (!("zombie".equals(type) || "skeleton".equals(type)) || resource == null || amount.signum() <= 0 || hp.signum() <= 0) return;
                    MongoManager.createMob(name, type, hp, 1);
                    ResourceManager.setMobReward(name, resource, amount);
                    MongoManager.setMobSpawn(name, target.posX, target.posY, target.posZ);
                    MobManager.respawnAll();
                } else if ("mob_delete".equals(parts[0]) && parts.length >= 2) {
                    if (MongoManager.getMob(parts[1]) != null) { MongoManager.deleteMob(parts[1]); MobManager.respawnAll(); }
                } else if ("mob_setspawn".equals(parts[0]) && parts.length >= 3) {
                    EntityPlayerMP target = target(admin, parts[2]);
                    if (target == null || MongoManager.getMob(parts[1]) == null) return;
                    MongoManager.setMobSpawn(parts[1], target.posX, target.posY, target.posZ); MobManager.respawnAll();
                }
            } catch (RuntimeException e) {
                RevolutionMod.logger.warn("Admin Panel action failed: " + action, e);
            }
            sendState(admin);
        }

        private static EntityPlayerMP target(EntityPlayerMP admin, String rawUuid) {
            UUID uuid;
            try { uuid = UUID.fromString(rawUuid); } catch (IllegalArgumentException e) { return null; }
            EntityPlayerMP target = admin.getServer().getPlayerList().getPlayerByUUID(uuid);
            if (target == null) admin.sendMessage(new net.minecraft.util.text.TextComponentString("§cЦелевой игрок должен быть онлайн."));
            return target;
        }

        private static void ensureCollectionZero(EntityPlayerMP target) {
            if (MongoManager.getCollectionNames(target.getUniqueID()).isEmpty()) {
                MongoManager.addCollection(target.getUniqueID(), "Земля"); MongoManager.setActiveCollection(target.getUniqueID(), 0);
            }
        }

        private static void syncRunes(EntityPlayerMP target) {
            NetworkHandler.INSTANCE.sendTo(new RuneSlotPacket(-1, 0, 0, false), target);
            for (MongoManager.SavedRune rune : MongoManager.getRunesInCollection(target.getUniqueID(), 0)) NetworkHandler.INSTANCE.sendTo(new RuneSlotPacket(rune.slot, rune.runeIndex, rune.layer, false), target);
            if (MongoManager.getActiveCollection(target.getUniqueID()) == 0) NetworkHandler.INSTANCE.sendTo(new RuneEnhancementPacket(RuneEnhancementManager.getLevels(target.getUniqueID(), 0)), target);
            PlayerStatsService.sendTo(target);
        }

        static void sendState(EntityPlayerMP admin) {
            List<String> resources = ResourceManager.getResourceNames();
            List<String> players = new ArrayList<>();
            List<String> balances = new ArrayList<>();
            for (EntityPlayerMP player : admin.getServer().getPlayerList().getPlayers()) {
                int[] bestRanks = new int[RuneInventory.BOOST_TYPE_COUNT];
                for (MongoManager.SavedRune rune : MongoManager.getRunesInCollection(player.getUniqueID(), 0)) {
                    if (rune.runeIndex < 0 || rune.runeIndex >= bestRanks.length) continue;
                    bestRanks[rune.runeIndex] = Math.max(bestRanks[rune.runeIndex], rune.layer);
                }
                int owned = 0, normal = 0, silver = 0, gold = 0;
                for (int rank : bestRanks) {
                    if (rank <= 0) continue;
                    owned++; if (rank >= RuneInventory.ADVANCED_RANK) gold++; else if (rank >= RuneInventory.IMPROVED_RANK) silver++; else normal++;
                }
                players.add(player.getUniqueID().toString() + "\t" + player.getName() + "\t" + player.dimension + "\t" + coordinate(player.posX) + "\t" + coordinate(player.posY)
                        + "\t" + coordinate(player.posZ) + "\t" + owned + "\t" + normal + "\t" + silver + "\t" + gold);
                for (String resource : resources) balances.add(player.getUniqueID().toString() + "\t" + resource + "\t" + LargeNumbers.canonical(ResourceManager.getPlayerResourceValue(player.getUniqueID(), resource)));
            }
            List<String> mobs = new ArrayList<>();
            for (MongoManager.CustomMob mob : MongoManager.getAllMobs()) {
                mobs.add(mob.name + "\t" + mob.type + "\t" + LargeNumbers.canonical(mob.hp) + "\t"
                        + ResourceManager.getMobResource(mob.name) + "\t" + LargeNumbers.canonical(ResourceManager.getMobAmountValue(mob.name))
                        + "\t" + coordinate(mob.x) + "\t" + coordinate(mob.y) + "\t" + coordinate(mob.z) + "\t" + mob.enabled);
            }
            NetworkHandler.INSTANCE.sendTo(new AdminPanelStatePacket(players, resources, mobs, balances), admin);
        }

        private static String coordinate(double value) { return String.format(Locale.ROOT, "%.1f", value); }
    }
}
