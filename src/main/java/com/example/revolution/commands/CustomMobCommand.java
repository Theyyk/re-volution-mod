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

    public CustomMobCommand(MobAdminService service) { this.service = service; }

    @Override public String getName() { return "custommob"; }
    @Override public String getUsage(ICommandSender sender) {
        return "/custommob create|location|spawn|setspawn|unspawn|delete|list";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args,
                Arrays.asList("create", "location", "spawn", "setspawn", "unspawn", "delete", "list"));
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("location")) {
                return getListOfStringsMatchingLastWord(args, Arrays.asList("set", "populate", "here", "list"));
            }
            if (args[0].equalsIgnoreCase("spawn")) return getListOfStringsMatchingLastWord(args, Arrays.asList("set", "list", "remove"));
            if (args[0].equalsIgnoreCase("create")) return getListOfStringsMatchingLastWord(args, Arrays.asList("<name>"));
            if (args[0].equalsIgnoreCase("setspawn") || args[0].equalsIgnoreCase("unspawn") || args[0].equalsIgnoreCase("delete")) {
                return getListOfStringsMatchingLastWord(args, mobNames());
            }
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("create")) {
            if (args.length == 3) return getListOfStringsMatchingLastWord(args, ResourceDefinitions.getResourceNames());
            if (args.length == 4) return getListOfStringsMatchingLastWord(args, Arrays.asList("<amount>"));
            if (args.length == 5) return getListOfStringsMatchingLastWord(args, Arrays.asList("<hp>"));
            if (args.length == 6) return getListOfStringsMatchingLastWord(args, Arrays.asList("zombie", "skeleton"));
        }
        if (args.length > 1 && args[0].equalsIgnoreCase("location")) {
            if (args[1].equalsIgnoreCase("set")) {
                if (args.length == 3) return getListOfStringsMatchingLastWord(args, Arrays.asList("<locationId>"));
                if (args.length == 4) return getListOfStringsMatchingLastWord(args, Arrays.asList("<mobSlotId>"));
                if (args.length == 5) return getListOfStringsMatchingLastWord(args, Arrays.asList("<radius>"));
            }
            if (args[1].equalsIgnoreCase("populate")) {
                if (args.length == 3) return getListOfStringsMatchingLastWord(args, Arrays.asList("<locationId>"));
                if (args.length == 4) return getListOfStringsMatchingLastWord(args, Arrays.asList("<mobSlotId>"));
                if (args.length == 5) return getListOfStringsMatchingLastWord(args, mobNames());
                if (args.length == 6) return getListOfStringsMatchingLastWord(args, Arrays.asList("<count>"));
            }
            if (args[1].equalsIgnoreCase("list") && args.length == 3) {
                return getListOfStringsMatchingLastWord(args, Arrays.asList("<locationId>"));
            }
        }
        if (args.length > 1 && args[0].equalsIgnoreCase("spawn") && args[1].equalsIgnoreCase("set")) {
            if (args.length == 5) return getListOfStringsMatchingLastWord(args, mobNames());
            if (args.length == 6) return getListOfStringsMatchingLastWord(args, Arrays.asList("fixed", "random"));
            if (args.length == 7) return getListOfStringsMatchingLastWord(args, Arrays.asList("stationary", "roaming"));
            if (args.length == 8) return getListOfStringsMatchingLastWord(args, Arrays.asList("player", "realm0"));
        }
        return new ArrayList<>();
    }

    private List<String> mobNames() {
        List<String> names = new ArrayList<>();
        for (MobDefinition mob : service.definitions()) names.add(mob.name);
        return names;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        service.execute(sender, args);
    }
}
