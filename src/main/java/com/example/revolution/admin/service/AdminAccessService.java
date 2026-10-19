package com.example.revolution.admin.service;

import com.example.revolution.RevolutionMod;
import com.example.revolution.admin.definition.AdminWhitelistEntry;
import com.example.revolution.admin.repository.AdminWhitelistRepository;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

/** In-memory permission policy; file representation and migration belong to the repository. */
public final class AdminAccessService {
    private final AdminWhitelistRepository repository;
    private final Set<UUID> allowed = new LinkedHashSet<>();
    private final List<AdminWhitelistEntry> entries = new ArrayList<>();

    public AdminAccessService(AdminWhitelistRepository repository) { this.repository = repository; }

    public synchronized void init(File configDirectory) { repository.initialize(configDirectory); reload(); }

    public synchronized void reload() {
        allowed.clear();
        entries.clear();
        for (AdminWhitelistEntry entry : repository.load()) {
            if (entry == null || entry.uuid == null) continue;
            try {
                UUID uuid = UUID.fromString(entry.uuid);
                allowed.add(uuid);
                entries.add(new AdminWhitelistEntry(uuid.toString(), entry.name == null ? "" : entry.name));
            } catch (IllegalArgumentException ignored) {
                RevolutionMod.logger.warn("Ignoring invalid admin panel UUID: " + entry.uuid);
            }
        }
    }

    public synchronized boolean isAllowed(UUID uuid) { return uuid != null && allowed.contains(uuid); }

    public boolean canUseModCommands(ICommandSender sender) {
        return !(sender instanceof EntityPlayerMP) || isAllowed(((EntityPlayerMP) sender).getUniqueID());
    }

    public synchronized boolean add(UUID uuid, String name) {
        if (uuid == null || !allowed.add(uuid)) return false;
        entries.add(new AdminWhitelistEntry(uuid.toString(), name == null ? "" : name));
        repository.save(entries);
        return true;
    }

    public synchronized boolean remove(UUID uuid) {
        if (uuid == null || !allowed.remove(uuid)) return false;
        entries.removeIf(entry -> uuid.toString().equals(entry.uuid));
        repository.save(entries);
        return true;
    }

    public synchronized List<AdminWhitelistEntry> entries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }
}
