"""Static fail-closed guard for the v1.3.2 local Admin API security boundary."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / 'src/main/java/com/example/revolution'


def read(relative):
    return (MAIN / relative).read_text(encoding='utf-8')


def main():
    api_root = MAIN / 'admin/api'
    assert api_root.is_dir(), 'Admin API package is missing'
    api_sources = {p.relative_to(MAIN).as_posix(): p.read_text(encoding='utf-8') for p in api_root.rglob('*.java')}
    required = {
        'admin/api/AdminApiServer.java',
        'admin/api/AdminApiRoutes.java',
        'admin/api/AdminApiHttp.java',
        'admin/api/AdminApiJson.java',
        'admin/api/AdminFormData.java',
        'admin/api/AdminServerThreadExecutor.java',
        'admin/api/AdminApiBindPolicy.java',
        'admin/api/auth/AdminApiAuthService.java',
        'admin/api/auth/AdminApiCapability.java',
        'admin/api/auth/AdminChallengeService.java',
        'admin/api/auth/AdminSessionService.java',
        'admin/api/auth/AdminInstallationSecretStore.java',
    }
    missing = sorted(required.difference(api_sources))
    assert not missing, 'Missing Admin API security components: ' + ', '.join(missing)

    forbidden_imports = (
        'com.example.revolution.persistence.',
        'com.mongodb.',
        'org.bson.',
    )
    forbidden_primitives = (
        'ServerLifecycle.services()',
        'Runtime.getRuntime().exec',
        'new ProcessBuilder',
        'Class.forName(',
        'java.lang.reflect',
        'NetworkHandler.INSTANCE',
    )
    combined = ''.join(api_sources.values())
    for path, source in api_sources.items():
        for prefix in forbidden_imports:
            assert f'import {prefix}' not in source, f'{path}: Admin API must not access persistence/Mongo directly'
        for primitive in forbidden_primitives:
            assert primitive not in source, f'{path}: forbidden generic execution/service-locator primitive: {primitive}'

    server = api_sources['admin/api/AdminApiServer.java']
    routes = api_sources['admin/api/AdminApiRoutes.java']
    http = api_sources['admin/api/AdminApiHttp.java']
    form = api_sources['admin/api/AdminFormData.java']
    thread_bridge = api_sources['admin/api/AdminServerThreadExecutor.java']
    bind = api_sources['admin/api/AdminApiBindPolicy.java']
    auth = api_sources['admin/api/auth/AdminApiAuthService.java']
    challenges = api_sources['admin/api/auth/AdminChallengeService.java']
    sessions = api_sources['admin/api/auth/AdminSessionService.java']
    secrets = api_sources['admin/api/auth/AdminInstallationSecretStore.java']

    assert 'InetAddress.getByName("127.0.0.1")' in bind, 'Admin API must use explicit IPv4 loopback'
    assert '"0.0.0.0"' not in combined, 'Wildcard Admin API bind is forbidden'
    assert 'AdminApiBindPolicy.requireAllowed(bindAddress)' in server, 'Server must enforce bind policy before listen'
    assert 'isAllowed(remote)' in server, 'Every request must verify loopback peer'
    assert 'Origin' in http and 'Sec-Fetch-Site' in http, 'Browser-origin requests must fail closed'
    assert 'Access-Control-Allow-Origin' not in combined, 'CORS must remain disabled in v1.3.2'
    assert '/api/v1/auth/challenge' in server and '/api/v1/auth/session' in server, 'Versioned auth routes missing'
    assert '/api/v1/status' in server, 'Protected status route missing'
    assert 'authorizeBearer' in server and 'authorizeBearer' in routes, 'Protected routes must authenticate session bearer tokens'

    expected_routes = {
        '/api/v1/resources',
        '/api/v1/players',
        '/api/v1/mobs',
        '/api/v1/resources/set',
        '/api/v1/resources/add',
        '/api/v1/resources/zero-all',
        '/api/v1/runes/clear',
        '/api/v1/runes/fill',
        '/api/v1/runes/reset-enhancements',
        '/api/v1/mobs/create',
        '/api/v1/mobs/delete',
        '/api/v1/mobs/set-spawn',
    }
    for route in expected_routes:
        assert route in routes, f'Missing explicit Admin API route: {route}'
    assert 'action=' not in routes.lower(), 'Generic action dispatcher is forbidden'
    for capability in ('PLAYERS_READ', 'RESOURCES_READ', 'RESOURCES_WRITE', 'RUNES_READ', 'RUNES_WRITE', 'MOBS_READ', 'MOBS_WRITE'):
        assert f'AdminApiCapability.{capability}' in routes, f'Route capability missing: {capability}'

    assert 'server.addScheduledTask(task)' in thread_bridge, 'Admin operations must be scheduled on Minecraft server thread'
    assert 'isCallingFromMinecraftThread()' in thread_bridge, 'Server-thread bridge must detect authoritative thread'
    assert 'TIMEOUT_SECONDS' in thread_bridge and 'task.cancel(false)' in thread_bridge, 'Server-thread handoff must be bounded/cancellable'
    assert 'serverThread.call(' in routes, 'Routes must delegate reads/mutations through server-thread bridge'
    assert 'HttpExchange' not in routes.split('serverThread.call(', 1)[1].split('));', 1)[0], 'HTTP exchange must not cross the server-thread boundary'
    assert 'MAX_FORM_BODY_BYTES = 4096' in http, 'Mutation request bodies must stay bounded'
    assert 'Transfer-Encoding' in http and 'Content-Encoding' in http, 'Chunked/compressed mutation bodies must fail closed'
    assert 'application/x-www-form-urlencoded' in http, 'Mutation body type must be explicit'
    assert 'unexpected form key' in form, 'Form parser must reject unknown fields'
    assert 'duplicate form key' in form, 'Form parser must reject duplicates'
    assert 'precision() > 100' in routes and '10_000L' in routes, 'Exact-number input bounds missing'
    assert '30_000_000.0D' in routes and '4096.0D' in routes, 'Coordinate input bounds missing'
    assert 'Admin API mutation operation=' in routes, 'State-changing routes must emit sanitized audit events'

    assert 'SecureRandom' in secrets and 'SECRET_BYTES = 32' in secrets, 'Installation secret must be 256-bit SecureRandom'
    assert 'revolution-admin.secret' in secrets, 'Installation secret runtime file contract changed'
    assert 'LinkOption.NOFOLLOW_LINKS' in secrets and 'Files.isSymbolicLink(path)' in secrets, 'Secret path must reject linked-file substitution'
    gitignore = (ROOT / '.gitignore').read_text(encoding='utf-8')
    assert 'revolution-admin.secret' in gitignore and '*.secret' in gitignore, 'Runtime Admin API secrets must be ignored by Git'
    assert 'HmacSHA256' in challenges, 'Challenge proof must use HMAC-SHA-256'
    assert 'MessageDigest.isEqual' in challenges, 'HMAC proof comparison must be constant-time'
    assert 'outstanding.remove(challengeId)' in challenges, 'Challenge must be consumed before verification'
    assert 'CHALLENGE_TTL_MILLIS' in challenges, 'Challenge expiration missing'
    assert 'IDLE_TTL_MILLIS' in sessions and 'ABSOLUTE_TTL_MILLIS' in sessions, 'Session expiry missing'
    assert 'sessions.clear()' in sessions, 'Sessions must be invalidated on shutdown'
    assert 'AdminAuthRateLimiter' in auth, 'Authentication endpoints must be rate-limited'

    services = read('lifecycle/ServerServices.java')
    lifecycle = read('lifecycle/ServerLifecycle.java')
    assert 'new AdminApiServer(logger, adminOperations, adminQueries)' in services, 'Admin API must receive typed services from ServerServices'
    assert 'new AdminQueryService(players, resources, mobRepository)' in services, 'Admin query service must be constructor-wired'
    assert 'adminApi.close()' in services, 'Admin API must close with ServerServices'
    assert 'current.startAdminApi(server, configDirectory)' in lifecycle, 'Dedicated lifecycle must pass MinecraftServer explicitly'
    assert 'if (dedicatedServer)' in lifecycle, 'Admin API must not be auto-started for integrated client context'

    old_transport = [
        MAIN / 'client/input/AdminPanelKeyHandler.java',
        MAIN / 'client/AdminPanelScreen.java',
        MAIN / 'network/admin/AdminPanelRequestPacket.java',
        MAIN / 'network/admin/AdminPanelStatePacket.java',
    ]
    assert not any(path.exists() for path in old_transport), 'Removed Minecraft admin transport returned'

    network = read('network/NetworkHandler.java')
    ids = [int(value) for value in re.findall(r'registerMessage\([^,]+, [^,]+, (\d+), Side\.', network)]
    assert 5 not in ids and 6 not in ids, 'Forge IDs 5/6 must stay reserved'
    assert 7 in ids and 'RuneEnhancementPacket.class, 7' in network, 'RuneEnhancement discriminator 7 changed'

    print('Admin API security architecture, typed routes, secret hardening, body bounds and server-thread guard passed.')


if __name__ == '__main__':
    main()
