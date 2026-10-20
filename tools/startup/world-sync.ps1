# Dot-source after common.ps1. This module never accesses MongoDB or copies mods/config.
$ErrorActionPreference = 'Stop'

function Test-WorldServerPort([string]$Server) {
    $properties = Read-Properties (Join-Path $Server 'server.properties')
    $port = 0
    if (-not [int]::TryParse([string]$properties['server-port'], [ref]$port) -or $port -lt 1 -or $port -gt 65535) { throw "Invalid server-port: $Server" }
    $client = New-Object Net.Sockets.TcpClient
    try { return ($client.ConnectAsync('127.0.0.1', $port).Wait(500) -and $client.Connected) }
    catch [AggregateException] { return $false }
    finally { $client.Dispose() }
}

function Assert-WorldServerOffline([string]$Server) {
    if (Test-WorldServerPort $Server) { throw "Server must be stopped: $Server" }
    $pidPath = Join-Path $Server '.server.pid'
    if (Test-Path -LiteralPath $pidPath) {
        $serverPid = 0
        if ([int]::TryParse([IO.File]::ReadAllText($pidPath).Trim(), [ref]$serverPid) -and $serverPid -gt 0) {
            $candidate = Get-Process -Id $serverPid -ErrorAction SilentlyContinue
            if ($candidate -and $candidate.ProcessName -match '^java(w)?$') { throw "Java is still starting/stopping: $Server" }
        }
    }
}

function Assert-NoWorldLinks([string]$Directory) {
    if ((Get-Item -LiteralPath $Directory -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw "Linked directory refused: $Directory" }
    if (Get-ChildItem -LiteralPath $Directory -Recurse -Force | Where-Object { $_.Attributes -band [IO.FileAttributes]::ReparsePoint } | Select-Object -First 1) {
        throw "Link inside world refused: $Directory"
    }
}

function Assert-WorldPair {
    foreach ($entry in @(@($Build, 'Server-Build'), @($Test, 'Server-Test'))) {
        $server = [IO.Path]::GetFullPath($entry[0])
        $expected = [IO.Path]::GetFullPath((Join-Path $Root $entry[1]))
        if ($server -ne $expected) { throw 'Unexpected Build/Test root' }
        if ((Get-Item -LiteralPath $server -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Linked server root refused' }
        $properties = Read-Properties (Join-Path $server 'server.properties')
        if ($properties['level-name'] -ne 'world') { throw 'World sync expects level-name=world on both servers' }
        $world = Join-Path $server 'world'
        if (-not (Test-Path -LiteralPath (Join-Path $world 'level.dat') -PathType Leaf)) { throw "Missing persistent level.dat: $world" }
        Assert-NoWorldLinks $world
    }
    if ($Build -eq $Test) { throw 'Build and Test must differ' }
    $bp = Read-Properties (Join-Path $Build 'server.properties')
    $tp = Read-Properties (Join-Path $Test 'server.properties')
    if ($bp['server-port'] -eq $tp['server-port']) { throw 'Build/Test ports must differ' }
}

function Open-WorldControlLock([string]$Name) {
    if ($Name -notin @('.server-workflow.lock', '.test-workflow.lock', '.build-launch.lock', '.build-snapshot.lock')) { throw 'Unexpected lock name' }
    return [IO.File]::Open((Join-Path (Join-Path $Root 'Script') $Name), 'OpenOrCreate', 'ReadWrite', 'None')
}

function Open-WorldSessionLease([string]$Server) {
    $path = Join-Path (Join-Path $Server 'world') 'session.lock'
    if (Test-Path -LiteralPath $path) { return [IO.File]::Open($path, 'Open', 'ReadWrite', 'None') }
    return $null
}

function Get-WorldFileFingerprint([string]$Path) {
    # Java keeps region files open for writing even while save-off is active.
    # Get-FileHash -LiteralPath uses an incompatible sharing mode on Windows.
    # Allow the existing writer handle without changing the source file.
    $sharing = [IO.FileShare]::ReadWrite -bor [IO.FileShare]::Delete
    $stream = [IO.File]::Open($Path, [IO.FileMode]::Open, [IO.FileAccess]::Read, $sharing)
    $algorithm = $null
    try {
        $length = $stream.Length
        $algorithm = [Security.Cryptography.SHA256]::Create()
        $hash = [BitConverter]::ToString($algorithm.ComputeHash($stream)).Replace('-', '')
        if ($stream.Length -ne $length) { throw "World file changed size while hashing: $Path" }
        return [pscustomobject]@{ Length = $length; SHA256 = $hash }
    } finally {
        if ($algorithm) { $algorithm.Dispose() }
        $stream.Dispose()
    }
}

function Get-WorldManifest([string]$Directory) {
    $base = [IO.Path]::GetFullPath($Directory).TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
    return @(Get-ChildItem -LiteralPath $Directory -File -Recurse -Force | Where-Object { $_.Name -ne 'session.lock' } | ForEach-Object {
        $fingerprint = Get-WorldFileFingerprint $_.FullName
        [pscustomobject]@{ Path = $_.FullName.Substring($base.Length); Length = $fingerprint.Length; SHA256 = $fingerprint.SHA256 }
    } | Sort-Object Path)
}

function Assert-WorldCopyMatches([string]$Source, [string]$Destination) {
    Assert-NoWorldLinks $Source
    Assert-NoWorldLinks $Destination
    $before = @(Get-WorldManifest $Source)
    $after = @(Get-WorldManifest $Destination)
    if ($before.Count -ne $after.Count) { throw 'World snapshot file count mismatch' }
    for ($i = 0; $i -lt $before.Count; $i++) {
        if ($before[$i].Path -cne $after[$i].Path -or $before[$i].Length -ne $after[$i].Length -or $before[$i].SHA256 -ne $after[$i].SHA256) {
            throw ('World snapshot verification failed: ' + $before[$i].Path)
        }
    }
    return $before
}

function Copy-WorldVerified([string]$Source, [string]$Destination) {
    if (Test-Path -LiteralPath $Destination) { throw "Snapshot target already exists: $Destination" }
    Assert-NoWorldLinks $Source
    New-Item -ItemType Directory -Path $Destination | Out-Null
    & robocopy.exe $Source $Destination /E /COPY:DAT /DCOPY:DAT /XJ /XF session.lock /R:1 /W:1 /NFL /NDL /NJH /NJS | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "World copy failed ($LASTEXITCODE); incomplete snapshot retained at $Destination" }
    return @(Assert-WorldCopyMatches $Source $Destination)
}

function New-WorldBackupDirectory([string]$Kind) {
    if ($Kind -notin @('promotion', 'test-reset')) { throw 'Unexpected backup type' }
    $parent = Join-Path (Join-Path $Root 'Script') 'WorldBackups'
    if (-not (Test-Path -LiteralPath $parent)) { New-Item -ItemType Directory -Path $parent | Out-Null }
    if ((Get-Item -LiteralPath $parent -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Linked backup directory refused' }
    $name = $Kind + '-' + [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmssZ') + '-' + [Guid]::NewGuid().ToString('N').Substring(0, 8)
    $path = Join-Path $parent $name
    New-Item -ItemType Directory -Path $path | Out-Null
    return $path
}

function Install-WorldStage([string]$Stage, [string]$Server, [string]$BackupDirectory) {
    # Verify every final absolute move endpoint; no recursive delete is used.
    $serverFull = [IO.Path]::GetFullPath($Server)
    if ($serverFull -notin @([IO.Path]::GetFullPath($Build), [IO.Path]::GetFullPath($Test))) { throw 'Unexpected destination server' }
    $stageFull = [IO.Path]::GetFullPath($Stage)
    if ([IO.Path]::GetDirectoryName($stageFull) -ne $serverFull -or [IO.Path]::GetFileName($stageFull) -notlike '.world-stage-*') { throw 'Unexpected world staging path' }
    $world = [IO.Path]::GetFullPath((Join-Path $serverFull 'world'))
    $backupRoot = [IO.Path]::GetFullPath((Join-Path (Join-Path $Root 'Script') 'WorldBackups')).TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
    $backupFull = [IO.Path]::GetFullPath($BackupDirectory)
    if (-not $backupFull.StartsWith($backupRoot, [StringComparison]::OrdinalIgnoreCase)) { throw 'Backup path escaped WorldBackups' }
    Assert-NoWorldLinks $stageFull
    Assert-NoWorldLinks $backupFull
    $old = Join-Path $backupFull 'previous-world'
    if (Test-Path -LiteralPath $old) { throw 'Previous-world backup already exists' }
    Assert-WorldServerOffline $Server
    Move-Item -LiteralPath $world -Destination $old
    try { Move-Item -LiteralPath $stageFull -Destination $world }
    catch {
        if (-not (Test-Path -LiteralPath $world)) { Move-Item -LiteralPath $old -Destination $world }
        throw
    }
    return $old
}

function Assert-WorldSyncReady {
    Assert-WorldPair
    $modeFile = Join-Path (Join-Path $Root 'Script') 'build-test-world-mode.json'
    if (-not (Test-Path -LiteralPath $modeFile)) { throw 'Initialize Build from Test first: promote-test-world-to-build.ps1' }
    $mode = Get-Content -LiteralPath $modeFile -Raw | ConvertFrom-Json
    if ($mode.Mode -ne 'world-only' -or $mode.Source -ne 'Server-Build' -or $mode.Destination -ne 'Server-Test') { throw 'Unexpected world-sync mode' }
}

function Reset-TestWorldFromBuild {
    Assert-WorldSyncReady
    Assert-WorldServerOffline $Test
    $snapshotLock = Open-WorldControlLock '.build-snapshot.lock'
    $buildLaunch = $null; $buildSession = $null; $testSession = $null; $savingDisabled = $false
    try {
        $testSession = Open-WorldSessionLease $Test
        try {
            if (Test-WorldServerPort $Build) {
                $savingDisabled = $true
                Invoke-Rcon 'save-off'
                Invoke-Rcon 'save-all flush'
            } else {
                $buildLaunch = Open-WorldControlLock '.build-launch.lock'
                Assert-WorldServerOffline $Build
                $buildSession = Open-WorldSessionLease $Build
            }
            $stage = Join-Path $Test ('.world-stage-' + [Guid]::NewGuid().ToString('N'))
            $manifest = @(Copy-WorldVerified (Join-Path $Build 'world') $stage)
        } finally {
            if ($buildSession) { $buildSession.Dispose(); $buildSession = $null }
            if ($buildLaunch) { $buildLaunch.Dispose(); $buildLaunch = $null }
            if ($savingDisabled) { Invoke-Rcon 'save-on' }
        }
        $backup = New-WorldBackupDirectory 'test-reset'
        $manifest | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $backup 'world-manifest.json') -Encoding UTF8
        if ($testSession) { $testSession.Dispose(); $testSession = $null }
        $old = Install-WorldStage $stage $Test $backup
        Write-Host "Build -> Test world snapshot verified ($($manifest.Count) files). Previous Test world: $old"
        Write-Host 'MongoDB, mods and settings were NOT copied from Build.'
    } finally {
        if ($testSession) { $testSession.Dispose() }
        if ($buildSession) { $buildSession.Dispose() }
        if ($buildLaunch) { $buildLaunch.Dispose() }
        $snapshotLock.Dispose()
    }
}

function Initialize-BuildWorldFromTest([switch]$CheckOnly) {
    Assert-WorldPair
    Assert-WorldServerOffline $Build
    Assert-WorldServerOffline $Test
    $modeFile = Join-Path (Join-Path $Root 'Script') 'build-test-world-mode.json'
    if (Test-Path -LiteralPath $modeFile) { throw 'World-only Build/Test mode was already initialized; refusing to promote twice' }
    $leases = New-Object 'System.Collections.Generic.List[System.IDisposable]'
    try {
        foreach ($name in @('.server-workflow.lock', '.test-workflow.lock', '.build-launch.lock', '.build-snapshot.lock')) { $leases.Add((Open-WorldControlLock $name)) }
        $testSession = Open-WorldSessionLease $Test
        $buildSession = Open-WorldSessionLease $Build
        if ($testSession) { $leases.Add($testSession) }
        if ($buildSession) { $leases.Add($buildSession) }
        if ($CheckOnly) { Write-Host 'Promotion preflight OK. Both servers stopped; worlds present; no world changed.'; return }
        $backup = New-WorldBackupDirectory 'promotion'
        $testBackup = Join-Path $backup 'Test-original'
        $manifest = @(Copy-WorldVerified (Join-Path $Test 'world') $testBackup)
        $stage = Join-Path $Build ('.world-stage-' + [Guid]::NewGuid().ToString('N'))
        $null = Copy-WorldVerified $testBackup $stage
        if ($buildSession) { $buildSession.Dispose() }
        $oldBuild = Install-WorldStage $stage $Build $backup
        $record = [ordered]@{ Mode = 'world-only'; Source = 'Server-Build'; Destination = 'Server-Test'; InitializedUtc = [DateTime]::UtcNow.ToString('o'); PromotedFrom = 'Server-Test'; OriginalTestBackup = $testBackup; OriginalBuildBackup = $oldBuild; Files = $manifest.Count }
        $manifest | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $backup 'world-manifest.json') -Encoding UTF8
        $record | ConvertTo-Json | Set-Content -LiteralPath $modeFile -Encoding UTF8
        Write-Host "Test world promoted to Build. Original Test backup: $testBackup"
        Write-Host "Previous Build world: $oldBuild"
        Write-Host 'No servers started. Databases, mods and settings unchanged.'
    } finally { foreach ($lease in $leases) { $lease.Dispose() } }
}
