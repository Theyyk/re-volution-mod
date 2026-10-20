param(
    [switch]$CheckOnly,
    [switch]$PrepareOnly
)

. "$PSScriptRoot\common.ps1"

# Normal runtime startup never imports a Build world.
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
    if (-not $preserveRealmWorld) {
        throw 'Realm runtime startup requires RealmStartupSync.java; select feature/v1.3.4-realm-foundation in Project.'
    }

    $properties = Read-Properties "$Server\server.properties"

    if (-not $properties['server-port']) {
        throw 'server-port is missing in server.properties'
    }

    if (-not $properties['level-name']) {
        throw 'level-name is missing in server.properties'
    }

    $worldMode = 'persistent Test Realm 0/1; startup Realm 1 -> 2/3'
    Write-Host "Test server ($worldMode):"
    Write-Host "  Path:     $Server"
    Write-Host "  World:    $($properties['level-name'])"
    Write-Host "  Port:     $($properties['server-port'])"
    Write-Host "  MongoDB:  $Database"
    Write-Host ""

    if ($CheckOnly) {
        Assert-ServerServicesReady
        Write-Host "Preflight OK. Realm 0/1 stay in Test; Forge syncs only Realm 1 -> 2/3. Build is not accessed."
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
    Write-Host "=== Preserving Test Realm 0 and Realm 1; no Build world copy ==="

    if ($PrepareOnly) {
        Write-Host ""
        Write-Host "Build/install complete. Server was not started."
        return
    }

    Write-Host ""
    Write-Host "=== Starting Test ==="
    Write-Host "Realm 0/1 stay in Test. Forge syncs Realm 1 -> 2/3 before world loading. MongoDB is unchanged."
    Write-Host ""

    Start-Forge $Server $Database
}
finally {
    $workflowLock.Dispose()
}
