package com.example.revolution.persistence;

import com.example.revolution.RevolutionMod;
import com.example.revolution.admin.definition.AdminWhitelistEntry;
import com.example.revolution.admin.repository.AdminWhitelistRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

/** Preserves the existing JSON format, migration paths and error policy. */
public final class FileAdminWhitelistRepository implements AdminWhitelistRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<AdminWhitelistEntry>>() {}.getType();
    private File file;
    public synchronized void initialize(File configDir) {
        File dir = new File(configDir, "RE-volution");
        if (!dir.exists() && !dir.mkdirs()) {
            RevolutionMod.logger.warn("Could not create admin panel config directory: " + dir);
        }
        file = new File(dir, "admin-panel-whitelist.json");
        File legacyFile = new File(new File(configDir, "customguimod"), "admin-panel-whitelist.json");
        if (!file.isFile() && legacyFile.isFile()) {
            try {
                java.nio.file.Files.copy(legacyFile.toPath(), file.toPath());
                RevolutionMod.logger.info("Migrated admin panel whitelist to RE-volution config directory");
            } catch (IOException e) {
                RevolutionMod.logger.warn("Could not migrate legacy admin panel whitelist; reading legacy file for this run", e);
                file = legacyFile;
            }
        }

    }
    @Override public List<AdminWhitelistEntry> load() {
        if (file == null) return Collections.emptyList();
        if (!file.exists()) {
            save(Collections.emptyList());
            return Collections.emptyList();
        }
        try (FileReader reader = new FileReader(file)) {
            List<AdminWhitelistEntry> loaded = GSON.fromJson(reader, LIST_TYPE);
            return loaded == null ? Collections.emptyList() : loaded;
        } catch (IOException e) {
            RevolutionMod.logger.error("Could not read admin panel whitelist", e);
            return Collections.emptyList();
        }
    }
    public void save(List<AdminWhitelistEntry> entries) {
        if (file == null) return;
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(entries, LIST_TYPE, writer);
        } catch (IOException e) {
            RevolutionMod.logger.error("Could not save admin panel whitelist", e);
        }
    }
}
