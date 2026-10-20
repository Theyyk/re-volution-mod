package com.example.revolution.realm.service;

import com.example.revolution.realm.definition.RealmCatalog;
import com.example.revolution.realm.definition.RealmDefinition;
import com.example.revolution.realm.world.RealmTeleporter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

/** Server-owned Realm lifecycle and transfer service. Location/mob-position concerns start in v1.3.5. */
public final class RealmService {
    public void initialize(MinecraftServer server) {
        for (RealmDefinition realm : RealmCatalog.all()) ensureStartPlatform(requireWorld(server, realm), realm);
    }

    /** Every login is assigned fresh; persisted previous dimension/position is intentionally ignored. */
    public RealmDefinition assignOnLogin(EntityPlayerMP player) {
        MinecraftServer server = player.getServer();
        if (server == null) throw new IllegalStateException("MinecraftServer is unavailable for Realm assignment");
        RealmDefinition selected = RealmCatalog.leastPopulated(population(server, player.getUniqueID()));
        join(server, player, selected.getRealmId());
        return selected;
    }

    public RealmDefinition join(MinecraftServer server, EntityPlayerMP player, int realmId) {
        RealmDefinition realm = RealmCatalog.byRealmId(realmId);
        if (realm == null) throw new IllegalArgumentException("Unknown Realm ID: " + realmId);

        if (realmId == 0) {
            WorldServer overworld = server.getWorld(0);
            if (overworld == null) throw new IllegalStateException("Overworld dimension 0 is unavailable");
            BlockPos spawn = overworld.getSpawnPoint();
            RealmDefinition destination = new RealmDefinition(0, 0, realm.getName(), spawn.getX(), spawn.getY(), spawn.getZ());
            transfer(server, player, overworld, destination);
            return realm;
        }

        WorldServer target = requireWorld(server, realm);
        ensureStartPlatform(target, realm);
        transfer(server, player, target, realm);
        return realm;
    }

    public Map<Integer, Integer> population(MinecraftServer server, UUID excludedPlayer) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (RealmDefinition realm : RealmCatalog.all()) counts.put(realm.getDimensionId(), 0);
        for (EntityPlayerMP online : server.getPlayerList().getPlayers()) {
            if (excludedPlayer != null && excludedPlayer.equals(online.getUniqueID())) continue;
            if (!RealmCatalog.isRealmDimension(online.dimension)) continue;
            counts.put(online.dimension, counts.get(online.dimension) + 1);
        }
        return counts;
    }

    private WorldServer requireWorld(MinecraftServer server, RealmDefinition realm) {
        WorldServer world = server.getWorld(realm.getDimensionId());
        if (world == null) {
            DimensionManager.initDimension(realm.getDimensionId());
            world = server.getWorld(realm.getDimensionId());
        }
        if (world == null) throw new IllegalStateException("Realm dimension did not initialize: " + realm.getDimensionId());
        return world;
    }

    private void ensureStartPlatform(WorldServer world, RealmDefinition realm) {
        // Saved template blocks are authoritative, including edits at the spawn platform.
        if (world.getChunkProvider().isChunkGeneratedAt(realm.getStartX() >> 4, realm.getStartZ() >> 4)) return;
        int y = realm.getStartY() - 1;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.setBlockState(new BlockPos(realm.getStartX() + dx, y, realm.getStartZ() + dz), Blocks.STONE.getDefaultState(), 2);
            }
        }
        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    world.setBlockToAir(new BlockPos(realm.getStartX() + dx, realm.getStartY() + dy, realm.getStartZ() + dz));
                }
            }
        }
    }

    private void transfer(MinecraftServer server, EntityPlayerMP player, WorldServer target, RealmDefinition realm) {
        double x = realm.getStartX() + 0.5D;
        double y = realm.getStartY();
        double z = realm.getStartZ() + 0.5D;
        if (player.dimension == realm.getDimensionId()) {
            player.connection.setPlayerLocation(x, y, z, player.rotationYaw, player.rotationPitch);
            player.motionX = 0.0D;
            player.motionY = 0.0D;
            player.motionZ = 0.0D;
            return;
        }
        server.getPlayerList().transferPlayerToDimension(player, realm.getDimensionId(), new RealmTeleporter(target, realm));
        player.connection.setPlayerLocation(x, y, z, player.rotationYaw, player.rotationPitch);
    }
}
