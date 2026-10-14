package com.example.customguimod;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomResourceCommand extends AdminOnlyCommand {

    @Override
    public String getName() {
        return "customresource";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/customresource list|balance";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("list", "balance"));
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("balance")) {
            return getListOfStringsMatchingLastWord(args, ResourceManager.getResourceNames());
        }

        return new ArrayList<>();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            sender.sendMessage(new TextComponentString("§cOnly for players"));
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;

        if (args.length == 0) {
            showUsage(player);
            return;
        }

        if (args[0].equalsIgnoreCase("list")) {
            List<String> resources = ResourceManager.getResourceNames();
            player.sendMessage(new TextComponentString("§6Ресурсы (" + resources.size() + "): §e" + String.join("§7, §e", resources)));
            return;
        }

        if (args[0].equalsIgnoreCase("balance")) {
            if (args.length != 2) {
                player.sendMessage(new TextComponentString("§c/customresource balance <resource>"));
                return;
            }

            String canonical = ResourceManager.findCanonicalName(args[1]);
            if (canonical == null) {
                player.sendMessage(new TextComponentString("§cРесурс не найден: " + args[1]));
                return;
            }

            int value = ResourceManager.getPlayerResource(player.getUniqueID(), canonical);
            player.sendMessage(new TextComponentString("§6" + ResourceManager.getDisplayName(canonical) + ": §e" + value));
            return;
        }

        player.sendMessage(new TextComponentString("§cСоздание и удаление ресурсов отключено: каталог ресурсов фиксирован."));
        showUsage(player);
    }

    private void showUsage(EntityPlayerMP player) {
        player.sendMessage(new TextComponentString("§e/customresource list"));
        player.sendMessage(new TextComponentString("§e/customresource balance <resource>"));
    }
}
