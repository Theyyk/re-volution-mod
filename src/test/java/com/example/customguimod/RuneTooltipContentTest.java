package com.example.customguimod;

import java.util.List;

final class RuneTooltipContentTest {
    static void run() {
        List<String> single = RuneTooltipContent.lines(
                EarthRuneCatalog.at(0),
                RuneInventory.IMPROVED_RANK,
                5,
                1.5D
        );
        check(single.size() == 5, "Single-stat tooltip should stay compact");
        check(single.get(0).equals("§bКинжал убийцы"), "Tooltip should start with the rune name");
        check(single.get(1).equals("§7Ранг: §fУлучшенная"), "Rank should follow the name");
        check(single.get(2).equals("§7Тип: §fКлик"), "Type should follow the rank");
        check(single.get(3).equals("§7Улучшение: §f+5"), "Enhancement should precede effects");
        check(single.get(4).equals("§7Эффекты: §fУрон Клика +4.5"),
                "Single final stat should remain on the effects line");

        List<String> hybrid = RuneTooltipContent.lines(
                EarthRuneCatalog.at(11),
                RuneInventory.IMPROVED_RANK,
                10,
                2.0D
        );
        check(hybrid.size() == 7, "Multi-stat tooltip should render effects vertically");
        check(hybrid.get(4).equals("§7Эффекты:"), "Multi-stat tooltip needs an effects heading");
        check(hybrid.get(5).equals("§fУрон Клика +6"), "Click final value should be formatted through RuneStatDisplay");
        check(hybrid.get(6).equals("§fУрон Яда +6"), "Poison final value should be formatted through RuneStatDisplay");

        List<String> noStats = RuneTooltipContent.lines(
                EarthRuneCatalog.at(25),
                RuneInventory.NORMAL_RANK,
                -4,
                1.0D
        );
        check(noStats.size() == 4, "A rune without stats should omit the effects section");
        check(noStats.get(3).equals("§7Улучшение: §f+0"), "Negative enhancement input must not leak into the tooltip");
        check(RuneTooltipContent.lines(null, 1, 0, 1.0D).isEmpty(), "Missing definitions should produce no tooltip");

        System.out.println("Rune tooltip content regression checks passed");
    }

    public static void main(String[] args) {
        run();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
