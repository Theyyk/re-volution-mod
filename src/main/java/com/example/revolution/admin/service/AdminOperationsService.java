package com.example.revolution.admin.service;

import com.example.revolution.mob.definition.MobDefinition;
import com.example.revolution.mob.repository.MobRepository;
import com.example.revolution.mob.service.ManagedMobService;
import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.RuneSlotPacket;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.player.service.PlayerStatsSyncService;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.resource.service.ResourceService;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.service.RunePacketFactory;
import com.example.revolution.rune.state.RuneInventory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * Typed server-side administrative operations.
 *
 * <p>This service contains no transport, authentication, HTTP, command parsing, or GUI logic.
 * Adapters are responsible for authorization and for resolving a target player before invoking it.</p>
 */
public final class AdminOperationsService {
    private final PlayerRepository players;
    private final ResourceService resources;
    private final PlayerStatsSyncService stats;
    private final MobRepository mobRepository;
    private final ManagedMobService mobs;
    private final RuneEnhancementRepository enhancements;

    public AdminOperationsService(
            PlayerRepository players,
            ResourceService resources,
            PlayerStatsSyncService stats,
            MobRepository mobRepository,
            ManagedMobService mobs,
            RuneEnhancementRepository enhancements) {
        this.players = players;
        this.resources = resources;
        this.stats = stats;
        this.mobRepository = mobRepository;
        this.mobs = mobs;
        this.enhancements = enhancements;
    }

    public void clearRunes(EntityPlayerMP target) {
        requireTarget(target);
        ensureCollectionZero(target);
        players.clearCollection(target.getUniqueID(), 0);
        syncRunes(target);
    }

    public void fillRunes(EntityPlayerMP target, int rank) {
        requireTarget(target);
        int normalizedRank = Math.max(
                RuneInventory.NORMAL_RANK,
                Math.min(RuneInventory.ADVANCED_RANK, rank));
        ensureCollectionZero(target);
        players.clearCollection(target.getUniqueID(), 0);
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            players.addRuneToCollection(target.getUniqueID(), 0, rune, rune, normalizedRank, 1);
        }
        syncRunes(target);
    }

    public void resetEnhancements(EntityPlayerMP target) {
        requireTarget(target);
        ensureCollectionZero(target);
        enhancements.resetCollection(target.getUniqueID(), 0);
        syncRunes(target);
    }

    public boolean setResource(EntityPlayerMP target, String resource, int value) {
        return setResource(target, resource, BigDecimal.valueOf(value));
    }

    public boolean setResource(EntityPlayerMP target, String resource, BigDecimal value) {
        requireTarget(target);
        String canonical = ResourceDefinitions.findCanonicalName(resource);
        if (canonical == null || value == null || value.signum() < 0) return false;
        if (!resources.setPlayerResourceValue(target.getUniqueID(), canonical, value)) return false;
        stats.sendTo(target);
        return true;
    }

    public boolean addResource(EntityPlayerMP target, String resource, int amount) {
        return addResource(target, resource, BigDecimal.valueOf(amount));
    }

    public boolean addResource(EntityPlayerMP target, String resource, BigDecimal amount) {
        requireTarget(target);
        String canonical = ResourceDefinitions.findCanonicalName(resource);
        if (canonical == null || amount == null) return false;
        if (!resources.addPlayerResourceValue(target.getUniqueID(), canonical, amount)) return false;
        stats.sendTo(target);
        return true;
    }

    public boolean zeroResource(EntityPlayerMP target, String resource) {
        return setResource(target, resource, BigDecimal.ZERO);
    }

    public void zeroAllResources(EntityPlayerMP target) {
        requireTarget(target);
        for (String resource : ResourceDefinitions.getResourceNames()) {
            resources.setPlayerResourceValue(target.getUniqueID(), resource, BigDecimal.ZERO);
        }
        stats.sendTo(target);
    }

    public boolean createMob(
            String name,
            String type,
            String resource,
            BigDecimal amount,
            BigDecimal hp,
            double x,
            double y,
            double z) {
        String normalizedName = name == null ? "" : name.trim();
        String normalizedType = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        String canonicalResource = ResourceDefinitions.findCanonicalName(resource);
        if (!validName(normalizedName) || mobRepository.getMob(normalizedName) != null) return false;
        if (!("zombie".equals(normalizedType) || "skeleton".equals(normalizedType))) return false;
        if (canonicalResource == null || amount == null || hp == null || amount.signum() <= 0 || hp.signum() <= 0) return false;
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return false;

        mobRepository.createMob(normalizedName, normalizedType, hp, 1);
        mobRepository.setMobReward(normalizedName, canonicalResource, amount);
        mobRepository.setMobSpawn(normalizedName, x, y, z);
        mobs.respawnAll();
        return true;
    }

    public boolean deleteMob(String name) {
        if (!validName(name) || mobRepository.getMob(name) == null) return false;
        mobRepository.deleteMob(name);
        mobs.respawnAll();
        return true;
    }

    public boolean setMobSpawn(String name, double x, double y, double z) {
        if (!validName(name) || mobRepository.getMob(name) == null) return false;
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return false;
        mobRepository.setMobSpawn(name, x, y, z);
        mobs.respawnAll();
        return true;
    }

    public boolean setMobSpawnAtPlayer(String name, EntityPlayerMP target) {
        requireTarget(target);
        return setMobSpawn(name, target.posX, target.posY, target.posZ);
    }

    public List<MobDefinition> mobDefinitions() {
        return mobRepository.getAllMobs();
    }

    private void ensureCollectionZero(EntityPlayerMP target) {
        if (players.getCollectionNames(target.getUniqueID()).isEmpty()) {
            players.addCollection(target.getUniqueID(), "Земля");
            players.setActiveCollection(target.getUniqueID(), 0);
        }
    }

    private void syncRunes(EntityPlayerMP target) {
        NetworkHandler.sendToClient(new RuneSlotPacket(-1, 0, 0, false), target);
        for (RuneRecord rune : players.getRunesInCollection(target.getUniqueID(), 0)) {
            NetworkHandler.sendToClient(
                    new RuneSlotPacket(rune.slot, rune.runeIndex, rune.layer, false),
                    target);
        }
        if (players.getActiveCollection(target.getUniqueID()) == 0) {
            NetworkHandler.sendToClient(
                    RunePacketFactory.enhancements(enhancements.getLevels(target.getUniqueID(), 0)),
                    target);
        }
        stats.sendTo(target);
    }

    private static boolean validName(String value) {
        if (value == null || value.isEmpty() || value.length() > 64) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20 || c == 0x7f) return false;
        }
        return true;
    }

    private static void requireTarget(EntityPlayerMP target) {
        if (target == null) throw new IllegalArgumentException("target player is required");
    }
}
