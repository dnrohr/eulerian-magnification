param(
    [string]$EvidenceRoot = "sample-videos\exports\live-validation",
    [switch]$Json,
    [switch]$FailOnMissing
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

    $parts = @(
        $Summary.label,
        $Summary.launch.mode,
        $Summary.launch.roiSource,
        $Summary.visualReview.targetDescription,
        $Summary.visualReview.visualClaim,
        @($Summary.uiDump.rendererLabels) -join " ",
        @($Summary.uiDump.phaseLabels) -join " "
    )

    return (($parts | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }) -join " ").ToLowerInvariant()
}

function New-Slot {
    param(
        [string]$Id,
        [string]$Title,
        [string[]]$Milestones,
        [string]$RequiredEvidence
    )

    return [ordered]@{
        id = $Id
        title = $Title
        milestones = $Milestones
        requiredEvidence = $RequiredEvidence
        satisfied = $false
        bundle = $null
        label = $null
        reason = "missing accepted evidence"
    }
}

$slots = [ordered]@{
    manualRoi = New-Slot -Id "manualRoi" -Title "Manual ROI known-target alignment" -Milestones @("M", "U") -RequiredEvidence "visual_validated final evidence with passing ROI measurement"
    autoRoi = New-Slot -Id "autoRoi" -Title "Automatic face/skin ROI alignment" -Milestones @("M", "U") -RequiredEvidence "visual_validated final evidence with passing ROI measurement"
    pulseLinear = New-Slot -Id "pulseLinear" -Title "Pulse live linear visual parity" -Milestones @("AE", "AP", "AT") -RequiredEvidence "visual_validated final evidence with renderer diagnostics"
    breathingLinear = New-Slot -Id "breathingLinear" -Title "Breathing live linear visual parity" -Milestones @("AE", "AP", "AT") -RequiredEvidence "visual_validated final evidence with renderer diagnostics"
    objectPhase = New-Slot -Id "objectPhase" -Title "Object vibration live phase visual parity" -Milestones @("AR", "AT") -RequiredEvidence "visual_validated final evidence with phase diagnostics"
    fastTremorPhase = New-Slot -Id "fastTremorPhase" -Title "Fast tremor live phase visual parity" -Milestones @("AR", "AT") -RequiredEvidence "visual_validated final evidence with phase diagnostics"
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

foreach ($summary in $acceptedSummaries) {
    if (-not (Test-FinalVisualEvidence -Summary $summary)) {
        continue
    }

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

    foreach ($slotId in $matchedSlots) {
        if (-not $slots[$slotId].satisfied) {
            $slots[$slotId].satisfied = $true
            $slots[$slotId].bundle = Split-Path -Parent $summary.summaryPath
            $slots[$slotId].label = $summary.label
            $slots[$slotId].reason = "accepted final evidence"
        }
    }
}

$slotList = @($slots.Values | ForEach-Object { [pscustomobject]$_ })
$missing = @($slotList | Where-Object { -not $_.satisfied })
$result = [pscustomobject]@{
    evidenceRoot = if ($rootPath) { $rootPath.Path } else { $EvidenceRoot }
    summaryCount = $summaryFiles.Count
    acceptedFinalEvidenceCount = @($acceptedSummaries | Where-Object { Test-FinalVisualEvidence -Summary $_ }).Count
    slots = $slotList
    missing = $missing
    readyForPresetDocs = (@($slotList | Where-Object { $_.id -in @("pulseLinear", "breathingLinear", "objectPhase", "fastTremorPhase") -and $_.satisfied }).Count -eq 4)
    allCloseoutEvidencePresent = ($missing.Count -eq 0)
}

if ($Json) {
    $result | ConvertTo-Json -Depth 8
} else {
    Write-Output "Pixel validation closeout"
    Write-Output "Evidence root: $($result.evidenceRoot)"
    Write-Output "Evidence summaries: $($result.summaryCount)"
    Write-Output "Accepted final evidence: $($result.acceptedFinalEvidenceCount)"
    Write-Output ""
    foreach ($slot in $slotList) {
        $mark = if ($slot.satisfied) { "[x]" } else { "[ ]" }
        Write-Output "$mark $($slot.title) [$($slot.milestones -join ', ')]"
        Write-Output "    Required: $($slot.requiredEvidence)"
        if ($slot.satisfied) {
            Write-Output "    Evidence: $($slot.bundle)"
        } else {
            Write-Output "    Status: $($slot.reason)"
        }
    }
    Write-Output ""
    Write-Output "Ready for preset docs update: $($result.readyForPresetDocs)"
    Write-Output "All closeout evidence present: $($result.allCloseoutEvidencePresent)"
}

if ($FailOnMissing -and $missing.Count -gt 0) {
    exit 2
}

exit 0
