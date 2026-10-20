# Dot-source after world-sync.ps1. Compose an offline save without importing Build dimensions/player data.
$ErrorActionPreference = 'Stop'

function Test-RealmPreservedEntry($Entry) {
    return ($Entry.Name -match '^DIM' -or $Entry.Name -in @('playerdata', 'stats', 'advancements'))
}

function Test-RealmControlEntry($Entry) {
    return ($Entry.Name -eq 'session.lock' -or $Entry.Name -like '.revolution-*')
}

function Assert-RealmOverworldSyncReady {
    Assert-WorldPair
    # Copying an open Build world is deliberately forbidden, even with save-off.
    Assert-WorldServerOffline $Build
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

function Reset-TestOverworldFromBuild {
    $leases = New-Object 'System.Collections.Generic.List[System.IDisposable]'
    try {
        $leases.Add((Open-WorldControlLock '.build-snapshot.lock'))
        $leases.Add((Open-WorldControlLock '.build-launch.lock'))
        $leases.Add((Open-WorldControlLock '.test-workflow.lock'))
        Assert-RealmOverworldSyncReady
        foreach ($server in @($Build, $Test)) {
            $session = Open-WorldSessionLease $server
            if ($session) { $leases.Add($session) }
            $realmLock = Join-Path $server 'world/.revolution-realm.lock'
            if (Test-Path -LiteralPath $realmLock) {
                $leases.Add([IO.File]::Open($realmLock, 'Open', 'ReadWrite', 'None'))
            }
        }
        $stage = Join-Path $Test ('.world-stage-' + [Guid]::NewGuid().ToString('N'))
        New-Item -ItemType Directory -Path $stage | Out-Null
        foreach ($entry in Get-ChildItem -LiteralPath (Join-Path $Build 'world') -Force) {
            if (-not (Test-RealmPreservedEntry $entry) -and -not (Test-RealmControlEntry $entry)) {
                Copy-RealmWorldEntryVerified $entry $stage
            }
        }
        foreach ($entry in Get-ChildItem -LiteralPath (Join-Path $Test 'world') -Force) {
            if (Test-RealmPreservedEntry $entry) { Copy-RealmWorldEntryVerified $entry $stage }
        }
        # No source DIM folder is imported; preserved Test dimensions are byte-verified copies.
        $manifest = @(Get-WorldManifest $stage)
        $backup = New-WorldBackupDirectory 'test-reset'
        $manifest | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $backup 'world-manifest.json') -Encoding UTF8
        # Windows directory publication requires closing file leases; launcher locks remain held.
        for ($i = $leases.Count - 1; $i -ge 3; $i--) { $leases[$i].Dispose(); $leases.RemoveAt($i) }
        $old = Install-WorldStage $stage $Test $backup
        Write-Host "Build overworld -> Test verified; Test Realm 1 and other dimensions/player data preserved. Backup: $old"
        Write-Host 'Forge will copy Test DIM201 -> DIM202/DIM203 before loading dimensions. MongoDB is unchanged.'
    } finally {
        for ($i = $leases.Count - 1; $i -ge 0; $i--) { $leases[$i].Dispose() }
    }
}
