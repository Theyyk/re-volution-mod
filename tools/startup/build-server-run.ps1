param(
    [switch]$CheckOnly,
    [switch]$PrepareOnly
)

. "$PSScriptRoot\common.ps1"
. "$PSScriptRoot\world-sync.ps1"
. "$PSScriptRoot\realm-world-sync.ps1"

# This launcher builds Project before starting it. Realm-enabled source owns its
# persistent world; older branches retain the existing Build-to-Test refresh.
$preserveRealmWorld = Test-Path -LiteralPath (Join-Path $Project 'src/main/java/com/example/revolution/realm/world/RealmStartupSync.java') -PathType Leaf

$Server = $Test
$Database = $TestDb

$workflowLock = [IO.File]::Open(
    "$PSScriptRoot\.server-workflow.lock",
    'OpenOrCreate',
    'ReadWrite',
    'None'
)

try {
    if (-not (Test-Path -LiteralPath $Server -PathType Container)) {
        throw "Missing server directory: $Server"
    }

    if ((Get-Item -LiteralPath $Server).Attributes -band [IO.FileAttributes]::ReparsePoint) {
        throw "Server path must not be a link: $Server"
    }

    if (-not (Test-Path -LiteralPath "$Server\server.properties")) {
        throw "Missing server.properties: $Server"
    }

    if (-not (Test-Path -LiteralPath "$Server\world\level.dat")) {
        throw "Persistent world is missing: $Server\world\level.dat"
    }

    Assert-TestStopped -ReadOnly:$CheckOnly
    if ($preserveRealmWorld) { Assert-RealmOverworldSyncReady }
    else { Assert-WorldSyncReady }

    $properties = Read-Properties "$Server\server.properties"

    if (-not $properties['server-port']) {
        throw 'server-port is missing in server.properties'
    }

    if (-not $properties['level-name']) {
        throw 'level-name is missing in server.properties'
    }

    $worldMode = if ($preserveRealmWorld) { 'Build overworld + persistent Test Realm 1' } else { 'legacy Build -> Test refresh' }
    Write-Host "Test server ($worldMode):"
    Write-Host "  Path:     $Server"
    Write-Host "  World:    $($properties['level-name'])"
    Write-Host "  Port:     $($properties['server-port'])"
    Write-Host "  MongoDB:  $Database"
    Write-Host ""

    if ($CheckOnly) {
        Assert-ServerServicesReady
        if ($preserveRealmWorld) {
            Write-Host "Preflight OK. Offline Build overworld will refresh Realm 0; Test Realm 1 stays; Forge syncs 1 -> 2/3."
        } else {
            Write-Host "Preflight OK. Legacy startup copies Build/world -> Test/world. MongoDB is not reset."
        }
        return
    }

    # Fail before building/installing or refreshing the world if Auth/pack preparation fails.
    if (-not $PrepareOnly) { Initialize-ServerServices $Server }

    Push-Location $Project
    try {
        Write-Host "=== Building RE-volution ==="

        & .\gradlew.bat build
        if ($LASTEXITCODE -ne 0) {
            throw 'Gradle build failed'
        }

        $artifact = Get-ModBuildArtifact
        $jar = $artifact.Path

        Write-Host ""
        Write-Host "=== Installing server mod ==="

        $serverMods = Join-Path $Server "mods"
        New-Item -ItemType Directory -Path $serverMods -Force | Out-Null

        Get-OwnModJars $serverMods |
            Remove-Item -Force

        Copy-Item -LiteralPath $jar -Destination $serverMods

        Write-Host ""
        Write-Host "=== Installing client mod ==="

        $clientMods = Join-Path $env:APPDATA '.minecraft\mods'
        New-Item -ItemType Directory -Path $clientMods -Force | Out-Null

        Get-OwnModJars $clientMods |
            Remove-Item -Force

        Copy-Item -LiteralPath $jar -Destination $clientMods

        Write-Host ""
        Write-Host "Installed $($artifact.FileName)"
        # Verify the installed architecture/database override before replacing Test/world.
        Assert-IsolatedMod $Server
        $null = @(Get-ServerLaunchArguments $Server $Database)
    }
    finally {
        Pop-Location
    }

    Write-Host ""
    if ($preserveRealmWorld) {
        if (-not $PrepareOnly) {
            Write-Host "=== Refreshing Realm 0 from offline Build; preserving Test Realm 1 ==="
            Reset-TestOverworldFromBuild
        }
    } else {
        Write-Host "=== Copying Build world to Test (legacy build) ==="
        Reset-TestWorldFromBuild
    }

    if ($PrepareOnly) {
        Write-Host ""
        Write-Host "Build/install complete. Server was not started."
        return
    }

    Write-Host ""
    Write-Host "=== Starting Test ==="
    Write-Host "Test MongoDB remains independent. Realm 0 comes from Build; Realm 1 is the persistent Test template."
    Write-Host ""

    Start-Forge $Server $Database
}
finally {
    $workflowLock.Dispose()
}
