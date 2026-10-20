param([switch]$CheckOnly)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"
. "$PSScriptRoot\world-sync.ps1"
. "$PSScriptRoot\realm-world-sync.ps1"

$workflowLock = Open-WorldControlLock '.server-workflow.lock'
try {
    Assert-TestStopped -ReadOnly:$CheckOnly
    Assert-RealmOverworldSyncReady
    if ($CheckOnly) {
        Write-Host 'Realm 0 copy preflight OK. No worlds changed, no RCON save commands sent, no server started.'
        return
    }
    Reset-TestOverworldFromBuild
    Write-Host 'Realm 0 replaced from Build. Test Realm 1/2/3 and player data preserved. No build/install/server startup or Realm 1 -> 2/3 sync was performed.'
} finally { $workflowLock.Dispose() }
