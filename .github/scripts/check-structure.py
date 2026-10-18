"""Check current Java terminology, branding metadata and the packaged artifact."""
from pathlib import Path
import json
import re
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[2]
LEXER = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|\b[A-Za-z_$][A-Za-z0-9_$]*\b', re.S)


def check_sources():
    for path in (ROOT / 'src').rglob('*.java'):
        source = path.read_text(encoding='utf-8')
        package = re.search(r'^package ([\w.]+);', source, re.M).group(1)
        assert path.parent.as_posix().endswith(package.replace('.', '/')), path
        for match in LEXER.finditer(source):
            token = match.group()
            # Mongo/config/command/wire strings and historical comments are explicit compatibility exceptions.
            if token.startswith(('//', '/*', '"', "'")):
                continue
            assert not re.search(r'Card|CARD|card|Deck|DECK|deck|EarthRune|earthRune', token), (path, token)
    gradle = (ROOT / 'build.gradle').read_text(encoding='utf-8')
    for name in re.findall(r"mainClass = '([^']+)'", gradle):
        assert (ROOT / 'src/test/java' / (name.replace('.', '/') + '.java')).is_file(), name


def check_branding():
    old_brand = 'Custom' + ' GUI Mod'
    template_vendor = 'examplemods' + 'areus'
    for path in ROOT.rglob('*'):
        if not path.is_file() or '.git' in path.parts:
            continue
        try:
            text = path.read_text(encoding='utf-8')
        except (UnicodeDecodeError, OSError):
            continue
        assert old_brand not in text, f'Legacy display branding remains in {path.relative_to(ROOT)}'
        assert template_vendor not in text, f'Forge template vendor remains in {path.relative_to(ROOT)}'

    gradle = (ROOT / 'build.gradle').read_text(encoding='utf-8')
    assert 'archivesBaseName = \'RE-volution\'' in gradle
    assert '"Specification-Title": "RE:volution Mod"' in gradle
    assert '"Specification-Vendor": "Theyyk"' in gradle
    assert '"Implementation-Title": "RE:volution Mod"' in gradle
    assert '"Implementation-Vendor" :"Theyyk"' in gradle

    mcmod = json.loads((ROOT / 'src/main/resources/mcmod.info').read_text(encoding='utf-8'))
    assert mcmod and mcmod[0]['modid'] == 'revolution'
    assert mcmod[0]['name'] == 'RE:volution Mod'
    assert mcmod[0]['authorList'] == ['Theyyk']


def check_jar(path):
    with zipfile.ZipFile(path) as jar:
        names = set(jar.namelist())
        for source in (ROOT / 'src/main/java').rglob('*.java'):
            name = source.relative_to(ROOT / 'src/main/java').with_suffix('.class').as_posix()
            assert name in names, f'Missing production class: {name}'
        for source in (ROOT / 'src/main/resources').rglob('*'):
            if source.is_file():
                name = source.relative_to(ROOT / 'src/main/resources').as_posix()
                assert jar.read(name) == source.read_bytes(), f'Resource changed or missing: {name}'
        for name in names:
            if name.endswith('.class'):
                assert not re.search(r'Card|Deck|EarthRune|FlyingRune|FlyingCard|TestSupport|Test\$', name), name
                assert not name.endswith('Test.class'), name
        assert 'com/example/revolution/network/RuneSlotPacket.class' in names
        assert 'com/example/revolution/network/RuneCollectionsPacket.class' in names
        assert 'com/example/revolution/rune/PrimaryRuneCatalog.class' in names
        assert 'com/example/revolution/client/RunesGuiScreen.class' in names


if __name__ == '__main__':
    check_sources()
    check_branding()
    if len(sys.argv) > 1:
        check_jar(Path(sys.argv[1]))
    print('Source terminology, branding metadata, package paths and Gradle test entry points passed.' if len(sys.argv) == 1
          else 'Source/branding checks and JAR classes/resources/test exclusion passed.')
