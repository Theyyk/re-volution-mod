package com.example.revolution.admin.service;

import com.example.revolution.admin.definition.AdminMobSnapshot;
import com.example.revolution.admin.definition.AdminPlayerSnapshot;
import com.example.revolution.mob.definition.MobDefinition;
import com.example.revolution.mob.repository.MobRepository;
import com.example.revolution.number.LargeNumbers;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.resource.service.ResourceService;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.state.RuneInventory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.player.EntityPlayerMP;

/** Read-only administrative projections; transport formatting stays in adapters. */
public final class AdminQueryService {
    private final PlayerRepository players;
    private final ResourceService resources;
    private final MobRepository mobs;

    public AdminQueryService(PlayerRepository players, ResourceService resources, MobRepository mobs) {
        this.players = players;
        this.resources = resources;
        this.mobs = mobs;
    }

    public List<AdminPlayerSnapshot> onlinePlayers(Iterable<EntityPlayerMP> online) {
        List<AdminPlayerSnapshot> snapshots = new ArrayList<>();
        List<String> resourceNames = ResourceDefinitions.getResourceNames();
        for (EntityPlayerMP player : online) {
            int[] bestRanks = new int[RuneInventory.BOOST_TYPE_COUNT];
            for (RuneRecord rune : players.getRunesInCollection(player.getUniqueID(), 0)) {
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
                balances.put(resource, LargeNumbers.canonical(resources.getPlayerResourceValue(player.getUniqueID(), resource)));
            }
            snapshots.add(new AdminPlayerSnapshot(
                    player.getUniqueID().toString(), player.getName(), player.dimension,
                    player.posX, player.posY, player.posZ,
                    owned, normal, improved, advanced, balances));
        }
        return snapshots;
    }

    public List<AdminMobSnapshot> mobs() {
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
