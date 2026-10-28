package com.example.revolution.admin.service;

import com.example.revolution.admin.definition.AdminMobSnapshot;
import com.example.revolution.admin.definition.AdminPlayerSnapshot;
import com.example.revolution.mob.definition.MobDefinition;
import com.example.revolution.mob.repository.MobRepository;
import com.example.revolution.number.LargeNumbers;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.player.repository.PlayerIdentity;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.resource.service.ResourceService;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.state.RuneInventory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.entity.player.EntityPlayerMP;

/** Read-only administrative projections; transport formatting stays in adapters. */
public final class AdminQueryService {
    private Runnable configurationGuard = () -> { };
    public void setConfigurationGuard(Runnable guard) { configurationGuard = guard; }
    private final PlayerRepository players;
    private final ResourceService resources;
    private final MobRepository mobs;

    public AdminQueryService(PlayerRepository players, ResourceService resources, MobRepository mobs) {
        this.players = players;
        this.resources = resources;
        this.mobs = mobs;
    }

    public List<AdminPlayerSnapshot> allPlayers(Iterable<EntityPlayerMP> online) {
        return allPlayers(online, uuid -> "");
    }

    public List<AdminPlayerSnapshot> allPlayers(Iterable<EntityPlayerMP> online, Function<UUID, String> cachedName) {
        List<AdminPlayerSnapshot> snapshots = new ArrayList<>();
        List<String> resourceNames = ResourceDefinitions.getResourceNames();
        Map<UUID, EntityPlayerMP> live = new HashMap<>();
        for (EntityPlayerMP player : online) {
            live.put(player.getUniqueID(), player);
            players.rememberIdentity(player.getUniqueID(), player.getName());
        }
        List<PlayerIdentity> known = players.listKnownPlayers();
        for (EntityPlayerMP player : live.values()) {
            boolean present = false;
            for (PlayerIdentity identity : known) if (identity.uuid.equals(player.getUniqueID())) { present = true; break; }
            if (!present) known.add(new PlayerIdentity(player.getUniqueID(), player.getName()));
        }
        for (PlayerIdentity identity : known) {
            EntityPlayerMP player = live.get(identity.uuid);
            UUID uuid = identity.uuid;
            int[] bestRanks = new int[RuneInventory.BOOST_TYPE_COUNT];
            for (RuneRecord rune : players.getRunesInCollection(uuid, 0)) {
                if (rune.runeIndex < 0 || rune.runeIndex >= bestRanks.length) continue;
                bestRanks[rune.runeIndex] = Math.max(bestRanks[rune.runeIndex], rune.layer);
            }
            int owned = 0, normal = 0, improved = 0, advanced = 0;
            for (int rank : bestRanks) {
                if (rank <= 0) continue;
                owned++;
                if (rank >= RuneInventory.ADVANCED_RANK) advanced++;
                else if (rank >= RuneInventory.IMPROVED_RANK) improved++;
                else normal++;
            }
            Map<String, String> balances = new LinkedHashMap<>();
            for (String resource : resourceNames) {
                balances.put(resource, LargeNumbers.canonical(resources.getPlayerResourceValue(uuid, resource)));
            }
            String name = player == null ? identity.lastKnownName : player.getName();
            if ((name == null || name.trim().isEmpty()) && cachedName != null) name = cachedName.apply(uuid);
            if (name == null || name.trim().isEmpty()) name = uuid.toString();
            else players.rememberIdentity(uuid, name);
            snapshots.add(new AdminPlayerSnapshot(
                    uuid.toString(), name, player != null, player == null ? 0 : player.dimension,
                    player == null ? 0.0D : player.posX, player == null ? 0.0D : player.posY, player == null ? 0.0D : player.posZ,
                    owned, normal, improved, advanced, balances));
        }
        snapshots.sort((left, right) -> {
            if (left.online != right.online) return left.online ? -1 : 1;
            return left.name.compareToIgnoreCase(right.name);
        });
        return snapshots;
    }

    /** Compatibility alias retained for older internal callers and tests. */
    public List<AdminPlayerSnapshot> onlinePlayers(Iterable<EntityPlayerMP> online) { return allPlayers(online); }

    public List<AdminMobSnapshot> mobs() {
        configurationGuard.run();
        List<AdminMobSnapshot> snapshots = new ArrayList<>();
        for (MobDefinition mob : mobs.getAllMobs()) {
            snapshots.add(new AdminMobSnapshot(
                    mob.name,
                    mob.type,
                    LargeNumbers.canonical(mob.hp),
                    mobs.getMobResource(mob.name),
                    LargeNumbers.canonical(mobs.getMobAmountValue(mob.name)),
                    mob.x, mob.y, mob.z, mob.enabled));
        }
        return snapshots;
    }

    public List<String> resourceNames() {
        return new ArrayList<>(ResourceDefinitions.getResourceNames());
    }
}
