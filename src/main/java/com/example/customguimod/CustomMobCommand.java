package com.example.customguimod;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomMobCommand extends AdminOnlyCommand {

    @Override public String getName() { return "custommob"; }
    @Override public String getUsage(ICommandSender sender) { return "/custommob create <name> <resource> <amount> <hp> <type> | setspawn|unspawn|delete|list"; }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, Arrays.asList("create", "setspawn", "unspawn", "delete", "list"));
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("create")) return getListOfStringsMatchingLastWord(args, Arrays.asList("<name>"));
            if (args[0].equalsIgnoreCase("setspawn") || args[0].equalsIgnoreCase("unspawn") || args[0].equalsIgnoreCase("delete")) {
                List<String> names = new ArrayList<>();
                for (MongoManager.CustomMob mob : MongoManager.getAllMobs()) names.add(mob.name);
                return getListOfStringsMatchingLastWord(args, names);
            }
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("create")) {
            if (args.length == 3) return getListOfStringsMatchingLastWord(args, ResourceManager.getResourceNames());
            if (args.length == 4) return getListOfStringsMatchingLastWord(args, Arrays.asList("<amount>"));
            if (args.length == 5) return getListOfStringsMatchingLastWord(args, Arrays.asList("<hp>"));
            if (args.length == 6) return getListOfStringsMatchingLastWord(args, Arrays.asList("zombie", "skeleton"));
        }
        return new ArrayList<>();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) { sender.sendMessage(new TextComponentString("§cOnly for players")); return; }
        EntityPlayerMP player = (EntityPlayerMP) sender;
        if (args.length == 0) { showUsage(player); return; }

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length != 6) { player.sendMessage(new TextComponentString("§c/custommob create <name> <resource> <amount> <hp> <type>")); return; }
            String name = args[1];
            String resource = ResourceManager.findCanonicalName(args[2]);
            if (MongoManager.getMob(name) != null) { player.sendMessage(new TextComponentString("§cМоб с именем " + name + " уже существует!")); return; }
            if (resource == null) { player.sendMessage(new TextComponentString("§cРесурс не найден: " + args[2])); return; }

            int amount;
            BigDecimal hp;
            try {
                amount = Integer.parseInt(args[3]);
                hp = LargeNumberValue.parseExact(args[4]);
            } catch (NumberFormatException e) {
                player.sendMessage(new TextComponentString("§camount должен быть целым числом, hp - положительным числом"));
                return;
            }
            if (amount <= 0 || hp.signum() <= 0) { player.sendMessage(new TextComponentString("§camount и hp должны быть больше 0")); return; }

            String type = args[5].toLowerCase();
            if (!type.equals("zombie") && !type.equals("skeleton")) { player.sendMessage(new TextComponentString("§cТип должен быть zombie или skeleton")); return; }

            MongoManager.createMob(name, type, hp, amount);
            ResourceManager.setMobReward(name, resource, amount);
            player.sendMessage(new TextComponentString("§aМоб создан: " + name + " §7(" + type + ", HP "
                    + LargeNumberFormatter.format(hp) + ", " + ResourceManager.getDisplayName(resource) + " x" + amount + ")"));
        } else if (args[0].equalsIgnoreCase("setspawn")) {
            if (args.length < 2) { player.sendMessage(new TextComponentString("§c/custommob setspawn <name>")); return; }
            String name = args[1];
            if (MongoManager.getMob(name) == null) { player.sendMessage(new TextComponentString("§cМоб не найден: " + name)); return; }
            MongoManager.setMobSpawn(name, player.posX, player.posY, player.posZ);
            player.sendMessage(new TextComponentString("§aТочка спавна для " + name + " установлена"));
            MobManager.respawnAll();
        } else if (args[0].equalsIgnoreCase("unspawn")) {
            if (args.length < 2) { player.sendMessage(new TextComponentString("§c/custommob unspawn <name>")); return; }
            MongoManager.unspawnMob(args[1]);
            player.sendMessage(new TextComponentString("§cМоб " + args[1] + " отключён"));
            MobManager.respawnAll();
        } else if (args[0].equalsIgnoreCase("delete")) {
            if (args.length < 2) { player.sendMessage(new TextComponentString("§c/custommob delete <name>")); return; }
            if (MongoManager.getMob(args[1]) == null) { player.sendMessage(new TextComponentString("§cМоб не найден: " + args[1])); return; }
            MongoManager.deleteMob(args[1]);
            player.sendMessage(new TextComponentString("§cМоб " + args[1] + " удалён"));
            MobManager.respawnAll();
        } else if (args[0].equalsIgnoreCase("list")) {
            List<MongoManager.CustomMob> mobs = MongoManager.getAllMobs();
            player.sendMessage(new TextComponentString("§6Мобы (" + mobs.size() + "):"));
            for (MongoManager.CustomMob mob : mobs) {
                String resource = ResourceManager.getMobResource(mob.name);
                int amount = ResourceManager.getMobAmount(mob.name);
                player.sendMessage(new TextComponentString("§7- " + mob.name + " (" + mob.type + ", HP "
                        + LargeNumberFormatter.format(mob.hp) + ", " + ResourceManager.getDisplayName(resource) + " x" + amount + ") "
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
}
