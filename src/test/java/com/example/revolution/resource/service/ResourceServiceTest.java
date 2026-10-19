package com.example.revolution.resource.service;

import com.example.revolution.resource.repository.ResourceRepository;
import com.example.revolution.rune.service.RuneAwakeningPaymentService;
import com.example.revolution.rune.state.RuneCombatState;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Application economy tested through its persistence port, without Minecraft or MongoDB startup. */
public final class ResourceServiceTest {
    public static void main(String[] args) {
        Map<String, BigDecimal> balances = new HashMap<>();
        ResourceRepository repository = (ResourceRepository) Proxy.newProxyInstance(ResourceRepository.class.getClassLoader(),
                new Class<?>[]{ResourceRepository.class}, (target, method, values) -> {
                    if (method.getName().equals("getPlayerResourceValue")) return balances.getOrDefault(values[1], BigDecimal.ZERO);
                    if (method.getName().equals("writePlayerResource")) { balances.put((String) values[1], (BigDecimal) values[2]); return null; }
                    throw new AssertionError("Unexpected resource operation: " + method.getName());
                });
        ResourceService service = new ResourceService(repository);
        UUID player = UUID.randomUUID();
        check(!service.grantPlayerResource(player, "unknown", 10), "reject unknown grants");
        check(!service.grantPlayerResource(player, "coins", -1), "reject negative grants");
        check(service.grantPlayerResource(player, "coins", new BigDecimal("999999999999999999999")), "exact grant");
        check(service.getPlayerResource(player, "coins") == Integer.MAX_VALUE, "legacy integer read saturates");
        check(service.spendPlayerResource(player, "coins", 5), "spend exact balance");
        check(balances.get("coins").equals(new BigDecimal("999999999999999999994")), "no precision loss");
        service.setPlayerResource(player, "rune_dust", 20);
        check(!service.spendPlayerResource(player, "rune_dust", 21), "reject unaffordable spend");
        check(balances.get("rune_dust").intValue() == 20, "failed spend preserves balance");
        check(!service.spendPlayerResource(player, "rune_dust", -1), "reject negative spend");
        check(service.spendPlayerResource(player, "rune_dust", 20), "allow exact spend");
        check(balances.get("rune_dust").signum() == 0, "spend reaches zero");

        String prefix = "revolution.runes.poison.awakening.";
        try {
            System.setProperty(prefix + "type", "BY_RANK");
            System.setProperty(prefix + "rank.1", "40");
            System.setProperty(prefix + "resource", "rune_dust");
            RuneAwakeningPaymentService payment = new RuneAwakeningPaymentService(service);
            check(!payment.charge(player, 1, 1).allowed, "insufficient awakening payment");
            service.setPlayerResource(player, "rune_dust", 50);
            RuneAwakeningPaymentService.Result result = payment.charge(player, 1, 1);
            check(result.allowed && result.paid && result.cost == 40 && result.balance == 10, "awakening charge contract");
            System.setProperty(prefix + "resource", "unknown");
            check(payment.charge(player, 1, 1).invalidConfiguration, "reject invalid payment resource");
            check(balances.get("rune_dust").intValue() == 10, "invalid payment never writes");
        } finally {
            for (String suffix : new String[]{"type", "rank.1", "resource"}) System.clearProperty(prefix + suffix);
        }
        RuneCombatState firstServer = new RuneCombatState();
        RuneCombatState secondServer = new RuneCombatState();
        firstServer.activate(player, 120L);
        check(firstServer.isActive(player, 119L) && !firstServer.isActive(player, 120L), "rage expiry boundary");
        check(!secondServer.isActive(player, 100L), "server state isolation");
        firstServer.activate(player, 200L);
        firstServer.clear();
        check(!firstServer.isActive(player, 150L), "shutdown clears transient combat state");
        System.out.println("Resource actions, awakening payments and server state isolation passed");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
