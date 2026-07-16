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
    $noVideo = Join-Path $root "no-video"
    New-Item -ItemType Directory -Path $pending, $complete, $noVideo -Force | Out-Null

    [System.IO.File]::WriteAllBytes((Join-Path $pending "screenrecord.mp4"), [byte[]](0, 0, 0, 24, 102, 116, 121, 112))
    [System.IO.File]::WriteAllBytes((Join-Path $complete "screenrecord.mp4"), [byte[]](0, 0, 0, 24, 102, 116, 121, 112))
    [System.IO.File]::WriteAllBytes((Join-Path $complete "review_contact_sheet.jpg"), [byte[]](255, 216, 255, 217))
    Write-JsonFile -Path (Join-Path $pending "evidence_summary.json") -Value ([ordered]@{ label = "pending-review" })
    Write-JsonFile -Path (Join-Path $complete "evidence_summary.json") -Value ([ordered]@{ label = "complete-review" })
    Write-JsonFile -Path (Join-Path $complete "review_contact_sheet_manifest.json") -Value ([ordered]@{ contactSheetSha256 = "sheet" })

    $result = & (Join-Path $PSScriptRoot "show_live_validation_review_queue.ps1") -EvidenceRoot $root -Json | ConvertFrom-Json
    Assert-Equal -Actual $result.bundleCount -Expected 3 -Message "Review queue bundle count mismatch."
    Assert-Equal -Actual $result.screenrecordBundleCount -Expected 2 -Message "Review queue screenrecord count mismatch."
    Assert-Equal -Actual $result.pendingReviewSheetCount -Expected 1 -Message "Review queue pending count mismatch."
    Assert-Equal -Actual $result.pendingReviewSheets[0].label -Expected "pending-review" -Message "Pending queue should use summary label."
    Assert-True -Condition ($result.pendingReviewSheets[0].command.Contains("export_live_validation_review_sheet.ps1")) -Message "Pending queue should include export command."

    $commands = @(& (Join-Path $PSScriptRoot "show_live_validation_review_queue.ps1") -EvidenceRoot $root -CommandsOnly -FfmpegPath "C:\ffmpeg\bin\ffmpeg.exe")
    Assert-Equal -Actual @($commands).Count -Expected 1 -Message "CommandsOnly should output one pending command."
    Assert-True -Condition ($commands[0].Contains("-FfmpegPath")) -Message "CommandsOnly should include explicit ffmpeg path."

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
