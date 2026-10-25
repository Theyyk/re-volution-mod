package com.example.revolution.commands;

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
        return "/location create <locationId> <mobSlotId> [seed]";
    }

    @Override
    public List<String> getTabCompletions(
            MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "create");
        if (args.length == 2 && "create".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "<locationId>");
        }
        if (args.length == 3 && "create".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "<mobSlotId>");
        }
        if (args.length == 4 && "create".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "<seed>");
        }
        return new ArrayList<>();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1 || !"create".equals(args[0].toLowerCase(Locale.ROOT))) {
            throw new CommandException(getUsage(sender));
        }
        if (args.length != 3 && args.length != 4) throw new CommandException(getUsage(sender));

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        int worldLocationId = parseInt(args[1], 0, 10_000_000);
        int mobSlotId = parseInt(args[2], 1, 99);
        Long explicitSeed = args.length == 4 ? parseSeed(args[3]) : null;

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
