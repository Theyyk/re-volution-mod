package com.example.revolution.mob.service;

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
    private final ManagedMobService mobs;

    public MobAdminService(MobRepository mobRepository, ManagedMobService mobs) {
        this.mobRepository = mobRepository;
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
            try {
                amount = Integer.parseInt(args[3]);
                hp = LargeNumbers.parseExact(args[4]);
            } catch (NumberFormatException e) {
                player.sendMessage(new TextComponentString("§camount должен быть целым числом, hp - положительным числом"));
                return;
            }
            if (amount <= 0 || hp.signum() <= 0) { player.sendMessage(new TextComponentString("§camount и hp должны быть больше 0")); return; }

            String type = args[5].toLowerCase();
            if (!type.equals("zombie") && !type.equals("skeleton")) { player.sendMessage(new TextComponentString("§cТип должен быть zombie или skeleton")); return; }

            mobRepository.createMob(name, type, hp, amount);
            mobRepository.setMobReward(name, resource, amount);
            player.sendMessage(new TextComponentString("§aМоб создан: " + name + " §7(" + type + ", HP "
                    + LargeNumbers.format(hp) + ", " + ResourceDefinitions.getDisplayName(resource) + " x" + amount + ")"));
        } else if (args[0].equalsIgnoreCase("setspawn")) {
            if (args.length < 2) { player.sendMessage(new TextComponentString("§c/custommob setspawn <name>")); return; }
            String name = args[1];
            if (mobRepository.getMob(name) == null) { player.sendMessage(new TextComponentString("§cМоб не найден: " + name)); return; }
            mobRepository.setMobSpawn(name, player.posX, player.posY, player.posZ);
            player.sendMessage(new TextComponentString("§aТочка спавна для " + name + " установлена"));
            mobs.respawnAll();
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
            List<MobDefinition> mobs = mobRepository.getAllMobs();
            player.sendMessage(new TextComponentString("§6Мобы (" + mobs.size() + "):"));
            for (MobDefinition mob : mobs) {
                String resource = mobRepository.getMobResource(mob.name);
                int amount = mobRepository.getMobAmount(mob.name);
                player.sendMessage(new TextComponentString("§7- " + mob.name + " (" + mob.type + ", HP "
                        + LargeNumbers.format(mob.hp) + ", " + ResourceDefinitions.getDisplayName(resource) + " x" + amount + ") "
                        + (mob.enabled ? "§aвкл" : "§cвыкл")));
            }
        } else showUsage(player);
        }
    private void showUsage(EntityPlayerMP player) {
        player.sendMessage(new TextComponentString("§e/custommob create <name> <resource> <amount> <hp> <type>"));
        player.sendMessage(new TextComponentString("§e/custommob setspawn <name>"));
        player.sendMessage(new TextComponentString("§e/custommob unspawn <name>"));
        player.sendMessage(new TextComponentString("§e/custommob delete <name>"));
        player.sendMessage(new TextComponentString("§e/custommob list"));
    }

    public List<MobDefinition> definitions() { return mobRepository.getAllMobs(); }
}
