"""Enforce the 1.3.1 architectural contract without requiring Minecraft or a JVM."""
from pathlib import Path
import hashlib
import json
import re

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / 'src/main/java/com/example/revolution'
TEST = ROOT / 'src/test/java/com/example/revolution'
BASE = 'com.example.revolution.'


def resource_digest(path, data):
    # Git checks text out with CRLF on Windows and LF on Linux. Binary assets stay exact.
    if Path(path).suffix in {'.md', '.json', '.mcmeta'}:
        data = data.replace(b'\r\n', b'\n')
    return hashlib.sha256(data).hexdigest()


def violations(path, source):
    """Pure rule function, also exercised with deliberately invalid source examples."""
    errors = []
    imports = re.findall(r'^import (?:static )?([^;]+);', source, re.M)
    dependencies = [value for value in imports if value.startswith(BASE)]
    package = path.rsplit('/', 1)[0] if '/' in path else ''

    def reject(condition, message):
        if condition:
            errors.append(f'{path}: {message}')

    reject(not package and path != 'RevolutionMod.java', 'root is reserved for Forge bootstrap')
    reject(package != 'persistence' and any(value.startswith(('com.mongodb.', 'org.bson.')) for value in imports),
           'Mongo/BSON belongs to persistence implementations')
    reject(not package.startswith('client') and any(value.startswith(('net.minecraft.client.', BASE + 'client.')) for value in imports),
           'client implementation leaked into common/server code')

    if package.startswith('client'):
        reject(any(re.search(r'\.(repository|persistence|service)\.', value) for value in dependencies),
               'client must use wire DTOs and shared rules, never server services/storage')
    if '/definition' in package or '/rules' in package:
        reject(any(re.search(r'\.(state|config|service|repository|persistence|network|client)\.', value) for value in dependencies),
               'definitions/rules must not depend on runtime or adapters')
        reject(any(value.startswith(('net.minecraft.', 'net.minecraftforge.')) for value in imports),
               'definitions/rules must be independent of the engine')
    if '/state' in package:
        reject(any(re.search(r'\.(service|repository|persistence|network|client)\.', value) for value in dependencies),
               'runtime state cannot call services, storage or transport')
    if '/repository' in package:
        reject(any(re.search(r'\.(service|persistence|network|client|lifecycle)\.', value) for value in dependencies),
               'repository ports must not expose implementations/adapters')
        reject(any(value.startswith(('net.minecraft.', 'net.minecraftforge.')) for value in imports),
               'repository ports expose values, not engine types')
    if package.endswith('/service'):
        reject(any(re.search(r'\.(client|commands|lifecycle|persistence)\.', value) for value in dependencies),
               'services receive dependencies through constructors')
        reject(any(value.startswith('net.minecraftforge.event.') or '.gameevent.' in value for value in imports),
               'Forge event objects stop at the adapter')
        reject(bool(re.search(r'^\s+(?:public|private|protected) static (?!final)[\w<>?, ]+\s+\w+\s*=', source, re.M)),
               'mutable service state must be instance-owned')
    if package == 'commands':
        reject(any(re.search(r'\.(repository|persistence)\.', value) for value in dependencies),
               'commands must invoke application services')
    if path.endswith('Packet.java'):
        reject(any(re.search(r'\.(service|config|repository|persistence|lifecycle)\.', value) for value in dependencies),
               'packets only carry and serialize values')
        reject('IMessageHandler' in source, 'packet handlers must be separate adapters')
    reject(path != 'network/NetworkHandler.java' and 'NetworkHandler.INSTANCE.sendTo(' in source,
           'server packets must use the optional-client guard')
    return errors


def validate():
    errors = []
    source_map = {path.relative_to(MAIN).as_posix(): path.read_text(encoding='utf-8') for path in MAIN.rglob('*.java')}
    for path, source in source_map.items():
        errors.extend(violations(path, source))
    # The only global accessor is at framework boundaries, never in application services.
    assert sum('MongoClients.create(' in source for source in source_map.values()) == 1, 'Expected one Mongo client owner'
    assert 'acceptableRemoteVersions = "*"' in source_map['RevolutionMod.java'], 'Optional-client handshake changed'
    assert 'clientHasRevolutionMod(player)' in source_map['network/NetworkHandler.java'], 'Missing optional-client send guard'
    assert 'lastAcceptedAt.clear()' in source_map['network/EnhancementRequestLimiter.java'], 'Limiter shutdown cleanup missing'
    assert 'RateLimitedPingPacketHandler.reset()' in source_map['lifecycle/ServerLifecycle.java'], 'Limiter not reset by lifecycle'
    assert 'stopping.close()' in source_map['lifecycle/ServerLifecycle.java'], 'Server graph not closed'

    # Minecraft 1.12.2 clients can still run on early Java 8. When source is compiled
    # against a newer JDK, direct calls such as FloatBuffer.clear()/flip()/position()
    # can be linked with newer covariant return descriptors and then fail on Java 8u51.
    # Any mutable NIO operation on a typed buffer must therefore be dispatched through
    # java.nio.Buffer explicitly. Scan every production source instead of guarding one GUI.
    nio_types = r'(?:Byte|Char|Double|Float|Int|Long|Short)Buffer'
    nio_mutators = r'(?:clear|flip|rewind|mark|reset|position|limit)'
    unsafe_nio = []
    for path, source in source_map.items():
        names = set(re.findall(r'(?:java\.nio\.)?' + nio_types + r'\s+(\w+)\b', source))
        for name in names:
            if re.search(r'(?<!\))\b' + re.escape(name) + r'\.' + nio_mutators + r'\s*\(', source):
                unsafe_nio.append(f'{path}: direct typed NIO mutation on {name}')
    assert not unsafe_nio, 'Early-Java-8 unsafe NIO calls:\n' + '\n'.join(unsafe_nio)
    assert '((java.nio.Buffer) transform).clear();' in source_map['client/GuiDrawing.java'], \
        'GUI surface buffer reset lost early-Java-8 compatibility'
    assert '((java.nio.Buffer) matrix).clear();' in source_map['client/GuiFontRenderer.java'], \
        'GUI font buffer reset lost early-Java-8 compatibility'

    registrations = [
        ('network/NetworkHandler.java', ['SERVER', 'CLIENT', 'CLIENT', 'CLIENT', 'CLIENT', 'SERVER', 'CLIENT', 'CLIENT']),
        ('client/ClientNetworkRegistration.java', ['CLIENT', 'CLIENT', 'CLIENT', 'CLIENT', 'CLIENT', 'SERVER', 'CLIENT', 'CLIENT']),
    ]
    packets = ['PingPacket', 'PongPacket', 'RuneSlotPacket', 'RuneCollectionsPacket', 'PlayerStatsPacket',
               'AdminPanelRequestPacket', 'AdminPanelStatePacket', 'RuneEnhancementPacket']
    for path, sides in registrations:
        actual = re.findall(r'registerMessage\((\w+)\.class, (\w+)\.class, (\d+), Side\.(\w+)\)', source_map[path])
        assert [(packet, int(number), side) for _, packet, number, side in actual] == list(zip(packets, range(8), sides)), path
        if path.startswith('network/'):
            assert actual[0][0] == 'RateLimitedPingPacketHandler', 'Enhancement anti-spam bypassed'

    for path in TEST.rglob('*.java'):
        assert (MAIN / path.parent.relative_to(TEST)).is_dir(), f'Test package does not mirror production: {path}'
    build = (ROOT / 'build.gradle').read_text(encoding='utf-8')
    entrypoints = set(re.findall(r"\w+Regression: '([\w.]+)'", build))
    for name in entrypoints:
        assert (ROOT / 'src/test/java' / (name.replace('.', '/') + '.java')).is_file(), name
    for path in TEST.rglob('*.java'):
        if 'public static void main(' in path.read_text(encoding='utf-8'):
            name = '.'.join(path.relative_to(ROOT / 'src/test/java').with_suffix('').parts)
            assert name in entrypoints, f'Unexecuted regression runner: {name}'
    assert 'org.gradle.java.home' not in (ROOT / 'gradle.properties').read_text(encoding='utf-8'), 'Local Java path committed'

    assets = json.loads((ROOT / 'docs/contracts/resource-assets.json').read_text(encoding='utf-8'))
    actual_assets = {path.relative_to(ROOT).as_posix(): resource_digest(path, path.read_bytes())
                     for path in (ROOT / 'src/main/resources').rglob('*') if path.is_file() and path.name != 'mcmod.info'}
    assert actual_assets == assets, 'Resource assets changed; review the resource contract explicitly'
    assert not errors, '\n'.join(errors)
    print('Architecture boundaries, early Java 8 NIO compatibility, mirrored runners, lifecycle, IDs 0-7, optional client and resource assets passed.')


if __name__ == '__main__':
    validate()
