$ErrorActionPreference = 'Stop'
$fixture = Join-Path $PSScriptRoot ('.launcher-fixture-' + [Guid]::NewGuid().ToString('N'))
$scriptDirectory = Join-Path $fixture 'Script'
$projectDirectory = Join-Path $fixture 'Project'
$serverDirectory = Join-Path $fixture 'Server-Test'
$sourceMarker = Join-Path $projectDirectory 'src/main/java/com/example/revolution/realm/world/RealmStartupSync.java'
$previousAppData = $env:APPDATA

function Require($Condition, $Message) { if (-not $Condition) { throw $Message } }
function Has-Call($Name) { return (Get-Content -LiteralPath (Join-Path $fixture 'calls.txt')) -contains $Name }
function Invoke-Case([string[]]$Arguments) {
    [IO.File]::WriteAllText((Join-Path $fixture 'calls.txt'), '')
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $scriptDirectory 'build-server-run.ps1') @Arguments
    Require ($LASTEXITCODE -eq 0) 'Fixture launcher failed'
    Require ((Get-Content -LiteralPath (Join-Path $serverDirectory 'world/DIM201/keep.dat')) -eq 'realm-one') 'Realm 1 changed outside Forge'
    Require ((Get-Content -LiteralPath (Join-Path $serverDirectory 'world/level.dat')) -eq 'overworld') 'Realm 0 metadata changed'
}

try {
    foreach ($directory in @($scriptDirectory, $projectDirectory, (Join-Path $serverDirectory 'world/DIM201'), (Split-Path $sourceMarker -Parent))) {
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
    }
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'build-server-run.ps1') -Destination $scriptDirectory
    'realm-enabled' | Set-Content -LiteralPath $sourceMarker
    'realm-one' | Set-Content -LiteralPath (Join-Path $serverDirectory 'world/DIM201/keep.dat')
    'overworld' | Set-Content -LiteralPath (Join-Path $serverDirectory 'world/level.dat')
    @('server-port=65192', 'level-name=world') | Set-Content -LiteralPath (Join-Path $serverDirectory 'server.properties')
    'fixture jar' | Set-Content -LiteralPath (Join-Path $fixture 'mod.jar')
    '@exit /b 0' | Set-Content -LiteralPath (Join-Path $projectDirectory 'gradlew.bat') -Encoding ASCII
    $env:APPDATA = Join-Path $fixture 'AppData'
    @'
$ErrorActionPreference = 'Stop'
$Root = Split-Path $PSScriptRoot -Parent
$Test = Join-Path $Root 'Server-Test'
$TestDb = 'fixture-only'
$Project = Join-Path $Root 'Project'
function Record($Name) { Add-Content -LiteralPath (Join-Path $Root 'calls.txt') -Value $Name }
function Assert-TestStopped([switch]$ReadOnly) { Record 'offline' }
function Assert-ServerServicesReady { Record 'services-check' }
function Initialize-ServerServices($Server) { Record 'services-init' }
function Read-Properties($Path) { return @{'server-port'='65192'; 'level-name'='world'} }
function Get-ModBuildArtifact { return [pscustomobject]@{Path=(Join-Path $Root 'mod.jar'); FileName='mod.jar'} }
function Get-OwnModJars($Path) { return @() }
function Assert-IsolatedMod($Server) { Record 'installed-check' }
function Get-ServerLaunchArguments($Server, $Database) { return @('fixture') }
function Start-Forge($Server, $Database) { Record 'start' }
'@ | Set-Content -LiteralPath (Join-Path $scriptDirectory 'common.ps1') -Encoding UTF8
    @'
function Assert-WorldSyncReady { Record 'build-world-check' }
function Reset-TestWorldFromBuild { Record 'world-reset' }
'@ | Set-Content -LiteralPath (Join-Path $scriptDirectory 'world-sync.ps1') -Encoding UTF8

    Invoke-Case @('-CheckOnly')
    Require (Has-Call 'services-check') 'CheckOnly missed service validation'
    Require (-not (Has-Call 'world-reset')) 'CheckOnly reset the world'
    Require (-not (Has-Call 'build-world-check')) 'Realm startup still depends on Build snapshot'
    Require (-not (Has-Call 'start')) 'CheckOnly started Forge'

    Invoke-Case @('-PrepareOnly')
    Require (Has-Call 'installed-check') 'PrepareOnly missed installed-mod guard'
    Require (-not (Has-Call 'world-reset')) 'Realm PrepareOnly reset the world'
    Require (-not (Has-Call 'start')) 'PrepareOnly started Forge'

    Invoke-Case @()
    Require (Has-Call 'start') 'Normal startup did not launch Forge'
    Require (-not (Has-Call 'world-reset')) 'Realm startup replaced authoritative world'

    Remove-Item -LiteralPath $sourceMarker
    Invoke-Case @('-CheckOnly')
    Require (Has-Call 'build-world-check') 'Legacy preflight lost its Build snapshot guard'
    Require (-not (Has-Call 'world-reset')) 'Legacy CheckOnly replaced data'
    Invoke-Case @('-PrepareOnly')
    Require (Has-Call 'world-reset') 'Legacy workflow changed unexpectedly'
    Require (-not (Has-Call 'start')) 'Legacy PrepareOnly started Forge'
    Invoke-Case @()
    Require ((Has-Call 'world-reset') -and (Has-Call 'start')) 'Legacy normal workflow changed'
    Write-Host 'PASS: Realm restart preserves world; read-only preflight and PrepareOnly never sync; legacy workflow retained. No real server or database used.'
} finally {
    $env:APPDATA = $previousAppData
    $resolvedFixture = [IO.Path]::GetFullPath($fixture)
    $resolvedParent = [IO.Path]::GetFullPath($PSScriptRoot)
    if ([IO.Path]::GetDirectoryName($resolvedFixture) -ne $resolvedParent -or [IO.Path]::GetFileName($resolvedFixture) -notlike '.launcher-fixture-*') {
        throw 'Refusing cleanup outside fixture directory'
    }
    if (Test-Path -LiteralPath $resolvedFixture) { Remove-Item -LiteralPath $resolvedFixture -Recurse -Force }
}
