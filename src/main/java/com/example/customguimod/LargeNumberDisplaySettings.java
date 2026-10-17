package com.example.customguimod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/** Local client preference for how values above centillion are displayed. */
final class LargeNumberDisplaySettings {
    private static final String KEY_MODE = "mode";
    private static LargeNumberDisplayMode mode = LargeNumberDisplayMode.FULL;
    private static File configFile;

    private LargeNumberDisplaySettings() {}

    static void init(File configDirectory) {
        if (configDirectory == null) return;
        configFile = new File(configDirectory, "customguimod-large-numbers.properties");
        load();
    }

    static LargeNumberDisplayMode getMode() {
        return mode;
    }

    static void setMode(LargeNumberDisplayMode newMode) {
        if (newMode == null) return;
        mode = newMode;
        save();
    }

    private static void load() {
        if (configFile == null || !configFile.isFile()) return;
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(configFile)) {
            properties.load(input);
            String raw = properties.getProperty(KEY_MODE, LargeNumberDisplayMode.FULL.name());
            mode = LargeNumberDisplayMode.valueOf(raw);
        } catch (IOException | IllegalArgumentException e) {
            mode = LargeNumberDisplayMode.FULL;
            CustomGuiMod.logger.warn("Could not load large number display preference; using FULL", e);
        }
    }

    private static void save() {
        if (configFile == null) return;
        Properties properties = new Properties();
        properties.setProperty(KEY_MODE, mode.name());
        try (FileOutputStream output = new FileOutputStream(configFile)) {
            properties.store(output, "RE:volution large number display settings");
        } catch (IOException e) {
            CustomGuiMod.logger.warn("Could not save large number display preference", e);
        }
    }
}
