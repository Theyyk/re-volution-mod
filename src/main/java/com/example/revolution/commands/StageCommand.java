package com.example.revolution.commands;

import com.example.revolution.location.service.StageTransitionService;
import com.example.revolution.location.service.StageTransitionService.Direction;
import com.example.revolution.location.service.StageTransitionService.TransitionResult;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

/** Admin-only DEV/manual acceptance command for v1.3.9 stage transport. */
public final class StageCommand extends AdminOnlyCommand {
    private final StageTransitionService transitions;

    public StageCommand(StageTransitionService transitions) {
        this.transitions = transitions;
    }

    @Override public String getName() { return "stage"; }
    @Override public String getUsage(ICommandSender sender) { return "/stage <next|previous> [player]"; }

    @Override
    public List<String> getTabCompletions(
            MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "next", "previous");
        if (args.length == 2) return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        return new ArrayList<>();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1 || args.length > 2) throw new CommandException(getUsage(sender));

        Direction direction;
        if ("next".equalsIgnoreCase(args[0])) direction = Direction.NEXT;
        else if ("previous".equalsIgnoreCase(args[0]) || "prev".equalsIgnoreCase(args[0])) direction = Direction.PREVIOUS;
        else throw new CommandException(getUsage(sender));

        EntityPlayerMP player = args.length == 2
                ? getPlayer(server, sender, args[1])
                : getCommandSenderAsPlayer(sender);
        TransitionResult result;
        try {
            result = transitions.move(player, direction);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new CommandException(e.getMessage());
        }

        sender.sendMessage(new TextComponentString(
                "Moved " + player.getName() + " from stage " + result.getFromInternalLocationId()
                        + " to " + result.getToInternalLocationId() + "."));
    }
}
