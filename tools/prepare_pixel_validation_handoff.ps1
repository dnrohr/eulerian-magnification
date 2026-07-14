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

$planner = Join-Path $PSScriptRoot "show_next_pixel_validation_plan.ps1"
$closeoutSummary = Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1"

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

$result = [pscustomobject]@{
    evidenceRoot = $EvidenceRoot
    outputRoot = $OutputRoot
    planPath = $planPath
    closeoutPath = $closeoutPath
    commandsPath = $commandsPath
    requestedSlots = @($plan.requestedSlots)
    invalidRequestedSlots = @($plan.invalidRequestedSlots)
    captureStage = $plan.captureStage
    recommendedCaptureCount = @($plan.recommendedCaptures).Count
    commandCount = @($commands).Count
    closeoutBlockerCount = @($closeout.closeoutBlockers).Count
    readyForPresetDocs = $closeout.readyForPresetDocs
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
    Write-Output "Plan: $($result.planPath)"
    Write-Output "Closeout: $($result.closeoutPath)"
    Write-Output "Commands: $($result.commandsPath)"
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
