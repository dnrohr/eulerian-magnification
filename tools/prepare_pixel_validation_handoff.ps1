param(
    [string]$EvidenceRoot = "sample-videos\exports\live-validation",
    [string]$OutputRoot = "",
    [string[]]$Slot = @(),
    [ValidateSet("All", "Setup", "Final")]
    [string]$CaptureStage = "All",
    [switch]$FailOnInvalidSlot,
    [switch]$FailOnEmptyQueue,
    [switch]$Json
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($OutputRoot)) {
    $OutputRoot = $EvidenceRoot
}

if (-not (Test-Path -LiteralPath $OutputRoot)) {
    New-Item -ItemType Directory -Path $OutputRoot | Out-Null
}

$planPath = Join-Path $OutputRoot "pixel_validation_plan.json"
$closeoutPath = Join-Path $OutputRoot "pixel_closeout_summary.json"
$commandsPath = Join-Path $OutputRoot "pixel_validation_commands.txt"
$handoffPath = Join-Path $OutputRoot "pixel_validation_handoff.md"
$manifestPath = Join-Path $OutputRoot "pixel_validation_handoff_manifest.json"

$planner = Join-Path $PSScriptRoot "show_next_pixel_validation_plan.ps1"
$closeoutSummary = Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1"

function Invoke-GitValue {
    param([string[]]$Arguments)

    try {
        $output = & git @Arguments 2>$null
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

$sourceBranch = Invoke-GitValue -Arguments @("rev-parse", "--abbrev-ref", "HEAD")
$sourceCommit = Invoke-GitValue -Arguments @("rev-parse", "HEAD")
$sourceStatus = Invoke-GitValue -Arguments @("status", "--porcelain")
$sourceClean = [string]::IsNullOrWhiteSpace($sourceStatus)
$sourceCommitReachableFromOriginMain = $false
if (-not [string]::IsNullOrWhiteSpace($sourceCommit)) {
    & git merge-base --is-ancestor $sourceCommit origin/main 2>$null
    $sourceCommitReachableFromOriginMain = ($LASTEXITCODE -eq 0)
}

$plannerArgs = @{
    EvidenceRoot = $EvidenceRoot
    OutputPath = $planPath
    CaptureStage = $CaptureStage
    Json = $true
}
if (@($Slot).Count -gt 0) {
    $plannerArgs.Slot = $Slot
}

$plan = & $planner @plannerArgs | ConvertFrom-Json
$commandsArgs = @{
    EvidenceRoot = $EvidenceRoot
    CaptureStage = $CaptureStage
    CommandsOnly = $true
}
if (@($Slot).Count -gt 0) {
    $commandsArgs.Slot = $Slot
}

$commands = @(& $planner @commandsArgs)
Set-Content -LiteralPath $commandsPath -Value ([string]::Join([Environment]::NewLine, $commands)) -Encoding utf8

$closeout = & $closeoutSummary -EvidenceRoot $EvidenceRoot -OutputPath $closeoutPath -Json | ConvertFrom-Json
$requestedSlotLabel = if (@($plan.requestedSlots).Count -gt 0) { $plan.requestedSlots -join ", " } else { "All" }

$handoffLines = @(
    "# Pixel Validation Handoff",
    "",
    ('- Evidence root: `{0}`' -f $EvidenceRoot),
    ('- Output root: `{0}`' -f $OutputRoot),
    ('- Capture stage: `{0}`' -f $plan.captureStage),
    ('- Requested slots: `{0}`' -f $requestedSlotLabel),
    "- Recommended captures: $(@($plan.recommendedCaptures).Count)",
    "- Closeout blockers: $(@($closeout.closeoutBlockers).Count)",
    "- Ready for preset docs: $($closeout.readyForPresetDocs)",
    "- Source branch: $sourceBranch",
    "- Source commit: $sourceCommit",
    "- Source clean: $sourceClean",
    "- Source commit reachable from origin/main: $sourceCommitReachableFromOriginMain",
    "",
    "## Artifacts",
    "",
    ('- Plan JSON: `{0}`' -f $planPath),
    ('- Closeout JSON: `{0}`' -f $closeoutPath),
    ('- Command list: `{0}`' -f $commandsPath),
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
    }
}

$handoffLines += @(
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
    "## Commands",
    "",
    '```powershell'
)
$handoffLines += $commands
$handoffLines += '```'
Set-Content -LiteralPath $handoffPath -Value $handoffLines -Encoding utf8

$artifactRecords = @(
    New-ArtifactRecord -Name "plan" -Path $planPath
    New-ArtifactRecord -Name "closeout" -Path $closeoutPath
    New-ArtifactRecord -Name "commands" -Path $commandsPath
    New-ArtifactRecord -Name "handoff" -Path $handoffPath
)
$manifest = [pscustomobject]@{
    evidenceRoot = $EvidenceRoot
    outputRoot = $OutputRoot
    source = [pscustomobject]@{
        branch = $sourceBranch
        commit = $sourceCommit
        clean = $sourceClean
        commitReachableFromOriginMain = $sourceCommitReachableFromOriginMain
    }
    artifacts = $artifactRecords
}
$manifest | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $manifestPath -Encoding utf8

$result = [pscustomobject]@{
    evidenceRoot = $EvidenceRoot
    outputRoot = $OutputRoot
    planPath = $planPath
    closeoutPath = $closeoutPath
    commandsPath = $commandsPath
    handoffPath = $handoffPath
    manifestPath = $manifestPath
    artifactHashes = $artifactRecords
    requestedSlots = @($plan.requestedSlots)
    invalidRequestedSlots = @($plan.invalidRequestedSlots)
    captureStage = $plan.captureStage
    recommendedCaptureCount = @($plan.recommendedCaptures).Count
    commandCount = @($commands).Count
    closeoutBlockerCount = @($closeout.closeoutBlockers).Count
    readyForPresetDocs = $closeout.readyForPresetDocs
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
    Write-Output "Capture stage: $($result.captureStage)"
    Write-Output "Recommended captures: $($result.recommendedCaptureCount)"
    Write-Output "Command templates: $($result.commandCount)"
    Write-Output "Closeout blockers: $($result.closeoutBlockerCount)"
    Write-Output "Source: $($result.source.branch) $($result.source.commit)"
    Write-Output "Source clean: $($result.source.clean)"
    Write-Output "Source reachable from origin/main: $($result.source.commitReachableFromOriginMain)"
    Write-Output "Plan: $($result.planPath)"
    Write-Output "Closeout: $($result.closeoutPath)"
    Write-Output "Commands: $($result.commandsPath)"
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
