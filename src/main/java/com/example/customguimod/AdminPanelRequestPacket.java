package com.example.customguimod;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AdminPanelRequestPacket implements IMessage {
    private String action;

    public AdminPanelRequestPacket() {}

    public AdminPanelRequestPacket(String action) {
        this.action = action;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, action == null ? "" : action);
    }

    @SideOnly(Side.SERVER)
    public static final class Handler implements IMessageHandler<AdminPanelRequestPacket, IMessage> {
        @Override
        public IMessage onMessage(AdminPanelRequestPacket message, MessageContext ctx) {
            EntityPlayerMP admin = ctx.getServerHandler().player;
            admin.getServerWorld().addScheduledTask(() -> handle(admin, message.action == null ? "" : message.action));
            return null;
        }

        private static void handle(EntityPlayerMP admin, String action) {
            if (!AdminPanelAccess.isAllowed(admin.getUniqueID())) {
                admin.sendMessage(new net.minecraft.util.text.TextComponentString("§cНет доступа к Admin Panel."));
                return;
            }

            if (action.equals("open") || action.equals("refresh")) {
                sendState(admin);
                return;
            }

            String[] parts = action.split("\\t", -1);
            if (parts.length == 0) return;

            try {
                if ("runes_clear".equals(parts[0]) && parts.length >= 2) {
                    EntityPlayerMP target = target(admin, parts[1]);
                    if (target == null) return;
                    ensureDeckZero(target);
                    MongoManager.clearDeck(target.getUniqueID(), 0);
                    syncRunes(target);
                } else if ("runes_fill".equals(parts[0]) && parts.length >= 3) {
                    EntityPlayerMP target = target(admin, parts[1]);
                    if (target == null) return;
                    int rank = Integer.parseInt(parts[2]);
                    rank = Math.max(RuneInventory.NORMAL_RANK, Math.min(RuneInventory.GOLD_RANK, rank));
                    ensureDeckZero(target);
                    MongoManager.clearDeck(target.getUniqueID(), 0);
                    for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
                        MongoManager.addCardToDeck(target.getUniqueID(), 0, rune, rune, rank, 1);
                    }
                    syncRunes(target);
                } else if ("resource_set".equals(parts[0]) && parts.length >= 4) {
                    EntityPlayerMP target = target(admin, parts[1]);
                    if (target == null) return;
                    ResourceManager.setPlayerResource(target.getUniqueID(), parts[2], Integer.parseInt(parts[3]));
                    PlayerStatsService.sendTo(target);
                } else if ("resource_add".equals(parts[0]) && parts.length >= 4) {
                    EntityPlayerMP target = target(admin, parts[1]);
                    if (target == null) return;
                    ResourceManager.addPlayerResource(target.getUniqueID(), parts[2], Integer.parseInt(parts[3]));
                    PlayerStatsService.sendTo(target);
                } else if ("resource_zero".equals(parts[0]) && parts.length >= 3) {
                    EntityPlayerMP target = target(admin, parts[1]);
                    if (target == null) return;
                    ResourceManager.setPlayerResource(target.getUniqueID(), parts[2], 0);
                    PlayerStatsService.sendTo(target);
                } else if ("resource_zero_all".equals(parts[0]) && parts.length >= 2) {
                    EntityPlayerMP target = target(admin, parts[1]);
                    if (target == null) return;
                    for (String resource : ResourceManager.getResourceNames()) {
                        ResourceManager.setPlayerResource(target.getUniqueID(), resource, 0);
                    }
                    PlayerStatsService.sendTo(target);
                } else if ("mob_create".equals(parts[0]) && parts.length >= 7) {
                    String name = parts[1].trim();
                    String type = parts[2].trim().toLowerCase();
                    String resource = ResourceManager.findCanonicalName(parts[3]);
                    int amount = Integer.parseInt(parts[4]);
                    int hp = Integer.parseInt(parts[5]);
                    EntityPlayerMP target = target(admin, parts[6]);
                    if (target == null) return;
                    if (name.isEmpty() || name.contains("\t") || MongoManager.getMob(name) != null) return;
                    if (!("zombie".equals(type) || "skeleton".equals(type)) || resource == null || amount <= 0 || hp <= 0) return;
                    MongoManager.createMob(name, type, hp, amount);
                    ResourceManager.setMobReward(name, resource, amount);
                    MongoManager.setMobSpawn(name, target.posX, target.posY, target.posZ);
                    MobManager.respawnAll();
                } else if ("mob_delete".equals(parts[0]) && parts.length >= 2) {
                    if (MongoManager.getMob(parts[1]) != null) {
                        MongoManager.deleteMob(parts[1]);
                        MobManager.respawnAll();
                    }
                } else if ("mob_setspawn".equals(parts[0]) && parts.length >= 3) {
                    EntityPlayerMP target = target(admin, parts[2]);
                    if (target == null || MongoManager.getMob(parts[1]) == null) return;
                    MongoManager.setMobSpawn(parts[1], target.posX, target.posY, target.posZ);
                    MobManager.respawnAll();
                }
            } catch (RuntimeException e) {
                CustomGuiMod.logger.warn("Admin Panel action failed: " + action, e);
            }

            sendState(admin);
        }

        private static EntityPlayerMP target(EntityPlayerMP admin, String rawUuid) {
            UUID uuid;
            try {
                uuid = UUID.fromString(rawUuid);
            } catch (IllegalArgumentException e) {
                return null;
            }
            EntityPlayerMP target = admin.getServer().getPlayerList().getPlayerByUUID(uuid);
            if (target == null) admin.sendMessage(new net.minecraft.util.text.TextComponentString("§cЦелевой игрок должен быть онлайн."));
            return target;
        }

        private static void ensureDeckZero(EntityPlayerMP target) {
            if (MongoManager.getDeckNames(target.getUniqueID()).isEmpty()) {
                MongoManager.addDeck(target.getUniqueID(), "Земля");
                MongoManager.setActiveDeck(target.getUniqueID(), 0);
            }
        }

        private static void syncRunes(EntityPlayerMP target) {
            NetworkHandler.INSTANCE.sendTo(new CardPacket(-1, 0, 0, false), target);
            for (MongoManager.SavedCard card : MongoManager.getCardsInDeck(target.getUniqueID(), 0)) {
                NetworkHandler.INSTANCE.sendTo(new CardPacket(card.slot, card.cardIndex, card.layer, false), target);
            }
            PlayerStatsService.sendTo(target);
        }

        static void sendState(EntityPlayerMP admin) {
            List<String> players = new ArrayList<>();
            for (EntityPlayerMP player : admin.getServer().getPlayerList().getPlayers()) {
                players.add(player.getUniqueID().toString() + "\t" + player.getName());
            }

            List<String> mobs = new ArrayList<>();
            for (MongoManager.CustomMob mob : MongoManager.getAllMobs()) {
                mobs.add(mob.name + "\t" + mob.type + "\t" + mob.hp + "\t"
                        + ResourceManager.getMobResource(mob.name) + "\t" + ResourceManager.getMobAmount(mob.name));
            }

            NetworkHandler.INSTANCE.sendTo(
                    new AdminPanelStatePacket(players, ResourceManager.getResourceNames(), mobs), admin);
        }
    }
}
