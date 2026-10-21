package com.example.revolution.mob.service;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.MovementMode;
import com.example.revolution.location.definition.SpawnMode;
import com.example.revolution.location.definition.SpawnRealmScope;
import com.example.revolution.location.definition.StoredMobSpawn;
import com.example.revolution.location.repository.LocationRepository;
import com.example.revolution.mob.definition.MobDefinition;
import com.example.revolution.mob.repository.MobRepository;
import com.example.revolution.number.LargeNumbers;
import com.example.revolution.resource.definition.ResourceDefinitions;
import java.math.BigDecimal;
import java.util.List;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;

public final class MobAdminService {
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
        if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
            for (LocationDefinition location : locations.getLocations()) {
                player.sendMessage(new TextComponentString("§7Location " + location.getLocationId() + " " + location.getName()
                        + " center=" + format(location.getCenterX()) + "," + format(location.getCenterZ()) + " radius=" + format(location.getRadius())));
            }
            return;
        }
        if (args.length < 4 || !args[1].equalsIgnoreCase("set")) {
            player.sendMessage(new TextComponentString("§c/custommob location set <locationId> <radius> [name]"));
            return;
        }
        try {
            int locationId = Integer.parseInt(args[2]);
            double radius = Double.parseDouble(args[3]);
            String name = args.length >= 5 ? args[4] : "Location " + locationId;
            LocationDefinition location = new LocationDefinition(locationId, name, player.posX, player.posZ, radius);
            locations.upsertLocation(location);
            player.sendMessage(new TextComponentString("§aLocation " + locationId + " сохранена: center="
                    + format(player.posX) + "," + format(player.posZ) + " radius=" + format(radius)));
            mobs.respawnAll();
        } catch (RuntimeException e) {
            player.sendMessage(new TextComponentString("§cНекорректные параметры Location: " + e.getMessage()));
        }
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
        player.sendMessage(new TextComponentString("§e/custommob location set <locationId> <radius> [name] | location list"));
        player.sendMessage(new TextComponentString("§e/custommob spawn set <locationId> <spawnId> <mobName> <fixed|random> <stationary|roaming> <player|realm0>"));
        player.sendMessage(new TextComponentString("§e/custommob spawn list <locationId> | spawn remove <locationId> <spawnId>"));
        player.sendMessage(new TextComponentString("§e/custommob setspawn <name> | unspawn <name> | delete <name> | list"));
    }

    public List<MobDefinition> definitions() { return mobRepository.getAllMobs(); }
}
