package com.example.customguimod;

import java.util.List;

final class EarthRuneCatalogTest {
    static void run() {
        check(EarthRuneCatalog.size() == 26, "The catalog must define all 26 positions");
        String[] expected = {"Кинжал убийцы", "Кинжал убийцы", "Кинжал убийцы", "Кинжал убийцы",
                "Накидка убийцы", "Накидка убийцы", "Накидка убийцы", "Перчатка скрытого удара", "Камень заточки", "Камень заточки",
                "Амулет ярости", "Токсичный осколок", "Токсичный осколок",
                "Клык змеи", "Клык змеи", "Клык змеи", "Клык змеи",
                "Печать заражения", "Печать заражения", "Печать заражения", "Усилитель токсина", "Усилитель токсина",
                "Метка заражения", "Метка заражения", "Метка заражения", "Реликвия охотника"};
        java.util.Set<String> textures = new java.util.HashSet<>();
        RuneInventory inventory = new RuneInventory();
        for (int slot = 0; slot < expected.length; slot++) {
            EarthRuneCatalog.Definition definition = EarthRuneCatalog.at(slot);
            check(definition.name.equals(expected[slot]), "Catalog order differs at " + slot);
            check(!definition.type.isEmpty(), "Every position needs a type");
            check(definition.stats.length <= 3, "Every rune must respect the three-stat limit");
            check(EarthRuneCatalog.iconIndex(slot) >= 0 && EarthRuneCatalog.iconIndex(slot) <= 10, "Every rune needs a placeholder icon");
            String texture = definition.iconTexture;
            check(texture.startsWith("customguimod:textures/gui/runes/") && texture.endsWith(".png"), "PNG path required");
            textures.add(texture);
            if (slot > 0 && expected[slot].equals(expected[slot - 1]))
                check(texture.equals(EarthRuneCatalog.at(slot - 1).iconTexture), "Duplicates must share PNG paths");
            inventory.set(slot, slot, slot % 3 + 1);
        }
        check(textures.size() == 11, "Exactly eleven unique PNG paths required");
        check(inventory.ownedCatalogSlotCount() == 26, "Repeated names must occupy distinct positions");
        check(inventory.get(0).layer == 1 && inventory.get(1).layer == 2,
                "Repeated runes must keep their own ownership and ranks");
        check(inventory.get(25).cardIndex == 25, "The last rune must remain a distinct purchased position");
        check(EarthRuneCatalog.iconIndex(0) == EarthRuneCatalog.iconIndex(3), "Repeated runes reuse the same item icon");
        check(EarthRuneCatalog.iconIndex(25) == 10, "Resource rune needs its own icon");
        inventory.clear();
        check(inventory.ownedCatalogSlotCount() == 0, "Changing statuses must clear owned positions");
        check(EarthRuneCatalog.at(0).name.equals("Кинжал убийцы"), "Definitions must remain visible without ownership");
        check(EarthRuneCatalog.at(11).type.equals("Клик / Яд"), "Hybrid rune type must combine click and poison");
        check(EarthRuneCatalog.at(11).stats.length == 2, "Hybrid rune should expose both click and poison stats");
        check(EarthRuneCatalog.at(11).stats[0].type == RuneStatType.CLICK_DAMAGE_FLAT,
                "Hybrid rune should expose click damage through RuneStatType");
        check(EarthRuneCatalog.at(11).stats[1].type == RuneStatType.POISON_DAMAGE_FLAT,
                "Hybrid rune should expose poison damage through RuneStatType");
        check(EarthRuneCatalog.at(17).type.equals("Яд"), "Poison rune type must remain poison");
        check(EarthRuneCatalog.at(17).stats.length == 1
                        && EarthRuneCatalog.at(17).stats[0].type == RuneStatType.POISON_DAMAGE_FLAT,
                "Poison rune should use the data-driven stat model");
        check(EarthRuneCatalog.at(25).type.equals("Ресурс"), "Hunter relic belongs to resources");
        check(EarthRuneCatalog.at(25).stats.length == 1,
                "Hunter relic should expose its v1.2.10 coin stat");
        check(EarthRuneCatalog.at(25).stats[0].type == RuneStatType.COIN_REWARD_PERCENT,
                "Hunter relic should use the coin reward stat type");
        check(RuneStatType.COIN_REWARD_PERCENT != RuneStatType.RESOURCE_REWARD_PERCENT,
                "Coin reward and rune-resource reward must remain independent stats");
        check(RuneStatType.COIN_REWARD_PERCENT.displayName.equals("Добыча монет"),
                "Coin reward must have its own player-facing label");
        check(RuneStatType.RESOURCE_REWARD_PERCENT.displayName.equals("Добыча ресурсов рун"),
                "Rune-resource reward must have its own player-facing label");
        check(RuneStatType.COIN_REWARD_PERCENT.format(1.25D).equals("+1.25%"),
                "Coin percent stat should use percent formatting");
        check(RuneStatType.RESOURCE_REWARD_PERCENT.format(1.25D).equals("+1.25%"),
                "Rune-resource percent stat should use percent formatting");

        RuneStat sample = RuneStat.scaled(RuneStatType.CLICK_DAMAGE_FLAT, 2.0D);
        RuneStat.Breakdown normal = sample.resolve(RuneInventory.NORMAL_RANK, 1.5D);
        check(close(normal.baseValue, 2.0D), "Normal stat base value must stay explicit");
        check(close(normal.awakeningBonus, 0.0D), "Normal rank has no awakening bonus");
        check(close(normal.enhancementBonus, 1.0D), "Enhancement bonus must be tracked separately");
        check(close(normal.otherBonus, 0.0D), "Other bonus is reserved but zero");
        check(close(normal.finalValue, 3.0D), "Final value must include enhancement");

        RuneStat.Breakdown improved = sample.resolve(RuneInventory.IMPROVED_RANK, 1.5D);
        check(close(improved.baseValue, 2.0D), "Improved stat keeps the same base value");
        check(close(improved.awakeningBonus, 4.0D), "Improved rank must expose its awakening contribution");
        check(close(improved.enhancementBonus, 3.0D), "Enhancement must apply after awakening");
        check(close(improved.finalValue, 9.0D), "Final value must combine awakening and enhancement");

        List<String> compactLines = RuneStatDisplay.finalValueLines(
                EarthRuneCatalog.at(11).stats,
                RuneInventory.IMPROVED_RANK,
                1.5D
        );
        check(compactLines.size() == 2, "Hybrid rune tooltip should expose two compact stat lines");
        check(compactLines.get(0).equals("Урон Клика +4.5"), "Compact tooltip must show final click value only");
        check(compactLines.get(1).equals("Урон Яда +4.5"), "Compact tooltip must show final poison value only");

        List<String> coinLines = RuneStatDisplay.finalValueLines(
                EarthRuneCatalog.at(25).stats,
                RuneInventory.IMPROVED_RANK,
                1.5D
        );
        check(coinLines.size() == 1, "Coin rune should expose one final stat line");
        check(coinLines.get(0).equals("Добыча монет +4.5%"),
                "Hunter relic final value should be explicitly coin-specific");

        RuneStat runeResourceSample = RuneStat.scaled(RuneStatType.RESOURCE_REWARD_PERCENT, 1.0D);
        List<String> runeResourceLines = RuneStatDisplay.finalValueLines(
                new RuneStat[]{runeResourceSample},
                RuneInventory.IMPROVED_RANK,
                1.5D
        );
        check(runeResourceLines.size() == 1, "Rune-resource stat should render independently");
        check(runeResourceLines.get(0).equals("Добыча ресурсов рун +4.5%"),
                "Rune-resource stat must not be conflated with coin reward");

        System.out.println("Fixed 26-position poison status catalog and v1.2.10 stat content regression checks passed");
    }

    public static void main(String[] args) {
        run();
    }

    private static boolean close(double left, double right) {
        return Math.abs(left - right) < 0.000001D;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
