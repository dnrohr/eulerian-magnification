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

function Invoke-HandoffExitCode {
    param(
        [string]$EvidenceRoot,
        [string]$OutputRoot,
        [string[]]$Slot = @(),
        [switch]$FailOnInvalidSlot,
        [switch]$FailOnEmptyQueue
    )

    $script = Join-Path $PSScriptRoot "prepare_pixel_validation_handoff.ps1"
    $powerShellExe = (Get-Process -Id $PID).Path
    $arguments = @("-NoProfile", "-File", $script, "-EvidenceRoot", $EvidenceRoot, "-OutputRoot", $OutputRoot)
    $arguments += @("-DeviceSerial", "PIXEL-HANDOFF-TEST")
    foreach ($slotId in $Slot) {
        $arguments += @("-Slot", $slotId)
    }
    if ($FailOnInvalidSlot) {
        $arguments += "-FailOnInvalidSlot"
    }
    if ($FailOnEmptyQueue) {
        $arguments += "-FailOnEmptyQueue"
    }

    & $powerShellExe @arguments *> $null
    return $LASTEXITCODE
}

$evidenceRoot = Join-Path ([System.IO.Path]::GetTempPath()) "eulerian-handoff-evidence-$([guid]::NewGuid().ToString('N'))"
$outputRoot = Join-Path ([System.IO.Path]::GetTempPath()) "eulerian-handoff-output-$([guid]::NewGuid().ToString('N'))"
New-Item -ItemType Directory -Path (Join-Path $evidenceRoot "pending-review") -Force | Out-Null
[System.IO.File]::WriteAllBytes((Join-Path $evidenceRoot "pending-review\screenrecord.mp4"), [byte[]](0, 0, 0, 24, 102, 116, 121, 112))

$result = & (Join-Path $PSScriptRoot "prepare_pixel_validation_handoff.ps1") `
    -EvidenceRoot $evidenceRoot `
    -OutputRoot $outputRoot `
    -DeviceSerial "PIXEL-HANDOFF-TEST" `
    -FfmpegPath "C:\ffmpeg\bin\ffmpeg.exe" `
    -Slot pulseLinear `
    -CaptureStage Final `
    -Json | ConvertFrom-Json

Assert-Equal -Actual $result.evidenceRoot -Expected $evidenceRoot -Message "Handoff should preserve evidence root."
Assert-Equal -Actual $result.outputRoot -Expected $outputRoot -Message "Handoff should preserve output root."
Assert-Equal -Actual $result.deviceSerial -Expected "PIXEL-HANDOFF-TEST" -Message "Handoff should preserve device serial."
Assert-Equal -Actual $result.captureStage -Expected "Final" -Message "Handoff should preserve capture stage."
Assert-Equal -Actual @($result.requestedSlots).Count -Expected 1 -Message "Handoff should preserve requested slot count."
Assert-Equal -Actual $result.requestedSlots[0] -Expected "pulseLinear" -Message "Handoff should preserve requested slot id."
Assert-Equal -Actual @($result.invalidRequestedSlots).Count -Expected 0 -Message "Known handoff slot should not be invalid."
Assert-Equal -Actual $result.recommendedCaptureCount -Expected 1 -Message "Filtered handoff should include one recommended capture."
Assert-Equal -Actual $result.commandCount -Expected 1 -Message "Filtered handoff should include one command template."
Assert-Equal -Actual $result.pendingReviewSheetCount -Expected 1 -Message "Handoff should report pending review sheets."
Assert-Equal -Actual $result.reviewCommandCount -Expected 1 -Message "Handoff should report review command count."
Assert-True -Condition (-not [string]::IsNullOrWhiteSpace($result.source.branch)) -Message "Handoff should report the source branch."
Assert-True -Condition (-not [string]::IsNullOrWhiteSpace($result.source.commit)) -Message "Handoff should report the source commit."
Assert-True -Condition ($result.source.commit.Length -ge 7) -Message "Handoff source commit should look like a Git commit."
Assert-True -Condition ($null -ne $result.source.clean) -Message "Handoff should report whether the source tree is clean."
Assert-True -Condition ($null -ne $result.source.commitReachableFromOriginMain) -Message "Handoff should report whether the source commit is reachable from origin/main."
Assert-True -Condition (-not [string]::IsNullOrWhiteSpace($result.manifestPath)) -Message "Handoff should report the manifest path."
Assert-Equal -Actual @($result.artifactHashes).Count -Expected 7 -Message "Handoff result should report hashes for every handoff artifact."
Assert-True -Condition (Test-Path -LiteralPath $result.planPath) -Message "Handoff should write plan JSON."
Assert-True -Condition (Test-Path -LiteralPath $result.closeoutPath) -Message "Handoff should write closeout JSON."
Assert-True -Condition (Test-Path -LiteralPath $result.commandsPath) -Message "Handoff should write command templates."
Assert-True -Condition (Test-Path -LiteralPath $result.reviewQueuePath) -Message "Handoff should write review queue JSON."
Assert-True -Condition (Test-Path -LiteralPath $result.reviewCommandsPath) -Message "Handoff should write review command templates."
Assert-True -Condition (Test-Path -LiteralPath $result.reviewDashboardPath) -Message "Handoff should write review dashboard HTML."
Assert-True -Condition (Test-Path -LiteralPath $result.handoffPath) -Message "Handoff should write a readable Markdown summary."
Assert-True -Condition (Test-Path -LiteralPath $result.manifestPath) -Message "Handoff should write a manifest JSON."

$plan = Get-Content -LiteralPath $result.planPath -Raw | ConvertFrom-Json
$closeout = Get-Content -LiteralPath $result.closeoutPath -Raw | ConvertFrom-Json
$commands = Get-Content -LiteralPath $result.commandsPath -Raw
$reviewQueue = Get-Content -LiteralPath $result.reviewQueuePath -Raw | ConvertFrom-Json
$reviewCommands = Get-Content -LiteralPath $result.reviewCommandsPath -Raw
$reviewDashboard = Get-Content -LiteralPath $result.reviewDashboardPath -Raw
$handoff = Get-Content -LiteralPath $result.handoffPath -Raw
$manifest = Get-Content -LiteralPath $result.manifestPath -Raw | ConvertFrom-Json

Assert-Equal -Actual @($plan.recommendedCaptures).Count -Expected 1 -Message "Written plan should preserve filtered recommended captures."
Assert-Equal -Actual $plan.deviceSerial -Expected "PIXEL-HANDOFF-TEST" -Message "Written plan should preserve the handoff device serial."
Assert-Equal -Actual $plan.recommendedCaptures[0].slot -Expected "pulseLinear" -Message "Written plan should preserve filtered slot."
Assert-True -Condition ($commands.Contains("live-linear-pulse-final")) -Message "Command handoff should include the filtered final command."
Assert-True -Condition ($commands.Contains('-DeviceSerial "PIXEL-HANDOFF-TEST"')) -Message "Command handoff should include the requested device serial."
Assert-True -Condition (-not $commands.Contains("live-linear-pulse-setup")) -Message "Final-only command handoff should omit setup command."
Assert-Equal -Actual $reviewQueue.pendingReviewSheetCount -Expected 1 -Message "Written review queue should preserve pending count."
Assert-True -Condition ($reviewCommands.Contains("export_live_validation_review_sheet.ps1")) -Message "Review commands should include the export helper."
Assert-True -Condition ($reviewCommands.Contains("-FfmpegPath")) -Message "Review commands should preserve the ffmpeg path."
Assert-True -Condition ($reviewDashboard.Contains("pending-review")) -Message "Review dashboard should include pending screenrecord labels."
Assert-True -Condition ($reviewDashboard.Contains("<video controls")) -Message "Review dashboard should include playable video elements."
Assert-Equal -Actual @($closeout.closeoutBlockers).Count -Expected 1 -Message "Missing evidence handoff should preserve closeout blockers."
Assert-True -Condition ($handoff.Contains("# Pixel Validation Handoff")) -Message "Markdown handoff should include a title."
Assert-True -Condition ($handoff.Contains('Requested slots: `pulseLinear`')) -Message "Markdown handoff should include requested slots."
Assert-True -Condition ($handoff.Contains('Device serial: `PIXEL-HANDOFF-TEST`')) -Message "Markdown handoff should include the device serial."
Assert-True -Condition ($handoff.Contains('Expected final label: `live-linear-pulse-final`')) -Message "Markdown handoff should include expected final labels."
Assert-True -Condition ($handoff.Contains("Source branch:")) -Message "Markdown handoff should include source branch."
Assert-True -Condition ($handoff.Contains("Source commit:")) -Message "Markdown handoff should include source commit."
Assert-True -Condition ($handoff.Contains("Source commit reachable from origin/main:")) -Message "Markdown handoff should include source reachability."
Assert-True -Condition ($handoff.Contains("Handoff manifest:")) -Message "Markdown handoff should include the manifest path."
Assert-True -Condition ($handoff.Contains("Review Sheet Queue")) -Message "Markdown handoff should include the review queue section."
Assert-True -Condition ($handoff.Contains("Review Sheet Commands")) -Message "Markdown handoff should include review commands."
Assert-True -Condition ($handoff.Contains("Review dashboard:")) -Message "Markdown handoff should include the review dashboard artifact."
Assert-True -Condition ($handoff.Contains("missingContactSheet")) -Message "Markdown handoff should include review-sheet issue reasons."
Assert-True -Condition ($handoff.Contains("Command:")) -Message "Markdown handoff should include per-bundle review-sheet commands."
Assert-True -Condition ($handoff.Contains("Pending review sheets: 1")) -Message "Markdown handoff should include pending review sheet count."
Assert-True -Condition ($handoff.Contains("```powershell")) -Message "Markdown handoff should include a PowerShell command block."
Assert-True -Condition ($handoff.Contains("live-linear-pulse-final")) -Message "Markdown handoff should include the filtered command."
Assert-True -Condition ($handoff.Contains("missingSlots")) -Message "Markdown handoff should summarize closeout blockers."
Assert-Equal -Actual @($manifest.artifacts).Count -Expected 7 -Message "Manifest should include every handoff artifact except itself."
Assert-Equal -Actual $manifest.deviceSerial -Expected "PIXEL-HANDOFF-TEST" -Message "Manifest should include the device serial."
Assert-Equal -Actual $manifest.review.pendingReviewSheetCount -Expected 1 -Message "Manifest should include review queue count."
foreach ($artifactName in @("plan", "closeout", "commands", "reviewQueue", "reviewCommands", "reviewDashboard", "handoff")) {
    $artifact = @($manifest.artifacts | Where-Object { $_.name -eq $artifactName } | Select-Object -First 1)
    Assert-Equal -Actual @($artifact).Count -Expected 1 -Message "Manifest should include artifact '$artifactName'."
    Assert-True -Condition (Test-Path -LiteralPath $artifact[0].path) -Message "Manifest artifact '$artifactName' path should exist."
    Assert-True -Condition ($artifact[0].sha256 -match "^[0-9a-f]{64}$") -Message "Manifest artifact '$artifactName' should include a lowercase SHA-256 hash."
    $actualHash = (Get-FileHash -LiteralPath $artifact[0].path -Algorithm SHA256).Hash.ToLowerInvariant()
    Assert-Equal -Actual $artifact[0].sha256 -Expected $actualHash -Message "Manifest artifact '$artifactName' hash mismatch."
}

$textOutput = & (Join-Path $PSScriptRoot "prepare_pixel_validation_handoff.ps1") `
    -EvidenceRoot $evidenceRoot `
    -OutputRoot $outputRoot `
    -DeviceSerial "PIXEL-HANDOFF-TEST" `
    -Slot notARealSlot

Assert-True -Condition (($textOutput -join "`n").Contains("Pixel validation handoff prepared")) -Message "Text handoff should print a heading."
Assert-True -Condition (($textOutput -join "`n").Contains("Device serial: PIXEL-HANDOFF-TEST")) -Message "Text handoff should print the device serial."
Assert-True -Condition (($textOutput -join "`n").Contains("Source:")) -Message "Text handoff should print source metadata."
Assert-True -Condition (($textOutput -join "`n").Contains("Handoff:")) -Message "Text handoff should print the Markdown handoff path."
Assert-True -Condition (($textOutput -join "`n").Contains("Review dashboard:")) -Message "Text handoff should print the review dashboard path."
Assert-True -Condition (($textOutput -join "`n").Contains("Manifest:")) -Message "Text handoff should print the manifest path."
Assert-True -Condition (($textOutput -join "`n").Contains("Warning: requested slot(s) not currently missing or unknown: notARealSlot")) -Message "Text handoff should warn about invalid slots."
Assert-True -Condition (($textOutput -join "`n").Contains("Warning: no recommended captures match the current filters.")) -Message "Text handoff should warn about empty queues."

$validExitCode = Invoke-HandoffExitCode -EvidenceRoot $evidenceRoot -OutputRoot $outputRoot -Slot pulseLinear -FailOnInvalidSlot -FailOnEmptyQueue
$invalidExitCode = Invoke-HandoffExitCode -EvidenceRoot $evidenceRoot -OutputRoot $outputRoot -Slot notARealSlot -FailOnInvalidSlot
$emptyExitCode = Invoke-HandoffExitCode -EvidenceRoot $evidenceRoot -OutputRoot $outputRoot -Slot notARealSlot -FailOnEmptyQueue

Assert-Equal -Actual $validExitCode -Expected 0 -Message "Handoff gates should allow valid non-empty filters."
Assert-Equal -Actual $invalidExitCode -Expected 21 -Message "Handoff should fail invalid slot filters with exit code 21."
Assert-Equal -Actual $emptyExitCode -Expected 22 -Message "Handoff should fail empty command queues with exit code 22."

Write-Output "Pixel validation handoff self-test passed."
