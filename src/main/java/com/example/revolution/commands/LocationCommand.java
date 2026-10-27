package com.example.revolution.commands;

import com.example.revolution.location.definition.LocationStageTemplate;
import com.example.revolution.location.generation.LocationGenerationPlan;
import com.example.revolution.location.service.LocationProvisioningService;
import com.example.revolution.location.world.REBuilderAdapter;
import com.example.revolution.location.service.StageTransitionTriggerService;
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
import org.apache.logging.log4j.LogManager;

/** Admin command for deterministic v1.3.12 Location provisioning. */
public final class LocationCommand extends AdminOnlyCommand {
    private final LocationProvisioningService provisioning;
    private final StageTransitionTriggerService triggers;
    private final REBuilderAdapter builds;

    public LocationCommand(LocationProvisioningService provisioning, StageTransitionTriggerService triggers, REBuilderAdapter builds) {
        if (provisioning == null) throw new IllegalArgumentException("provisioning is required");
        if (triggers == null) throw new IllegalArgumentException("triggers is required");
        if (builds == null) throw new IllegalArgumentException("builds is required");
        this.provisioning = provisioning;
        this.triggers = triggers;
        this.builds = builds;
    }

    @Override public String getName() { return "location"; }

    @Override public String getUsage(ICommandSender sender) {
        return "/location create <locationId> <mobSlotId> [x y z] | "
                + "/location stagetemplate <locationId> <mobRadius> <triggerOffset> <triggerRadius> | "
                + "/location buildtemplate <locationId> <templateName>";
    }

    @Override
    public List<String> getTabCompletions(
            MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "create", "stagetemplate", "buildtemplate");
        if (args.length == 2 && ("create".equalsIgnoreCase(args[0]) || "stagetemplate".equalsIgnoreCase(args[0])
                || "buildtemplate".equalsIgnoreCase(args[0]))) {
            return getListOfStringsMatchingLastWord(args, "<locationId>");
        }
        if (args.length == 0) return new ArrayList<>();
        if (args.length == 3 && "buildtemplate".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "<templateName>");
        }
        if ("stagetemplate".equalsIgnoreCase(args[0])) {
            String hint = stageTemplateArgumentHint(args.length);
            if (hint != null) return getListOfStringsMatchingLastWord(args, hint);
        }
        if (args.length == 3 && "create".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "<mobSlotId>");
        }
        if ("create".equalsIgnoreCase(args[0])) {
            String hint = createCoordinateArgumentHint(args.length);
            if (hint != null) return getListOfStringsMatchingLastWord(args, hint);
        }
        return new ArrayList<>();
    }

    static String stageTemplateArgumentHint(int argsLength) {
        if (argsLength == 3) return "<mobRadius>";
        if (argsLength == 4) return "<triggerOffset>";
        if (argsLength == 5) return "<triggerRadius>";
        return null;
    }

    static String createCoordinateArgumentHint(int argsLength) {
        if (argsLength == 4) return "<x>";
        if (argsLength == 5) return "<y>";
        if (argsLength == 6) return "<z>";
        return null;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) throw new CommandException(getUsage(sender));
        String action = args[0].toLowerCase(Locale.ROOT);
        if ("buildtemplate".equals(action)) {
            if (args.length != 3) throw new CommandException(getUsage(sender));
            int worldLocationId = parseInt(args[1], 0, 10_000_000);
            try {
                provisioning.setBuildTemplate(worldLocationId, args[2]);
                sender.sendMessage(new TextComponentString("Build template Location " + worldLocationId + ": " + args[2]));
            } catch (IllegalArgumentException | IllegalStateException e) {
                throw new CommandException(e.getMessage());
            }
            return;
        }
        if ("stagetemplate".equals(action)) {
            setStageTemplate(sender, args);
            return;
        }
        if (!"create".equals(action)) throw new CommandException(getUsage(sender));
        createStage(sender, args);
    }

    private void setStageTemplate(ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 5) throw new CommandException(getUsage(sender));
        int worldLocationId = parseInt(args[1], 0, 10_000_000);
        double mobRadius = parseDouble(args[2], 0.01D, 10000.0D);
        double triggerOffset = parseDouble(args[3], 0.0D, 10000.0D);
        double triggerRadius = parseDouble(args[4], 0.01D, 10000.0D);
        try {
            LocationStageTemplate template = provisioning.setStageTemplate(
                    worldLocationId, mobRadius, triggerOffset, triggerRadius);
            sender.sendMessage(new TextComponentString(
                    "Stage template Location " + template.getWorldLocationId()
                            + ": mobRadius=" + template.getMobRadius()
                            + " triggerOffset=" + template.getTriggerOffset()
                            + " triggerRadius=" + template.getTriggerRadius()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new CommandException(e.getMessage());
        }
    }

    private void createStage(ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 3 && args.length != 6) throw new CommandException(getUsage(sender));
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        int worldLocationId = parseInt(args[1], 0, 10_000_000);
        int mobSlotId = parseInt(args[2], 1, 99);
        LocationStageTemplate stageTemplate = provisioning.getStageTemplate(worldLocationId);

        double anchorX;
        double anchorY;
        double anchorZ;
        if (args.length == 6) {
            anchorX = parseDouble(args[3]);
            anchorY = parseDouble(args[4]);
            anchorZ = parseDouble(args[5]);
        } else {
            BlockPos anchor = player.getPosition();
            anchorX = anchor.getX();
            anchorY = anchor.getY();
            anchorZ = anchor.getZ();
        }

        LocationGenerationPlan plan;
        try {
            plan = provisioning.createStage(
                    worldLocationId,
                    mobSlotId,
                    anchorX,
                    anchorY,
                    anchorZ,
                    player.world.getSeed(),
                    null,
                    builds.forPlayer(player));
            triggers.rememberCreatedTrigger(worldLocationId, mobSlotId, stageTemplate);
        } catch (RuntimeException e) {
            LogManager.getLogger(LocationCommand.class).error(
                    "Location create failed for location={} slot={} anchor={},{},{}",
                    worldLocationId, mobSlotId, anchorX, anchorY, anchorZ, e);
            String message = e.getMessage();
            if (message == null || message.trim().isEmpty()) message = e.getClass().getSimpleName();
            throw new CommandException("Location create failed: " + message);
        }

        sender.sendMessage(new TextComponentString(
                "Created Location " + worldLocationId + "-" + mobSlotId
                        + " from template=" + plan.getTemplateId()
                        + " seed=" + plan.getSeed()
                        + " anchor=" + format(anchorX) + "," + format(anchorY) + "," + format(anchorZ)
                        + " center=" + format(plan.getCenterX()) + "," + format(plan.getCenterZ())
                        + " radius=" + plan.getRadius()
                        + " rotation=" + plan.getRotationDegrees()
                        + " triggerOffset=" + stageTemplate.getTriggerOffset()
                        + " triggerRadius=" + stageTemplate.getTriggerRadius()
                        + ". Canonical mob=Mob" + worldLocationId + "_" + mobSlotId + " population=1."));
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
