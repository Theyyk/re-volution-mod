package com.example.revolution.persistence;

import com.example.revolution.RevolutionMod;
import com.example.revolution.admin.service.AdminAccessService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;

public final class FileAdminWhitelistRepositoryTest {
    public static void main(String[] args) throws Exception {
        RevolutionMod.logger = LogManager.getLogger("WhitelistContractTest");
        Path directory = Files.createTempDirectory("revolution-whitelist-test-");
        Path legacyDirectory = Files.createDirectory(directory.resolve("customguimod"));
        Path legacy = legacyDirectory.resolve("admin-panel-whitelist.json");
        Path currentDirectory = directory.resolve("RE-volution");
        Path current = currentDirectory.resolve("admin-panel-whitelist.json");
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        String json = "[{\"uuid\":\"" + first + "\",\"name\":\"Original\"},{\"uuid\":\"invalid\",\"name\":\"Ignored\"}]";
        try {
            Files.write(legacy, json.getBytes(StandardCharsets.UTF_8));
            AdminAccessService access = new AdminAccessService(new FileAdminWhitelistRepository());
            access.init(directory.toFile());
            check(Files.isRegularFile(current) && access.isAllowed(first), "legacy whitelist migration and permission");
            check(access.entries().size() == 1, "invalid UUIDs are ignored");
            check(access.add(second, "Second") && !access.add(second, "Duplicate"), "deduplicate by UUID");
            AdminAccessService reloaded = new AdminAccessService(new FileAdminWhitelistRepository());
            reloaded.init(directory.toFile());
            check(reloaded.isAllowed(second) && reloaded.entries().size() == 2, "JSON field compatibility after save");
            check(reloaded.remove(first), "remove stored permission");
            reloaded.reload();
            check(!reloaded.isAllowed(first) && reloaded.isAllowed(second), "reload persisted removal");
            check(new String(Files.readAllBytes(legacy), StandardCharsets.UTF_8).equals(json), "migration preserves legacy file");
        } finally {
            Files.deleteIfExists(current); Files.deleteIfExists(currentDirectory);
            Files.deleteIfExists(legacy); Files.deleteIfExists(legacyDirectory); Files.deleteIfExists(directory);
        }
        System.out.println("Whitelist JSON migration, validation, save/reload and permission contracts passed");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
