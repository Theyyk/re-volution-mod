package com.example.revolution.resource.service;

import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.resource.definition.ResourceDefinitions;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class ResourceActionFoundationTest {
    private ResourceActionFoundationTest() {}

    public static void main(String[] args) throws Exception {
        fixedResourceCatalogIsClosed();
        retiredLightningNameIsBlockedByPolicy();
        sharedResourceActionApiExists();
        legacyLightningPlayerApiIsGone();
        dynamicResourceMutationApiIsGone();
        System.out.println("ResourceActionFoundationTest passed");
    }

    private static void fixedResourceCatalogIsClosed() {
        List<String> expected = Arrays.asList(
                "coins", "crystals",
                ResourceDefinitions.RUNE_DUST,
                ResourceDefinitions.RUNE_SHARDS,
                ResourceDefinitions.RUNE_CORES,
                ResourceDefinitions.AURA_KEY,
                ResourceDefinitions.PEARL
        );
        assertEquals(expected, ResourceDefinitions.getResourceNames(), "fixed resource catalog");
        assertEquals("coins", ResourceDefinitions.findCanonicalName("COINS"), "currency lookup must remain case-insensitive");
        assertEquals(ResourceDefinitions.RUNE_DUST, ResourceDefinitions.findCanonicalName("RUNE_DUST"), "rune resource lookup must remain case-insensitive");
        assertEquals(ResourceDefinitions.AURA_KEY, ResourceDefinitions.findCanonicalName("AURA_KEY"), "Aura Key lookup must remain case-insensitive");
        assertEquals(ResourceDefinitions.PEARL, ResourceDefinitions.findCanonicalName("PEARL"), "Pearl lookup must remain case-insensitive");
        assertEquals(null, ResourceDefinitions.findCanonicalName("custom_resource"), "unknown custom resources must be rejected");
    }

    private static void retiredLightningNameIsBlockedByPolicy() {
        assertTrue(ResourceDefinitions.isRetiredResource("lightnings"), "lightnings must be retired");
        assertTrue(ResourceDefinitions.isRetiredResource("LIGHTNINGS"), "retired resource check must be case-insensitive");
        assertFalse(ResourceDefinitions.isRetiredResource("rune_dust"), "rune_dust must remain active");
        assertFalse(ResourceDefinitions.isRetiredResource("aura_key"), "Aura Key must remain active");
        assertFalse(ResourceDefinitions.isRetiredResource("pearl"), "Pearl must remain active");
        assertEquals(null, ResourceDefinitions.findCanonicalName("lightnings"), "lightnings must not resolve as a resource");
        assertEquals("Пыль рун", ResourceDefinitions.getDisplayName(ResourceDefinitions.RUNE_DUST), "rune dust display name");
        assertEquals("Осколки рун", ResourceDefinitions.getDisplayName(ResourceDefinitions.RUNE_SHARDS), "rune shards display name");
        assertEquals("Ядра рун", ResourceDefinitions.getDisplayName(ResourceDefinitions.RUNE_CORES), "rune cores display name");
        assertEquals("Ключ Ауры", ResourceDefinitions.getDisplayName(ResourceDefinitions.AURA_KEY), "Aura Key display name");
        assertEquals("Жемчуг", ResourceDefinitions.getDisplayName(ResourceDefinitions.PEARL), "Pearl display name");
    }

    private static void sharedResourceActionApiExists() throws Exception {
        Method canAfford = ResourceService.class.getMethod("canAfford", UUID.class, String.class, int.class);
        Method spend = ResourceService.class.getMethod("spendPlayerResource", UUID.class, String.class, int.class);
        Method grant = ResourceService.class.getMethod("grantPlayerResource", UUID.class, String.class, int.class);

        assertEquals(boolean.class, canAfford.getReturnType(), "canAfford return type");
        assertEquals(boolean.class, spend.getReturnType(), "spendPlayerResource return type");
        assertEquals(boolean.class, grant.getReturnType(), "grantPlayerResource return type");
    }

    private static void legacyLightningPlayerApiIsGone() {
        for (Method method : PlayerRepository.class.getDeclaredMethods()) {
            String name = method.getName();
            if ("getLightnings".equals(name) || "setLightnings".equals(name)) {
                throw new AssertionError("Legacy lightning player API must be removed: " + name);
            }
        }
    }

    private static void dynamicResourceMutationApiIsGone() {
        for (Method method : ResourceService.class.getDeclaredMethods()) {
            String name = method.getName();
            if ("createResource".equals(name) || "deleteResource".equals(name)) {
                throw new AssertionError("Dynamic resource mutation API must be removed: " + name);
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
