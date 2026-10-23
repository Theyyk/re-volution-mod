package com.example.revolution.commands;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.MobZoneIds;
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
    @Override public String getUsage(ICommandSender sender) {
        return "/stage <next|previous> [player] | /stage setpoint <locationId> <mobSlotId> [x y z [yaw pitch]]";
    }

    @Override
    public List<String> getTabCompletions(
            MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "next", "previous", "setpoint");
        if (args.length == 2 && ("next".equalsIgnoreCase(args[0]) || "previous".equalsIgnoreCase(args[0]) || "prev".equalsIgnoreCase(args[0]))) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        return new ArrayList<>();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) throw new CommandException(getUsage(sender));
        if ("setpoint".equalsIgnoreCase(args[0])) {
            configurePoint(sender, args);
            return;
        }

        if (args.length > 2) throw new CommandException(getUsage(sender));
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
                "Moved " + player.getName()
                        + " from Location " + MobZoneIds.worldLocationId(result.getFromInternalLocationId())
                        + ", mobSlot " + MobZoneIds.mobSlotId(result.getFromInternalLocationId())
                        + " to Location " + MobZoneIds.worldLocationId(result.getToInternalLocationId())
                        + ", mobSlot " + MobZoneIds.mobSlotId(result.getToInternalLocationId()) + "."));
    }

    private void configurePoint(ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 3 && args.length != 6 && args.length != 8) {
            throw new CommandException(getUsage(sender));
        }

        int worldLocationId = parseInt(args[1]);
        int mobSlotId = parseInt(args[2]);
        LocationDefinition updated;
        try {
            if (args.length == 3) {
                EntityPlayerMP player = getCommandSenderAsPlayer(sender);
                updated = transitions.configureTransitionPoint(
                        worldLocationId,
                        mobSlotId,
                        player.posX,
                        player.posY,
                        player.posZ,
                        player.rotationYaw,
                        player.rotationPitch);
            } else {
                double x = parseDouble(args[3]);
                double y = parseDouble(args[4]);
                double z = parseDouble(args[5]);
                float yaw = args.length == 8 ? (float) parseDouble(args[6]) : 0.0F;
                float pitch = args.length == 8 ? (float) parseDouble(args[7]) : 0.0F;
                updated = transitions.configureTransitionPoint(
                        worldLocationId, mobSlotId, x, y, z, yaw, pitch);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new CommandException(e.getMessage());
        }

        sender.sendMessage(new TextComponentString(
                "Transition point saved for Location " + worldLocationId
                        + ", mobSlot " + mobSlotId
                        + " at " + format(updated.getTransitionX())
                        + ", " + format(updated.getTransitionY())
                        + ", " + format(updated.getTransitionZ()) + "."));
    }

    private String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
