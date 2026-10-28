package com.example.revolution.mob.service;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.LocationKind;
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
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;

public final class MobAdminService {
    private static final int MAX_POPULATE_COUNT = 500;
    private static final int MAX_SPAWN_RISE = 3;
    private static final int SAFE_POINT_RING_COUNT = 4;
    private static final int SAFE_POINT_ANGLES = 12;

    private final MobRepository mobRepository;
    private final LocationRepository locations;
    private final ManagedMobService mobs;

    public MobAdminService(MobRepository mobRepository, LocationRepository locations, ManagedMobService mobs) {
        this.mobRepository = mobRepository;
        this.locations = locations;
        this.mobs = mobs;
    }

    public void execute(ICommandSender sender, String[] args) {
        mobs.requireConfigurationReady();
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
        if (args.length == 4 && args[1].equalsIgnoreCase("setboss")) {
            setBossZone(player, args);
            return;
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("clearboss")) {
            clearBossZone(player, args);
            return;
        }
        if (args.length == 6 && args[1].equalsIgnoreCase("populate")) {
            populateMobZone(player, args);
            return;
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("clear")) {
            clearMobZone(player, args);
            return;
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("remove")) {
            removeMobZone(player, args);
            return;
        }
        player.sendMessage(new TextComponentString("§c/custommob location set <locationId> <mobSlotId> <radius>"));
        player.sendMessage(new TextComponentString("§c/custommob location setboss <locationId> <mobSlotId>"));
        player.sendMessage(new TextComponentString("§c/custommob location clearboss <locationId> <mobSlotId>"));
        player.sendMessage(new TextComponentString("§c/custommob location populate <locationId> <mobSlotId> <mobName> <count>"));
        player.sendMessage(new TextComponentString("§c/custommob location clear <locationId> <mobSlotId>"));
        player.sendMessage(new TextComponentString("§c/custommob location remove <locationId> <mobSlotId>"));
        player.sendMessage(new TextComponentString("§c/custommob location here | location list <locationId>"));
    }

    private void setMobZone(EntityPlayerMP player, String[] args) {
        try {
            int worldLocationId = Integer.parseInt(args[2]);
            int mobSlotId = Integer.parseInt(args[3]);
            double radius = Double.parseDouble(args[4]);
            int internalId = MobZoneIds.encode(worldLocationId, mobSlotId);
            ensureNoStageAfterBoss(worldLocationId, mobSlotId);
            LocationDefinition existing = locations.getLocation(internalId);
            LocationKind kind = existing != null && existing.isBossZone() ? LocationKind.BOSS_ZONE : LocationKind.MOB_ZONE;
            LocationDefinition zone = new LocationDefinition(internalId,
                    (kind == LocationKind.BOSS_ZONE ? "BossZone " : "MobZone ") + worldLocationId + ":" + mobSlotId,
                    player.posX, player.posZ, radius, kind);

            int cleared = clearZonePopulation(internalId);
            locations.upsertLocation(zone);
            player.sendMessage(new TextComponentString("§a" + (zone.isBossZone() ? "Boss" : "Mob") + " zone сохранена: location=" + worldLocationId
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

    private void setBossZone(EntityPlayerMP player, String[] args) {
        try {
            int worldLocationId = Integer.parseInt(args[2]);
            int mobSlotId = Integer.parseInt(args[3]);
            int internalId = MobZoneIds.encode(worldLocationId, mobSlotId);
            LocationDefinition target = locations.getLocation(internalId);
            if (target == null) throw new IllegalArgumentException("Mob zone не найдена");
            if (!isFinalConfiguredZone(worldLocationId, mobSlotId)) {
                throw new IllegalArgumentException("Boss Zone должна быть последним configured mobSlot Location");
            }
            for (LocationDefinition zone : zonesForWorldLocation(worldLocationId)) {
                if (zone.isBossZone() && zone.getLocationId() != internalId) {
                    throw new IllegalArgumentException("В Location уже настроена другая Boss Zone: slot="
                            + MobZoneIds.mobSlotId(zone.getLocationId()));
                }
            }
            if (!target.isBossZone()) locations.upsertLocation(withKind(target, LocationKind.BOSS_ZONE));
            player.sendMessage(new TextComponentString("§aBoss Zone назначена: location=" + worldLocationId + " slot=" + mobSlotId));
            mobs.respawnAll();
        } catch (RuntimeException e) {
            player.sendMessage(new TextComponentString("§cНе удалось назначить Boss Zone: " + e.getMessage()));
        }
    }

    private void clearBossZone(EntityPlayerMP player, String[] args) {
        try {
            int worldLocationId = Integer.parseInt(args[2]);
            int mobSlotId = Integer.parseInt(args[3]);
            int internalId = MobZoneIds.encode(worldLocationId, mobSlotId);
            LocationDefinition target = locations.getLocation(internalId);
            if (target == null) throw new IllegalArgumentException("Mob zone не найдена");
            if (!target.isBossZone()) throw new IllegalArgumentException("Указанная зона не является Boss Zone");
            locations.upsertLocation(withKind(target, LocationKind.MOB_ZONE));
            player.sendMessage(new TextComponentString("§aBoss Zone снята: location=" + worldLocationId + " slot=" + mobSlotId));
            mobs.respawnAll();
        } catch (RuntimeException e) {
            player.sendMessage(new TextComponentString("§cНе удалось снять Boss Zone: " + e.getMessage()));
        }
    }

    private LocationDefinition withKind(LocationDefinition source, LocationKind kind) {
        String name = (kind == LocationKind.BOSS_ZONE ? "BossZone " : "MobZone ")
                + MobZoneIds.worldLocationId(source.getLocationId()) + ":" + MobZoneIds.mobSlotId(source.getLocationId());
        if (source.hasTransitionPoint()) {
            return new LocationDefinition(source.getLocationId(), name, source.getCenterX(), source.getCenterZ(), source.getRadius(), kind,
                    source.getTransitionX(), source.getTransitionY(), source.getTransitionZ(), source.getTransitionYaw(), source.getTransitionPitch());
        }
        return new LocationDefinition(source.getLocationId(), name, source.getCenterX(), source.getCenterZ(), source.getRadius(), kind);
    }

    private void ensureNoStageAfterBoss(int worldLocationId, int proposedSlot) {
        for (LocationDefinition zone : zonesForWorldLocation(worldLocationId)) {
            if (!zone.isBossZone()) continue;
            int bossSlot = MobZoneIds.mobSlotId(zone.getLocationId());
            if (proposedSlot > bossSlot) throw new IllegalArgumentException("Нельзя создать stage после Boss Zone slot=" + bossSlot);
        }
    }

    private boolean isFinalConfiguredZone(int worldLocationId, int mobSlotId) {
        int maxSlot = -1;
        for (LocationDefinition zone : zonesForWorldLocation(worldLocationId)) {
            maxSlot = Math.max(maxSlot, MobZoneIds.mobSlotId(zone.getLocationId()));
        }
        return maxSlot == mobSlotId;
    }

    private List<LocationDefinition> zonesForWorldLocation(int worldLocationId) {
        List<LocationDefinition> result = new ArrayList<>();
        for (LocationDefinition zone : locations.getLocations()) {
            if (!MobZoneIds.isMobZoneId(zone.getLocationId())) continue;
            if (MobZoneIds.worldLocationId(zone.getLocationId()) == worldLocationId) result.add(zone);
        }
        return result;
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

            int fixedCount = MobZoneLayout.fixedCount(count);
            int randomCount = MobZoneLayout.randomCount(count);
            double baseY = player.posY;
            List<StoredMobSpawn> generated = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                boolean fixed = i < fixedCount;
                double preferredX;
                double preferredZ;
                if (fixed) {
                    MobZoneLayout.HorizontalPoint point = MobZoneLayout.fixedPoint(i, fixedCount,
                            zone.getCenterX(), zone.getCenterZ(), zone.getRadius());
                    preferredX = point.getX();
                    preferredZ = point.getZ();
                } else {
                    preferredX = zone.getCenterX();
                    preferredZ = zone.getCenterZ();
                }
                double[] safe = findGeneratedSafePoint(player, zone, preferredX, preferredZ, baseY);
                if (safe == null) {
                    throw new IllegalArgumentException("no safe surface for generated spawn slot " + (i + 1)
                            + " (tree/high surfaces are rejected)");
                }
                StoredMobSpawn spawn = new StoredMobSpawn(
                        internalId,
                        i + 1,
                        mobName,
                        safe[0],
                        safe[1],
                        safe[2],
                        0.0F,
                        0.0F,
                        fixed ? SpawnMode.FIXED_POINT : SpawnMode.RANDOM_AREA,
                        MovementMode.ROAMING,
                        SpawnRealmScope.PLAYER_REALMS,
                        true);
                generated.add(spawn);
            }

            clearZonePopulation(internalId);
            for (StoredMobSpawn spawn : generated) locations.upsertMobSpawn(spawn);

            player.sendMessage(new TextComponentString("§a" + (zone.isBossZone() ? "Boss" : "Mob") + " zone заполнена: location=" + worldLocationId + " slot=" + mobSlotId
                    + " mob=" + mobName + " count=" + count + " fixed=" + fixedCount + " random=" + randomCount));
            player.sendMessage(new TextComponentString("§7Все мобы: ROAMING. Для нечётного count лишний моб относится к FIXED_POINT."));
            player.sendMessage(new TextComponentString("§7Spawn surface: не выше baseY + " + MAX_SPAWN_RISE + ", листья и брёвна исключены."));
            mobs.respawnAll();
        } catch (RuntimeException e) {
            player.sendMessage(new TextComponentString("§cНе удалось заполнить mob zone: " + e.getMessage()));
        }
    }

    private void clearMobZone(EntityPlayerMP player, String[] args) {
        try {
            int worldLocationId = Integer.parseInt(args[2]);
            int mobSlotId = Integer.parseInt(args[3]);
            int internalId = MobZoneIds.encode(worldLocationId, mobSlotId);
            LocationDefinition zone = locations.getLocation(internalId);
            if (zone == null) {
                player.sendMessage(new TextComponentString("§cMob zone не найдена: location=" + worldLocationId + " slot=" + mobSlotId));
                return;
            }
            int removedSpawns = clearZonePopulation(internalId);
            player.sendMessage(new TextComponentString("§a" + (zone.isBossZone() ? "Boss" : "Mob") + " zone очищена: location=" + worldLocationId + " slot=" + mobSlotId
                    + " removedSpawns=" + removedSpawns + ". Геометрия и тип зоны сохранены."));
            mobs.respawnAll();
        } catch (RuntimeException e) {
            player.sendMessage(new TextComponentString("§cНе удалось очистить mob zone: " + e.getMessage()));
        }
    }

    private void removeMobZone(EntityPlayerMP player, String[] args) {
        try {
            int worldLocationId = Integer.parseInt(args[2]);
            int mobSlotId = Integer.parseInt(args[3]);
            int internalId = MobZoneIds.encode(worldLocationId, mobSlotId);
            LocationDefinition zone = locations.getLocation(internalId);
            List<StoredMobSpawn> existing = locations.getMobSpawns(internalId);
            if (zone == null && existing.isEmpty()) {
                player.sendMessage(new TextComponentString("§cMob zone не найдена: location=" + worldLocationId + " slot=" + mobSlotId));
                return;
            }
            int removedSpawns = clearZonePopulation(internalId);
            locations.deleteLocation(internalId);
            player.sendMessage(new TextComponentString("§aMob zone удалена: location=" + worldLocationId + " slot=" + mobSlotId
                    + " removedSpawns=" + removedSpawns));
            mobs.respawnAll();
        } catch (RuntimeException e) {
            player.sendMessage(new TextComponentString("§cНе удалось удалить mob zone: " + e.getMessage()));
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
                + " type=" + (zone.isBossZone() ? "BOSS" : "NORMAL")
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

    private double[] findGeneratedSafePoint(
            final EntityPlayerMP player,
            LocationDefinition zone,
            double preferredX,
            double preferredZ,
            final double baseY) {
        Double y = surfaceY(player, preferredX, preferredZ, baseY);
        if (y != null) return new double[] {preferredX, y, preferredZ};
        for (int ring = 1; ring <= SAFE_POINT_RING_COUNT; ring++) {
            double radius = ring;
            for (int index = 0; index < SAFE_POINT_ANGLES; index++) {
                double angle = (Math.PI * 2.0D * index / SAFE_POINT_ANGLES) + ring * 0.37D;
                double x = preferredX + Math.cos(angle) * radius;
                double z = preferredZ + Math.sin(angle) * radius;
                if (!zone.contains(x, z)) continue;
                y = surfaceY(player, x, z, baseY);
                if (y != null) return new double[] {x, y, z};
            }
        }
        return MobZoneSurfaceSearch.findNearestBlockCenter(zone, preferredX, preferredZ,
                new MobZoneSurfaceSearch.SurfaceProbe() {
                    @Override
                    public Double surfaceY(double x, double z) {
                        return MobAdminService.this.surfaceY(player, x, z, baseY);
                    }
                });
    }

    private Double surfaceY(EntityPlayerMP player, double x, double z, double baseY) {
        int centerY = MathHelper.clamp(MathHelper.floor(baseY), 2, Math.max(2, player.world.getActualHeight() - 2));
        for (int offset = 0; offset <= MAX_SPAWN_RISE; offset++) {
            int up = centerY + offset;
            if (up < player.world.getActualHeight() - 1 && isSafeStandingSurface(player, x, up, z)) return (double) up;
            if (offset == 0) continue;
            int down = centerY - offset;
            if (down >= 2 && isSafeStandingSurface(player, x, down, z)) return (double) down;
        }
        for (int y = centerY - MAX_SPAWN_RISE - 1; y >= 2; y--) {
            if (isSafeStandingSurface(player, x, y, z)) return (double) y;
        }
        return null;
    }

    private boolean isSafeStandingSurface(EntityPlayerMP player, double x, int y, double z) {
        int blockX = MathHelper.floor(x);
        int blockZ = MathHelper.floor(z);
        BlockPos ground = new BlockPos(blockX, y - 1, blockZ);
        BlockPos feet = new BlockPos(blockX, y, blockZ);
        BlockPos head = new BlockPos(blockX, y + 1, blockZ);
        IBlockState state = player.world.getBlockState(ground);
        if (state.getBlock() instanceof BlockLeaves || state.getBlock() instanceof BlockLog) return false;
        return state.getMaterial().blocksMovement() && !state.getMaterial().isLiquid()
                && player.world.isAirBlock(feet) && player.world.isAirBlock(head);
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
        player.sendMessage(new TextComponentString("§e/custommob location setboss <locationId> <mobSlotId> | clearboss <locationId> <mobSlotId>"));
        player.sendMessage(new TextComponentString("§e/custommob location populate <locationId> <mobSlotId> <mobName> <count>"));
        player.sendMessage(new TextComponentString("§e/custommob location clear <locationId> <mobSlotId>"));
        player.sendMessage(new TextComponentString("§e/custommob location remove <locationId> <mobSlotId>"));
        player.sendMessage(new TextComponentString("§e/custommob location here | location list <locationId>"));
        player.sendMessage(new TextComponentString("§e/custommob spawn set <locationId> <spawnId> <mobName> <fixed|random> <stationary|roaming> <player|realm0>"));
        player.sendMessage(new TextComponentString("§e/custommob spawn list <locationId> | spawn remove <locationId> <spawnId>"));
        player.sendMessage(new TextComponentString("§e/custommob setspawn <name> | unspawn <name> | delete <name> | list"));
    }

    public List<MobDefinition> definitions() { return mobRepository.getAllMobs(); }
}
