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
        [switch]$FailOnEmptyQueue,
        [switch]$FailOnPendingReviewSheets,
        [switch]$FailOnDirtySource,
        [switch]$FailOnUnpushedSource,
        [switch]$FailOnDeviceUnavailable,
        [string]$AdbPath = "",
        [string]$SourceRoot = ""
    )

    $script = Join-Path $PSScriptRoot "prepare_pixel_validation_handoff.ps1"
    $powerShellExe = (Get-Process -Id $PID).Path
    $arguments = @("-NoProfile", "-File", $script, "-EvidenceRoot", $EvidenceRoot, "-OutputRoot", $OutputRoot)
    $arguments += @("-DeviceSerial", "PIXEL-HANDOFF-TEST")
    if (-not [string]::IsNullOrWhiteSpace($AdbPath)) {
        $arguments += @("-AdbPath", $AdbPath)
    }
    if (-not [string]::IsNullOrWhiteSpace($SourceRoot)) {
        $arguments += @("-SourceRoot", $SourceRoot)
    }
    foreach ($slotId in $Slot) {
        $arguments += @("-Slot", $slotId)
    }
    if ($FailOnInvalidSlot) {
        $arguments += "-FailOnInvalidSlot"
    }
    if ($FailOnEmptyQueue) {
        $arguments += "-FailOnEmptyQueue"
    }
    if ($FailOnPendingReviewSheets) {
        $arguments += "-FailOnPendingReviewSheets"
    }
    if ($FailOnDirtySource) {
        $arguments += "-FailOnDirtySource"
    }
    if ($FailOnUnpushedSource) {
        $arguments += "-FailOnUnpushedSource"
    }
    if ($FailOnDeviceUnavailable) {
        $arguments += "-FailOnDeviceUnavailable"
    }

    & $powerShellExe @arguments *> $null
    return $LASTEXITCODE
}

$evidenceRoot = Join-Path ([System.IO.Path]::GetTempPath()) "eulerian-handoff-evidence-$([guid]::NewGuid().ToString('N'))"
$outputRoot = Join-Path ([System.IO.Path]::GetTempPath()) "eulerian-handoff-output-$([guid]::NewGuid().ToString('N'))"
New-Item -ItemType Directory -Path (Join-Path $evidenceRoot "pending-review") -Force | Out-Null
New-Item -ItemType Directory -Path $outputRoot -Force | Out-Null
[System.IO.File]::WriteAllBytes((Join-Path $evidenceRoot "pending-review\screenrecord.mp4"), [byte[]](0, 0, 0, 24, 102, 116, 121, 112))
$fakeAdb = Join-Path $outputRoot "fake-adb.cmd"
Set-Content -LiteralPath $fakeAdb -Encoding ascii -Value @(
    "@echo off",
    "echo List of devices attached",
    "echo PIXEL-HANDOFF-TEST device product:pixel model:Pixel_8a"
)
$fakeNoDeviceAdb = Join-Path $outputRoot "fake-no-device-adb.cmd"
Set-Content -LiteralPath $fakeNoDeviceAdb -Encoding ascii -Value @(
    "@echo off",
    "echo List of devices attached"
)

$result = & (Join-Path $PSScriptRoot "prepare_pixel_validation_handoff.ps1") `
    -EvidenceRoot $evidenceRoot `
    -OutputRoot $outputRoot `
    -DeviceSerial "PIXEL-HANDOFF-TEST" `
    -AdbPath $fakeAdb `
    -FfmpegPath "C:\ffmpeg\bin\ffmpeg.exe" `
    -Slot pulseLinear `
    -CaptureStage Final `
    -Json | ConvertFrom-Json

Assert-Equal -Actual $result.evidenceRoot -Expected $evidenceRoot -Message "Handoff should preserve evidence root."
Assert-Equal -Actual $result.outputRoot -Expected $outputRoot -Message "Handoff should preserve output root."
Assert-Equal -Actual $result.deviceSerial -Expected "PIXEL-HANDOFF-TEST" -Message "Handoff should preserve device serial."
Assert-Equal -Actual $result.captureStage -Expected "Final" -Message "Handoff should preserve capture stage."
Assert-True -Condition ($result.source.commit.Length -ge 7) -Message "Handoff source commit should look like a Git commit."
Assert-True -Condition ($result.thermalReadiness.command.Contains("wait_for_device_thermal_ready.ps1")) -Message "Handoff should report a thermal readiness command."
Assert-True -Condition ($result.thermalReadiness.command.Contains('-DeviceSerial "PIXEL-HANDOFF-TEST"')) -Message "Handoff thermal command should preserve device serial."
Assert-Equal -Actual $result.deviceAvailability.checked -Expected $true -Message "Handoff should check device availability when adb is available."
Assert-Equal -Actual $result.deviceAvailability.connected -Expected $true -Message "Handoff should report the expected Pixel serial as connected."
Assert-True -Condition ("PIXEL-HANDOFF-TEST" -in @($result.deviceAvailability.connectedSerials)) -Message "Handoff should list connected device serials."
Assert-True -Condition ($result.installCommand.Contains("install_debug_on_pixel.ps1")) -Message "Handoff should expose a debug install command."
Assert-True -Condition ($result.installCommand.Contains('-DeviceSerial "PIXEL-HANDOFF-TEST"')) -Message "Handoff install command should preserve device serial."
Assert-True -Condition ($result.installCommand.Contains("-Build")) -Message "Handoff install command should build the debug app."
Assert-True -Condition ($result.installCommand.Contains("-Launch")) -Message "Handoff install command should launch the debug app."
Assert-Equal -Actual @($result.requestedSlots).Count -Expected 1 -Message "Handoff should preserve requested slot count."
Assert-Equal -Actual $result.requestedSlots[0] -Expected "pulseLinear" -Message "Handoff should preserve requested slot id."
Assert-Equal -Actual @($result.invalidRequestedSlots).Count -Expected 0 -Message "Known handoff slot should not be invalid."
Assert-Equal -Actual $result.recommendedCaptureCount -Expected 1 -Message "Filtered handoff should include one recommended capture."
Assert-Equal -Actual $result.commandCount -Expected 1 -Message "Filtered handoff should include one command template."
Assert-Equal -Actual $result.pendingReviewSheetCount -Expected 1 -Message "Handoff should report pending review sheets."
Assert-Equal -Actual $result.pendingReviewSheetIssueCounts.missingContactSheet -Expected 1 -Message "Handoff should report pending review-sheet issue counts."
Assert-Equal -Actual $result.reviewCommandCount -Expected 1 -Message "Handoff should report review command count."
Assert-Equal -Actual $result.roiFinalHelperCommandCount -Expected 0 -Message "Non-ROI handoff should not report ROI final helper commands."
Assert-True -Condition (-not [string]::IsNullOrWhiteSpace($result.source.branch)) -Message "Handoff should report the source branch."
Assert-True -Condition (-not [string]::IsNullOrWhiteSpace($result.source.commit)) -Message "Handoff should report the source commit."
Assert-True -Condition ($null -ne $result.source.clean) -Message "Handoff should report whether the source tree is clean."
Assert-True -Condition ($null -ne $result.source.commitReachableFromOriginMain) -Message "Handoff should report whether the source commit is reachable from origin/main."
Assert-True -Condition (-not [string]::IsNullOrWhiteSpace($result.manifestPath)) -Message "Handoff should report the manifest path."
Assert-Equal -Actual @($result.artifactHashes).Count -Expected 8 -Message "Handoff result should report hashes for every handoff artifact."
Assert-True -Condition (Test-Path -LiteralPath $result.planPath) -Message "Handoff should write plan JSON."
Assert-True -Condition (Test-Path -LiteralPath $result.closeoutPath) -Message "Handoff should write closeout JSON."
Assert-True -Condition (Test-Path -LiteralPath $result.commandsPath) -Message "Handoff should write command templates."
Assert-True -Condition (Test-Path -LiteralPath $result.runbookPath) -Message "Handoff should write the ordered validation runbook."
Assert-True -Condition (Test-Path -LiteralPath $result.reviewQueuePath) -Message "Handoff should write review queue JSON."
Assert-True -Condition (Test-Path -LiteralPath $result.reviewCommandsPath) -Message "Handoff should write review command templates."
Assert-True -Condition (Test-Path -LiteralPath $result.reviewDashboardPath) -Message "Handoff should write review dashboard HTML."
Assert-True -Condition (Test-Path -LiteralPath $result.handoffPath) -Message "Handoff should write a readable Markdown summary."
Assert-True -Condition (Test-Path -LiteralPath $result.manifestPath) -Message "Handoff should write a manifest JSON."

$plan = Get-Content -LiteralPath $result.planPath -Raw | ConvertFrom-Json
$closeout = Get-Content -LiteralPath $result.closeoutPath -Raw | ConvertFrom-Json
$commands = Get-Content -LiteralPath $result.commandsPath -Raw
$runbook = Get-Content -LiteralPath $result.runbookPath -Raw
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
Assert-True -Condition ($runbook.Contains("install_debug_on_pixel.ps1")) -Message "Runbook should include the install command."
Assert-True -Condition ($runbook.Contains("wait_for_device_thermal_ready.ps1")) -Message "Runbook should include the thermal readiness command."
Assert-True -Condition ($runbook.Contains("live-linear-pulse-final")) -Message "Runbook should include filtered capture commands."
Assert-True -Condition ($runbook.Contains("export_live_validation_review_sheet.ps1")) -Message "Runbook should include review sheet commands."
Assert-True -Condition ($runbook.Contains("# 0. Confirm this handoff still matches the intended source and device state.")) -Message "Runbook should include source/device preflight guidance."
Assert-True -Condition ($runbook.Contains("Source commit:")) -Message "Runbook should include the handoff source commit."
Assert-True -Condition ($runbook.Contains("Source commit reachable from origin/main:")) -Message "Runbook should include source reachability state."
Assert-True -Condition ($runbook.Contains("Expected device connected at handoff time: True")) -Message "Runbook should include device availability state."
Assert-True -Condition ($runbook.Contains("Handoff manifest:")) -Message "Runbook should include the handoff manifest path."
Assert-True -Condition ($runbook.Contains("verify_pixel_validation_handoff.ps1")) -Message "Runbook should include the handoff verifier command."
Assert-True -Condition ($runbook.Contains("-FailOnArtifactMismatch")) -Message "Runbook verifier should fail on artifact mismatches."
Assert-True -Condition ($runbook.Contains("-FailOnSourceMismatch")) -Message "Runbook verifier should fail on source mismatches."
Assert-True -Condition ($runbook.Contains("-FailOnDeviceUnavailable")) -Message "Runbook verifier should fail on missing expected device."
Assert-True -Condition ($runbook.Contains("-FailOnHandoffConsistencyMismatch")) -Message "Runbook verifier should fail on stale handoff helper consistency."
Assert-True -Condition ($runbook.Contains("status --short")) -Message "Runbook should include a clean-source recheck command."
Assert-True -Condition ($runbook.Contains("rev-parse HEAD")) -Message "Runbook should include a source commit recheck command."
Assert-True -Condition ($runbook.Contains("merge-base --is-ancestor")) -Message "Runbook should include an origin/main reachability recheck command."
Assert-True -Condition (-not $runbook.Contains("prepare_roi_final_capture_command.ps1")) -Message "Non-ROI runbook should not include ROI helper noise."
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
Assert-True -Condition ($handoff.Contains("Operator setup:")) -Message "Markdown handoff should include operator setup guidance."
Assert-True -Condition ($handoff.Contains("Acceptance checks:")) -Message "Markdown handoff should include acceptance checks."
Assert-True -Condition ($handoff.Contains("not an ROI-only tint")) -Message "Markdown handoff should include slot-specific acceptance guidance."
Assert-True -Condition ($handoff.Contains("Source branch:")) -Message "Markdown handoff should include source branch."
Assert-True -Condition ($handoff.Contains("Source commit:")) -Message "Markdown handoff should include source commit."
Assert-True -Condition ($handoff.Contains("Source commit reachable from origin/main:")) -Message "Markdown handoff should include source reachability."
Assert-True -Condition ($handoff.Contains("Expected device connected: True")) -Message "Markdown handoff should include device availability."
Assert-True -Condition ($handoff.Contains("Install command:")) -Message "Markdown handoff should include the install command summary."
Assert-True -Condition ($handoff.Contains("install_debug_on_pixel.ps1")) -Message "Markdown handoff should include the debug install helper."
Assert-True -Condition ($handoff.Contains("Thermal Preflight")) -Message "Markdown handoff should include thermal preflight guidance."
Assert-True -Condition ($handoff.Contains("wait_for_device_thermal_ready.ps1")) -Message "Markdown handoff should include the thermal readiness command."
Assert-True -Condition ($handoff.Contains("Handoff manifest:")) -Message "Markdown handoff should include the manifest path."
Assert-True -Condition ($handoff.Contains("Review Sheet Queue")) -Message "Markdown handoff should include the review queue section."
Assert-True -Condition ($handoff.Contains("Review Sheet Commands")) -Message "Markdown handoff should include review commands."
Assert-True -Condition ($handoff.Contains("Review dashboard:")) -Message "Markdown handoff should include the review dashboard artifact."
Assert-True -Condition ($handoff.Contains("missingContactSheet")) -Message "Markdown handoff should include review-sheet issue reasons."
Assert-True -Condition ($handoff.Contains("Command:")) -Message "Markdown handoff should include per-bundle review-sheet commands."
Assert-True -Condition ($handoff.Contains("Pending review sheets: 1")) -Message "Markdown handoff should include pending review sheet count."
Assert-True -Condition ($handoff.Contains("Pending review-sheet issue types: 1")) -Message "Markdown handoff should include review issue type count."
Assert-True -Condition ($handoff.Contains("```powershell")) -Message "Markdown handoff should include a PowerShell command block."
Assert-True -Condition ($handoff.Contains("live-linear-pulse-final")) -Message "Markdown handoff should include the filtered command."
Assert-True -Condition ($handoff.Contains("missingSlots")) -Message "Markdown handoff should summarize closeout blockers."
Assert-Equal -Actual @($manifest.artifacts).Count -Expected 8 -Message "Manifest should include every handoff artifact except itself."
Assert-Equal -Actual $manifest.deviceSerial -Expected "PIXEL-HANDOFF-TEST" -Message "Manifest should include the device serial."
Assert-Equal -Actual $manifest.review.pendingReviewSheetCount -Expected 1 -Message "Manifest should include review queue count."
Assert-Equal -Actual $manifest.review.issueCounts.missingContactSheet -Expected 1 -Message "Manifest should include review issue counts."
Assert-True -Condition ($manifest.thermalReadiness.command.Contains("wait_for_device_thermal_ready.ps1")) -Message "Manifest should include thermal readiness command metadata."
Assert-Equal -Actual $manifest.deviceAvailability.connected -Expected $true -Message "Manifest should include device availability metadata."
Assert-True -Condition ($manifest.installCommand.Contains("install_debug_on_pixel.ps1")) -Message "Manifest should include the install command."
Assert-Equal -Actual @($manifest.roiFinalHelperCommands).Count -Expected 0 -Message "Non-ROI manifest should not include ROI helper commands."
foreach ($artifactName in @("plan", "closeout", "commands", "runbook", "reviewQueue", "reviewCommands", "reviewDashboard", "handoff")) {
    $artifact = @($manifest.artifacts | Where-Object { $_.name -eq $artifactName } | Select-Object -First 1)
    Assert-Equal -Actual @($artifact).Count -Expected 1 -Message "Manifest should include artifact '$artifactName'."
    Assert-True -Condition (Test-Path -LiteralPath $artifact[0].path) -Message "Manifest artifact '$artifactName' path should exist."
    Assert-True -Condition ($artifact[0].sha256 -match "^[0-9a-f]{64}$") -Message "Manifest artifact '$artifactName' should include a lowercase SHA-256 hash."
    $actualHash = (Get-FileHash -LiteralPath $artifact[0].path -Algorithm SHA256).Hash.ToLowerInvariant()
    Assert-Equal -Actual $artifact[0].sha256 -Expected $actualHash -Message "Manifest artifact '$artifactName' hash mismatch."
}

$roiOutputRoot = Join-Path ([System.IO.Path]::GetTempPath()) "eulerian-handoff-roi-output-$([guid]::NewGuid().ToString('N'))"
$roiResult = & (Join-Path $PSScriptRoot "prepare_pixel_validation_handoff.ps1") `
    -EvidenceRoot $evidenceRoot `
    -OutputRoot $roiOutputRoot `
    -DeviceSerial "PIXEL-HANDOFF-TEST" `
    -AdbPath $fakeAdb `
    -Slot manualRoi `
    -CaptureStage All `
    -Json | ConvertFrom-Json

$roiRunbook = Get-Content -LiteralPath $roiResult.runbookPath -Raw
$roiHandoff = Get-Content -LiteralPath $roiResult.handoffPath -Raw
$roiManifest = Get-Content -LiteralPath $roiResult.manifestPath -Raw | ConvertFrom-Json
Assert-Equal -Actual $roiResult.roiFinalHelperCommandCount -Expected 1 -Message "Manual ROI handoff should report one ROI final helper command."
Assert-True -Condition ($roiRunbook.Contains("# 3a. Prepare ROI final commands after setup screenshots.")) -Message "ROI runbook should include the helper step."
Assert-True -Condition ($roiRunbook.Contains("prepare_roi_final_capture_command.ps1")) -Message "ROI runbook should include the final-command helper."
Assert-True -Condition ($roiRunbook.Contains("-Slot manualRoi")) -Message "ROI runbook helper should target manual ROI."
Assert-True -Condition ($roiRunbook.Contains("<manual-roi-setup-bundle>")) -Message "ROI runbook helper should show the setup bundle placeholder."
Assert-True -Condition ($roiRunbook.Contains("<left,top,right,bottom-from-setup-screenshot>")) -Message "ROI runbook helper should show the pixel-bounds placeholder."
Assert-True -Condition ($roiHandoff.Contains("ROI Final Command Helpers")) -Message "ROI Markdown handoff should include helper guidance."
Assert-True -Condition ($roiHandoff.Contains("Paste the printed final capture command")) -Message "ROI Markdown handoff should explain how to use the helper."
Assert-True -Condition ($roiHandoff.Contains("prepare_roi_final_capture_command.ps1")) -Message "ROI Markdown handoff should include the helper command."
Assert-Equal -Actual @($roiManifest.roiFinalHelperCommands).Count -Expected 1 -Message "ROI manifest should include the helper command."
Assert-True -Condition ($roiManifest.roiFinalHelperCommands[0].Contains("-Slot manualRoi")) -Message "ROI manifest helper command should preserve the slot."

$textOutput = & (Join-Path $PSScriptRoot "prepare_pixel_validation_handoff.ps1") `
    -EvidenceRoot $evidenceRoot `
    -OutputRoot $outputRoot `
    -DeviceSerial "PIXEL-HANDOFF-TEST" `
    -AdbPath $fakeAdb `
    -Slot notARealSlot

Assert-True -Condition (($textOutput -join "`n").Contains("Pixel validation handoff prepared")) -Message "Text handoff should print a heading."
Assert-True -Condition (($textOutput -join "`n").Contains("Device serial: PIXEL-HANDOFF-TEST")) -Message "Text handoff should print the device serial."
Assert-True -Condition (($textOutput -join "`n").Contains("Source:")) -Message "Text handoff should print source metadata."
Assert-True -Condition (($textOutput -join "`n").Contains("Expected device connected: True")) -Message "Text handoff should print device availability."
Assert-True -Condition (($textOutput -join "`n").Contains("Install command:")) -Message "Text handoff should print the install command."
Assert-True -Condition (($textOutput -join "`n").Contains("Pending review-sheet issue types:")) -Message "Text handoff should print review issue type count."
Assert-True -Condition (($textOutput -join "`n").Contains("Handoff:")) -Message "Text handoff should print the Markdown handoff path."
Assert-True -Condition (($textOutput -join "`n").Contains("Runbook:")) -Message "Text handoff should print the validation runbook path."
Assert-True -Condition (($textOutput -join "`n").Contains("Review dashboard:")) -Message "Text handoff should print the review dashboard path."
Assert-True -Condition (($textOutput -join "`n").Contains("Manifest:")) -Message "Text handoff should print the manifest path."
Assert-True -Condition (($textOutput -join "`n").Contains("Warning: requested slot(s) not currently missing or unknown: notARealSlot")) -Message "Text handoff should warn about invalid slots."
Assert-True -Condition (($textOutput -join "`n").Contains("Warning: no recommended captures match the current filters.")) -Message "Text handoff should warn about empty queues."

$validExitCode = Invoke-HandoffExitCode -EvidenceRoot $evidenceRoot -OutputRoot $outputRoot -Slot pulseLinear -FailOnInvalidSlot -FailOnEmptyQueue -FailOnDeviceUnavailable -AdbPath $fakeAdb
$invalidExitCode = Invoke-HandoffExitCode -EvidenceRoot $evidenceRoot -OutputRoot $outputRoot -Slot notARealSlot -FailOnInvalidSlot
$emptyExitCode = Invoke-HandoffExitCode -EvidenceRoot $evidenceRoot -OutputRoot $outputRoot -Slot notARealSlot -FailOnEmptyQueue
$pendingReviewExitCode = Invoke-HandoffExitCode -EvidenceRoot $evidenceRoot -OutputRoot $outputRoot -Slot pulseLinear -FailOnPendingReviewSheets
$deviceUnavailableExitCode = Invoke-HandoffExitCode -EvidenceRoot $evidenceRoot -OutputRoot $outputRoot -Slot pulseLinear -FailOnDeviceUnavailable -AdbPath $fakeNoDeviceAdb
$repoRoot = Split-Path -Parent $PSScriptRoot
$dirtyProbePath = Join-Path $repoRoot "handoff_dirty_source_probe.tmp"
try {
    "dirty source probe" | Set-Content -LiteralPath $dirtyProbePath -Encoding utf8
    $dirtySourceExitCode = Invoke-HandoffExitCode -EvidenceRoot $evidenceRoot -OutputRoot $outputRoot -Slot pulseLinear -FailOnDirtySource
} finally {
    if (Test-Path -LiteralPath $dirtyProbePath) {
        Remove-Item -LiteralPath $dirtyProbePath -Force
    }
}
$unpublishedRepoRoot = Join-Path ([System.IO.Path]::GetTempPath()) "eulerian-handoff-unpublished-repo-$([guid]::NewGuid().ToString('N'))"
try {
    New-Item -ItemType Directory -Path $unpublishedRepoRoot -Force | Out-Null
    Push-Location -LiteralPath $unpublishedRepoRoot
    try {
        & git init *> $null
        & git config user.email "test@example.invalid"
        & git config user.name "Eulerian Test"
        "unpublished source" | Set-Content -LiteralPath (Join-Path $unpublishedRepoRoot "source.txt") -Encoding utf8
        & git add source.txt
        & git commit -m "Unpublished source fixture" *> $null
    } finally {
        Pop-Location
    }
    $unpushedSourceExitCode = Invoke-HandoffExitCode -EvidenceRoot $evidenceRoot -OutputRoot $outputRoot -Slot pulseLinear -FailOnUnpushedSource -SourceRoot $unpublishedRepoRoot
} finally {
    if (Test-Path -LiteralPath $unpublishedRepoRoot) {
        Remove-Item -LiteralPath $unpublishedRepoRoot -Recurse -Force
    }
}

Assert-Equal -Actual $validExitCode -Expected 0 -Message "Handoff gates should allow valid non-empty filters."
Assert-Equal -Actual $invalidExitCode -Expected 21 -Message "Handoff should fail invalid slot filters with exit code 21."
Assert-Equal -Actual $emptyExitCode -Expected 22 -Message "Handoff should fail empty command queues with exit code 22."
Assert-Equal -Actual $pendingReviewExitCode -Expected 23 -Message "Handoff should fail pending review-sheet issues with exit code 23."
Assert-Equal -Actual $dirtySourceExitCode -Expected 24 -Message "Handoff should fail dirty source trees with exit code 24."
Assert-Equal -Actual $unpushedSourceExitCode -Expected 25 -Message "Handoff should fail source commits that are not reachable from origin/main with exit code 25."
Assert-Equal -Actual $deviceUnavailableExitCode -Expected 26 -Message "Handoff should fail unavailable expected devices with exit code 26."

Write-Output "Pixel validation handoff self-test passed."
