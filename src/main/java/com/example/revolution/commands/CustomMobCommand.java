package com.example.revolution.commands;

import com.example.revolution.mob.definition.MobDefinition;
import com.example.revolution.mob.service.MobAdminService;
import com.example.revolution.resource.definition.ResourceDefinitions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

public class CustomMobCommand extends AdminOnlyCommand {
    private final MobAdminService service;

    public CustomMobCommand(MobAdminService service) {
        this.service = service;
    }

    @Override public String getName() { return "custommob"; }
    @Override public String getUsage(ICommandSender sender) { return "/custommob create <name> <resource> <amount> <hp> <type> | setspawn|unspawn|delete|list"; }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, Arrays.asList("create", "setspawn", "unspawn", "delete", "list"));
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("create")) return getListOfStringsMatchingLastWord(args, Arrays.asList("<name>"));
            if (args[0].equalsIgnoreCase("setspawn") || args[0].equalsIgnoreCase("unspawn") || args[0].equalsIgnoreCase("delete")) {
                List<String> names = new ArrayList<>();
                for (MobDefinition mob : service.definitions()) names.add(mob.name);
                return getListOfStringsMatchingLastWord(args, names);
            }
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("create")) {
            if (args.length == 3) return getListOfStringsMatchingLastWord(args, ResourceDefinitions.getResourceNames());
            if (args.length == 4) return getListOfStringsMatchingLastWord(args, Arrays.asList("<amount>"));
            if (args.length == 5) return getListOfStringsMatchingLastWord(args, Arrays.asList("<hp>"));
            if (args.length == 6) return getListOfStringsMatchingLastWord(args, Arrays.asList("zombie", "skeleton"));
        }
        return new ArrayList<>();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        service.execute(sender, args);
    }

}
