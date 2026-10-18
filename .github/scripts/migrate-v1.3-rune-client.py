"""Temporary helper for the v1.2.12 -> v1.3.0 rune/client package split.

The script is intentionally mechanical: it moves the agreed classes, updates package
statements, rewrites old fully-qualified references and adds imports for moved types.
Visibility is NOT widened here; compile failures are used to identify real package
boundaries that need an explicit API instead of blanket public modifiers.
"""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
MAIN_ROOT = ROOT / "src/main/java"
TEST_ROOT = ROOT / "src/test/java"
BASE = "com.example.customguimod"

RUNE = {
    "PrimaryRuneCatalog",
    "RuneAwakeningDecision",
    "RuneAwakeningPaymentService",
    "RuneCombatMath",
    "RuneCombatService",
    "RuneCompositeEffect",
    "RuneEconomyConfig",
    "RuneEnhancementManager",
    "RuneInventory",
    "RuneMobEffects",
    "RuneProgression",
    "RuneRewardBonusService",
    "RuneStat",
    "RuneStatDisplay",
    "RuneStatType",
    "RuneStatusRegistry",
    "RuneStorageRules",
    "RuneTargetedPower",
}

# Keep the coupled client GUI classes together. This creates one client boundary instead of
# replacing the old root-package coupling with multiple client subpackages and public helpers.
CLIENT = {
    "AdminPanelScreen",
    "AdminPanelStateClientHandler",
    "ClientPacketHandler",
    "ClientPlayerStats",
    "ClientRuneCombatPreview",
    "ClientRuneEnhancements",
    "ClientRuneRanks",
    "GuiControls",
    "GuiDrawing",
    "GuiFontRenderer",
    "GuiLayout",
    "GuiTheme",
    "KeyInputHandler",
    "PlayerStatsPacketHandler",
    "PoisonStatsUiHandler",
    "RuneAwakeningInput",
    "RuneCollectionSyncState",
    "RuneCollectionsPacketHandler",
    "RuneEnhancementPacketHandler",
    "RuneGuiPreloader",
    "RunePurchaseUiHandler",
    "RuneResourcesGuiScreen",
    "RuneSlotPacketHandler",
    "RuneStatusChatFilter",
    "RuneTexturePreloader",
    "RuneTooltipContent",
    "RuneTooltipRenderer",
    "RuneUxUiHandler",
    "RunesGuiScreen",
}

NETWORK = {
    "RuneCollectionsPacket",
    "RuneCollectionsPacketServerHandler",
    "RuneEnhancementPacket",
    "RuneEnhancementPacketServerHandler",
}

MOVE = {}
for name in RUNE:
    MOVE[name] = BASE + ".rune"
for name in CLIENT:
    MOVE[name] = BASE + ".client"
for name in NETWORK:
    MOVE[name] = BASE + ".network"

TEST_MOVE = {
    "PrimaryRuneCatalogTest": BASE + ".rune",
    "RuneCombatMathTest": BASE + ".rune",
    "RuneEconomyConfigTest": BASE + ".rune",
    "RuneInventoryTest": BASE + ".rune",
    "RuneProgressionTest": BASE + ".rune",
    "RuneStatContentTest": BASE + ".rune",
    "MultiStatusRuneEconomyTest": BASE + ".rune",
    "GuiLayoutTest": BASE + ".client",
    "RuneAwakeningInputTest": BASE + ".client",
    "RuneEnhancementPacketTest": BASE + ".client",
}

PKG_RE = re.compile(r"(?m)^package\s+([\w.]+);\s*$")
IMPORT_RE = re.compile(r"(?m)^import\s+([\w.]+);\s*$")


def path_for(root, package, filename):
    return root / Path(package.replace(".", "/")) / filename


def package_of(text):
    match = PKG_RE.search(text)
    if not match:
        raise RuntimeError("Java source without package declaration")
    return match.group(1)


def replace_package(text, package):
    return PKG_RE.sub("package %s;" % package, text, count=1)


def add_imports(text, package, own_name):
    # Replace explicit root-qualified references/imports first.
    for name, target in MOVE.items():
        text = text.replace(BASE + "." + name, target + "." + name)

    existing = set(IMPORT_RE.findall(text))
    additions = []
    for name, target in sorted(MOVE.items()):
        if name == own_name or target == package:
            continue
        if not re.search(r"\b%s\b" % re.escape(name), text):
            continue
        fqcn = target + "." + name
        if fqcn not in existing:
            additions.append("import %s;" % fqcn)
            existing.add(fqcn)
    if not additions:
        return text
    package_match = PKG_RE.search(text)
    insert_at = package_match.end()
    return text[:insert_at] + "\n\n" + "\n".join(additions) + text[insert_at:]


def migrate_root_sources(root, mapping):
    source_dir = root / Path(BASE.replace(".", "/"))
    for class_name, target_package in sorted(mapping.items()):
        old = source_dir / (class_name + ".java")
        if not old.is_file():
            continue
        text = old.read_text(encoding="utf-8")
        text = replace_package(text, target_package)
        new = path_for(root, target_package, old.name)
        new.parent.mkdir(parents=True, exist_ok=True)
        new.write_text(text, encoding="utf-8")
        old.unlink()


def rewrite_all_java(root):
    for path in sorted(root.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        pkg = package_of(text)
        own_name = path.stem
        updated = add_imports(text, pkg, own_name)
        if updated != text:
            path.write_text(updated, encoding="utf-8")


def update_gradle_test_entry_points():
    gradle = ROOT / "build.gradle"
    text = gradle.read_text(encoding="utf-8")
    for name, package in TEST_MOVE.items():
        text = text.replace(BASE + "." + name, package + "." + name)
    gradle.write_text(text, encoding="utf-8")


def main():
    migrate_root_sources(MAIN_ROOT, MOVE)
    migrate_root_sources(TEST_ROOT, TEST_MOVE)
    rewrite_all_java(MAIN_ROOT)
    rewrite_all_java(TEST_ROOT)
    update_gradle_test_entry_points()
    print("Prepared v1.3 rune/client package split for compile validation.")


if __name__ == "__main__":
    main()
