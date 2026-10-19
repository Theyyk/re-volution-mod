package com.example.revolution.commands;

import com.example.revolution.player.service.PlayerCollectionService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

public class CustomStatusCommand extends AdminOnlyCommand {
    private final PlayerCollectionService service;

    public CustomStatusCommand(PlayerCollectionService service) {
        this.service = service;
    }

    @Override
    public String getName() {
        return "customstatus";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/customstatus create|switch|list|delete";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("create", "switch", "list", "delete"));
        }

        if (args.length == 2 && sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            if (args[0].equalsIgnoreCase("create")) {
                return getListOfStringsMatchingLastWord(args, Arrays.asList("<name>"));
            }
            if (args[0].equalsIgnoreCase("switch") || args[0].equalsIgnoreCase("delete")) {
                List<String> indexes = new ArrayList<>();
                List<String> statuses = service.names(player.getUniqueID());
                for (int i = 0; i < statuses.size(); i++) indexes.add(String.valueOf(i));
                return getListOfStringsMatchingLastWord(args, indexes);
            }
        }

        return new ArrayList<>();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        service.execute(sender, args);
    }
}
