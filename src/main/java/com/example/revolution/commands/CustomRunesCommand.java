package com.example.revolution.commands;

import com.example.revolution.rune.service.RuneAdminService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

public class CustomRunesCommand extends AdminOnlyCommand {
    private final RuneAdminService service;

    public CustomRunesCommand(RuneAdminService service) {
        this.service = service;
    }

    @Override
    public String getName() {
        return "customrunes";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/customrunes <clear|resetenhancements>";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("clear", "resetenhancements"));
        }
        return new ArrayList<>();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        service.execute(sender, args);
    }
}
