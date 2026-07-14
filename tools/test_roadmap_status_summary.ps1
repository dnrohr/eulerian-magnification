param(
    [string]$OutputRoot = ""
)

$ErrorActionPreference = "Stop"

function Assert-Equal {
    param(
        [object]$Actual,
        [object]$Expected,
        [string]$Message
    )

    if ($Actual -ne $Expected) {
        throw "$Message Expected '$Expected' but got '$Actual'."
    }
}

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Write-TextFile {
    param(
        [string]$Path,
        [string[]]$Lines
    )

    $directory = Split-Path -Parent $Path
    if (-not (Test-Path -LiteralPath $directory)) {
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
    }
    $Lines | Out-File -LiteralPath $Path -Encoding utf8
}

$root = if ([string]::IsNullOrWhiteSpace($OutputRoot)) {
    Join-Path ([System.IO.Path]::GetTempPath()) ("eulerian-roadmap-status-test-" + [Guid]::NewGuid().ToString("N"))
} else {
    $OutputRoot
}

if (Test-Path -LiteralPath $root) {
    Remove-Item -LiteralPath $root -Recurse -Force
}
New-Item -ItemType Directory -Path $root -Force | Out-Null

$taskRoot = Join-Path $root "tasks"
$indexPath = Join-Path $taskRoot "README.md"

Write-TextFile -Path $indexPath -Lines @(
    "# Synthetic Task Backlog",
    "",
    "| Milestone | File | Status |",
    "| --- | --- | --- |",
    "| A | [MILESTONE_A.md](MILESTONE_A.md) | Complete |",
    "| B | [MILESTONE_B.md](MILESTONE_B.md) | In Progress |",
    "| C | [MILESTONE_C.md](MILESTONE_C.md) | Complete |"
)

Write-TextFile -Path (Join-Path $taskRoot "MILESTONE_A.md") -Lines @(
    "# Milestone A",
    "",
    "Status: Complete",
    "",
    "No phone-only validation remains."
)

Write-TextFile -Path (Join-Path $taskRoot "MILESTONE_B.md") -Lines @(
    "# Milestone B",
    "",
    "Status: In progress",
    "",
    "Remaining validation needs Pixel visual validation with a known target."
)

Write-TextFile -Path (Join-Path $taskRoot "MILESTONE_C.md") -Lines @(
    "# Milestone C",
    "",
    "Status: In progress",
    "",
    "This intentionally disagrees with the index."
)

$summaryScript = Join-Path $PSScriptRoot "summarize_roadmap_status.ps1"
$json = & $summaryScript -TaskIndexPath $indexPath -Json | ConvertFrom-Json

Assert-Equal -Actual $json.total -Expected 3 -Message "Total milestone count"
Assert-Equal -Actual $json.statusCounts.Complete -Expected 2 -Message "Complete count"
Assert-Equal -Actual $json.statusCounts.'In Progress' -Expected 1 -Message "In-progress count"
Assert-Equal -Actual @($json.inProgress).Count -Expected 1 -Message "In-progress row count"
Assert-Equal -Actual $json.inProgress[0].milestone -Expected "B" -Message "In-progress milestone"
Assert-True -Condition ([bool]$json.inProgress[0].phoneGated) -Message "In-progress milestone should be phone gated."
Assert-Equal -Actual @($json.mismatches).Count -Expected 1 -Message "Mismatch count"
Assert-Equal -Actual $json.mismatches[0].milestone -Expected "C" -Message "Mismatch milestone"

$text = & $summaryScript -TaskIndexPath $indexPath
Assert-True -Condition (($text -join "`n") -match "Roadmap milestones: 3") -Message "Text output should include total count."
Assert-True -Condition (($text -join "`n") -match "Status mismatches:") -Message "Text output should include mismatch section."

Write-Output "Roadmap status summary self-test passed: $root"
