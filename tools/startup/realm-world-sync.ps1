# Dot-source after world-sync.ps1. Explicit Realm 0 import; never used by normal startup.
$ErrorActionPreference = 'Stop'

function Test-RealmPreservedEntry($Entry) {
    return ($Entry.Name -match '^DIM' -or $Entry.Name -in @('playerdata', 'stats', 'advancements'))
}

function Test-RealmControlEntry($Entry) {
    return ($Entry.Name -eq 'session.lock' -or $Entry.Name -like '.revolution-*')
}

function Assert-RealmOverworldSyncReady {
    Assert-WorldPair
    if (Test-WorldServerPort $Build) {
        $properties = Read-Properties (Join-Path $Build 'server.properties')
        $port = 0
        if ($properties['enable-rcon'] -ne 'true' -or -not $properties['rcon.password'] -or
            -not [int]::TryParse([string]$properties['rcon.port'], [ref]$port) -or $port -lt 1 -or $port -gt 65535) {
            throw 'Running Build requires configured RCON for save-off / save-all flush / save-on'
        }
    } else { Assert-WorldServerOffline $Build }
    Assert-WorldServerOffline $Test
    $world = Join-Path $Test 'world'
    if (Test-Path -LiteralPath (Join-Path $world '.revolution-realm-sync')) {
        throw 'Recover the interrupted Realm transaction before refreshing overworld'
    }
    if (-not (Test-Path -LiteralPath (Join-Path $world 'DIM201') -PathType Container)) {
        if ((Test-Path -LiteralPath (Join-Path $world 'DIM202')) -or (Test-Path -LiteralPath (Join-Path $world 'DIM203'))) {
            throw 'Realm 1 template is missing; existing Realm 2/3 must be preserved'
        }
    }
}

function Copy-RealmWorldEntryVerified($Entry, [string]$Stage) {
    $destination = Join-Path $Stage $Entry.Name
    if (Test-Path -LiteralPath $destination) { throw "Duplicate staged entry: $destination" }
    if ($Entry.PSIsContainer) {
        $null = Copy-WorldVerified $Entry.FullName $destination
    } else {
        $before = Get-WorldFileFingerprint $Entry.FullName
        Copy-Item -LiteralPath $Entry.FullName -Destination $destination
        $after = Get-WorldFileFingerprint $destination
        $current = Get-WorldFileFingerprint $Entry.FullName
        if ($before.Length -ne $after.Length -or $before.SHA256 -ne $after.SHA256 -or
            $before.Length -ne $current.Length -or $before.SHA256 -ne $current.SHA256) {
            throw "World entry changed or failed verification: $($Entry.FullName)"
        }
    }
}

function Assert-TestToBuildRealm0Ready {
    Assert-WorldPair
    Assert-WorldServerOffline $Build
    Assert-WorldServerOffline $Test
}

function Reset-BuildOverworldFromTest {
    $leases = New-Object 'System.Collections.Generic.List[System.IDisposable]'
    $fileLeases = New-Object 'System.Collections.Generic.List[System.IDisposable]'
    try {
        foreach ($name in @('.build-snapshot.lock', '.build-launch.lock', '.test-workflow.lock')) {
            $leases.Add((Open-WorldControlLock $name))
        }
        Assert-TestToBuildRealm0Ready
        foreach ($server in @($Build, $Test)) {
            $session = Open-WorldSessionLease $server
            if ($session) { $fileLeases.Add($session) }
            $realmLock = Join-Path $server 'world/.revolution-realm.lock'
            if (Test-Path -LiteralPath $realmLock) { $fileLeases.Add([IO.File]::Open($realmLock, 'Open', 'ReadWrite', 'None')) }
        }
        $stage = Join-Path $Build ('.world-stage-' + [Guid]::NewGuid().ToString('N'))
        New-Item -ItemType Directory -Path $stage | Out-Null
        foreach ($entry in Get-ChildItem -LiteralPath (Join-Path $Test 'world') -Force) {
            if (-not (Test-RealmPreservedEntry $entry) -and -not (Test-RealmControlEntry $entry)) {
                Copy-RealmWorldEntryVerified $entry $stage
            }
        }
        foreach ($entry in Get-ChildItem -LiteralPath (Join-Path $Build 'world') -Force) {
            if (Test-RealmPreservedEntry $entry) { Copy-RealmWorldEntryVerified $entry $stage }
        }
        $manifest = @(Get-WorldManifest $stage)
        $backup = New-WorldBackupDirectory 'promotion'
        $manifest | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $backup 'world-manifest.json') -Encoding UTF8
        for ($i = $fileLeases.Count - 1; $i -ge 0; $i--) { $fileLeases[$i].Dispose() }
        $fileLeases.Clear()
        $old = Install-WorldStage $stage $Build $backup
        Write-Host "Test Realm 0 -> Build verified. Test dimensions were excluded; previous Build world: $old"
    } finally {
        for ($i = $fileLeases.Count - 1; $i -ge 0; $i--) { $fileLeases[$i].Dispose() }
        for ($i = $leases.Count - 1; $i -ge 0; $i--) { $leases[$i].Dispose() }
    }
}

function Reset-TestOverworldFromBuild {
    $leases = New-Object 'System.Collections.Generic.List[System.IDisposable]'
    $fileLeases = New-Object 'System.Collections.Generic.List[System.IDisposable]'
    $savingDisabled = $false
    try {
        $leases.Add((Open-WorldControlLock '.build-snapshot.lock'))
        $leases.Add((Open-WorldControlLock '.test-workflow.lock'))
        Assert-RealmOverworldSyncReady
        # Test must be offline; a running Build is captured after a flushed save with saving paused.
        $serversToLease = @($Test)
        if (-not (Test-WorldServerPort $Build)) {
            $leases.Add((Open-WorldControlLock '.build-launch.lock'))
            Assert-WorldServerOffline $Build
            $serversToLease += $Build
        }
        foreach ($server in $serversToLease) {
            $session = Open-WorldSessionLease $server
            if ($session) { $fileLeases.Add($session) }
            $realmLock = Join-Path $server 'world/.revolution-realm.lock'
            if (Test-Path -LiteralPath $realmLock) {
                $fileLeases.Add([IO.File]::Open($realmLock, 'Open', 'ReadWrite', 'None'))
            }
        }
        $stage = Join-Path $Test ('.world-stage-' + [Guid]::NewGuid().ToString('N'))
        New-Item -ItemType Directory -Path $stage | Out-Null
        try {
            if (Test-WorldServerPort $Build) {
                # Set first so even an ambiguous save-off/RCON failure triggers save-on.
                $savingDisabled = $true
                Invoke-Rcon 'save-off'
                Invoke-Rcon 'save-all flush'
            }
            foreach ($entry in Get-ChildItem -LiteralPath (Join-Path $Build 'world') -Force) {
                if (-not (Test-RealmPreservedEntry $entry) -and -not (Test-RealmControlEntry $entry)) {
                    Copy-RealmWorldEntryVerified $entry $stage
                }
            }
        } finally {
            if ($savingDisabled) { Invoke-Rcon 'save-on'; $savingDisabled = $false }
        }
        foreach ($entry in Get-ChildItem -LiteralPath (Join-Path $Test 'world') -Force) {
            if (Test-RealmPreservedEntry $entry) { Copy-RealmWorldEntryVerified $entry $stage }
        }
        # No source DIM folder is imported; preserved Test dimensions are byte-verified copies.
        $manifest = @(Get-WorldManifest $stage)
        $backup = New-WorldBackupDirectory 'test-reset'
        $manifest | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $backup 'world-manifest.json') -Encoding UTF8
        # Windows directory publication requires closing file leases; launcher locks remain held.
        for ($i = $fileLeases.Count - 1; $i -ge 0; $i--) { $fileLeases[$i].Dispose() }
        $fileLeases.Clear()
        $old = Install-WorldStage $stage $Test $backup
        Write-Host "Build overworld -> Test verified; Test Realm 1 and other dimensions/player data preserved. Backup: $old"
        Write-Host 'Realm 1/2/3 were not synchronized. That happens on the next normal Forge startup. MongoDB is unchanged.'
    } finally {
        try {
            # Retry restoration if the first save-on itself failed; never publish after that failure.
            if ($savingDisabled) { Invoke-Rcon 'save-on' }
        } finally {
            for ($i = $fileLeases.Count - 1; $i -ge 0; $i--) { $fileLeases[$i].Dispose() }
            for ($i = $leases.Count - 1; $i -ge 0; $i--) { $leases[$i].Dispose() }
        }
    }
}
