package com.example.revolution.commands;

import com.example.revolution.location.definition.LocationStageTemplate;
import com.example.revolution.location.generation.LocationGenerationPlan;
import com.example.revolution.location.service.LocationProvisioningService;
import com.example.revolution.mob.service.ManagedMobService;
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

/** Admin command for deterministic v1.3.12 Location provisioning. */
public final class LocationCommand extends AdminOnlyCommand {
    private final LocationProvisioningService provisioning;
    private final ManagedMobService mobs;

    public LocationCommand(LocationProvisioningService provisioning, ManagedMobService mobs) {
        if (provisioning == null) throw new IllegalArgumentException("provisioning is required");
        if (mobs == null) throw new IllegalArgumentException("mobs is required");
        this.provisioning = provisioning;
        this.mobs = mobs;
    }

    @Override public String getName() { return "location"; }

    @Override public String getUsage(ICommandSender sender) {
        return "/location create <locationId> <mobSlotId> [seed] | "
                + "/location stagetemplate <locationId> <mobRadius> <triggerOffset> <triggerRadius>";
    }

    @Override
    public List<String> getTabCompletions(
            MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "create", "stagetemplate");
        if (args.length == 2 && ("create".equalsIgnoreCase(args[0]) || "stagetemplate".equalsIgnoreCase(args[0]))) {
            return getListOfStringsMatchingLastWord(args, "<locationId>");
        }
        if ("stagetemplate".equalsIgnoreCase(args[0])) {
            String hint = stageTemplateArgumentHint(args.length);
            if (hint != null) return getListOfStringsMatchingLastWord(args, hint);
        }
        if (args.length == 3 && "create".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "<mobSlotId>");
        }
        if (args.length == 4 && "create".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "<seed>");
        }
        return new ArrayList<>();
    }

    static String stageTemplateArgumentHint(int argsLength) {
        if (argsLength == 3) return "<mobRadius>";
        if (argsLength == 4) return "<triggerOffset>";
        if (argsLength == 5) return "<triggerRadius>";
        return null;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) throw new CommandException(getUsage(sender));
        String action = args[0].toLowerCase(Locale.ROOT);
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
        if (args.length != 3 && args.length != 4) throw new CommandException(getUsage(sender));
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        int worldLocationId = parseInt(args[1], 0, 10_000_000);
        int mobSlotId = parseInt(args[2], 1, 99);
        Long explicitSeed = args.length == 4 ? parseSeed(args[3]) : null;
        LocationStageTemplate stageTemplate = provisioning.getStageTemplate(worldLocationId);

        LocationGenerationPlan plan;
        try {
            plan = provisioning.createStage(
                    worldLocationId,
                    mobSlotId,
                    player.posX,
                    player.posY,
                    player.posZ,
                    player.world.getSeed(),
                    explicitSeed);
            // Reload managed stage topology immediately so the new empty stage is visible without a restart.
            mobs.respawnAll();
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new CommandException(e.getMessage());
        }

        sender.sendMessage(new TextComponentString(
                "Created Location " + worldLocationId + "-" + mobSlotId
                        + " from template=" + plan.getTemplateId()
                        + " seed=" + plan.getSeed()
                        + " radius=" + plan.getRadius()
                        + " rotation=" + plan.getRotationDegrees()
                        + " decorations=" + plan.getDecorations().size()
                        + " triggerOffset=" + stageTemplate.getTriggerOffset()
                        + " triggerRadius=" + stageTemplate.getTriggerRadius()
                        + ". Stage is empty; use populate separately."));
    }

    private static Long parseSeed(String raw) throws CommandException {
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException e) {
            throw new CommandException("seed must be a signed 64-bit integer");
        }
    }
}
