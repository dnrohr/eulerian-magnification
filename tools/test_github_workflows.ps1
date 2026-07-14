$ErrorActionPreference = "Stop"

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Assert-Contains {
    param(
        [string]$Content,
        [string]$Expected,
        [string]$Message
    )

    Assert-True -Condition $Content.Contains($Expected) -Message $Message
}

$workflowPath = Join-Path $PSScriptRoot "..\.github\workflows\offline-tooling.yml"
$workflowPath = (Resolve-Path -LiteralPath $workflowPath).Path
$workflow = Get-Content -LiteralPath $workflowPath -Raw

Assert-Contains -Content $workflow -Expected "name: Offline Tooling" -Message "Offline tooling workflow name is missing."
Assert-Contains -Content $workflow -Expected "push:" -Message "Offline tooling workflow push trigger is missing."
Assert-Contains -Content $workflow -Expected "pull_request:" -Message "Offline tooling workflow pull_request trigger is missing."
Assert-Contains -Content $workflow -Expected "      - main" -Message "Offline tooling workflow must run on main pushes."
Assert-Contains -Content $workflow -Expected "runs-on: windows-latest" -Message "Offline tooling workflow should run on Windows."
Assert-Contains -Content $workflow -Expected "shell: pwsh" -Message "Offline tooling workflow should use PowerShell."
Assert-Contains -Content $workflow -Expected "run: .\tools\test_offline_project_tooling.ps1" -Message "Offline tooling workflow should run the offline project tooling suite."

$offlineSuite = Join-Path $PSScriptRoot "test_offline_project_tooling.ps1"
Assert-True -Condition (Test-Path -LiteralPath $offlineSuite) -Message "Workflow target script does not exist: $offlineSuite"

Write-Output "GitHub workflow contract self-test passed."
