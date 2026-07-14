param(
    [string]$EvidenceRoot = "sample-videos\exports\live-validation",
    [switch]$Json,
    [switch]$FailOnMissing,
    [switch]$FailOnUnmatched,
    [switch]$FailOnAmbiguous,
    [switch]$FailOnDuplicate,
    [switch]$FailOnCloseoutNotReady,
    [switch]$FailOnPresetDocsNotReady
)

$ErrorActionPreference = "Stop"

function Test-GatePassed {
    param(
        $Summary,
        [string]$Gate
    )

    return $Summary.requiredGates -and
        ($Summary.requiredGates.PSObject.Properties.Name -contains $Gate) -and
        ($Summary.requiredGates.$Gate.passed -eq $true)
}

function Test-FinalVisualEvidence {
    param($Summary)

    foreach ($gate in @("cleanSource", "visualValidation", "screenrecord", "thermalReady", "cameraFps", "focusedApp", "noWarnings")) {
        if (-not (Test-GatePassed -Summary $Summary -Gate $gate)) {
            return $false
        }
    }

    return $Summary.evidenceVerdict -and $Summary.evidenceVerdict.status -eq "visual_validated"
}

function Get-SummaryText {
    param($Summary)

    $parts = @()
    foreach ($value in @(
        $Summary.label,
        $Summary.launch.roiSource,
        $Summary.visualReview.targetDescription,
        $Summary.visualReview.visualClaim
    )) {
        if (-not [string]::IsNullOrWhiteSpace($value)) {
            $parts += [string]$value
        }
    }
    foreach ($value in @($Summary.uiDump.rendererLabels) + @($Summary.uiDump.phaseLabels)) {
        if (-not [string]::IsNullOrWhiteSpace($value)) {
            $parts += [string]$value
        }
    }

    return (($parts | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }) -join " ").ToLowerInvariant()
}

function New-Slot {
    param(
        [string]$Id,
        [string]$Title,
        [string[]]$Milestones,
        [string]$RequiredEvidence,
        [string]$Protocol,
        [string]$NextCommand
    )

    return [ordered]@{
        id = $Id
        title = $Title
        milestones = $Milestones
        requiredEvidence = $RequiredEvidence
        protocol = $Protocol
        nextCommand = $NextCommand
        satisfied = $false
        bundle = $null
        label = $null
        reason = "missing accepted evidence"
    }
}

$slots = [ordered]@{
    manualRoi = New-Slot -Id "manualRoi" -Title "Manual ROI known-target alignment" -Milestones @("M", "U") -RequiredEvidence "visual_validated final evidence with passing ROI measurement" -Protocol "docs/testing/ROI_DEVICE_VALIDATION.md" -NextCommand "manual-roi-known-target-setup, then manual-roi-known-target-final"
    autoRoi = New-Slot -Id "autoRoi" -Title "Automatic face/skin ROI alignment" -Milestones @("M", "U") -RequiredEvidence "visual_validated final evidence with passing ROI measurement" -Protocol "docs/testing/ROI_DEVICE_VALIDATION.md" -NextCommand "auto-face-roi-setup, then auto-face-roi-final"
    pulseLinear = New-Slot -Id "pulseLinear" -Title "Pulse live linear visual parity" -Milestones @("AE", "AP", "AT") -RequiredEvidence "visual_validated final evidence with renderer diagnostics" -Protocol "docs/experiments/pixel8a_live_linear_validation.md" -NextCommand "live-linear-pulse-setup, then live-linear-pulse-final"
    breathingLinear = New-Slot -Id "breathingLinear" -Title "Breathing live linear visual parity" -Milestones @("AE", "AP", "AT") -RequiredEvidence "visual_validated final evidence with renderer diagnostics" -Protocol "docs/experiments/pixel8a_live_linear_validation.md" -NextCommand "live-linear-breathing-setup, then live-linear-breathing-final"
    objectPhase = New-Slot -Id "objectPhase" -Title "Object vibration live phase visual parity" -Milestones @("AR", "AT") -RequiredEvidence "visual_validated final evidence with phase diagnostics" -Protocol "docs/experiments/pixel8a_live_phase_validation.md" -NextCommand "live-phase-object-setup, then live-phase-object-final"
    fastTremorPhase = New-Slot -Id "fastTremorPhase" -Title "Fast tremor live phase visual parity" -Milestones @("AR", "AT") -RequiredEvidence "visual_validated final evidence with phase diagnostics" -Protocol "docs/experiments/pixel8a_live_phase_validation.md" -NextCommand "live-phase-fast-tremor-setup, then live-phase-fast-tremor-final"
}

$rootPath = Resolve-Path -LiteralPath $EvidenceRoot -ErrorAction SilentlyContinue
$summaryFiles = @()
if ($rootPath) {
    $summaryFiles = @(Get-ChildItem -LiteralPath $rootPath.Path -Recurse -Filter "evidence_summary.json" -File)
}

$acceptedSummaries = @()
foreach ($file in $summaryFiles) {
    $summary = Get-Content -LiteralPath $file.FullName -Raw | ConvertFrom-Json
    $summary | Add-Member -NotePropertyName summaryPath -NotePropertyValue $file.FullName -Force
    $acceptedSummaries += $summary
}

$acceptedFinalSummaries = @()
$unmatchedAcceptedFinalEvidence = @()
$ambiguousAcceptedFinalEvidence = @()
$duplicateAcceptedFinalEvidence = @()
foreach ($summary in $acceptedSummaries) {
    if (-not (Test-FinalVisualEvidence -Summary $summary)) {
        continue
    }

    $acceptedFinalSummaries += $summary
    $text = Get-SummaryText -Summary $summary
    $roiPassed = Test-GatePassed -Summary $summary -Gate "roiMeasurement"
    $rendererPassed = Test-GatePassed -Summary $summary -Gate "rendererDiagnostics"
    $phasePassed = Test-GatePassed -Summary $summary -Gate "phaseDiagnostics"

    $matchedSlots = @()
    if ($roiPassed -and $text -match 'manual') { $matchedSlots += "manualRoi" }
    if ($roiPassed -and $text -match 'auto|automatic|face|skin') { $matchedSlots += "autoRoi" }
    if ($rendererPassed -and $text -match 'pulse') { $matchedSlots += "pulseLinear" }
    if ($rendererPassed -and $text -match 'breath|slow-motion|slow motion') { $matchedSlots += "breathingLinear" }
    if ($phasePassed -and $text -match 'object|vibration') { $matchedSlots += "objectPhase" }
    if ($phasePassed -and $text -match 'fast|tremor') { $matchedSlots += "fastTremorPhase" }

    if ($matchedSlots.Count -eq 0) {
        $unmatchedAcceptedFinalEvidence += [pscustomobject]@{
            bundle = Split-Path -Parent $summary.summaryPath
            label = $summary.label
            mode = $summary.launch.mode
            roiSource = $summary.launch.roiSource
            visualClaim = $summary.visualReview.visualClaim
            reason = "accepted final evidence did not match any closeout slot"
        }
    }
    if ($matchedSlots.Count -gt 1) {
        $ambiguousAcceptedFinalEvidence += [pscustomobject]@{
            bundle = Split-Path -Parent $summary.summaryPath
            label = $summary.label
            mode = $summary.launch.mode
            roiSource = $summary.launch.roiSource
            visualClaim = $summary.visualReview.visualClaim
            matchedSlots = $matchedSlots
            reason = "accepted final evidence matched multiple closeout slots"
        }
    }

    foreach ($slotId in $matchedSlots) {
        if (-not $slots[$slotId].satisfied) {
            $slots[$slotId].satisfied = $true
            $slots[$slotId].bundle = Split-Path -Parent $summary.summaryPath
            $slots[$slotId].label = $summary.label
            $slots[$slotId].reason = "accepted final evidence"
        } else {
            $duplicateAcceptedFinalEvidence += [pscustomobject]@{
                slot = $slotId
                bundle = Split-Path -Parent $summary.summaryPath
                label = $summary.label
                originalBundle = $slots[$slotId].bundle
                originalLabel = $slots[$slotId].label
                reason = "accepted final evidence matched an already satisfied closeout slot"
            }
        }
    }
}

$slotList = @($slots.Values | ForEach-Object { [pscustomobject]$_ })
$missing = @($slotList | Where-Object { -not $_.satisfied })
$allCloseoutEvidencePresent = ($missing.Count -eq 0)
$allCloseoutEvidenceClean = (
    $allCloseoutEvidencePresent -and
    $unmatchedAcceptedFinalEvidence.Count -eq 0 -and
    $ambiguousAcceptedFinalEvidence.Count -eq 0 -and
    $duplicateAcceptedFinalEvidence.Count -eq 0
)
$result = [pscustomobject]@{
    evidenceRoot = if ($rootPath) { $rootPath.Path } else { $EvidenceRoot }
    summaryCount = $summaryFiles.Count
    acceptedFinalEvidenceCount = $acceptedFinalSummaries.Count
    unmatchedAcceptedFinalEvidence = $unmatchedAcceptedFinalEvidence
    ambiguousAcceptedFinalEvidence = $ambiguousAcceptedFinalEvidence
    duplicateAcceptedFinalEvidence = $duplicateAcceptedFinalEvidence
    slots = $slotList
    missing = $missing
    readyForPresetDocs = (@($slotList | Where-Object { $_.id -in @("pulseLinear", "breathingLinear", "objectPhase", "fastTremorPhase") -and $_.satisfied }).Count -eq 4)
    allCloseoutEvidencePresent = $allCloseoutEvidencePresent
    allCloseoutEvidenceClean = $allCloseoutEvidenceClean
}

if ($Json) {
    $result | ConvertTo-Json -Depth 8
} else {
    Write-Output "Pixel validation closeout"
    Write-Output "Evidence root: $($result.evidenceRoot)"
    Write-Output "Evidence summaries: $($result.summaryCount)"
    Write-Output "Accepted final evidence: $($result.acceptedFinalEvidenceCount)"
    Write-Output "Unmatched accepted final evidence: $(@($result.unmatchedAcceptedFinalEvidence).Count)"
    Write-Output "Ambiguous accepted final evidence: $(@($result.ambiguousAcceptedFinalEvidence).Count)"
    Write-Output "Duplicate accepted final evidence: $(@($result.duplicateAcceptedFinalEvidence).Count)"
    Write-Output ""
    foreach ($slot in $slotList) {
        $mark = if ($slot.satisfied) { "[x]" } else { "[ ]" }
        Write-Output "$mark $($slot.title) [$($slot.milestones -join ', ')]"
        Write-Output "    Required: $($slot.requiredEvidence)"
        Write-Output "    Protocol: $($slot.protocol)"
        if ($slot.satisfied) {
            Write-Output "    Evidence: $($slot.bundle)"
        } else {
            Write-Output "    Status: $($slot.reason)"
            Write-Output "    Next: $($slot.nextCommand)"
        }
    }
    Write-Output ""
    Write-Output "Ready for preset docs update: $($result.readyForPresetDocs)"
    Write-Output "All closeout evidence present: $($result.allCloseoutEvidencePresent)"
    Write-Output "All closeout evidence clean: $($result.allCloseoutEvidenceClean)"
    if (@($result.unmatchedAcceptedFinalEvidence).Count -gt 0) {
        Write-Output ""
        Write-Output "Unmatched accepted final evidence:"
        foreach ($evidence in @($result.unmatchedAcceptedFinalEvidence)) {
            Write-Output "- $($evidence.label): $($evidence.bundle)"
            Write-Output "    $($evidence.reason)"
        }
    }
    if (@($result.ambiguousAcceptedFinalEvidence).Count -gt 0) {
        Write-Output ""
        Write-Output "Ambiguous accepted final evidence:"
        foreach ($evidence in @($result.ambiguousAcceptedFinalEvidence)) {
            Write-Output "- $($evidence.label): $($evidence.bundle)"
            Write-Output "    $($evidence.reason): $($evidence.matchedSlots -join ', ')"
        }
    }
    if (@($result.duplicateAcceptedFinalEvidence).Count -gt 0) {
        Write-Output ""
        Write-Output "Duplicate accepted final evidence:"
        foreach ($evidence in @($result.duplicateAcceptedFinalEvidence)) {
            Write-Output "- $($evidence.label): $($evidence.bundle)"
            Write-Output "    $($evidence.reason): $($evidence.slot)"
            Write-Output "    Original: $($evidence.originalLabel): $($evidence.originalBundle)"
        }
    }
}

if ($FailOnMissing -and $missing.Count -gt 0) {
    exit 2
}
if ($FailOnUnmatched -and $unmatchedAcceptedFinalEvidence.Count -gt 0) {
    exit 3
}
if ($FailOnAmbiguous -and $ambiguousAcceptedFinalEvidence.Count -gt 0) {
    exit 5
}
if ($FailOnDuplicate -and $duplicateAcceptedFinalEvidence.Count -gt 0) {
    exit 6
}
if ($FailOnCloseoutNotReady -and -not $result.allCloseoutEvidenceClean) {
    exit 7
}
if ($FailOnPresetDocsNotReady -and -not $result.readyForPresetDocs) {
    exit 4
}

exit 0
