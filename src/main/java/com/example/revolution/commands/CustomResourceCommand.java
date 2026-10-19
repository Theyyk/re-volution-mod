package com.example.revolution.commands;

import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.resource.service.ResourceService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

public class CustomResourceCommand extends AdminOnlyCommand {
    private final ResourceService resources;

    public CustomResourceCommand(ResourceService resources) {
        this.resources = resources;
    }

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
            return getListOfStringsMatchingLastWord(args, ResourceDefinitions.getResourceNames());
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
            List<String> resources = ResourceDefinitions.getResourceNames();
            player.sendMessage(new TextComponentString("§6Ресурсы (" + resources.size() + "): §e" + String.join("§7, §e", resources)));
            return;
        }

        if (args[0].equalsIgnoreCase("balance")) {
            if (args.length != 2) {
                player.sendMessage(new TextComponentString("§c/customresource balance <resource>"));
                return;
            }

            String canonical = ResourceDefinitions.findCanonicalName(args[1]);
            if (canonical == null) {
                player.sendMessage(new TextComponentString("§cРесурс не найден: " + args[1]));
                return;
            }

            int value = resources.getPlayerResource(player.getUniqueID(), canonical);
            player.sendMessage(new TextComponentString("§6" + ResourceDefinitions.getDisplayName(canonical) + ": §e" + value));
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
