from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / 'src/main/java/com/example/revolution'
TEST = ROOT / 'src/test/java/com/example/revolution'

MOVES = {
    MAIN / 'AdminPanelRequestPacket.java': MAIN / 'network/admin/AdminPanelRequestPacket.java',
    MAIN / 'PingPacket.java': MAIN / 'network/PingPacket.java',
    MAIN / 'CustomRunesCommand.java': MAIN / 'commands/CustomRunesCommand.java',
    MAIN / 'CustomStatusCommand.java': MAIN / 'commands/CustomStatusCommand.java',
    MAIN / 'DamageBonusHandler.java': MAIN / 'combat/DamageBonusHandler.java',
    MAIN / 'PlayerJoinHandler.java': MAIN / 'player/PlayerJoinHandler.java',
    MAIN / 'PlayerStatsService.java': MAIN / 'player/PlayerStatsService.java',
    TEST / 'RuneStatusRegistryTest.java': TEST / 'rune/RuneStatusRegistryTest.java',
    TEST / 'RuneTestSupport.java': TEST / 'rune/RuneTestSupport.java',
    TEST / 'RuneTransportContractTest.java': TEST / 'network/RuneTransportContractTest.java',
}

OLD_TO_NEW = {
    'com.example.revolution.network.admin.AdminPanelRequestPacket': 'com.example.revolution.network.admin.AdminPanelRequestPacket',
    'com.example.revolution.network.PingPacket': 'com.example.revolution.network.PingPacket',
    'com.example.revolution.commands.CustomRunesCommand': 'com.example.revolution.commands.CustomRunesCommand',
    'com.example.revolution.commands.CustomStatusCommand': 'com.example.revolution.commands.CustomStatusCommand',
    'com.example.revolution.combat.DamageBonusHandler': 'com.example.revolution.combat.DamageBonusHandler',
    'com.example.revolution.player.PlayerJoinHandler': 'com.example.revolution.player.PlayerJoinHandler',
    'com.example.revolution.player.PlayerStatsService': 'com.example.revolution.player.PlayerStatsService',
    'com.example.revolution.rune.RuneStatusRegistryTest': 'com.example.revolution.rune.RuneStatusRegistryTest',
    'com.example.revolution.rune.RuneTestSupport': 'com.example.revolution.rune.RuneTestSupport',
    'com.example.revolution.network.RuneTransportContractTest': 'com.example.revolution.network.RuneTransportContractTest',
}

ROOT_TYPES = {
    'RevolutionMod': 'com.example.revolution.RevolutionMod',
    'BrandingProperties': 'com.example.revolution.BrandingProperties',
    'ServerInit': 'com.example.revolution.ServerInit',
}


def package_for(path: Path) -> str:
    root = ROOT / ('src/main/java' if '/src/main/java/' in path.as_posix() else 'src/test/java')
    return '.'.join(path.parent.relative_to(root).parts)


def replace_package(text: str, package: str) -> str:
    return re.sub(r'^package\s+[\w.]+\s*;', f'package {package};', text, count=1, flags=re.M)


def add_import(text: str, fqcn: str) -> str:
    stmt = f'import {fqcn};'
    if stmt in text:
        return text
    m = re.search(r'^package\s+[\w.]+\s*;\s*', text, re.M)
    if not m:
        return text
    return text[:m.end()] + '\n' + stmt + '\n' + text[m.end():]


def materialize_moves():
    for src, dst in MOVES.items():
        if src.exists():
            dst.parent.mkdir(parents=True, exist_ok=True)
            text = src.read_text(encoding='utf-8')
            text = replace_package(text, package_for(dst))
            dst.write_text(text, encoding='utf-8')
            src.unlink()
        elif not dst.exists():
            raise AssertionError(f'Missing both source and destination: {src} -> {dst}')


def rewrite_java():
    symbol_map = {old.rsplit('.', 1)[1]: new for old, new in OLD_TO_NEW.items()}
    symbol_map.update(ROOT_TYPES)
    moved_destinations = {dst.resolve() for dst in MOVES.values()}

    for path in list((ROOT / 'src').rglob('*.java')):
        text = path.read_text(encoding='utf-8')
        original = text
        for old, new in OLD_TO_NEW.items():
            text = text.replace(old, new)

        package_match = re.search(r'^package\s+([\w.]+)\s*;', text, re.M)
        package = package_match.group(1) if package_match else ''
        for symbol, fqcn in symbol_map.items():
            if not re.search(rf'\b{re.escape(symbol)}\b', text):
                continue
            target_package = fqcn.rsplit('.', 1)[0]
            if package == target_package:
                continue
            # Do not import the type into its own source file.
            if path.resolve() in moved_destinations and path.stem == symbol:
                continue
            if path.stem == symbol and fqcn in ROOT_TYPES.values():
                continue
            text = add_import(text, fqcn)

        if text != original:
            path.write_text(text, encoding='utf-8')


def rewrite_text_references():
    extensions = {'.gradle', '.yml', '.yaml', '.py', '.md', '.properties'}
    for path in ROOT.rglob('*'):
        if not path.is_file() or path.suffix.lower() not in extensions:
            continue
        if '.git' in path.parts:
            continue
        text = path.read_text(encoding='utf-8')
        original = text
        for old, new in OLD_TO_NEW.items():
            text = text.replace(old, new)
        if text != original:
            path.write_text(text, encoding='utf-8')


def strengthen_structure_check():
    path = ROOT / '.github/scripts/check-structure.py'
    text = path.read_text(encoding='utf-8')
    marker = 'def check_sources():\n'
    guard = (
        "def check_sources():\n"
        "    main_root = ROOT / 'src/main/java/com/example/revolution'\n"
        "    allowed_main_root = {'BrandingProperties.java', 'RevolutionMod.java', 'ServerInit.java'}\n"
        "    actual_main_root = {p.name for p in main_root.glob('*.java')}\n"
        "    assert actual_main_root == allowed_main_root, ('Unexpected root production classes', sorted(actual_main_root))\n"
        "    test_root = ROOT / 'src/test/java/com/example/revolution'\n"
        "    assert not list(test_root.glob('*.java')), ('Unexpected root test classes', sorted(p.name for p in test_root.glob('*.java')))\n"
    )
    if 'allowed_main_root' not in text:
        if marker not in text:
            raise AssertionError('check_sources marker not found')
        text = text.replace(marker, guard, 1)
        path.write_text(text, encoding='utf-8')


def verify_layout():
    expected_main = {'BrandingProperties.java', 'RevolutionMod.java', 'ServerInit.java'}
    actual_main = {p.name for p in MAIN.glob('*.java')}
    assert actual_main == expected_main, (actual_main, expected_main)
    assert not list(TEST.glob('*.java')), [p.name for p in TEST.glob('*.java')]
    for src, dst in MOVES.items():
        assert not src.exists(), src
        assert dst.exists(), dst
        expected_package = package_for(dst)
        text = dst.read_text(encoding='utf-8')
        assert f'package {expected_package};' in text, dst


if __name__ == '__main__':
    materialize_moves()
    rewrite_java()
    rewrite_text_references()
    strengthen_structure_check()
    verify_layout()
    print('v1.3 package finalization materialized successfully.')
