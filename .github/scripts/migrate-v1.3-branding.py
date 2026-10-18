from pathlib import Path
import re
import shutil

ROOT = Path(__file__).resolve().parents[2]

HISTORICAL_PREFIXES = (
    ".github/release-notes/",
    "docs/releases/",
)
HISTORICAL_FILES = {
    "docs/cleanup-v1.3-plan.md",
    "docs/v1.1.4-archive-integration.md",
    ".github/scripts/check-release-notes.py",
    ".github/workflows/v1.3-branding-audit.yml",
    ".github/scripts/migrate-v1.3-branding.py",
    ".github/workflows/v1.3-branding-migration.yml",
}
BINARY_SUFFIXES = {".otf", ".png", ".jar", ".zip", ".class"}


def move_tree(old_rel, new_rel):
    old = ROOT / old_rel
    new = ROOT / new_rel
    if not old.exists():
        return
    new.parent.mkdir(parents=True, exist_ok=True)
    if new.exists():
        raise RuntimeError("Target already exists: %s" % new_rel)
    shutil.move(str(old), str(new))


def rename_file(old_rel, new_rel):
    old = ROOT / old_rel
    new = ROOT / new_rel
    if not old.exists():
        return
    new.parent.mkdir(parents=True, exist_ok=True)
    old.rename(new)


def is_historical(rel):
    rel = rel.as_posix()
    return rel in HISTORICAL_FILES or any(rel.startswith(prefix) for prefix in HISTORICAL_PREFIXES)


def rewrite_current_text():
    replacements = (
        ("com.example.customguimod", "com.example.revolution"),
        ("CustomGuiMod", "RevolutionMod"),
        ("Custom GUI Mod", "RE:volution"),
        ("customgui:main", "revolution:main"),
        ("assets/customguimod", "assets/revolution"),
        ("customguimod:", "revolution:"),
        ("key.categories.customguimod", "key.categories.revolution"),
        ("key.customguimod", "key.revolution"),
        ("customguimod-", "RE-volution-"),
        ("customguimod", "revolution"),
    )
    for path in ROOT.rglob("*"):
        if not path.is_file() or ".git" in path.parts or path.suffix.lower() in BINARY_SUFFIXES:
            continue
        rel = path.relative_to(ROOT)
        if is_historical(rel):
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        updated = text
        for old, new in replacements:
            updated = updated.replace(old, new)
        if updated != text:
            path.write_text(updated, encoding="utf-8")


def write_branding_properties():
    path = ROOT / "src/main/java/com/example/revolution/BrandingProperties.java"
    path.write_text(
        """package com.example.revolution;\n\n/** Preferred v1.3 property names with a temporary fallback for pre-v1.3 launch scripts. */\npublic final class BrandingProperties {\n    private static final String PREFIX = \"revolution.\";\n    private static final String LEGACY_PREFIX = \"customguimod.\";\n\n    private BrandingProperties() {}\n\n    public static String get(String key) {\n        return get(key, null);\n    }\n\n    public static String get(String key, String fallback) {\n        String value = System.getProperty(key);\n        if ((value == null || value.trim().isEmpty()) && key != null && key.startsWith(PREFIX)) {\n            value = System.getProperty(LEGACY_PREFIX + key.substring(PREFIX.length()));\n        }\n        return value == null ? fallback : value;\n    }\n}\n""",
        encoding="utf-8",
    )


def add_import(text, fqcn):
    statement = "import %s;" % fqcn
    if statement in text:
        return text
    package = re.search(r"(?m)^package\s+[\w.]+;\s*$", text)
    if not package:
        raise RuntimeError("Missing package declaration")
    return text[:package.end()] + "\n\n" + statement + text[package.end():]


def migrate_properties():
    targets = [
        "src/main/java/com/example/revolution/persistence/MongoManager.java",
        "src/main/java/com/example/revolution/resource/ResourceManager.java",
        "src/main/java/com/example/revolution/rune/RuneEnhancementManager.java",
        "src/main/java/com/example/revolution/rune/RuneEconomyConfig.java",
        "src/main/java/com/example/revolution/rune/RuneStatusRegistry.java",
    ]
    for rel in targets:
        path = ROOT / rel
        text = path.read_text(encoding="utf-8")
        text = add_import(text, "com.example.revolution.BrandingProperties")
        text = text.replace("System.getProperty(", "BrandingProperties.get(")
        path.write_text(text, encoding="utf-8")


def migrate_config_files():
    admin = ROOT / "src/main/java/com/example/revolution/admin/AdminPanelAccess.java"
    text = admin.read_text(encoding="utf-8")
    text = text.replace('File dir = new File(configDir, "revolution");', 'File dir = new File(configDir, "RE-volution");')
    old = '        file = new File(dir, "admin-panel-whitelist.json");\n        reload();'
    new = '''        file = new File(dir, "admin-panel-whitelist.json");\n        File legacyFile = new File(new File(configDir, "customguimod"), "admin-panel-whitelist.json");\n        if (!file.isFile() && legacyFile.isFile()) {\n            try {\n                java.nio.file.Files.copy(legacyFile.toPath(), file.toPath());\n                RevolutionMod.logger.info("Migrated admin panel whitelist to RE-volution config directory");\n            } catch (IOException e) {\n                RevolutionMod.logger.warn("Could not migrate legacy admin panel whitelist; reading legacy file for this run", e);\n                file = legacyFile;\n            }\n        }\n        reload();'''
    if old not in text:
        raise RuntimeError("AdminPanelAccess migration anchor missing")
    admin.write_text(text.replace(old, new), encoding="utf-8")

    numbers = ROOT / "src/main/java/com/example/revolution/number/LargeNumberDisplaySettings.java"
    text = numbers.read_text(encoding="utf-8")
    old = '        configFile = new File(configDirectory, "RE-volution-large-numbers.properties");\n        load();'
    new = '''        configFile = new File(configDirectory, "RE-volution-large-numbers.properties");\n        File legacyFile = new File(configDirectory, "customguimod-large-numbers.properties");\n        if (!configFile.isFile() && legacyFile.isFile()) {\n            try {\n                java.nio.file.Files.copy(legacyFile.toPath(), configFile.toPath());\n                RevolutionMod.logger.info("Migrated large number settings to RE-volution config file");\n            } catch (IOException e) {\n                RevolutionMod.logger.warn("Could not migrate legacy large number settings; reading legacy file for this run", e);\n                configFile = legacyFile;\n            }\n        }\n        load();'''
    if old not in text:
        raise RuntimeError("LargeNumberDisplaySettings migration anchor missing")
    numbers.write_text(text.replace(old, new), encoding="utf-8")


def update_readme():
    path = ROOT / "README.md"
    text = path.read_text(encoding="utf-8")
    text = re.sub(
        r"\n## Переход к v1\.3\.0\n.*?(?=\n## |\Z)",
        "\n",
        text,
        flags=re.S,
    )
    text = re.sub(
        r"2\. Скачайте `RE-volution-1\.2\.12\.jar`[^\n]*",
        "2. Скачайте JAR из актуального релиза. Начиная с v1.3.0 артефакт называется `RE-volution-<version>.jar`.",
        text,
    )
    path.write_text(text, encoding="utf-8")


def update_mcmod_info():
    path = ROOT / "src/main/resources/mcmod.info"
    text = path.read_text(encoding="utf-8")
    text = text.replace('"modid": "revolution"', '"modid": "revolution"')
    text = text.replace('"name": "RE:volution"', '"name": "RE:volution"')
    text = text.replace('"description": "Кастомный GUI"', '"description": "RE:volution for Minecraft 1.12.2"')
    path.write_text(text, encoding="utf-8")


def update_main_class():
    path = ROOT / "src/main/java/com/example/revolution/RevolutionMod.java"
    text = path.read_text(encoding="utf-8")
    text = text.replace('logger.info("RevolutionMod preInit done")', 'logger.info("RE:volution preInit done")')
    text = text.replace('logger.info("RevolutionMod client init done")', 'logger.info("RE:volution client init done")')
    path.write_text(text, encoding="utf-8")


def main():
    move_tree("src/main/java/com/example/customguimod", "src/main/java/com/example/revolution")
    move_tree("src/test/java/com/example/customguimod", "src/test/java/com/example/revolution")
    move_tree("src/main/resources/assets/customguimod", "src/main/resources/assets/revolution")
    rename_file(
        "src/main/java/com/example/revolution/CustomGuiMod.java",
        "src/main/java/com/example/revolution/RevolutionMod.java",
    )
    rewrite_current_text()
    write_branding_properties()
    migrate_properties()
    migrate_config_files()
    update_readme()
    update_mcmod_info()
    update_main_class()
    print("Prepared RE:volution v1.3 branding migration.")


if __name__ == "__main__":
    main()
