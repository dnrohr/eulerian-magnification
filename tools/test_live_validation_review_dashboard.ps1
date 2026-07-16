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

function Assert-Equal {
    param(
        $Actual,
        $Expected,
        [string]$Message
    )

    if ($Actual -ne $Expected) {
        throw "$Message Expected '$Expected' but got '$Actual'."
    }
}

function Write-JsonFile {
    param(
        [string]$Path,
        [object]$Value
    )

    $Value | ConvertTo-Json -Depth 8 | Out-File -LiteralPath $Path -Encoding utf8
}

$root = Join-Path ([System.IO.Path]::GetTempPath()) "eulerian-review-dashboard-test-$([guid]::NewGuid().ToString('N'))"
New-Item -ItemType Directory -Path $root -Force | Out-Null

try {
    $pending = Join-Path $root "pending"
    $complete = Join-Path $root "complete"
    New-Item -ItemType Directory -Path $pending, $complete -Force | Out-Null
    [System.IO.File]::WriteAllBytes((Join-Path $pending "screenrecord.mp4"), [byte[]](0, 0, 0, 24, 102, 116, 121, 112))
    [System.IO.File]::WriteAllBytes((Join-Path $complete "screenrecord.mp4"), [byte[]](0, 0, 0, 24, 102, 116, 121, 113))
    [System.IO.File]::WriteAllBytes((Join-Path $complete "review_contact_sheet.jpg"), [byte[]](255, 216, 255, 217))
    Write-JsonFile -Path (Join-Path $pending "evidence_summary.json") -Value ([ordered]@{ label = "pending-dashboard" })
    Write-JsonFile -Path (Join-Path $complete "evidence_summary.json") -Value ([ordered]@{ label = "complete-dashboard" })
    Write-JsonFile -Path (Join-Path $complete "review_contact_sheet_manifest.json") -Value ([ordered]@{
        screenrecordSha256 = (Get-FileHash -LiteralPath (Join-Path $complete "screenrecord.mp4") -Algorithm SHA256).Hash
        contactSheetSha256 = (Get-FileHash -LiteralPath (Join-Path $complete "review_contact_sheet.jpg") -Algorithm SHA256).Hash
    })

    $dashboardPath = Join-Path $root "dashboard.html"
    & (Join-Path $PSScriptRoot "export_live_validation_review_dashboard.ps1") -EvidenceRoot $root -OutputPath $dashboardPath *> $null
    Assert-Equal -Actual $LASTEXITCODE -Expected 0 -Message "Review dashboard export exit code mismatch."
    Assert-True -Condition (Test-Path -LiteralPath $dashboardPath) -Message "Review dashboard should be written."
    $html = Get-Content -LiteralPath $dashboardPath -Raw
    Assert-True -Condition ($html.Contains("pending-dashboard")) -Message "Dashboard should include pending bundle label."
    Assert-True -Condition ($html.Contains("complete-dashboard")) -Message "Dashboard should include complete bundle label."
    Assert-True -Condition ($html.Contains("missingContactSheet")) -Message "Dashboard should include review sheet issue reason."
    Assert-True -Condition ($html.Contains("<video controls")) -Message "Dashboard should include playable videos."
    Assert-True -Condition ($html.Contains("review_contact_sheet.jpg")) -Message "Dashboard should include existing contact sheet images."
    Assert-True -Condition ($html.Contains("export_live_validation_review_sheet.ps1")) -Message "Dashboard should include regeneration commands."

    $pendingOnlyPath = Join-Path $root "pending-dashboard.html"
    & (Join-Path $PSScriptRoot "export_live_validation_review_dashboard.ps1") -EvidenceRoot $root -OutputPath $pendingOnlyPath -PendingOnly *> $null
    Assert-Equal -Actual $LASTEXITCODE -Expected 0 -Message "Pending-only review dashboard exit code mismatch."
    $pendingHtml = Get-Content -LiteralPath $pendingOnlyPath -Raw
    Assert-True -Condition ($pendingHtml.Contains("pending-dashboard")) -Message "Pending-only dashboard should include pending bundle."
    Assert-True -Condition (-not $pendingHtml.Contains("complete-dashboard")) -Message "Pending-only dashboard should omit complete bundle."
} finally {
    Remove-Item -LiteralPath $root -Recurse -Force
}

Write-Output "Live validation review dashboard self-test passed."
