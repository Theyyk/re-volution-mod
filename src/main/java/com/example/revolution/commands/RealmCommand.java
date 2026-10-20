package com.example.revolution.commands;

import com.example.revolution.realm.definition.RealmCatalog;
import com.example.revolution.realm.definition.RealmDefinition;
import com.example.revolution.realm.service.RealmService;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

/** Admin Realm transfer command: /realm join <realmId> [player]. */
public final class RealmCommand extends AdminOnlyCommand {
    private final RealmService realms;

    public RealmCommand(RealmService realms) { this.realms = realms; }

    @Override public String getName() { return "realm"; }
    @Override public String getUsage(ICommandSender sender) { return "/realm join <realmId> [player]"; }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "join");
        if (args.length == 2 && "join".equalsIgnoreCase(args[0])) {
            List<String> ids = new ArrayList<>();
            for (RealmDefinition realm : RealmCatalog.all()) ids.add(Integer.toString(realm.getRealmId()));
            return getListOfStringsMatchingLastWord(args, ids);
        }
        if (args.length == 3 && "join".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        return new ArrayList<>();
    }

    @Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2 || args.length > 3 || !"join".equalsIgnoreCase(args[0])) {
            throw new CommandException(getUsage(sender));
        }
        int realmId = parseInt(args[1]);
        EntityPlayerMP player = args.length == 3 ? getPlayer(server, sender, args[2]) : getCommandSenderAsPlayer(sender);
        RealmDefinition realm;
        try {
            realm = realms.join(server, player, realmId);
        } catch (IllegalArgumentException e) {
            throw new CommandException(e.getMessage());
        }
        sender.sendMessage(new TextComponentString("Moved " + player.getName() + " to " + realm.getName() + "."));
    }
}
