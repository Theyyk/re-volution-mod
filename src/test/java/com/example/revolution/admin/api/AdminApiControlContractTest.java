package com.example.revolution.admin.api;

import com.example.revolution.admin.definition.AdminRuneSnapshot;
import java.util.Arrays;

/** Standalone contract regression for the Control Web/Admin API boundary. */
public final class AdminApiControlContractTest {
    public static void main(String[] args) {
        testStatusCapabilities();
        testRoutePaths();
        testRuneJsonSchema();
        testMechanicsBadRequestDiagnostics();
        System.out.println("Admin API Control contract regression passed");
    }

    private static void testStatusCapabilities() {
        String full = AdminApiServer.capabilitiesJson(true, true, true);
        for (String capability : new String[] {
                "server.read", "players.read", "resources.read", "resources.write",
                "runes.read", "runes.write", "mobs.read", "mobs.write",
                "stages.read", "stages.write", "progression.read", "progression.write",
                "enhancements.write", "balance.read", "balance.write",
                "mechanics.read", "effects.read"
        }) {
            require(full.contains("\"" + capability + "\""), "missing capability: " + capability);
        }

        String base = AdminApiServer.capabilitiesJson(false, false, false);
        for (String capability : new String[] {
                "server.read", "players.read", "resources.read", "resources.write",
                "runes.read", "runes.write", "mobs.read", "mobs.write", "effects.read"
        }) {
            require(base.contains("\"" + capability + "\""), "missing base capability: " + capability);
        }
        require(!base.contains("\"mechanics.read\""), "mechanics.read advertised without mechanics service");
        require(!base.contains("\"balance.read\""), "balance.read advertised without balance service");
        require(!base.contains("\"stages.read\""), "stages.read advertised without tools service");
    }

    private static void testRoutePaths() {
        require("/api/v1/players/runes".equals(AdminApiRoutes.RUNES_READ_PATH), "Rune read route changed");
        require("/api/v1/players/mechanics".equals(AdminMechanicsRoutes.PATH), "Mechanics route changed");
        require("/api/v1/players/mechanics/simulate-time".equals(AdminMechanicsRoutes.TIME_SIMULATION_PATH),
                "Time simulation route changed");
        require("/api/v1/players/effects".equals(AdminEffectsRoutes.PATH), "Effects route changed");
    }

    private static void testRuneJsonSchema() {
        AdminRuneSnapshot.Rune rune = new AdminRuneSnapshot.Rune(2, 7, 3, 11);
        AdminRuneSnapshot.Collection collection = new AdminRuneSnapshot.Collection(
                0, "Main", true, Arrays.asList(rune));
        AdminRuneSnapshot snapshot = new AdminRuneSnapshot(
                "00000000-0000-0000-0000-000000000001", 0, Arrays.asList(collection));
        String json = AdminApiJson.runes(snapshot);

        require(json.contains("\"schemaVersion\":1"), "Rune schemaVersion missing");
        require(json.contains("\"playerUuid\":\"00000000-0000-0000-0000-000000000001\""), "Rune playerUuid missing");
        require(json.contains("\"activeCollection\":0"), "Rune activeCollection missing");
        require(json.contains("\"index\":0"), "Rune collection index missing");
        require(json.contains("\"name\":\"Main\""), "Rune collection name missing");
        require(json.contains("\"active\":true"), "Rune active flag missing");
        require(json.contains("\"slot\":2"), "Rune slot missing");
        require(json.contains("\"runeIndex\":7"), "Rune index missing");
        require(json.contains("\"layer\":3"), "Rune layer missing");
        require(json.contains("\"level\":11"), "Rune level missing");
    }

    private static void testMechanicsBadRequestDiagnostics() {
        String payload = AdminMechanicsRoutes.badRequestPayload(new IllegalArgumentException("GOD_1 linkedGodId is required"));
        require(payload.contains("\"error\":\"bad_request\""), "Mechanics bad request error code changed");
        require(payload.contains("\"message\":\"GOD_1 linkedGodId is required\""), "Mechanics validation reason missing");

        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 200; i++) longMessage.append('x');
        String safe = AdminMechanicsRoutes.safeBadRequestMessage(longMessage.toString());
        require(safe != null && safe.length() == 160, "Mechanics validation reason must be bounded");
        require("bad value".equals(AdminMechanicsRoutes.safeBadRequestMessage("bad\nvalue")),
                "Mechanics validation reason must normalize control characters");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
