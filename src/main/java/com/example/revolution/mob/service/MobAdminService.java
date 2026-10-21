package com.example.revolution.mob.service;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.definition.MovementMode;
import com.example.revolution.location.definition.SpawnMode;
import com.example.revolution.location.definition.SpawnRealmScope;
import com.example.revolution.location.definition.StoredMobSpawn;
import com.example.revolution.location.repository.LocationRepository;
import com.example.revolution.location.service.MobZoneLayout;
import com.example.revolution.mob.definition.MobDefinition;
import com.example.revolution.mob.repository.MobRepository;
import com.example.revolution.number.LargeNumbers;
import com.example.revolution.resource.definition.ResourceDefinitions;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;

public final class MobAdminService {
    private static final int MAX_POPULATE_COUNT = 500;

    private final MobRepository mobRepository;
    private final LocationRepository locations;
    private final ManagedMobService mobs;

    public MobAdminService(MobRepository mobRepository, LocationRepository locations, ManagedMobService mobs) {
        this.mobRepository = mobRepository;
        this.locations = locations;
        this.mobs = mobs;
    }

    public void execute(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayerMP)) { sender.sendMessage(new TextComponentString("§cOnly for players")); return; }
        EntityPlayerMP player = (EntityPlayerMP) sender;
        if (args.length == 0) { showUsage(player); return; }

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length != 6) { player.sendMessage(new TextComponentString("§c/custommob create <name> <resource> <amount> <hp> <type>")); return; }
            String name = args[1];
            String resource = ResourceDefinitions.findCanonicalName(args[2]);
            if (mobRepository.getMob(name) != null) { player.sendMessage(new TextComponentString("§cМоб с именем " + name + " уже существует!")); return; }
            if (resource == null) { player.sendMessage(new TextComponentString("§cРесурс не найден: " + args[2])); return; }
            int amount;
            BigDecimal hp;
            try { amount = Integer.parseInt(args[3]); hp = LargeNumbers.parseExact(args[4]); }
            catch (NumberFormatException e) { player.sendMessage(new TextComponentString("§camount должен быть целым числом, hp - положительным числом")); return; }
            if (amount <= 0 || hp.signum() <= 0) { player.sendMessage(new TextComponentString("§camount и hp должны быть больше 0")); return; }
            String type = args[5].toLowerCase();
            if (!type.equals("zombie") && !type.equals("skeleton")) { player.sendMessage(new TextComponentString("§cТип должен быть zombie или skeleton")); return; }
            mobRepository.createMob(name, type, hp, amount);
            mobRepository.setMobReward(name, resource, amount);
            player.sendMessage(new TextComponentString("§aМоб создан: " + name));
        } else if (args[0].equalsIgnoreCase("setspawn")) {
            if (args.length < 2) { player.sendMessage(new TextComponentString("§c/custommob setspawn <name>")); return; }
            if (mobRepository.getMob(args[1]) == null) { player.sendMessage(new TextComponentString("§cМоб не найден: " + args[1])); return; }
            mobRepository.setMobSpawn(args[1], player.posX, player.posY, player.posZ);
            player.sendMessage(new TextComponentString("§aLegacy Realm 0 точка спавна установлена"));
            mobs.respawnAll();
        } else if (args[0].equalsIgnoreCase("location")) {
            handleLocation(player, args);
        } else if (args[0].equalsIgnoreCase("spawn")) {
            handleSpawn(player, args);
        } else if (args[0].equalsIgnoreCase("unspawn")) {
            if (args.length < 2) { player.sendMessage(new TextComponentString("§c/custommob unspawn <name>")); return; }
            mobRepository.unspawnMob(args[1]);
            player.sendMessage(new TextComponentString("§cМоб " + args[1] + " отключён"));
            mobs.respawnAll();
        } else if (args[0].equalsIgnoreCase("delete")) {
            if (args.length < 2) { player.sendMessage(new TextComponentString("§c/custommob delete <name>")); return; }
            if (mobRepository.getMob(args[1]) == null) { player.sendMessage(new TextComponentString("§cМоб не найден: " + args[1])); return; }
            mobRepository.deleteMob(args[1]);
            player.sendMessage(new TextComponentString("§cМоб " + args[1] + " удалён"));
            mobs.respawnAll();
        } else if (args[0].equalsIgnoreCase("list")) {
            List<MobDefinition> definitions = mobRepository.getAllMobs();
            player.sendMessage(new TextComponentString("§6Мобы (" + definitions.size() + "):"));
            for (MobDefinition mob : definitions) player.sendMessage(new TextComponentString("§7- " + mob.name + " (" + mob.type + ")"));
        } else showUsage(player);
    }

    private void handleLocation(EntityPlayerMP player, String[] args) {
        if (args.length == 2 && args[1].equalsIgnoreCase("here")) {
            showZonesHere(player);
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
            showZoneList(player, args);
            return;
        }
        if (args.length == 5 && args[1].equalsIgnoreCase("set")) {
            setMobZone(player, args);
            return;
        }
        if (args.length == 6 && args[1].equalsIgnoreCase("populate")) {
            populateMobZone(player, args);
            return;
        }
        player.sendMessage(new TextComponentString("§c/custommob location set <locationId> <mobSlotId> <radius>"));
        player.sendMessage(new TextComponentString("§c/custommob location populate <locationId> <mobSlotId> <mobName> <count>"));
        player.sendMessage(new TextComponentString("§c/custommob location here | location list <locationId>"));
    }

    private void setMobZone(EntityPlayerMP player, String[] args) {
        try {
            int worldLocationId = Integer.parseInt(args[2]);
            int mobSlotId = Integer.parseInt(args[3]);
            double radius = Double.parseDouble(args[4]);
            int internalId = MobZoneIds.encode(worldLocationId, mobSlotId);
            LocationDefinition zone = new LocationDefinition(internalId, "MobZone " + worldLocationId + ":" + mobSlotId,
                    player.posX, player.posZ, radius);

            int cleared = clearZonePopulation(internalId);
            locations.upsertLocation(zone);
            player.sendMessage(new TextComponentString("§aMob zone сохранена: location=" + worldLocationId
                    + " slot=" + mobSlotId + " center=" + format(player.posX) + "," + format(player.posZ)
                    + " radius=" + format(radius)));
            if (cleared > 0) {
                player.sendMessage(new TextComponentString("§eПредыдущее заполнение зоны очищено (" + cleared
                        + "). Выполни location populate заново."));
            }
            mobs.respawnAll();
        } catch (RuntimeException e) {
            player.sendMessage(new TextComponentString("§cНекорректные параметры mob zone: " + e.getMessage()));
        }
    }

    private void populateMobZone(EntityPlayerMP player, String[] args) {
        try {
            int worldLocationId = Integer.parseInt(args[2]);
            int mobSlotId = Integer.parseInt(args[3]);
            String mobName = args[4];
            int count = Integer.parseInt(args[5]);
            if (count <= 0 || count > MAX_POPULATE_COUNT) {
                throw new IllegalArgumentException("count must be between 1 and " + MAX_POPULATE_COUNT);
            }
            if (mobRepository.getMob(mobName) == null) {
                player.sendMessage(new TextComponentString("§cМоб не найден: " + mobName));
                return;
            }

            int internalId = MobZoneIds.encode(worldLocationId, mobSlotId);
            LocationDefinition zone = locations.getLocation(internalId);
            if (zone == null) {
                player.sendMessage(new TextComponentString("§cMob zone не найдена: location=" + worldLocationId + " slot=" + mobSlotId));
                return;
            }

            clearZonePopulation(internalId);
            int fixedCount = MobZoneLayout.fixedCount(count);
            int randomCount = MobZoneLayout.randomCount(count);
            for (int i = 0; i < count; i++) {
                boolean fixed = i < fixedCount;
                double x;
                double z;
                if (fixed) {
                    MobZoneLayout.HorizontalPoint point = MobZoneLayout.fixedPoint(i, fixedCount,
                            zone.getCenterX(), zone.getCenterZ(), zone.getRadius());
                    x = point.getX();
                    z = point.getZ();
                } else {
                    x = zone.getCenterX();
                    z = zone.getCenterZ();
                }
                double y = surfaceY(player, x, z);
                StoredMobSpawn spawn = new StoredMobSpawn(
                        internalId,
                        i + 1,
                        mobName,
                        x,
                        y,
                        z,
                        0.0F,
                        0.0F,
                        fixed ? SpawnMode.FIXED_POINT : SpawnMode.RANDOM_AREA,
                        MovementMode.ROAMING,
                        SpawnRealmScope.PLAYER_REALMS,
                        true);
                locations.upsertMobSpawn(spawn);
            }

            player.sendMessage(new TextComponentString("§aMob zone заполнена: location=" + worldLocationId + " slot=" + mobSlotId
                    + " mob=" + mobName + " count=" + count + " fixed=" + fixedCount + " random=" + randomCount));
            player.sendMessage(new TextComponentString("§7Все обычные мобы: ROAMING. Для нечётного count лишний моб относится к FIXED_POINT."));
            mobs.respawnAll();
        } catch (RuntimeException e) {
            player.sendMessage(new TextComponentString("§cНе удалось заполнить mob zone: " + e.getMessage()));
        }
    }

    private void showZonesHere(EntityPlayerMP player) {
        List<LocationDefinition> matches = new ArrayList<>();
        for (LocationDefinition location : locations.getLocations()) {
            if (!MobZoneIds.isMobZoneId(location.getLocationId())) continue;
            if (location.contains(player.posX, player.posZ)) matches.add(location);
        }
        matches.sort(zoneComparator());
        if (matches.isEmpty()) {
            player.sendMessage(new TextComponentString("§7Ты не находишься ни в одной настроенной mob zone."));
            return;
        }
        player.sendMessage(new TextComponentString("§6Mob zones в текущей точке (" + matches.size() + "):"));
        for (LocationDefinition zone : matches) showZone(player, zone);
    }

    private void showZoneList(EntityPlayerMP player, String[] args) {
        Integer filterLocation = null;
        if (args.length >= 3) {
            try { filterLocation = Integer.parseInt(args[2]); }
            catch (NumberFormatException e) { player.sendMessage(new TextComponentString("§clocationId должен быть числом")); return; }
        }

        List<LocationDefinition> zones = new ArrayList<>();
        for (LocationDefinition location : locations.getLocations()) {
            if (!MobZoneIds.isMobZoneId(location.getLocationId())) continue;
            if (filterLocation != null && MobZoneIds.worldLocationId(location.getLocationId()) != filterLocation) continue;
            zones.add(location);
        }
        zones.sort(zoneComparator());
        if (zones.isEmpty()) {
            player.sendMessage(new TextComponentString("§7Mob zones не найдены" + (filterLocation == null ? "." : " для Location " + filterLocation + ".")));
            return;
        }
        for (LocationDefinition zone : zones) showZone(player, zone);
    }

    private void showZone(EntityPlayerMP player, LocationDefinition zone) {
        int internalId = zone.getLocationId();
        int worldLocationId = MobZoneIds.worldLocationId(internalId);
        int mobSlotId = MobZoneIds.mobSlotId(internalId);
        List<StoredMobSpawn> spawns = locations.getMobSpawns(internalId);
        String mobName = spawns.isEmpty() ? "-" : spawns.get(0).getMobName();
        int fixed = 0;
        int random = 0;
        for (StoredMobSpawn spawn : spawns) {
            if (spawn.getSpawnMode() == SpawnMode.FIXED_POINT) fixed++;
            else if (spawn.getSpawnMode() == SpawnMode.RANDOM_AREA) random++;
        }
        player.sendMessage(new TextComponentString("§7Location " + worldLocationId + " / Mob Slot " + mobSlotId
                + " radius=" + format(zone.getRadius()) + " center=" + format(zone.getCenterX()) + "," + format(zone.getCenterZ())
                + " mob=" + mobName + " count=" + spawns.size() + " fixed=" + fixed + " random=" + random));
    }

    private Comparator<LocationDefinition> zoneComparator() {
        return Comparator.comparingInt((LocationDefinition zone) -> MobZoneIds.worldLocationId(zone.getLocationId()))
                .thenComparingInt(zone -> MobZoneIds.mobSlotId(zone.getLocationId()));
    }

    private int clearZonePopulation(int internalLocationId) {
        List<StoredMobSpawn> existing = new ArrayList<>(locations.getMobSpawns(internalLocationId));
        for (StoredMobSpawn spawn : existing) locations.deleteMobSpawn(internalLocationId, spawn.getSpawnId());
        return existing.size();
    }

    private double surfaceY(EntityPlayerMP player, double x, double z) {
        BlockPos height = player.world.getHeight(new BlockPos(MathHelper.floor(x), 0, MathHelper.floor(z)));
        int y = height.getY();
        if (y <= 1 || y >= player.world.getActualHeight() - 1) return player.posY;
        return y;
    }

    private void handleSpawn(EntityPlayerMP player, String[] args) {
        if (args.length >= 3 && args[1].equalsIgnoreCase("list")) {
            try {
                int locationId = Integer.parseInt(args[2]);
                for (StoredMobSpawn spawn : locations.getMobSpawns(locationId)) {
                    player.sendMessage(new TextComponentString("§7Spawn " + spawn.getSpawnId() + " mob=" + spawn.getMobName()
                            + " " + spawn.getSpawnMode() + "/" + spawn.getMovementMode() + " scope=" + spawn.getRealmScope()));
                }
            } catch (NumberFormatException e) { player.sendMessage(new TextComponentString("§clocationId должен быть числом")); }
            return;
        }
        if (args.length >= 4 && args[1].equalsIgnoreCase("remove")) {
            try {
                int locationId = Integer.parseInt(args[2]);
                int spawnId = Integer.parseInt(args[3]);
                locations.deleteMobSpawn(locationId, spawnId);
                player.sendMessage(new TextComponentString("§aMob Spawn удалён"));
                mobs.respawnAll();
            } catch (NumberFormatException e) { player.sendMessage(new TextComponentString("§cIDs должны быть числами")); }
            return;
        }
        if (args.length != 8 || !args[1].equalsIgnoreCase("set")) {
            player.sendMessage(new TextComponentString("§c/custommob spawn set <locationId> <spawnId> <mobName> <fixed|random> <stationary|roaming> <player|realm0>"));
            return;
        }
        try {
            int locationId = Integer.parseInt(args[2]);
            int spawnId = Integer.parseInt(args[3]);
            String mobName = args[4];
            LocationDefinition location = locations.getLocation(locationId);
            if (location == null) { player.sendMessage(new TextComponentString("§cLocation не найдена: " + locationId)); return; }
            if (mobRepository.getMob(mobName) == null) { player.sendMessage(new TextComponentString("§cМоб не найден: " + mobName)); return; }
            if (!location.contains(player.posX, player.posZ)) { player.sendMessage(new TextComponentString("§cТочка игрока должна быть внутри Location Area")); return; }
            SpawnMode spawnMode = args[5].equalsIgnoreCase("random") ? SpawnMode.RANDOM_AREA : SpawnMode.FIXED_POINT;
            if (!args[5].equalsIgnoreCase("random") && !args[5].equalsIgnoreCase("fixed")) throw new IllegalArgumentException("spawn mode");
            MovementMode movement = args[6].equalsIgnoreCase("roaming") ? MovementMode.ROAMING : MovementMode.STATIONARY;
            if (!args[6].equalsIgnoreCase("roaming") && !args[6].equalsIgnoreCase("stationary")) throw new IllegalArgumentException("movement mode");
            SpawnRealmScope scope = args[7].equalsIgnoreCase("realm0") ? SpawnRealmScope.REALM_0 : SpawnRealmScope.PLAYER_REALMS;
            if (!args[7].equalsIgnoreCase("realm0") && !args[7].equalsIgnoreCase("player")) throw new IllegalArgumentException("realm scope");
            StoredMobSpawn spawn = new StoredMobSpawn(locationId, spawnId, mobName, player.posX, player.posY, player.posZ,
                    player.rotationYaw, player.rotationPitch, spawnMode, movement, scope, true);
            locations.upsertMobSpawn(spawn);
            player.sendMessage(new TextComponentString("§aMob Spawn " + spawnId + " сохранён: " + spawnMode + "/" + movement + " " + scope));
            mobs.respawnAll();
        } catch (RuntimeException e) {
            player.sendMessage(new TextComponentString("§cНекорректные параметры Mob Spawn: " + e.getMessage()));
        }
    }

    private String format(double value) { return String.format(java.util.Locale.ROOT, "%.2f", value); }

    private void showUsage(EntityPlayerMP player) {
        player.sendMessage(new TextComponentString("§e/custommob create <name> <resource> <amount> <hp> <type>"));
        player.sendMessage(new TextComponentString("§e/custommob location set <locationId> <mobSlotId> <radius>"));
        player.sendMessage(new TextComponentString("§e/custommob location populate <locationId> <mobSlotId> <mobName> <count>"));
        player.sendMessage(new TextComponentString("§e/custommob location here | location list <locationId>"));
        player.sendMessage(new TextComponentString("§e/custommob spawn set <locationId> <spawnId> <mobName> <fixed|random> <stationary|roaming> <player|realm0>"));
        player.sendMessage(new TextComponentString("§e/custommob spawn list <locationId> | spawn remove <locationId> <spawnId>"));
        player.sendMessage(new TextComponentString("§e/custommob setspawn <name> | unspawn <name> | delete <name> | list"));
    }

    public List<MobDefinition> definitions() { return mobRepository.getAllMobs(); }
}
