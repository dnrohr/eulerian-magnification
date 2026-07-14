param(
    [string]$TaskIndexPath = "docs\tasks\README.md",
    [switch]$Json,
    [switch]$FailOnMismatch
)

$ErrorActionPreference = "Stop"

function Convert-ToStatusKey {
    param([string]$Status)
    return ($Status.Trim().ToLowerInvariant() -replace "\s+", " ")
}

$index = Resolve-Path -LiteralPath $TaskIndexPath
$taskRoot = Split-Path -Parent $index.Path
$rows = @()

foreach ($line in Get-Content -LiteralPath $index.Path) {
    if ($line -match '^\| ([A-Z]+) \| \[(.+?)\]\((.+?)\) \| (.+?) \|$') {
        $milestone = $Matches[1]
        $title = $Matches[2]
        $relativePath = $Matches[3]
        $indexStatus = $Matches[4].Trim()
        $taskPath = Join-Path $taskRoot $relativePath
        $fileStatus = $null
        $phoneGated = $false
        $openTasks = @()

        if (Test-Path -LiteralPath $taskPath) {
            $content = Get-Content -LiteralPath $taskPath
            $statusLine = $content | Where-Object { $_ -match '^Status:\s*(.+?)\s*$' } | Select-Object -First 1
            if ($statusLine -match '^Status:\s*(.+?)\s*$') {
                $fileStatus = $Matches[1].Trim()
            }
            $phoneGated = [bool]($content | Where-Object {
                $_ -match 'Pixel|phone|device|known target|visual validation|watched target'
            } | Select-Object -First 1)
            $openTasks = @($content | ForEach-Object {
                if ($_ -match '^- \[ \] (.+?)\s*$') {
                    $Matches[1].Trim()
                }
            })
        }

        $rows += [pscustomobject]@{
            milestone = $milestone
            title = $title
            path = $relativePath
            indexStatus = $indexStatus
            fileStatus = $fileStatus
            statusMismatch = ($null -ne $fileStatus -and (Convert-ToStatusKey $fileStatus) -ne (Convert-ToStatusKey $indexStatus))
            phoneGated = $phoneGated
            openTasks = $openTasks
        }
    }
}

$statusCounts = [ordered]@{}
foreach ($row in $rows) {
    if (-not $statusCounts.Contains($row.indexStatus)) {
        $statusCounts[$row.indexStatus] = 0
    }
    $statusCounts[$row.indexStatus] += 1
}

$inProgress = @($rows | Where-Object { (Convert-ToStatusKey $_.indexStatus) -eq "in progress" })
$mismatches = @($rows | Where-Object { $_.statusMismatch })

$summary = [pscustomobject]@{
    total = $rows.Count
    statusCounts = $statusCounts
    inProgress = $inProgress
    mismatches = $mismatches
}

if ($Json) {
    $summary | ConvertTo-Json -Depth 6
    if ($FailOnMismatch -and $mismatches.Count -gt 0) {
        exit 2
    }
    exit 0
}

Write-Output "Roadmap milestones: $($summary.total)"
foreach ($key in $statusCounts.Keys) {
    Write-Output "$key`: $($statusCounts[$key])"
}

if ($inProgress.Count -gt 0) {
    Write-Output ""
    Write-Output "In progress:"
    foreach ($row in $inProgress) {
        $suffix = if ($row.phoneGated) { " (phone/visual gated)" } else { "" }
        Write-Output "- $($row.milestone): $($row.path)$suffix"
        foreach ($task in $row.openTasks) {
            Write-Output "  - [ ] $task"
        }
        if ($row.openTasks.Count -eq 0) {
            Write-Output "  - No unchecked task bullets; inspect Remaining/Done When sections."
        }
    }
}

if ($mismatches.Count -gt 0) {
    Write-Output ""
    Write-Output "Status mismatches:"
    foreach ($row in $mismatches) {
        Write-Output "- $($row.milestone): index '$($row.indexStatus)' vs file '$($row.fileStatus)'"
    }
}

if ($FailOnMismatch -and $mismatches.Count -gt 0) {
    exit 2
}

exit 0
