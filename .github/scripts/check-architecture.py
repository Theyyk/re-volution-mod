"""Enforce the RE:volution architectural contract without requiring Minecraft or a JVM."""
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

    required_adapter_paths = {
        'client/lifecycle/ClientLifecycle.java',
        'client/network/ClientNetworkRegistration.java',
        'client/network/PingPacketClientHandler.java',
        'client/network/PongPacketHandler.java',
        'client/network/RuneSlotPacketHandler.java',
        'client/network/RuneCollectionsPacketHandler.java',
        'client/network/PlayerStatsPacketHandler.java',
        'client/network/RuneEnhancementPacketHandler.java',
        'client/network/StageTravelMenuRequestClientHandler.java',
        'client/network/StageTravelMenuPacketClientHandler.java',
        'client/network/StageTravelTeleportRequestClientHandler.java',
        'client/network/FastAutoTogglePacketClientHandler.java',
        'client/input/KeyInputHandler.java',
        'client/input/EnhancementClickGuard.java',
        'mob/event/ManagedMobEvents.java',
        'mob/event/MobDeathHandler.java',
        'player/event/PlayerJoinHandler.java',
    }
    missing_adapters = sorted(required_adapter_paths.difference(source_map))
    assert not missing_adapters, 'Adapter package topology regressed: ' + ', '.join(missing_adapters)
    forbidden_old_adapter_paths = {
        'client/ClientLifecycle.java', 'client/ClientNetworkRegistration.java',
        'client/KeyInputHandler.java', 'client/AdminPanelKeyHandler.java', 'client/EnhancementClickGuard.java',
        'client/input/AdminPanelKeyHandler.java',
        'client/AdminPanelScreen.java',
        'client/network/AdminPanelRequestClientHandler.java',
        'client/network/AdminPanelStateClientHandler.java',
        'network/admin/AdminPanelRequestHandler.java',
        'network/admin/AdminPanelRequestPacket.java',
        'network/admin/AdminPanelStatePacket.java',
        'network/admin/AdminPanelStateServerHandler.java',
        'admin/service/AdminPanelService.java',
        'mob/ManagedMobEvents.java', 'mob/MobDeathHandler.java', 'player/PlayerJoinHandler.java',
    }
    stale_adapters = sorted(forbidden_old_adapter_paths.intersection(source_map))
    assert not stale_adapters, 'Removed/relocated adapters returned: ' + ', '.join(stale_adapters)

    assert sum('MongoClients.create(' in source for source in source_map.values()) == 1, 'Expected one Mongo client owner'
    assert 'acceptableRemoteVersions = "*"' in source_map['RevolutionMod.java'], 'Optional-client handshake changed'
    assert 'clientHasRevolutionMod(player)' in source_map['network/NetworkHandler.java'], 'Missing optional-client send guard'
    assert 'lastAcceptedAt.clear()' in source_map['network/EnhancementRequestLimiter.java'], 'Limiter shutdown cleanup missing'
    assert 'RateLimitedPingPacketHandler.reset()' in source_map['lifecycle/ServerLifecycle.java'], 'Limiter not reset by lifecycle'
    assert 'stopping.close()' in source_map['lifecycle/ServerLifecycle.java'], 'Server graph not closed'

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

    expected_registrations = {
        'network/NetworkHandler.java': [
            ('PingPacket', 0, 'SERVER'),
            ('PongPacket', 1, 'CLIENT'),
            ('RuneSlotPacket', 2, 'CLIENT'),
            ('RuneCollectionsPacket', 3, 'CLIENT'),
            ('PlayerStatsPacket', 4, 'CLIENT'),
            ('RuneEnhancementPacket', 7, 'CLIENT'),
            ('MobZoneOverlayRequestPacket', 8, 'SERVER'),
            ('MobZoneOverlayPacket', 9, 'CLIENT'),
            ('StageTravelMenuRequestPacket', 10, 'SERVER'),
            ('StageTravelMenuPacket', 11, 'CLIENT'),
            ('StageTravelTeleportRequestPacket', 12, 'SERVER'),
            ('FastAutoTogglePacket', 13, 'SERVER'),
        ],
        'client/network/ClientNetworkRegistration.java': [
            ('PingPacket', 0, 'CLIENT'),
            ('PongPacket', 1, 'CLIENT'),
            ('RuneSlotPacket', 2, 'CLIENT'),
            ('RuneCollectionsPacket', 3, 'CLIENT'),
            ('PlayerStatsPacket', 4, 'CLIENT'),
            ('RuneEnhancementPacket', 7, 'CLIENT'),
            ('MobZoneOverlayRequestPacket', 8, 'CLIENT'),
            ('MobZoneOverlayPacket', 9, 'CLIENT'),
            ('StageTravelMenuRequestPacket', 10, 'CLIENT'),
            ('StageTravelMenuPacket', 11, 'CLIENT'),
            ('StageTravelTeleportRequestPacket', 12, 'CLIENT'),
            ('FastAutoTogglePacket', 13, 'CLIENT'),
        ],
    }
    for path, expected in expected_registrations.items():
        actual = re.findall(r'registerMessage\((\w+)\.class, (\w+)\.class, (\d+), Side\.(\w+)\)', source_map[path])
        compact = [(packet, int(number), side) for _, packet, number, side in actual]
        assert compact == expected, path
        assert all(number not in {5, 6} for _, number, _ in compact), path + ': admin packet IDs 5/6 must stay reserved'
        assert ('RuneEnhancementPacket', 7, 'CLIENT') in compact, path + ': discriminator 7 changed'
        assert ('FastAutoTogglePacket', 13, 'SERVER' if path.startswith('network/') else 'CLIENT') in compact, path + ': discriminator 13 changed'
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
    if actual_assets != assets:
        expected_paths = set(assets)
        actual_paths = set(actual_assets)
        details = []
        for path in sorted(expected_paths - actual_paths):
            details.append('Missing resource: ' + path)
        for path in sorted(actual_paths - expected_paths):
            details.append('Unexpected resource: ' + path + ' actual=' + actual_assets[path])
        for path in sorted(expected_paths & actual_paths):
            if assets[path] != actual_assets[path]:
                details.append('Digest mismatch: ' + path + ' expected=' + assets[path] + ' actual=' + actual_assets[path])
        raise AssertionError('Resource assets changed; review the resource contract explicitly\n' + '\n'.join(details))
    assert not errors, '\n'.join(errors)
    print('Architecture boundaries, adapter topology, early Java 8 NIO compatibility, mirrored runners, reserved admin IDs 5/6, discriminators 7/10/11/12/13, optional client and resource assets passed.')


if __name__ == '__main__':
    validate()
