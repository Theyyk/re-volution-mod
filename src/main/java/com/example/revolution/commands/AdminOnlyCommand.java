package com.example.revolution.commands;

import com.example.revolution.lifecycle.ServerLifecycle;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

/** All player-issued mod commands require the dedicated Admin Panel whitelist. */
public abstract class AdminOnlyCommand extends CommandBase {
    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return ServerLifecycle.services().adminAccess.canUseModCommands(sender);
    }
}
