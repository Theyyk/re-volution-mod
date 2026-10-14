package com.example.customguimod;

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
        check(EarthRuneCatalog.at(11).stats.length == 2, "Hybrid rune should demonstrate multiple stats in v1.2.9");
        check(EarthRuneCatalog.at(11).stats[0].type == RuneStatType.CLICK_DAMAGE_FLAT,
                "Hybrid rune should expose click damage through RuneStatType");
        check(EarthRuneCatalog.at(11).stats[1].type == RuneStatType.POISON_DAMAGE_FLAT,
                "Hybrid rune should expose poison damage through RuneStatType");
        check(EarthRuneCatalog.at(17).type.equals("Яд"), "Poison multiplier belongs to poison damage");
        check(EarthRuneCatalog.at(17).stats.length == 1
                        && EarthRuneCatalog.at(17).stats[0].type == RuneStatType.POISON_DAMAGE_FLAT,
                "Poison rune should use the data-driven stat model");
        check(EarthRuneCatalog.at(25).type.equals("Ресурс"), "Hunter relic belongs to resources");
        check(EarthRuneCatalog.at(25).stats.length == 0,
                "Full resource-stat content remains deferred to v1.2.10");
        System.out.println("Fixed 26-position poison status catalog regression checks passed");
    }

    public static void main(String[] args) {
        run();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
