package com.example.revolution.rune.service;

import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.player.service.PlayerStatsService;
import com.example.revolution.rune.definition.RuneDamageChannel;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.rules.RuneDamageMath;
import com.example.revolution.rune.state.RuneCombatState;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Pins the distinct global combat/active-collection reward contracts after dependency injection. */
public final class RuneCombatServiceTest {
    public static void main(String[] args) {
        UUID player = UUID.randomUUID();
        int[] activeCollection = {0};
        int[] collectionCount = {2};
        List<RuneRecord> first = Arrays.asList(new RuneRecord(0, 0, 1, 1), new RuneRecord(10, 10, 1, 1), new RuneRecord(25, 25, 1, 1));
        List<RuneRecord> second = Arrays.asList(new RuneRecord(0, 0, 2, 1), new RuneRecord(25, 25, 2, 1));
        PlayerRepository players = proxy(PlayerRepository.class, (target, method, values) -> {
            switch (method.getName()) {
                case "getCollectionNames": return collectionCount[0] == 1
                        ? Arrays.asList("First") : Arrays.asList("First", "Second");
                case "getActiveCollection": return activeCollection[0];
                case "getRunesInCollection": return (int) values[1] == 0 ? first : second;
                default: throw new AssertionError(method);
            }
        });
        RuneEnhancementRepository enhancements = proxy(RuneEnhancementRepository.class, (target, method, values) -> {
            if (!method.getName().equals("getLevels")) throw new AssertionError(method);
            int[] levels = new int[26];
            if ((int) values[1] == 0) levels[0] = 2;
            return levels;
        });
        String key = "revolution.runes.deck.1.status";
        String previous = System.getProperty(key);
        try {
            System.setProperty(key, "1");
            RuneCombatState state = new RuneCombatState();
            RuneCombatService combat = new RuneCombatService(players, enhancements, state);

            double clickWithBothCollections = combat.resolveStable(player).clickFlat;
            RuneDamageMath.Snapshot damageWithBothCollections = combat.resolveDamageStable(player);
            collectionCount[0] = 1;
            double clickWithFirstCollection = combat.resolveStable(player).clickFlat;
            RuneDamageMath.Snapshot damageWithFirstCollection = combat.resolveDamageStable(player);

            check(close(clickWithBothCollections, clickWithFirstCollection),
                    "C2 POISON power does not masquerade as C1 CLICK flat damage");
            check(close(
                    damageWithBothCollections.channel(RuneDamageChannel.POISON).flat
                            - damageWithFirstCollection.channel(RuneDamageChannel.POISON).flat,
                    30.0D),
                    "C2 Improved slot 0 contributes its fixed POISON flat value");
            check(close(
                    damageWithBothCollections.channel(RuneDamageChannel.POISON).multiplier
                            / damageWithFirstCollection.channel(RuneDamageChannel.POISON).multiplier,
                    1.15D),
                    "C2 Improved capstone contributes its fixed POISON multiplier");
            check(close(
                    damageWithBothCollections.allDamageMultiplier
                            / damageWithFirstCollection.allDamageMultiplier,
                    1.05D),
                    "C2 Improved capstone contributes its fixed all-damage multiplier");

            collectionCount[0] = 2;
            activeCollection[0] = 1;
            check(close(combat.resolveStable(player).clickFlat, clickWithBothCollections),
                    "viewed collection does not toggle owned combat power");
            PlayerStatsService stats = new PlayerStatsService(players, enhancements);
            check(close(stats.getTotalDamageBonusValue(player), 9.2D), "legacy additive player-stat fallback remains separate");
            RuneRewardBonusService rewards = new RuneRewardBonusService(players, enhancements);
            check(close(rewards.percentFor(player, "coins"), 25.0D), "reward uses active collection ZBT relic value");
            activeCollection[0] = 0;
            check(close(rewards.percentFor(player, "coins"), 15.0D), "reward switches with active collection");
            check(rewards.percentFor(player, "crystals") == 0.0D, "crystals excluded from rune reward bonuses");
            check(!combat.resolve(player, 100L).rageActive, "triggering hit starts without rage");
            combat.activateRageOnMobHit(player, 100L);
            check(combat.resolve(player, 179L).rageActive && !combat.resolve(player, 180L).rageActive,
                    "Normal rage lasts four seconds and refreshes from hit activation");
            combat.activateRageOnMobHit(player, 300L);
            RuneCombatService nextServer = new RuneCombatService(players, enhancements, new RuneCombatState());
            check(!nextServer.resolve(player, 301L).rageActive, "separate server lifetimes have separate rage");
            state.clear();
            check(!combat.resolve(player, 301L).rageActive, "shutdown drops transient effects");
        } finally {
            if (previous == null) System.clearProperty(key); else System.setProperty(key, previous);
        }
        System.out.println("Fixed-channel rune combat, active-collection rewards and rage lifetime passed");
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
    private static boolean close(double left, double right) { return Math.abs(left - right) < 0.000001D; }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
