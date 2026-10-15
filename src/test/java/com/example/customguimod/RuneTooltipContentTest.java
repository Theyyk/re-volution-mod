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
        check(single.size() == 6, "Single-stat tooltip should keep the effect value on its own line");
        check(single.get(0).equals("§bКинжал убийцы"), "Tooltip should start with the rune name");
        check(single.get(1).equals("§7Ранг: §fУлучшенная"), "Rank should follow the name");
        check(single.get(2).equals("§7Тип: §fКлик"), "Type should follow the rank");
        check(single.get(3).equals("§7Улучшение: §f+5"), "Enhancement should precede effects");
        check(single.get(4).equals("§7Эффекты:"), "Single-stat tooltip needs the same effects heading as multi-stat tooltips");
        check(single.get(5).equals("§fУрон Клика +4.5"),
                "Single final stat should render below the effects heading");

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

        List<String> resource = RuneTooltipContent.lines(
                EarthRuneCatalog.at(25),
                RuneInventory.NORMAL_RANK,
                4,
                1.4D
        );
        check(resource.size() == 6, "Resource rune should expose its stat in the standard tooltip layout");
        check(resource.get(2).equals("§7Тип: §fРесурс"), "Resource rune type should remain visible");
        check(resource.get(5).equals("§fДобыча монет +1.4%"),
                "Hunter relic should render a coin-specific percent final value");

        EarthRuneCatalog.Definition noStatDefinition = new EarthRuneCatalog.Definition(
                "Без эффекта", "Тест", "hunter_relic.png"
        );
        List<String> noStats = RuneTooltipContent.lines(
                noStatDefinition,
                RuneInventory.NORMAL_RANK,
                -4,
                1.0D
        );
        check(noStats.size() == 4, "A definition without stats should omit the effects section");
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
