"""Temporary helper for the v1.2.12 -> v1.3.0 rune/client package split.

The script is intentionally mechanical: it moves the agreed classes, updates package
statements, rewrites old fully-qualified references and adds imports for moved types.
Only the types that form real cross-package boundaries are promoted to public; the rest
of the implementation keeps its existing visibility so the migration does not become a
blanket public-API expansion.
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

PUBLIC_TYPES = {
    "PrimaryRuneCatalog",
    "RuneAwakeningDecision",
    "RuneAwakeningPaymentService",
    "RuneCombatMath",
    "RuneCombatService",
    "RuneCompositeEffect",
    "RuneEconomyConfig",
    "RuneEnhancementManager",
    "RuneInventory",
    "RuneProgression",
    "RuneStat",
    "RuneStatDisplay",
    "RuneStatType",
    "RuneStatusRegistry",
    "RuneTargetedPower",
    "GuiFontRenderer",
    "RuneTexturePreloader",
}

# Existing root helpers referenced from the new rune package need explicit imports after moving.
ROOT_IMPORTS = {
    "RuneMobEffects": ["MobRewardBonus"],
    "RuneRewardBonusService": ["MobRewardBonus"],
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


def promote_top_level_type(text, class_name):
    if class_name not in PUBLIC_TYPES:
        return text
    patterns = [
        (r"(?m)^(final\s+class\s+%s\b)" % re.escape(class_name), r"public \1"),
        (r"(?m)^(class\s+%s\b)" % re.escape(class_name), r"public \1"),
        (r"(?m)^(enum\s+%s\b)" % re.escape(class_name), r"public \1"),
        (r"(?m)^(interface\s+%s\b)" % re.escape(class_name), r"public \1"),
    ]
    for pattern, replacement in patterns:
        updated, count = re.subn(pattern, replacement, text, count=1)
        if count:
            return updated
    return text


def promote_boundary_members(text, class_name):
    """Expose only direct members of explicitly selected boundary types.

    The old flat package relied on package-private access. After the split those direct members
    become the narrow Java boundary. Private implementation remains private; nested member
    internals are not widened here unless the nested type itself is part of the exposed model.
    """
    if class_name not in PUBLIC_TYPES:
        return text
    out = []
    for line in text.splitlines(True):
        if not line.startswith("    ") or line.startswith("        "):
            out.append(line)
            continue
        stripped = line[4:]
        if not stripped or stripped.startswith(("public ", "private ", "protected ", "@", "//", "/*", "*", "}", "static {")):
            out.append(line)
            continue
        declaration = False
        if re.match(r"(?:static\s+)?(?:final\s+)?(?:class|enum|interface)\b", stripped):
            declaration = True
        elif ";" in stripped and not stripped.lstrip().startswith(("return ", "throw ")):
            declaration = True
        elif "(" in stripped and (stripped.rstrip().endswith("{") or stripped.rstrip().endswith(";")):
            declaration = True
        if declaration:
            line = "    public " + stripped
        out.append(line)
    return "".join(out)


def add_named_import(text, fqcn):
    if fqcn in IMPORT_RE.findall(text):
        return text
    package_match = PKG_RE.search(text)
    insert_at = package_match.end()
    return text[:insert_at] + "\n\nimport " + fqcn + ";" + text[insert_at:]


def add_imports(text, package, own_name):
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
    for root_name in ROOT_IMPORTS.get(own_name, []):
        fqcn = BASE + "." + root_name
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
        text = promote_top_level_type(text, class_name)
        text = promote_boundary_members(text, class_name)
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
