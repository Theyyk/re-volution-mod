package com.example.revolution.resource.service;

import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.resource.repository.ResourceRepository;
import com.example.revolution.rune.definition.RuneRankUpRule;
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

        RuneAwakeningPaymentService payment = new RuneAwakeningPaymentService(service);
        RuneRankUpRule paidRule = new RuneRankUpRule(
                1,
                2,
                26,
                ResourceDefinitions.RUNE_DUST,
                40,
                RuneRankUpRule.ClearMode.AT_OR_BELOW_SOURCE,
                true);
        check(!payment.charge(player, paidRule).allowed, "insufficient awakening payment");
        service.setPlayerResource(player, "rune_dust", 50);
        RuneAwakeningPaymentService.Result result = payment.charge(player, paidRule);
        check(result.allowed && result.paid && result.cost == 40 && result.balance == 10, "awakening charge contract");
        RuneRankUpRule invalidRule = new RuneRankUpRule(
                1,
                2,
                26,
                "",
                40,
                RuneRankUpRule.ClearMode.AT_OR_BELOW_SOURCE,
                true);
        check(payment.charge(player, invalidRule).invalidConfiguration, "reject invalid resolved payment rule");
        check(balances.get("rune_dust").intValue() == 10, "invalid payment never writes");

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
