package com.example.revolution.admin.service;

import com.example.revolution.admin.definition.AdminPlayerSnapshot;
import com.example.revolution.player.repository.PlayerIdentity;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.resource.repository.ResourceRepository;
import com.example.revolution.resource.service.ResourceService;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public final class AdminOfflinePlayerTest {
    @Test
    public void persistedPlayerIsListedOfflineWithCachedNameAndBalances() {
        UUID uuid = UUID.randomUUID();
        PlayerRepository players = proxy(PlayerRepository.class, (method, args) -> {
            if (method.equals("listKnownPlayers")) return Arrays.asList(new PlayerIdentity(uuid, ""));
            if (method.equals("getRunesInCollection")) return Collections.emptyList();
            return defaultValue(method);
        });
        ResourceRepository resourceStore = proxy(ResourceRepository.class, (method, args) ->
                method.equals("getPlayerResourceValue") ? new BigDecimal("1250000") : defaultValue(method));
        AdminQueryService queries = new AdminQueryService(players, new ResourceService(resourceStore), null);

        List<AdminPlayerSnapshot> result = queries.allPlayers(
                Collections.emptyList(), requested -> requested.equals(uuid) ? "OfflineHero" : "");

        assertEquals(1, result.size());
        assertFalse(result.get(0).online);
        assertEquals("OfflineHero", result.get(0).name);
        assertEquals("1250000", result.get(0).balances.get("coins"));
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Handler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (instance, method, args) -> handler.invoke(method.getName(), args));
    }

    private static Object defaultValue(String method) {
        if (method.startsWith("getCollectionNames")) return Collections.emptyList();
        return null;
    }

    private interface Handler { Object invoke(String method, Object[] args) throws Throwable; }
}
