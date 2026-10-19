package com.example.revolution.number;

import com.example.revolution.RevolutionMod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/** Local client preference for large-number presentation. */
final class LargeNumberDisplaySettings {
    private static final String KEY_MODE = "mode";
    private static final LargeNumberDisplayMode DEFAULT_MODE = LargeNumberDisplayMode.SIMPLIFIED;
    private static LargeNumberDisplayMode mode = DEFAULT_MODE;
    private static File configFile;

    private LargeNumberDisplaySettings() {}

    static void init(File configDirectory) {
        if (configDirectory == null) return;
        configFile = new File(configDirectory, "RE-volution-large-numbers.properties");
        File legacyFile = new File(configDirectory, "customguimod-large-numbers.properties");
        if (!configFile.isFile() && legacyFile.isFile()) {
            try {
                java.nio.file.Files.copy(legacyFile.toPath(), configFile.toPath());
                RevolutionMod.logger.info("Migrated large number settings to RE-volution config file");
            } catch (IOException e) {
                RevolutionMod.logger.warn("Could not migrate legacy large number settings; reading legacy file for this run", e);
                configFile = legacyFile;
            }
        }
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
        if (configFile == null || !configFile.isFile()) {
            mode = DEFAULT_MODE;
            return;
        }
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(configFile)) {
            properties.load(input);
            String raw = properties.getProperty(KEY_MODE, DEFAULT_MODE.name());
            mode = parseMode(raw);
        } catch (IOException | IllegalArgumentException e) {
            mode = DEFAULT_MODE;
            RevolutionMod.logger.warn("Could not load large number display preference; using " + DEFAULT_MODE.name(), e);
        }
    }

    private static LargeNumberDisplayMode parseMode(String raw) {
        if (raw == null) return DEFAULT_MODE;
        String normalized = raw.trim().toUpperCase(java.util.Locale.ROOT);
        if ("SCIENTIFIC".equals(normalized)) return LargeNumberDisplayMode.ENGINEERING;
        if ("LETTER".equals(normalized) || "ABBREVIATED".equals(normalized) || "SHORT".equals(normalized)) {
            return LargeNumberDisplayMode.SIMPLIFIED;
        }
        if ("FULL".equals(normalized)) return LargeNumberDisplayMode.FULL;
        return LargeNumberDisplayMode.valueOf(normalized);
    }

    private static void save() {
        if (configFile == null) return;
        Properties properties = new Properties();
        properties.setProperty(KEY_MODE, mode.name());
        try (FileOutputStream output = new FileOutputStream(configFile)) {
            properties.store(output, "RE:volution large number display settings");
        } catch (IOException e) {
            RevolutionMod.logger.warn("Could not save large number display preference", e);
        }
    }
}
