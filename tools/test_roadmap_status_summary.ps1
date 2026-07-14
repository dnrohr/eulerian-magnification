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
    "| C | [MILESTONE_C.md](MILESTONE_C.md) | Complete |",
    "| D | [MILESTONE_D.md](MILESTONE_D.md) | In Progress |"
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
    "## Tasks",
    "",
    "- [x] Finish the offline setup.",
    "- [ ] Capture watched Pixel evidence.",
    "- [ ] Document final target result.",
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

Write-TextFile -Path (Join-Path $taskRoot "MILESTONE_D.md") -Lines @(
    "# Milestone D",
    "",
    "Status: In progress",
    "",
    "Remaining work is described in prose only."
)

$summaryScript = Join-Path $PSScriptRoot "summarize_roadmap_status.ps1"
$json = & $summaryScript -TaskIndexPath $indexPath -Json | ConvertFrom-Json

Assert-Equal -Actual $json.total -Expected 4 -Message "Total milestone count"
Assert-Equal -Actual $json.statusCounts.Complete -Expected 2 -Message "Complete count"
Assert-Equal -Actual $json.statusCounts.'In Progress' -Expected 2 -Message "In-progress count"
Assert-Equal -Actual @($json.inProgress).Count -Expected 2 -Message "In-progress row count"
Assert-Equal -Actual $json.inProgress[0].milestone -Expected "B" -Message "In-progress milestone"
Assert-True -Condition ([bool]$json.inProgress[0].phoneGated) -Message "In-progress milestone should be phone gated."
Assert-Equal -Actual @($json.inProgress[0].openTasks).Count -Expected 2 -Message "Open task count"
Assert-Equal -Actual $json.inProgress[0].openTasks[0] -Expected "Capture watched Pixel evidence." -Message "First open task"
Assert-Equal -Actual @($json.inProgress[1].openTasks).Count -Expected 0 -Message "Open prose-only task count"
Assert-Equal -Actual @($json.mismatches).Count -Expected 1 -Message "Mismatch count"
Assert-Equal -Actual $json.mismatches[0].milestone -Expected "C" -Message "Mismatch milestone"

$text = & $summaryScript -TaskIndexPath $indexPath
Assert-True -Condition (($text -join "`n") -match "Roadmap milestones: 4") -Message "Text output should include total count."
Assert-True -Condition (($text -join "`n") -match "\[ \] Capture watched Pixel evidence\.") -Message "Text output should include open tasks."
Assert-True -Condition (($text -join "`n") -match "No unchecked task bullets") -Message "Text output should call out prose-only remaining work."
Assert-True -Condition (($text -join "`n") -match "Status mismatches:") -Message "Text output should include mismatch section."

Write-Output "Roadmap status summary self-test passed: $root"
