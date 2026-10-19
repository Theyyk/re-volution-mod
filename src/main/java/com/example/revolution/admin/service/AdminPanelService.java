package com.example.revolution.admin.service;

import com.example.revolution.RevolutionMod;
import com.example.revolution.mob.definition.MobDefinition;
import com.example.revolution.mob.repository.MobRepository;
import com.example.revolution.mob.service.ManagedMobService;
import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.RuneSlotPacket;
import com.example.revolution.network.admin.AdminPanelStatePacket;
import com.example.revolution.number.LargeNumbers;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.player.service.PlayerStatsSyncService;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.resource.service.ResourceService;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.service.RunePacketFactory;
import com.example.revolution.rune.state.RuneInventory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;

public final class AdminPanelService {
    private final AdminAccessService access;
    private final PlayerRepository players;
    private final ResourceService resources;
    private final PlayerStatsSyncService stats;
    private final MobRepository mobRepository;
    private final ManagedMobService mobs;
    private final RuneEnhancementRepository enhancements;

    public AdminPanelService(
            AdminAccessService access,
            PlayerRepository players,
            ResourceService resources,
            PlayerStatsSyncService stats,
            MobRepository mobRepository,
            ManagedMobService mobs,
            RuneEnhancementRepository enhancements) {
        this.access = access;
        this.players = players;
        this.resources = resources;
        this.stats = stats;
        this.mobRepository = mobRepository;
        this.mobs = mobs;
        this.enhancements = enhancements;
    }

    public void handle(EntityPlayerMP admin, String action) {
        if (!access.isAllowed(admin.getUniqueID())) return;
        if (action.equals("open") || action.equals("refresh")) { sendState(admin); return; }
        String[] parts = action.split("\\t", -1);
        if (parts.length == 0) return;
        try {
            if ("runes_clear".equals(parts[0]) && parts.length >= 2) {
                EntityPlayerMP target = target(admin, parts[1]); if (target == null) return;
                ensureCollectionZero(target); players.clearCollection(target.getUniqueID(), 0); syncRunes(target);
            } else if ("runes_fill".equals(parts[0]) && parts.length >= 3) {
                EntityPlayerMP target = target(admin, parts[1]); if (target == null) return;
                int rank = Math.max(RuneInventory.NORMAL_RANK, Math.min(RuneInventory.ADVANCED_RANK, Integer.parseInt(parts[2])));
                ensureCollectionZero(target); players.clearCollection(target.getUniqueID(), 0);
                for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) players.addRuneToCollection(target.getUniqueID(), 0, rune, rune, rank, 1);
                syncRunes(target);
            } else if ("runes_reset_enhancements".equals(parts[0]) && parts.length >= 2) {
                EntityPlayerMP target = target(admin, parts[1]); if (target == null) return;
                ensureCollectionZero(target); enhancements.resetCollection(target.getUniqueID(), 0); syncRunes(target);
            } else if ("resource_set".equals(parts[0]) && parts.length >= 4) {
                EntityPlayerMP target = target(admin, parts[1]); if (target == null) return;
                resources.setPlayerResource(target.getUniqueID(), parts[2], Integer.parseInt(parts[3])); stats.sendTo(target);
            } else if ("resource_add".equals(parts[0]) && parts.length >= 4) {
                EntityPlayerMP target = target(admin, parts[1]); if (target == null) return;
                resources.addPlayerResource(target.getUniqueID(), parts[2], Integer.parseInt(parts[3])); stats.sendTo(target);
            } else if ("resource_zero".equals(parts[0]) && parts.length >= 3) {
                EntityPlayerMP target = target(admin, parts[1]); if (target == null) return;
                resources.setPlayerResource(target.getUniqueID(), parts[2], 0); stats.sendTo(target);
            } else if ("resource_zero_all".equals(parts[0]) && parts.length >= 2) {
                EntityPlayerMP target = target(admin, parts[1]); if (target == null) return;
                for (String resource : ResourceDefinitions.getResourceNames()) resources.setPlayerResource(target.getUniqueID(), resource, 0);
                stats.sendTo(target);
            } else if ("mob_create".equals(parts[0]) && parts.length >= 7) {
                String name = parts[1].trim();
                String type = parts[2].trim().toLowerCase(Locale.ROOT);
                String resource = ResourceDefinitions.findCanonicalName(parts[3]);
                BigDecimal amount = LargeNumbers.parseFlexible(parts[4]);
                BigDecimal hp = LargeNumbers.parseExact(parts[5]);
                EntityPlayerMP target = target(admin, parts[6]);
                if (target == null) return;
                if (name.isEmpty() || name.contains("\t") || mobRepository.getMob(name) != null) return;
                if (!("zombie".equals(type) || "skeleton".equals(type)) || resource == null || amount.signum() <= 0 || hp.signum() <= 0) return;
                mobRepository.createMob(name, type, hp, 1);
                mobRepository.setMobReward(name, resource, amount);
                mobRepository.setMobSpawn(name, target.posX, target.posY, target.posZ);
                mobs.respawnAll();
            } else if ("mob_delete".equals(parts[0]) && parts.length >= 2) {
                if (mobRepository.getMob(parts[1]) != null) { mobRepository.deleteMob(parts[1]); mobs.respawnAll(); }
            } else if ("mob_setspawn".equals(parts[0]) && parts.length >= 3) {
                EntityPlayerMP target = target(admin, parts[2]);
                if (target == null || mobRepository.getMob(parts[1]) == null) return;
                mobRepository.setMobSpawn(parts[1], target.posX, target.posY, target.posZ); mobs.respawnAll();
            }
        } catch (RuntimeException e) {
            RevolutionMod.logger.warn("Admin Panel action failed: " + action, e);
        }
        sendState(admin);
    }

    private EntityPlayerMP target(EntityPlayerMP admin, String rawUuid) {
        UUID uuid;
        try { uuid = UUID.fromString(rawUuid); } catch (IllegalArgumentException e) { return null; }
        EntityPlayerMP target = admin.getServer().getPlayerList().getPlayerByUUID(uuid);
        if (target == null) admin.sendMessage(new net.minecraft.util.text.TextComponentString("§cЦелевой игрок должен быть онлайн."));
        return target;
    }

    private void ensureCollectionZero(EntityPlayerMP target) {
        if (players.getCollectionNames(target.getUniqueID()).isEmpty()) {
            players.addCollection(target.getUniqueID(), "Земля"); players.setActiveCollection(target.getUniqueID(), 0);
        }
    }

    private void syncRunes(EntityPlayerMP target) {
        NetworkHandler.sendToClient(new RuneSlotPacket(-1, 0, 0, false), target);
        for (RuneRecord rune : players.getRunesInCollection(target.getUniqueID(), 0)) NetworkHandler.sendToClient(new RuneSlotPacket(rune.slot, rune.runeIndex, rune.layer, false), target);
        if (players.getActiveCollection(target.getUniqueID()) == 0) NetworkHandler.sendToClient(RunePacketFactory.enhancements(enhancements.getLevels(target.getUniqueID(), 0)), target);
        stats.sendTo(target);
    }

    public void sendState(EntityPlayerMP admin) {
        List<String> resourceNames = ResourceDefinitions.getResourceNames();
        List<String> playerRows = new ArrayList<>();
        List<String> balances = new ArrayList<>();
        for (EntityPlayerMP player : admin.getServer().getPlayerList().getPlayers()) {
            int[] bestRanks = new int[RuneInventory.BOOST_TYPE_COUNT];
            for (RuneRecord rune : players.getRunesInCollection(player.getUniqueID(), 0)) {
                if (rune.runeIndex < 0 || rune.runeIndex >= bestRanks.length) continue;
                bestRanks[rune.runeIndex] = Math.max(bestRanks[rune.runeIndex], rune.layer);
            }
            int owned = 0, normal = 0, silver = 0, gold = 0;
            for (int rank : bestRanks) {
                if (rank <= 0) continue;
                owned++; if (rank >= RuneInventory.ADVANCED_RANK) gold++; else if (rank >= RuneInventory.IMPROVED_RANK) silver++; else normal++;
            }
            playerRows.add(player.getUniqueID().toString() + "\t" + player.getName() + "\t" + player.dimension + "\t" + coordinate(player.posX) + "\t" + coordinate(player.posY)
                    + "\t" + coordinate(player.posZ) + "\t" + owned + "\t" + normal + "\t" + silver + "\t" + gold);
            for (String resource : resourceNames) balances.add(player.getUniqueID().toString() + "\t" + resource + "\t" + LargeNumbers.canonical(resources.getPlayerResourceValue(player.getUniqueID(), resource)));
        }
        List<String> mobs = new ArrayList<>();
        for (MobDefinition mob : mobRepository.getAllMobs()) {
            mobs.add(mob.name + "\t" + mob.type + "\t" + LargeNumbers.canonical(mob.hp) + "\t"
                    + mobRepository.getMobResource(mob.name) + "\t" + LargeNumbers.canonical(mobRepository.getMobAmountValue(mob.name))
                    + "\t" + coordinate(mob.x) + "\t" + coordinate(mob.y) + "\t" + coordinate(mob.z) + "\t" + mob.enabled);
        }
        NetworkHandler.sendToClient(new AdminPanelStatePacket(playerRows, resourceNames, mobs, balances), admin);
    }

    private String coordinate(double value) { return String.format(Locale.ROOT, "%.1f", value); }
}
