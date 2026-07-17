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

function Invoke-ReviewQueue {
    param(
        [string]$EvidenceRoot,
        [switch]$CommandsOnly,
        [switch]$FailOnPending,
        [string]$OutputPath = "",
        [string]$FfmpegPath = ""
    )

    $script = Join-Path $PSScriptRoot "show_live_validation_review_queue.ps1"
    $stdout = Join-Path $EvidenceRoot "review_queue_stdout.txt"
    $args = @{
        EvidenceRoot = $EvidenceRoot
        Json = (-not [bool]$CommandsOnly)
    }
    if ($CommandsOnly) {
        $args.CommandsOnly = $true
    }
    if ($FailOnPending) {
        $args.FailOnPending = $true
    }
    if (-not [string]::IsNullOrWhiteSpace($OutputPath)) {
        $args.OutputPath = $OutputPath
    }
    if (-not [string]::IsNullOrWhiteSpace($FfmpegPath)) {
        $args.FfmpegPath = $FfmpegPath
    }

    & $script @args *> $stdout
    return $LASTEXITCODE
}

$root = Join-Path ([System.IO.Path]::GetTempPath()) "eulerian-review-queue-test-$([guid]::NewGuid().ToString('N'))"
New-Item -ItemType Directory -Path $root -Force | Out-Null

try {
    $pending = Join-Path $root "pending"
    $complete = Join-Path $root "complete"
    $missingManifest = Join-Path $root "missing-manifest"
    $staleManifest = Join-Path $root "stale-manifest"
    $noVideo = Join-Path $root "no-video"
    New-Item -ItemType Directory -Path $pending, $complete, $missingManifest, $staleManifest, $noVideo -Force | Out-Null

    [System.IO.File]::WriteAllBytes((Join-Path $pending "screenrecord.mp4"), [byte[]](0, 0, 0, 24, 102, 116, 121, 112))
    [System.IO.File]::WriteAllBytes((Join-Path $complete "screenrecord.mp4"), [byte[]](0, 0, 0, 24, 102, 116, 121, 112))
    [System.IO.File]::WriteAllBytes((Join-Path $complete "review_contact_sheet.jpg"), [byte[]](255, 216, 255, 217))
    [System.IO.File]::WriteAllBytes((Join-Path $missingManifest "screenrecord.mp4"), [byte[]](0, 0, 0, 24, 102, 116, 121, 113))
    [System.IO.File]::WriteAllBytes((Join-Path $missingManifest "review_contact_sheet.jpg"), [byte[]](255, 216, 255, 218))
    [System.IO.File]::WriteAllBytes((Join-Path $staleManifest "screenrecord.mp4"), [byte[]](0, 0, 0, 24, 102, 116, 121, 114))
    [System.IO.File]::WriteAllBytes((Join-Path $staleManifest "review_contact_sheet.jpg"), [byte[]](255, 216, 255, 219))
    Write-JsonFile -Path (Join-Path $pending "evidence_summary.json") -Value ([ordered]@{ label = "pending-review" })
    Write-JsonFile -Path (Join-Path $complete "evidence_summary.json") -Value ([ordered]@{ label = "complete-review" })
    Write-JsonFile -Path (Join-Path $missingManifest "evidence_summary.json") -Value ([ordered]@{ label = "missing-manifest-review" })
    Write-JsonFile -Path (Join-Path $staleManifest "evidence_summary.json") -Value ([ordered]@{ label = "stale-manifest-review" })
    Write-JsonFile -Path (Join-Path $complete "review_contact_sheet_manifest.json") -Value ([ordered]@{
        screenrecordSha256 = (Get-FileHash -LiteralPath (Join-Path $complete "screenrecord.mp4") -Algorithm SHA256).Hash
        contactSheetSha256 = (Get-FileHash -LiteralPath (Join-Path $complete "review_contact_sheet.jpg") -Algorithm SHA256).Hash
    })
    Write-JsonFile -Path (Join-Path $staleManifest "review_contact_sheet_manifest.json") -Value ([ordered]@{
        screenrecordSha256 = "wrong-screenrecord-sha256"
        contactSheetSha256 = "wrong-contact-sheet-sha256"
    })

    $result = & (Join-Path $PSScriptRoot "show_live_validation_review_queue.ps1") -EvidenceRoot $root -Json | ConvertFrom-Json
    Assert-Equal -Actual $result.bundleCount -Expected 5 -Message "Review queue bundle count mismatch."
    Assert-Equal -Actual $result.screenrecordBundleCount -Expected 4 -Message "Review queue screenrecord count mismatch."
    Assert-Equal -Actual $result.pendingReviewSheetCount -Expected 3 -Message "Review queue pending count mismatch."
    $pendingReview = @($result.pendingReviewSheets | Where-Object { $_.label -eq "pending-review" })[0]
    Assert-Equal -Actual $pendingReview.label -Expected "pending-review" -Message "Pending queue should use summary label."
    Assert-Equal -Actual $pendingReview.reviewSheetIssue -Expected "missingContactSheet" -Message "Missing contact sheet issue mismatch."
    Assert-Equal -Actual (@($result.pendingReviewSheets | Where-Object { $_.label -eq "missing-manifest-review" })[0].reviewSheetIssue) -Expected "missingManifest" -Message "Missing manifest issue mismatch."
    Assert-Equal -Actual (@($result.pendingReviewSheets | Where-Object { $_.label -eq "stale-manifest-review" })[0].reviewSheetIssue) -Expected "screenrecordHashMismatch,contactSheetHashMismatch" -Message "Stale manifest issue mismatch."
    Assert-Equal -Actual (@($result.bundles | Where-Object { $_.label -eq "complete-review" })[0].pendingReviewSheet) -Expected $false -Message "Matching review sheet should not be pending."
    Assert-Equal -Actual (@($result.bundles | Where-Object { $_.label -eq "complete-review" })[0].screenrecordSha256Matches) -Expected $true -Message "Matching screenrecord hash should be reported."
    Assert-Equal -Actual (@($result.bundles | Where-Object { $_.label -eq "complete-review" })[0].contactSheetSha256Matches) -Expected $true -Message "Matching contact sheet hash should be reported."
    Assert-True -Condition ($pendingReview.command.Contains("export_live_validation_review_sheet.ps1")) -Message "Pending queue should include export command."
    Assert-True -Condition ($pendingReview.command.Contains("-RefreshSummary")) -Message "Pending queue should refresh evidence_summary.json after exporting a review sheet."

    $commands = @(& (Join-Path $PSScriptRoot "show_live_validation_review_queue.ps1") -EvidenceRoot $root -CommandsOnly -FfmpegPath "C:\ffmpeg\bin\ffmpeg.exe")
    Assert-Equal -Actual @($commands).Count -Expected 3 -Message "CommandsOnly should output one command per pending review sheet."
    Assert-True -Condition ($commands[0].Contains("-FfmpegPath")) -Message "CommandsOnly should include explicit ffmpeg path."
    Assert-True -Condition ($commands[0].Contains("-RefreshSummary")) -Message "CommandsOnly should include summary refresh."

    $outputPath = Join-Path $root "review_queue.json"
    $outputExitCode = Invoke-ReviewQueue -EvidenceRoot $root -OutputPath $outputPath
    Assert-Equal -Actual $outputExitCode -Expected 0 -Message "Review queue output path exit mismatch."
    Assert-True -Condition (Test-Path -LiteralPath $outputPath) -Message "Review queue should write OutputPath JSON."

    $pendingExitCode = Invoke-ReviewQueue -EvidenceRoot $root -FailOnPending
    Assert-Equal -Actual $pendingExitCode -Expected 2 -Message "FailOnPending should exit 2 when review sheets are pending."
} finally {
    Remove-Item -LiteralPath $root -Recurse -Force
}

Write-Output "Live validation review queue self-test passed."
