package com.example.customguimod.admin;

import com.example.customguimod.CustomGuiMod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Separate whitelist for the in-game admin/test panel and mod commands. */
public final class AdminPanelAccess {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<Entry>>() {}.getType();
    private static final Set<UUID> ALLOWED = new LinkedHashSet<>();
    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static File file;

    private AdminPanelAccess() {}

    public static synchronized void init(File configDir) {
        File dir = new File(configDir, "customguimod");
        if (!dir.exists() && !dir.mkdirs()) {
            CustomGuiMod.logger.warn("Could not create admin panel config directory: " + dir);
        }
        file = new File(dir, "admin-panel-whitelist.json");
        reload();
    }

    public static synchronized void reload() {
        ALLOWED.clear();
        ENTRIES.clear();
        if (file == null) return;
        if (!file.exists()) {
            save();
            return;
        }
        try (FileReader reader = new FileReader(file)) {
            List<Entry> loaded = GSON.fromJson(reader, LIST_TYPE);
            if (loaded == null) return;
            for (Entry entry : loaded) {
                if (entry == null || entry.uuid == null) continue;
                try {
                    UUID uuid = UUID.fromString(entry.uuid);
                    ALLOWED.add(uuid);
                    ENTRIES.add(new Entry(uuid.toString(), entry.name == null ? "" : entry.name));
                } catch (IllegalArgumentException ignored) {
                    CustomGuiMod.logger.warn("Ignoring invalid admin panel UUID: " + entry.uuid);
                }
            }
        } catch (IOException e) {
            CustomGuiMod.logger.error("Could not read admin panel whitelist", e);
        }
    }

    public static synchronized boolean isAllowed(UUID uuid) {
        return uuid != null && ALLOWED.contains(uuid);
    }

    public static boolean canUseModCommands(ICommandSender sender) {
        return !(sender instanceof EntityPlayerMP)
                || isAllowed(((EntityPlayerMP) sender).getUniqueID());
    }

    public static synchronized boolean add(UUID uuid, String name) {
        if (uuid == null) return false;
        if (ALLOWED.contains(uuid)) return false;
        ALLOWED.add(uuid);
        ENTRIES.add(new Entry(uuid.toString(), name == null ? "" : name));
        save();
        return true;
    }

    public static synchronized boolean remove(UUID uuid) {
        if (uuid == null || !ALLOWED.remove(uuid)) return false;
        ENTRIES.removeIf(entry -> uuid.toString().equals(entry.uuid));
        save();
        return true;
    }

    public static synchronized List<Entry> entries() {
        return Collections.unmodifiableList(new ArrayList<>(ENTRIES));
    }

    private static void save() {
        if (file == null) return;
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(ENTRIES, LIST_TYPE, writer);
        } catch (IOException e) {
            CustomGuiMod.logger.error("Could not save admin panel whitelist", e);
        }
    }

    public static final class Entry {
        public String uuid;
        public String name;

        public Entry(String uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
    }
}
