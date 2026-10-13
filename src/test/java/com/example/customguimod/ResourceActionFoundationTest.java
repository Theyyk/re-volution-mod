package com.example.customguimod;

import java.lang.reflect.Method;
import java.util.UUID;

public final class ResourceActionFoundationTest {
    private ResourceActionFoundationTest() {}

    public static void main(String[] args) throws Exception {
        retiredLightningNameIsBlockedByPolicy();
        sharedResourceActionApiExists();
        legacyLightningPlayerApiIsGone();
        System.out.println("ResourceActionFoundationTest passed");
    }

    private static void retiredLightningNameIsBlockedByPolicy() {
        assertTrue(ResourceManager.isRetiredResource("lightnings"), "lightnings must be retired");
        assertTrue(ResourceManager.isRetiredResource("LIGHTNINGS"), "retired resource check must be case-insensitive");
        assertFalse(ResourceManager.isRetiredResource("rune_dust"), "rune_dust must remain active");
        assertEquals("Пыль рун", ResourceManager.getDisplayName(ResourceManager.RUNE_DUST), "rune dust display name");
        assertEquals("Осколки рун", ResourceManager.getDisplayName(ResourceManager.RUNE_SHARDS), "rune shards display name");
        assertEquals("Ядра рун", ResourceManager.getDisplayName(ResourceManager.RUNE_CORES), "rune cores display name");
    }

    private static void sharedResourceActionApiExists() throws Exception {
        Method canAfford = ResourceManager.class.getMethod("canAfford", UUID.class, String.class, int.class);
        Method spend = ResourceManager.class.getMethod("spendPlayerResource", UUID.class, String.class, int.class);
        Method grant = ResourceManager.class.getMethod("grantPlayerResource", UUID.class, String.class, int.class);

        assertEquals(boolean.class, canAfford.getReturnType(), "canAfford return type");
        assertEquals(boolean.class, spend.getReturnType(), "spendPlayerResource return type");
        assertEquals(boolean.class, grant.getReturnType(), "grantPlayerResource return type");
    }

    private static void legacyLightningPlayerApiIsGone() {
        for (Method method : MongoManager.class.getDeclaredMethods()) {
            String name = method.getName();
            if ("getLightnings".equals(name) || "setLightnings".equals(name)) {
                throw new AssertionError("Legacy lightning player API must be removed: " + name);
            }
        }
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void assertFalse(boolean value, String message) {
        if (value) throw new AssertionError(message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }
}
