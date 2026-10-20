param([switch]$CheckOnly)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"
. "$PSScriptRoot\world-sync.ps1"
. "$PSScriptRoot\realm-world-sync.ps1"

$workflowLock = Open-WorldControlLock '.server-workflow.lock'
try {
    Assert-TestStopped -ReadOnly:$CheckOnly
    Assert-TestToBuildRealm0Ready
    if ($CheckOnly) {
        Write-Host 'Test Realm 0 -> Build preflight OK. No worlds changed and no server started.'
        return
    }
    Reset-BuildOverworldFromTest
    Write-Host 'Build Realm 0 replaced from Test Realm 0. Test Realm 1/2/3 were not read/copied; Build dimensions/player data preserved. No server started.'
} finally { $workflowLock.Dispose() }
