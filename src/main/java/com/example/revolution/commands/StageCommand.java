package com.example.revolution.commands;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.definition.StageTransitionTriggerDefinition;
import com.example.revolution.location.definition.StageTransitionTriggerDefinition.DisplayMode;
import com.example.revolution.location.service.StageTransitionService;
import com.example.revolution.location.service.StageTransitionService.Direction;
import com.example.revolution.location.service.StageTransitionService.TransitionResult;
import com.example.revolution.location.service.StageTransitionTriggerService;
import com.example.revolution.mob.service.MobProgressionService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    private final StageTransitionTriggerService triggers;
    private final MobProgressionService progression;

    public StageCommand(
            StageTransitionService transitions,
            StageTransitionTriggerService triggers,
            MobProgressionService progression) {
        this.transitions = transitions;
        this.triggers = triggers;
        this.progression = progression;
    }

    @Override public String getName() { return "stage"; }
    @Override public String getUsage(ICommandSender sender) {
        return "/stage <next|previous> [player] | /stage setpoint <locationId> <mobSlotId> [x y z [yaw pitch]]"
                + " | /stage clearpoint <locationId> <mobSlotId>"
                + " | /stage settrigger <locationId> <mobSlotId> <offset> <radius>"
                + " | /stage cleartrigger <locationId> <mobSlotId>"
                + " | /stage setmessage <locationId> <mobSlotId> <locked|forward|backward> <text...>"
                + " | /stage clearmessage <locationId> <mobSlotId> <locked|forward|backward>"
                + " | /stage display <locationId> <mobSlotId> <chat|actionbar>"
                + " | /stage auto <on|off> [player]"
                + " | /stage revoke <locationId> <mobSlotId> [player]"
                + " | /stage reset <locationId> [player]";
    }

    @Override
    public List<String> getTabCompletions(
            MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args,
                    "next", "previous", "setpoint", "clearpoint", "settrigger", "cleartrigger",
                    "setmessage", "clearmessage", "display", "auto", "revoke", "reset");
        }
        if (args.length == 2 && ("next".equalsIgnoreCase(args[0])
                || "previous".equalsIgnoreCase(args[0]) || "prev".equalsIgnoreCase(args[0]))) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        if (args.length == 2 && "auto".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "on", "off");
        }
        if (args.length == 3 && "auto".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        if (args.length == 4 && "revoke".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        if (args.length == 3 && "reset".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        if (args.length == 4 && ("setmessage".equalsIgnoreCase(args[0]) || "clearmessage".equalsIgnoreCase(args[0]))) {
            return getListOfStringsMatchingLastWord(args, "locked", "forward", "backward");
        }
        if (args.length == 4 && "display".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "chat", "actionbar");
        }
        return new ArrayList<>();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) throw new CommandException(getUsage(sender));
        String action = args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "setpoint": configurePoint(sender, args); return;
            case "clearpoint": clearPoint(sender, args); return;
            case "settrigger": configureTrigger(sender, args); return;
            case "cleartrigger": clearTrigger(sender, args); return;
            case "setmessage": setMessage(sender, args); return;
            case "clearmessage": clearMessage(sender, args); return;
            case "display": setDisplay(sender, args); return;
            case "auto": setAuto(server, sender, args); return;
            case "revoke": revoke(server, sender, args); return;
            case "reset": reset(server, sender, args); return;
            default: break;
        }

        if (args.length > 2) throw new CommandException(getUsage(sender));
        Direction direction;
        if ("next".equals(action)) direction = Direction.NEXT;
        else if ("previous".equals(action) || "prev".equals(action)) direction = Direction.PREVIOUS;
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
        if (args.length != 3 && args.length != 6 && args.length != 8) throw new CommandException(getUsage(sender));
        int worldLocationId = parseInt(args[1]);
        int mobSlotId = parseInt(args[2]);
        LocationDefinition updated;
        try {
            if (args.length == 3) {
                EntityPlayerMP player = getCommandSenderAsPlayer(sender);
                updated = transitions.configureTransitionPoint(
                        worldLocationId, mobSlotId,
                        player.posX, player.posY, player.posZ,
                        player.rotationYaw, player.rotationPitch);
            } else {
                double x = parseDouble(args[3]);
                double y = parseDouble(args[4]);
                double z = parseDouble(args[5]);
                float yaw = args.length == 8 ? (float) parseDouble(args[6]) : 0.0F;
                float pitch = args.length == 8 ? (float) parseDouble(args[7]) : 0.0F;
                updated = transitions.configureTransitionPoint(worldLocationId, mobSlotId, x, y, z, yaw, pitch);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new CommandException(e.getMessage());
        }
        sender.sendMessage(new TextComponentString(
                "Transition point saved for Location " + worldLocationId + ", mobSlot " + mobSlotId
                        + " at " + format(updated.getTransitionX()) + ", " + format(updated.getTransitionY())
                        + ", " + format(updated.getTransitionZ()) + "."));
    }

    private void clearPoint(ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 3) throw new CommandException(getUsage(sender));
        int worldLocationId = parseInt(args[1]);
        int mobSlotId = parseInt(args[2]);
        try { transitions.clearTransitionPoint(worldLocationId, mobSlotId); }
        catch (IllegalArgumentException | IllegalStateException e) { throw new CommandException(e.getMessage()); }
        sender.sendMessage(new TextComponentString(
                "Transition point cleared for Location " + worldLocationId + ", mobSlot " + mobSlotId + "."));
    }

    private void configureTrigger(ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 5) throw new CommandException(getUsage(sender));
        int worldLocationId = parseInt(args[1]);
        int mobSlotId = parseInt(args[2]);
        double offset = parseDouble(args[3], 0.0D);
        double radius = parseDouble(args[4], 0.01D);
        StageTransitionTriggerDefinition trigger;
        try { trigger = triggers.configureTrigger(worldLocationId, mobSlotId, offset, radius); }
        catch (IllegalArgumentException | IllegalStateException e) { throw new CommandException(e.getMessage()); }
        sender.sendMessage(new TextComponentString(
                "Boundary trigger saved for Location " + worldLocationId + ", mobSlot " + mobSlotId
                        + " offset=" + format(trigger.getOffset())
                        + " radius=" + format(trigger.getRadius())
                        + " (forward/backward 180 degrees, 1s delay)."));
    }

    private void clearTrigger(ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 3) throw new CommandException(getUsage(sender));
        int worldLocationId = parseInt(args[1]);
        int mobSlotId = parseInt(args[2]);
        triggers.clearTrigger(worldLocationId, mobSlotId);
        sender.sendMessage(new TextComponentString(
                "Boundary trigger cleared for Location " + worldLocationId + ", mobSlot " + mobSlotId + "."));
    }

    private void setMessage(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 5) throw new CommandException(getUsage(sender));
        int worldLocationId = parseInt(args[1]);
        int mobSlotId = parseInt(args[2]);
        String kind = messageKind(args[3]);
        StringBuilder text = new StringBuilder();
        for (int i = 4; i < args.length; i++) {
            if (text.length() > 0) text.append(' ');
            text.append(args[i]);
        }
        try { triggers.setMessage(worldLocationId, mobSlotId, kind, text.toString()); }
        catch (IllegalArgumentException | IllegalStateException e) { throw new CommandException(e.getMessage()); }
        sender.sendMessage(new TextComponentString(
                "Transition " + kind + " message saved for Location "
                        + worldLocationId + ", mobSlot " + mobSlotId + "."));
    }

    private void clearMessage(ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 4) throw new CommandException(getUsage(sender));
        int worldLocationId = parseInt(args[1]);
        int mobSlotId = parseInt(args[2]);
        String kind = messageKind(args[3]);
        try { triggers.clearMessage(worldLocationId, mobSlotId, kind); }
        catch (IllegalArgumentException | IllegalStateException e) { throw new CommandException(e.getMessage()); }
        sender.sendMessage(new TextComponentString(
                "Transition " + kind + " message cleared for Location "
                        + worldLocationId + ", mobSlot " + mobSlotId + "."));
    }

    private void setDisplay(ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 4) throw new CommandException(getUsage(sender));
        int worldLocationId = parseInt(args[1]);
        int mobSlotId = parseInt(args[2]);
        DisplayMode mode;
        if ("chat".equalsIgnoreCase(args[3])) mode = DisplayMode.CHAT;
        else if ("actionbar".equalsIgnoreCase(args[3])) mode = DisplayMode.ACTION_BAR;
        else throw new CommandException("Display mode must be chat or actionbar");
        try { triggers.setDisplayMode(worldLocationId, mobSlotId, mode); }
        catch (IllegalArgumentException | IllegalStateException e) { throw new CommandException(e.getMessage()); }
        sender.sendMessage(new TextComponentString(
                "Transition display mode set to " + mode.name() + " for Location "
                        + worldLocationId + ", mobSlot " + mobSlotId + "."));
    }

    private void setAuto(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 2 && args.length != 3) throw new CommandException(getUsage(sender));
        boolean enabled;
        if ("on".equalsIgnoreCase(args[1])) enabled = true;
        else if ("off".equalsIgnoreCase(args[1])) enabled = false;
        else throw new CommandException("auto must be on or off");
        EntityPlayerMP player = args.length == 3
                ? getPlayer(server, sender, args[2])
                : getCommandSenderAsPlayer(sender);
        triggers.setAutoTransitionEnabled(player.getUniqueID(), enabled);
        sender.sendMessage(new TextComponentString(
                "Auto stage transitions " + (enabled ? "enabled" : "disabled") + " for " + player.getName() + "."));
    }

    private void revoke(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 3 && args.length != 4) throw new CommandException(getUsage(sender));
        int worldLocationId = parseInt(args[1], 0);
        int mobSlotId = parseInt(args[2], 1, 99);
        EntityPlayerMP player = args.length == 4
                ? getPlayer(server, sender, args[3])
                : getCommandSenderAsPlayer(sender);
        if (!progression.revokeFrom(player.getUniqueID(), worldLocationId, mobSlotId)) {
            throw new CommandException("Could not revoke progression from Location " + worldLocationId
                    + ", mobSlot " + mobSlotId + ". Check that the stage exists and persistence is available.");
        }
        sender.sendMessage(new TextComponentString(
                "Progression revoked for " + player.getName() + " from Location " + worldLocationId
                        + ", mobSlot " + mobSlotId + " onward, including all later Locations."));
    }

    private void reset(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 2 && args.length != 3) throw new CommandException(getUsage(sender));
        int worldLocationId = parseInt(args[1], 0);
        EntityPlayerMP player = args.length == 3
                ? getPlayer(server, sender, args[2])
                : getCommandSenderAsPlayer(sender);
        if (!progression.resetFromLocation(player.getUniqueID(), worldLocationId)) {
            throw new CommandException("Could not reset progression from Location " + worldLocationId
                    + ". Persistence may be unavailable.");
        }
        sender.sendMessage(new TextComponentString(
                "Progression reset for " + player.getName() + " from Location " + worldLocationId
                        + " onward, including all later Locations."));
    }

    private String messageKind(String value) throws CommandException {
        if ("locked".equalsIgnoreCase(value)) return "locked";
        if ("forward".equalsIgnoreCase(value)) return "forward";
        if ("backward".equalsIgnoreCase(value)) return "backward";
        throw new CommandException("Message kind must be locked, forward or backward");
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
