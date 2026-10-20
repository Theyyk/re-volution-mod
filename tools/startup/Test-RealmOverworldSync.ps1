$ErrorActionPreference = 'Stop'
$fixture = Join-Path $PSScriptRoot ('.overworld-fixture-' + [Guid]::NewGuid().ToString('N'))
$Root = $fixture
$Build = Join-Path $Root 'Server-Build'
$Test = Join-Path $Root 'Server-Test'

function Require($Condition, $Message) { if (-not $Condition) { throw $Message } }
function Write-Fixture([string]$Path, [string]$Value) {
    New-Item -ItemType Directory -Path (Split-Path $Path -Parent) -Force | Out-Null
    [IO.File]::WriteAllText($Path, $Value)
}
function Read-Fixture([string]$Path) { return [IO.File]::ReadAllText($Path) }
function Read-Properties($Path) {
    $result = @{}
    foreach ($line in [IO.File]::ReadAllLines($Path)) { if ($line -match '^([^#=]+)=(.*)$') { $result[$Matches[1]] = $Matches[2] } }
    return $result
}
function Require-Failure([scriptblock]$Action) {
    $failed = $false
    try { & $Action } catch { $failed = $true }
    Require $failed 'Unsafe operation was accepted'
}

. "$PSScriptRoot\world-sync.ps1"
. "$PSScriptRoot\realm-world-sync.ps1"
function Test-WorldServerPort([string]$Server) { return $false }

try {
    New-Item -ItemType Directory -Path (Join-Path $Root 'Script') -Force | Out-Null
    foreach ($server in @($Build, $Test)) {
        $port = if ($server -eq $Build) { 65191 } else { 65192 }
        Write-Fixture (Join-Path $server 'server.properties') "level-name=world`nserver-port=$port`n"
        Write-Fixture (Join-Path $server 'world/session.lock') 'session'
        Write-Fixture (Join-Path $server 'world/.revolution-realm.lock') 'lock file'
    }
    Write-Fixture (Join-Path $Build 'world/level.dat') 'build-level'
    Write-Fixture (Join-Path $Build 'world/region/new.mca') 'build-overworld'
    Write-Fixture (Join-Path $Build 'world/data/map.dat') 'build-overworld-map'
    Write-Fixture (Join-Path $Build 'world/DIM201/region/template.mca') 'DO NOT IMPORT BUILD REALM'
    Write-Fixture (Join-Path $Build 'world/DIM-1/region/nether.mca') 'DO NOT IMPORT BUILD NETHER'
    Write-Fixture (Join-Path $Build 'world/playerdata/player.dat') 'DO NOT IMPORT BUILD PLAYER'
    Write-Fixture (Join-Path $Test 'world/level.dat') 'old-test-level'
    Write-Fixture (Join-Path $Test 'world/region/obsolete.mca') 'old-test-overworld'
    Write-Fixture (Join-Path $Test 'world/DIM201/region/template.mca') 'test-template'
    Write-Fixture (Join-Path $Test 'world/DIM201/data/local.dat') 'test-template-data'
    Write-Fixture (Join-Path $Test 'world/DIM202/region/second.mca') 'second-before-forge'
    Write-Fixture (Join-Path $Test 'world/DIM203/region/third.mca') 'third-before-forge'
    Write-Fixture (Join-Path $Test 'world/DIM-1/region/nether.mca') 'test-nether'
    foreach ($name in @('playerdata', 'stats', 'advancements')) { Write-Fixture (Join-Path $Test "world/$name/player.dat") "test-$name" }
    Write-Fixture (Join-Path $Test 'config-sentinel.txt') 'config'
    Write-Fixture (Join-Path $Test 'mongo-sentinel.txt') 'mongo'
    $buildBefore = @(Get-WorldManifest (Join-Path $Build 'world')) | ConvertTo-Json -Depth 4
    $testBefore = @(Get-WorldManifest (Join-Path $Test 'world')) | ConvertTo-Json -Depth 4

    Assert-RealmOverworldSyncReady
    Require (-not (Test-Path -LiteralPath (Join-Path $Root 'Script/WorldBackups'))) 'Preflight created a backup'
    Require ((@(Get-WorldManifest (Join-Path $Test 'world')) | ConvertTo-Json -Depth 4) -eq $testBefore) 'Preflight modified the world'

    # Locked Build DIM201 must never be read/copied by the overworld operation.
    $excludedWriter = [IO.File]::Open((Join-Path $Build 'world/DIM201/region/template.mca'), 'Open', 'ReadWrite', 'None')
    try { Reset-TestOverworldFromBuild } finally { $excludedWriter.Dispose() }
    Require ($LASTEXITCODE -eq 0) 'Successful robocopy leaked a failure exit code to the caller'
    Require ((Read-Fixture (Join-Path $Test 'world/level.dat')) -eq 'build-level') 'Overworld metadata not copied'
    Require ((Read-Fixture (Join-Path $Test 'world/region/new.mca')) -eq 'build-overworld') 'Overworld blocks not copied'
    Require (-not (Test-Path -LiteralPath (Join-Path $Test 'world/region/obsolete.mca'))) 'Stale overworld chunk survived'
    Require ((Read-Fixture (Join-Path $Test 'world/data/map.dat')) -eq 'build-overworld-map') 'Overworld data not copied'
    Require ((Read-Fixture (Join-Path $Test 'world/DIM201/region/template.mca')) -eq 'test-template') 'Realm 1 replaced from Build'
    Require ((Read-Fixture (Join-Path $Test 'world/DIM201/data/local.dat')) -eq 'test-template-data') 'Realm 1 data lost'
    Require ((Read-Fixture (Join-Path $Test 'world/DIM202/region/second.mca')) -eq 'second-before-forge') 'Realm 2 modified before Forge'
    Require ((Read-Fixture (Join-Path $Test 'world/DIM203/region/third.mca')) -eq 'third-before-forge') 'Realm 3 modified before Forge'
    Require ((Read-Fixture (Join-Path $Test 'world/DIM-1/region/nether.mca')) -eq 'test-nether') 'Other Test dimensions changed'
    foreach ($name in @('playerdata', 'stats', 'advancements')) { Require ((Read-Fixture (Join-Path $Test "world/$name/player.dat")) -eq "test-$name") 'Shared player data changed' }
    Require ((@(Get-WorldManifest (Join-Path $Build 'world')) | ConvertTo-Json -Depth 4) -eq $buildBefore) 'Build source changed'
    $backup = @(Get-ChildItem -LiteralPath (Join-Path $Root 'Script/WorldBackups') -Directory)[0]
    Require ((Read-Fixture (Join-Path $backup.FullName 'previous-world/level.dat')) -eq 'old-test-level') 'Original world backup missing'

    Write-Fixture (Join-Path $Test 'world/DIM201/region/template.mca') 'latest-test-template'
    Write-Fixture (Join-Path $Build 'world/region/new.mca') 'latest-build-overworld'
    Reset-TestOverworldFromBuild
    Require ((Read-Fixture (Join-Path $Test 'world/DIM201/region/template.mca')) -eq 'latest-test-template') 'Second restart lost latest Realm 1'
    Require ((Read-Fixture (Join-Path $Test 'world/region/new.mca')) -eq 'latest-build-overworld') 'Second restart missed Build changes'
    $stable = @(Get-WorldManifest (Join-Path $Test 'world')) | ConvertTo-Json -Depth 4

    function Test-WorldServerPort([string]$Server) { return $Server -eq $Build }
    Require-Failure { Reset-TestOverworldFromBuild }
    function Test-WorldServerPort([string]$Server) { return $false }
    $held = Open-WorldControlLock '.build-launch.lock'
    try { Require-Failure { Reset-TestOverworldFromBuild } } finally { $held.Dispose() }

    $copyFunction = (Get-Item Function:\Copy-RealmWorldEntryVerified).ScriptBlock
    function Copy-RealmWorldEntryVerified($Entry, [string]$Stage) { throw 'Injected copy failure' }
    Require-Failure { Reset-TestOverworldFromBuild }
    Set-Item Function:\Copy-RealmWorldEntryVerified $copyFunction
    Require ((@(Get-WorldManifest (Join-Path $Test 'world')) | ConvertTo-Json -Depth 4) -eq $stable) 'Failed copy modified Test'

    function Move-Item($LiteralPath, $Destination) {
        if ([IO.Path]::GetFileName($LiteralPath) -like '.world-stage-*') { throw 'Injected publish failure' }
        Microsoft.PowerShell.Management\Move-Item -LiteralPath $LiteralPath -Destination $Destination
    }
    Require-Failure { Reset-TestOverworldFromBuild }
    Remove-Item Function:\Move-Item
    Require ((@(Get-WorldManifest (Join-Path $Test 'world')) | ConvertTo-Json -Depth 4) -eq $stable) 'Failed publication did not restore Test'

    New-Item -ItemType Directory -Path (Join-Path $Test 'world/.revolution-realm-sync') | Out-Null
    Require-Failure { Assert-RealmOverworldSyncReady }
    Remove-Item -LiteralPath (Join-Path $Test 'world/.revolution-realm-sync')
    Rename-Item -LiteralPath (Join-Path $Test 'world/DIM201') -NewName 'DIM201-preserved'
    Require-Failure { Assert-RealmOverworldSyncReady }
    Rename-Item -LiteralPath (Join-Path $Test 'world/DIM201-preserved') -NewName 'DIM201'
    Require ((Read-Fixture (Join-Path $Test 'config-sentinel.txt')) -eq 'config') 'Server config changed'
    Require ((Read-Fixture (Join-Path $Test 'mongo-sentinel.txt')) -eq 'mongo') 'Database sentinel changed'
    Write-Host 'PASS: offline Build overworld refreshed twice; Test Realm 1/dimensions/player data preserved; backups, busy Build, locks, failed copies/publication, interrupted sync and missing template checked.'
} finally {
    $resolved = [IO.Path]::GetFullPath($fixture)
    if ([IO.Path]::GetDirectoryName($resolved) -ne [IO.Path]::GetFullPath($PSScriptRoot) -or [IO.Path]::GetFileName($resolved) -notlike '.overworld-fixture-*') {
        throw 'Refusing cleanup outside fixture directory'
    }
    if (Test-Path -LiteralPath $resolved) { Remove-Item -LiteralPath $resolved -Recurse -Force }
}
