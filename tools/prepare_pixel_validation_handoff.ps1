param(
    [string]$EvidenceRoot = "sample-videos\exports\live-validation",
    [string]$OutputRoot = "",
    [string]$SourceRoot = "",
    [string]$DeviceSerial = "47091JEKB05516",
    [string]$AdbPath = "",
    [string]$FfmpegPath = "",
    [string[]]$Slot = @(),
    [ValidateSet("All", "Setup", "Final")]
    [string]$CaptureStage = "All",
    [switch]$FailOnInvalidSlot,
    [switch]$FailOnEmptyQueue,
    [switch]$FailOnPendingReviewSheets,
    [switch]$FailOnDirtySource,
    [switch]$FailOnUnpushedSource,
    [switch]$FailOnDeviceUnavailable,
    [switch]$Json
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($OutputRoot)) {
    $OutputRoot = $EvidenceRoot
}

$defaultSourceRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($SourceRoot)) {
    $SourceRoot = $defaultSourceRoot
}

if (-not (Test-Path -LiteralPath $OutputRoot)) {
    New-Item -ItemType Directory -Path $OutputRoot | Out-Null
}

$planPath = Join-Path $OutputRoot "pixel_validation_plan.json"
$closeoutPath = Join-Path $OutputRoot "pixel_closeout_summary.json"
$commandsPath = Join-Path $OutputRoot "pixel_validation_commands.txt"
$runbookPath = Join-Path $OutputRoot "pixel_validation_runbook.txt"
$reviewQueuePath = Join-Path $OutputRoot "live_validation_review_queue.json"
$reviewCommandsPath = Join-Path $OutputRoot "live_validation_review_commands.txt"
$reviewDashboardPath = Join-Path $OutputRoot "live_validation_review_dashboard.html"
$handoffPath = Join-Path $OutputRoot "pixel_validation_handoff.md"
$manifestPath = Join-Path $OutputRoot "pixel_validation_handoff_manifest.json"

$planner = Join-Path $PSScriptRoot "show_next_pixel_validation_plan.ps1"
$closeoutSummary = Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1"
$reviewQueueScript = Join-Path $PSScriptRoot "show_live_validation_review_queue.ps1"
$reviewDashboardScript = Join-Path $PSScriptRoot "export_live_validation_review_dashboard.ps1"

function Invoke-GitValue {
    param([string[]]$Arguments)

    try {
        $output = & git -C $SourceRoot @Arguments 2>$null
        if ($LASTEXITCODE -ne 0) {
            return $null
        }
        return ($output -join "`n").Trim()
    } catch {
        return $null
    }
}

function New-ArtifactRecord {
    param(
        [string]$Name,
        [string]$Path
    )

    $hash = Get-FileHash -LiteralPath $Path -Algorithm SHA256
    return [pscustomobject]@{
        name = $Name
        path = $Path
        sha256 = $hash.Hash.ToLowerInvariant()
    }
}

function Find-Adb {
    param([string]$ExplicitPath)

    if (-not [string]::IsNullOrWhiteSpace($ExplicitPath)) {
        if (Test-Path -LiteralPath $ExplicitPath) {
            return (Resolve-Path -LiteralPath $ExplicitPath).Path
        }
        throw "Requested adb path does not exist: $ExplicitPath"
    }

    $sdkAdb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
    if (Test-Path -LiteralPath $sdkAdb) {
        return $sdkAdb
    }

    $pathAdb = Get-Command adb -ErrorAction SilentlyContinue
    if ($pathAdb) {
        return $pathAdb.Source
    }

    return $null
}

function Get-DeviceAvailability {
    param(
        [string]$ExpectedSerial,
        [string]$ExplicitAdbPath
    )

    $adb = Find-Adb -ExplicitPath $ExplicitAdbPath
    if ([string]::IsNullOrWhiteSpace($adb)) {
        return [pscustomobject]@{
            adbPath = $null
            checked = $false
            expectedSerial = $ExpectedSerial
            connected = $false
            connectedSerials = @()
            note = "adb.exe was not found; device availability was not checked."
        }
    }

    try {
        $output = & $adb devices -l 2>&1
        $exitCode = $LASTEXITCODE
    } catch {
        return [pscustomobject]@{
            adbPath = $adb
            checked = $false
            expectedSerial = $ExpectedSerial
            connected = $false
            connectedSerials = @()
            note = "adb devices failed: $($_.Exception.Message)"
        }
    }

    if ($exitCode -ne 0) {
        return [pscustomobject]@{
            adbPath = $adb
            checked = $false
            expectedSerial = $ExpectedSerial
            connected = $false
            connectedSerials = @()
            note = "adb devices failed with exit code ${exitCode}: $($output -join ' ')"
        }
    }

    $connectedSerials = @()
    foreach ($line in @($output)) {
        if ($line -match '^(\S+)\s+device\b') {
            $connectedSerials += $Matches[1]
        }
    }
    $connected = -not [string]::IsNullOrWhiteSpace($ExpectedSerial) -and $ExpectedSerial -in $connectedSerials
    $note = if ($connected) {
        "Expected Pixel serial is connected."
    } elseif ($connectedSerials.Count -gt 0) {
        "ADB is available, but the expected Pixel serial is not connected."
    } else {
        "ADB is available, but no connected devices were reported."
    }

    return [pscustomobject]@{
        adbPath = $adb
        checked = $true
        expectedSerial = $ExpectedSerial
        connected = $connected
        connectedSerials = $connectedSerials
        note = $note
    }
}

function Format-CommandArgument {
    param([string]$Value)

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return '""'
    }
    return '"' + ($Value -replace '"', '\"') + '"'
}

$sourceBranch = Invoke-GitValue -Arguments @("rev-parse", "--abbrev-ref", "HEAD")
$sourceCommit = Invoke-GitValue -Arguments @("rev-parse", "HEAD")
$sourceStatus = Invoke-GitValue -Arguments @("status", "--porcelain")
$sourceClean = [string]::IsNullOrWhiteSpace($sourceStatus)
$sourceCommitReachableFromOriginMain = $false
if (-not [string]::IsNullOrWhiteSpace($sourceCommit)) {
    try {
        & git -C $SourceRoot merge-base --is-ancestor $sourceCommit origin/main *> $null
        $sourceCommitReachableFromOriginMain = ($LASTEXITCODE -eq 0)
    } catch {
        $sourceCommitReachableFromOriginMain = $false
    }
}

$plannerArgs = @{
    EvidenceRoot = $EvidenceRoot
    OutputPath = $planPath
    CaptureStage = $CaptureStage
    DeviceSerial = $DeviceSerial
    Json = $true
}
if (@($Slot).Count -gt 0) {
    $plannerArgs.Slot = $Slot
}

$plan = & $planner @plannerArgs | ConvertFrom-Json
$commandsArgs = @{
    EvidenceRoot = $EvidenceRoot
    CaptureStage = $CaptureStage
    DeviceSerial = $DeviceSerial
    CommandsOnly = $true
}
if (@($Slot).Count -gt 0) {
    $commandsArgs.Slot = $Slot
}

$commands = @(& $planner @commandsArgs)
Set-Content -LiteralPath $commandsPath -Value ([string]::Join([Environment]::NewLine, $commands)) -Encoding utf8
$commandText = @($commands) -join "`n"
$roiFinalHelperCommands = @()
if ($commandText.Contains("<visible-target-bounds-in-screenshot-space>")) {
    $roiFinalHelperCommands += ('.\tools\prepare_roi_final_capture_command.ps1 -Slot manualRoi -SetupBundle "<manual-roi-setup-bundle>" -PixelBounds "<left,top,right,bottom-from-setup-screenshot>" -DeviceSerial ' + (Format-CommandArgument $DeviceSerial) + ' -EvidenceRoot ' + (Format-CommandArgument $EvidenceRoot))
}
if ($commandText.Contains("<visible-face-or-skin-target-bounds-in-screenshot-space>")) {
    $roiFinalHelperCommands += ('.\tools\prepare_roi_final_capture_command.ps1 -Slot autoRoi -SetupBundle "<auto-roi-setup-bundle>" -PixelBounds "<left,top,right,bottom-from-setup-screenshot>" -DeviceSerial ' + (Format-CommandArgument $DeviceSerial) + ' -EvidenceRoot ' + (Format-CommandArgument $EvidenceRoot))
}

$closeout = & $closeoutSummary -EvidenceRoot $EvidenceRoot -OutputPath $closeoutPath -Json | ConvertFrom-Json
$reviewQueueArgs = @{
    EvidenceRoot = $EvidenceRoot
    OutputPath = $reviewQueuePath
    Json = $true
}
if (-not [string]::IsNullOrWhiteSpace($FfmpegPath)) {
    $reviewQueueArgs.FfmpegPath = $FfmpegPath
}
$reviewQueue = & $reviewQueueScript @reviewQueueArgs | ConvertFrom-Json
$reviewCommandArgs = @{
    EvidenceRoot = $EvidenceRoot
    CommandsOnly = $true
}
if (-not [string]::IsNullOrWhiteSpace($FfmpegPath)) {
    $reviewCommandArgs.FfmpegPath = $FfmpegPath
}
$reviewCommands = @(& $reviewQueueScript @reviewCommandArgs)
Set-Content -LiteralPath $reviewCommandsPath -Value ([string]::Join([Environment]::NewLine, $reviewCommands)) -Encoding utf8
& $reviewDashboardScript -EvidenceRoot $EvidenceRoot -OutputPath $reviewDashboardPath -PendingOnly *> $null
$requestedSlotLabel = if (@($plan.requestedSlots).Count -gt 0) { $plan.requestedSlots -join ", " } else { "All" }
$reviewIssueCounts = @{}
foreach ($entry in @($reviewQueue.pendingReviewSheets)) {
    $issue = if ([string]::IsNullOrWhiteSpace($entry.reviewSheetIssue)) { "pending" } else { $entry.reviewSheetIssue }
    if (-not $reviewIssueCounts.ContainsKey($issue)) {
        $reviewIssueCounts[$issue] = 0
    }
    $reviewIssueCounts[$issue] += 1
}
$deviceAvailability = Get-DeviceAvailability -ExpectedSerial $DeviceSerial -ExplicitAdbPath $AdbPath
$installCommandParts = @(
    ".\tools\install_debug_on_pixel.ps1",
    "-DeviceSerial $(Format-CommandArgument $DeviceSerial)",
    "-Build",
    "-Launch"
)
if (-not [string]::IsNullOrWhiteSpace($AdbPath)) {
    $installCommandParts = @($installCommandParts[0], "-AdbPath $(Format-CommandArgument $AdbPath)") + @($installCommandParts | Select-Object -Skip 1)
}
$installCommand = $installCommandParts -join " "
$runbookLines = @(
    "# Pixel validation runbook",
    "# 0. Confirm this handoff still matches the intended source and device state.",
    "# Source branch: $sourceBranch",
    "# Source commit: $sourceCommit",
    "# Source clean at handoff time: $sourceClean",
    "# Source commit reachable from origin/main: $sourceCommitReachableFromOriginMain",
    "# Expected device connected at handoff time: $($deviceAvailability.connected)",
    "# Handoff manifest: $manifestPath",
    ".\tools\verify_pixel_validation_handoff.ps1 -ManifestPath $(Format-CommandArgument $manifestPath) -SourceRoot $(Format-CommandArgument $SourceRoot) -FailOnArtifactMismatch -FailOnSourceMismatch -FailOnDeviceUnavailable -FailOnHandoffConsistencyMismatch",
    "git -C $(Format-CommandArgument $SourceRoot) status --short",
    "git -C $(Format-CommandArgument $SourceRoot) rev-parse HEAD",
    "git -C $(Format-CommandArgument $SourceRoot) merge-base --is-ancestor $(Format-CommandArgument $sourceCommit) origin/main",
    "",
    "# 1. Install and launch the current debug build.",
    $installCommand,
    "",
    "# 2. Wait for a cool enough device before watched capture.",
    $plan.thermalReadiness.command,
    "",
    "# 3. Capture the requested validation evidence.",
    @($commands)
)
if (@($roiFinalHelperCommands).Count -gt 0) {
    $runbookLines += @(
        "",
        "# 3a. Prepare ROI final commands after setup screenshots.",
        "# Replace setup-bundle and PixelBounds placeholders after measuring the setup screenshot.",
        @($roiFinalHelperCommands)
    )
}
$runbookLines += @(
    "",
    "# 4. Generate or refresh screenrecord review sheets.",
    @($reviewCommands)
)
Set-Content -LiteralPath $runbookPath -Value $runbookLines -Encoding utf8

$handoffLines = @(
    "# Pixel Validation Handoff",
    "",
    ('- Evidence root: `{0}`' -f $EvidenceRoot),
    ('- Output root: `{0}`' -f $OutputRoot),
    ('- Device serial: `{0}`' -f $DeviceSerial),
    ('- Capture stage: `{0}`' -f $plan.captureStage),
    ('- Requested slots: `{0}`' -f $requestedSlotLabel),
    "- Recommended captures: $(@($plan.recommendedCaptures).Count)",
    ('- Thermal readiness command: `{0}`' -f $plan.thermalReadiness.command),
    "- Pending review sheets: $($reviewQueue.pendingReviewSheetCount)",
    "- Pending review-sheet issue types: $($reviewIssueCounts.Count)",
    "- Closeout blockers: $(@($closeout.closeoutBlockers).Count)",
    "- Ready for preset docs: $($closeout.readyForPresetDocs)",
    "- Source branch: $sourceBranch",
    "- Source commit: $sourceCommit",
    "- Source clean: $sourceClean",
    "- Source commit reachable from origin/main: $sourceCommitReachableFromOriginMain",
    "- Device availability checked: $($deviceAvailability.checked)",
    "- Expected device connected: $($deviceAvailability.connected)",
    "- Connected device serials: $(@($deviceAvailability.connectedSerials) -join ', ')",
    "- Device availability note: $($deviceAvailability.note)",
    ('- Install command: `{0}`' -f $installCommand),
    "",
    "## Artifacts",
    "",
    ('- Plan JSON: `{0}`' -f $planPath),
    ('- Closeout JSON: `{0}`' -f $closeoutPath),
    ('- Command list: `{0}`' -f $commandsPath),
    ('- Runbook: `{0}`' -f $runbookPath),
    ('- Review queue JSON: `{0}`' -f $reviewQueuePath),
    ('- Review command list: `{0}`' -f $reviewCommandsPath),
    ('- Review dashboard: `{0}`' -f $reviewDashboardPath),
    ('- Handoff manifest: `{0}`' -f $manifestPath),
    "",
    "## Recommended Captures",
    ""
)

if (@($plan.recommendedCaptures).Count -eq 0) {
    $handoffLines += "- No recommended captures match the current filters."
} else {
    foreach ($capture in @($plan.recommendedCaptures)) {
        $handoffLines += "- $($capture.title) [$($capture.milestones -join ', ')]"
        $handoffLines += ('  - Slot: `{0}`' -f $capture.slot)
        $handoffLines += ('  - Expected final label: `{0}`' -f $capture.expectedFinalLabel)
        $handoffLines += ('  - Protocol: `{0}`' -f $capture.protocol)
        if (@($capture.operatorSetup).Count -gt 0) {
            $handoffLines += '  - Operator setup:'
            foreach ($setupStep in @($capture.operatorSetup)) {
                $handoffLines += ('    - {0}' -f $setupStep)
            }
        }
        if (@($capture.acceptanceChecks).Count -gt 0) {
            $handoffLines += '  - Acceptance checks:'
            foreach ($acceptanceCheck in @($capture.acceptanceChecks)) {
                $handoffLines += ('    - {0}' -f $acceptanceCheck)
            }
        }
    }
}

$handoffLines += @(
    "",
    "## Thermal Preflight",
    "",
    "Install and launch the current debug build on the expected Pixel before watched capture:",
    "",
    '```powershell',
    $installCommand,
    '```',
    "",
    "Run this before a watched phone validation session if the device may be warm, the camera preview looks frozen, or FPS is low:",
    "",
    '```powershell',
    $plan.thermalReadiness.command,
    '```',
    "",
    "## Closeout Blockers",
    ""
)

if (@($closeout.closeoutBlockers).Count -eq 0) {
    $handoffLines += "- None."
} else {
    foreach ($blocker in @($closeout.closeoutBlockers)) {
        $handoffLines += "- $($blocker.kind): $($blocker.message)"
    }
}

$handoffLines += @(
    "",
    "## Review Sheet Queue",
    ""
)

if (@($reviewQueue.pendingReviewSheets).Count -eq 0) {
    $handoffLines += "- No captured screenrecords have pending review-sheet issues."
} else {
    foreach ($entry in @($reviewQueue.pendingReviewSheets)) {
        $issue = if ([string]::IsNullOrWhiteSpace($entry.reviewSheetIssue)) { "pending" } else { $entry.reviewSheetIssue }
        $handoffLines += "- $($entry.label): $issue"
        $handoffLines += ('  - Bundle: `{0}`' -f $entry.bundle)
        $handoffLines += ('  - Command: `{0}`' -f $entry.command)
    }
}

if (@($roiFinalHelperCommands).Count -gt 0) {
    $handoffLines += @(
        "",
        "## ROI Final Command Helpers",
        "",
        "Run the matching helper after the setup capture, using pixel bounds measured from that setup `screenshot.png`. Paste the printed final capture command instead of the placeholder command.",
        "",
        '```powershell'
    )
    $handoffLines += $roiFinalHelperCommands
    $handoffLines += '```'
}

$handoffLines += @(
    "",
    "## Commands",
    "",
    '```powershell'
)
$handoffLines += $commands
$handoffLines += '```'

$handoffLines += @(
    "",
    "## Review Sheet Commands",
    "",
    '```powershell'
)
$handoffLines += $reviewCommands
$handoffLines += '```'
Set-Content -LiteralPath $handoffPath -Value $handoffLines -Encoding utf8

$artifactRecords = @(
    New-ArtifactRecord -Name "plan" -Path $planPath
    New-ArtifactRecord -Name "closeout" -Path $closeoutPath
    New-ArtifactRecord -Name "commands" -Path $commandsPath
    New-ArtifactRecord -Name "runbook" -Path $runbookPath
    New-ArtifactRecord -Name "reviewQueue" -Path $reviewQueuePath
    New-ArtifactRecord -Name "reviewCommands" -Path $reviewCommandsPath
    New-ArtifactRecord -Name "reviewDashboard" -Path $reviewDashboardPath
    New-ArtifactRecord -Name "handoff" -Path $handoffPath
)
$manifest = [pscustomobject]@{
    evidenceRoot = $EvidenceRoot
    outputRoot = $OutputRoot
    deviceSerial = $DeviceSerial
    source = [pscustomobject]@{
        branch = $sourceBranch
        commit = $sourceCommit
        clean = $sourceClean
        commitReachableFromOriginMain = $sourceCommitReachableFromOriginMain
    }
    artifacts = $artifactRecords
    review = [pscustomobject]@{
        pendingReviewSheetCount = $reviewQueue.pendingReviewSheetCount
        screenrecordBundleCount = $reviewQueue.screenrecordBundleCount
        issueCounts = [pscustomobject]$reviewIssueCounts
        ffmpegPath = $FfmpegPath
    }
    thermalReadiness = $plan.thermalReadiness
    deviceAvailability = $deviceAvailability
    installCommand = $installCommand
    roiFinalHelperCommands = $roiFinalHelperCommands
}
$manifest | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $manifestPath -Encoding utf8

$result = [pscustomobject]@{
    evidenceRoot = $EvidenceRoot
    outputRoot = $OutputRoot
    deviceSerial = $DeviceSerial
    planPath = $planPath
    closeoutPath = $closeoutPath
    commandsPath = $commandsPath
    runbookPath = $runbookPath
    reviewQueuePath = $reviewQueuePath
    reviewCommandsPath = $reviewCommandsPath
    reviewDashboardPath = $reviewDashboardPath
    handoffPath = $handoffPath
    manifestPath = $manifestPath
    artifactHashes = $artifactRecords
    requestedSlots = @($plan.requestedSlots)
    invalidRequestedSlots = @($plan.invalidRequestedSlots)
    captureStage = $plan.captureStage
    recommendedCaptureCount = @($plan.recommendedCaptures).Count
    commandCount = @($commands).Count
    pendingReviewSheetCount = $reviewQueue.pendingReviewSheetCount
    pendingReviewSheetIssueCounts = [pscustomobject]$reviewIssueCounts
    reviewCommandCount = @($reviewCommands).Count
    roiFinalHelperCommandCount = @($roiFinalHelperCommands).Count
    closeoutBlockerCount = @($closeout.closeoutBlockers).Count
    readyForPresetDocs = $closeout.readyForPresetDocs
    thermalReadiness = $plan.thermalReadiness
    deviceAvailability = $deviceAvailability
    installCommand = $installCommand
    source = [pscustomobject]@{
        branch = $sourceBranch
        commit = $sourceCommit
        clean = $sourceClean
        commitReachableFromOriginMain = $sourceCommitReachableFromOriginMain
    }
}

if ($Json) {
    $result | ConvertTo-Json -Depth 6
} else {
    Write-Output "Pixel validation handoff prepared"
    Write-Output "Evidence root: $($result.evidenceRoot)"
    Write-Output "Output root: $($result.outputRoot)"
    Write-Output "Device serial: $($result.deviceSerial)"
    Write-Output "Capture stage: $($result.captureStage)"
    Write-Output "Recommended captures: $($result.recommendedCaptureCount)"
    Write-Output "Command templates: $($result.commandCount)"
    Write-Output "Pending review sheets: $($result.pendingReviewSheetCount)"
    Write-Output "Pending review-sheet issue types: $(@($result.pendingReviewSheetIssueCounts.PSObject.Properties).Count)"
    Write-Output "Review commands: $($result.reviewCommandCount)"
    Write-Output "Closeout blockers: $($result.closeoutBlockerCount)"
    Write-Output "Source: $($result.source.branch) $($result.source.commit)"
    Write-Output "Source clean: $($result.source.clean)"
    Write-Output "Source reachable from origin/main: $($result.source.commitReachableFromOriginMain)"
    Write-Output "Device availability checked: $($result.deviceAvailability.checked)"
    Write-Output "Expected device connected: $($result.deviceAvailability.connected)"
    Write-Output "Device availability note: $($result.deviceAvailability.note)"
    Write-Output "Install command: $($result.installCommand)"
    Write-Output "Plan: $($result.planPath)"
    Write-Output "Closeout: $($result.closeoutPath)"
    Write-Output "Commands: $($result.commandsPath)"
    Write-Output "Runbook: $($result.runbookPath)"
    Write-Output "Review queue: $($result.reviewQueuePath)"
    Write-Output "Review commands: $($result.reviewCommandsPath)"
    Write-Output "Review dashboard: $($result.reviewDashboardPath)"
    Write-Output "Handoff: $($result.handoffPath)"
    Write-Output "Manifest: $($result.manifestPath)"
    if (@($result.invalidRequestedSlots).Count -gt 0) {
        Write-Output "Warning: requested slot(s) not currently missing or unknown: $($result.invalidRequestedSlots -join ', ')"
    }
    if ($result.recommendedCaptureCount -eq 0) {
        Write-Output "Warning: no recommended captures match the current filters."
    }
}

if ($FailOnInvalidSlot -and @($result.invalidRequestedSlots).Count -gt 0) {
    exit 21
}
if ($FailOnEmptyQueue -and $result.recommendedCaptureCount -eq 0) {
    exit 22
}
if ($FailOnPendingReviewSheets -and $result.pendingReviewSheetCount -gt 0) {
    exit 23
}
if ($FailOnDirtySource -and -not $result.source.clean) {
    exit 24
}
if ($FailOnUnpushedSource -and -not $result.source.commitReachableFromOriginMain) {
    exit 25
}
if ($FailOnDeviceUnavailable -and -not $result.deviceAvailability.connected) {
    exit 26
}
