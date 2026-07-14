param(
    [string]$EvidenceRoot = "sample-videos\exports\live-validation",
    [switch]$Json,
    [switch]$FailOnMissing,
    [switch]$FailOnUnmatched,
    [switch]$FailOnAmbiguous,
    [switch]$FailOnDuplicate,
    [switch]$FailOnNonMain,
    [switch]$FailOnUnpushedSource,
    [switch]$FailOnMissingArtifactHashes,
    [switch]$FailOnNonFinalLabel,
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

function Get-SourceValue {
    param(
        $Summary,
        [string]$Name
    )

    if ($Summary.source -and ($Summary.source.PSObject.Properties.Name -contains $Name)) {
        return $Summary.source.$Name
    }
    return $null
}

function Get-ArtifactSha256 {
    param(
        $Summary,
        [string]$Name
    )

    if ($Summary.artifacts -and
        ($Summary.artifacts.PSObject.Properties.Name -contains $Name) -and
        $Summary.artifacts.$Name -and
        ($Summary.artifacts.$Name.PSObject.Properties.Name -contains "sha256")) {
        return $Summary.artifacts.$Name.sha256
    }
    return $null
}

function New-EvidenceReport {
    param(
        $Summary
    )

    return [ordered]@{
        bundle = Split-Path -Parent $Summary.summaryPath
        label = $Summary.label
        sourceBranch = Get-SourceValue -Summary $Summary -Name "branch"
        sourceCommit = Get-SourceValue -Summary $Summary -Name "commit"
        sourceShortCommit = Get-SourceValue -Summary $Summary -Name "shortCommit"
        screenshotSha256 = Get-ArtifactSha256 -Summary $Summary -Name "screenshot"
        screenrecordSha256 = Get-ArtifactSha256 -Summary $Summary -Name "screenrecord"
        mode = $Summary.launch.mode
        roiSource = $Summary.launch.roiSource
        visualClaim = $Summary.visualReview.visualClaim
    }
}

function Test-SourceBranchReady {
    param($Summary)

    $branch = Get-SourceValue -Summary $Summary -Name "branch"
    return -not [string]::IsNullOrWhiteSpace($branch) -and $branch -eq "main"
}

function Test-SourceCommitOnOriginMain {
    param($Summary)

    $commit = Get-SourceValue -Summary $Summary -Name "commit"
    if ([string]::IsNullOrWhiteSpace($commit)) {
        return $false
    }

    $repoRoot = Split-Path -Parent $PSScriptRoot
    $oldErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        & git -C $repoRoot cat-file -e "$commit^{commit}" *> $null
        if ($LASTEXITCODE -ne 0) {
            return $false
        }

        & git -C $repoRoot merge-base --is-ancestor $commit origin/main *> $null
        return $LASTEXITCODE -eq 0
    } finally {
        $ErrorActionPreference = $oldErrorActionPreference
    }
}

function Test-ArtifactHashesPresent {
    param($Summary)

    return -not [string]::IsNullOrWhiteSpace((Get-ArtifactSha256 -Summary $Summary -Name "screenshot")) -and
        -not [string]::IsNullOrWhiteSpace((Get-ArtifactSha256 -Summary $Summary -Name "screenrecord"))
}

function Test-FinalLabel {
    param($Summary)

    if ([string]::IsNullOrWhiteSpace($Summary.label)) {
        return $false
    }

    return ([string]$Summary.label).ToLowerInvariant() -match '(^|-)final($|-)'
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
        sourceBranch = $null
        sourceCommit = $null
        sourceShortCommit = $null
        screenshotSha256 = $null
        screenrecordSha256 = $null
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
$nonMainAcceptedFinalEvidence = @()
$unpushedAcceptedFinalEvidence = @()
$missingArtifactHashAcceptedFinalEvidence = @()
$nonFinalLabelAcceptedFinalEvidence = @()
foreach ($summary in $acceptedSummaries) {
    if (-not (Test-FinalVisualEvidence -Summary $summary)) {
        continue
    }

    $acceptedFinalSummaries += $summary
    if (-not (Test-SourceBranchReady -Summary $summary)) {
        $report = New-EvidenceReport -Summary $summary
        $report.reason = "accepted final evidence was not captured from main"
        $nonMainAcceptedFinalEvidence += [pscustomobject]$report
    }
    if (-not (Test-SourceCommitOnOriginMain -Summary $summary)) {
        $report = New-EvidenceReport -Summary $summary
        $report.reason = "accepted final evidence source commit is not reachable from origin/main"
        $unpushedAcceptedFinalEvidence += [pscustomobject]$report
    }
    if (-not (Test-ArtifactHashesPresent -Summary $summary)) {
        $report = New-EvidenceReport -Summary $summary
        $report.reason = "accepted final evidence is missing screenshot or screenrecord SHA-256"
        $missingArtifactHashAcceptedFinalEvidence += [pscustomobject]$report
    }
    if (-not (Test-FinalLabel -Summary $summary)) {
        $report = New-EvidenceReport -Summary $summary
        $report.reason = "accepted final evidence label is not a final capture label"
        $nonFinalLabelAcceptedFinalEvidence += [pscustomobject]$report
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

    if ($matchedSlots.Count -eq 0) {
        $report = New-EvidenceReport -Summary $summary
        $report.reason = "accepted final evidence did not match any closeout slot"
        $unmatchedAcceptedFinalEvidence += [pscustomobject]$report
    }
    if ($matchedSlots.Count -gt 1) {
        $report = New-EvidenceReport -Summary $summary
        $report.matchedSlots = $matchedSlots
        $report.reason = "accepted final evidence matched multiple closeout slots"
        $ambiguousAcceptedFinalEvidence += [pscustomobject]$report
    }

    foreach ($slotId in $matchedSlots) {
        if (-not $slots[$slotId].satisfied) {
            $slots[$slotId].satisfied = $true
            $slots[$slotId].bundle = Split-Path -Parent $summary.summaryPath
            $slots[$slotId].label = $summary.label
            $slots[$slotId].sourceBranch = Get-SourceValue -Summary $summary -Name "branch"
            $slots[$slotId].sourceCommit = Get-SourceValue -Summary $summary -Name "commit"
            $slots[$slotId].sourceShortCommit = Get-SourceValue -Summary $summary -Name "shortCommit"
            $slots[$slotId].screenshotSha256 = Get-ArtifactSha256 -Summary $summary -Name "screenshot"
            $slots[$slotId].screenrecordSha256 = Get-ArtifactSha256 -Summary $summary -Name "screenrecord"
            $slots[$slotId].reason = "accepted final evidence"
        } else {
            $report = New-EvidenceReport -Summary $summary
            $report.slot = $slotId
            $report.originalBundle = $slots[$slotId].bundle
            $report.originalLabel = $slots[$slotId].label
            $report.originalSourceBranch = $slots[$slotId].sourceBranch
            $report.originalSourceCommit = $slots[$slotId].sourceCommit
            $report.originalSourceShortCommit = $slots[$slotId].sourceShortCommit
            $report.originalScreenshotSha256 = $slots[$slotId].screenshotSha256
            $report.originalScreenrecordSha256 = $slots[$slotId].screenrecordSha256
            $report.reason = "accepted final evidence matched an already satisfied closeout slot"
            $duplicateAcceptedFinalEvidence += [pscustomobject]$report
        }
    }
}

$slotList = @($slots.Values | ForEach-Object { [pscustomobject]$_ })
$missing = @($slotList | Where-Object { -not $_.satisfied })
$presetVisualSlotsPresent = (@($slotList | Where-Object { $_.id -in @("pulseLinear", "breathingLinear", "objectPhase", "fastTremorPhase") -and $_.satisfied }).Count -eq 4)
$presetDocsEvidenceClean = (
    $presetVisualSlotsPresent -and
    $unmatchedAcceptedFinalEvidence.Count -eq 0 -and
    $ambiguousAcceptedFinalEvidence.Count -eq 0 -and
    $duplicateAcceptedFinalEvidence.Count -eq 0 -and
    $nonMainAcceptedFinalEvidence.Count -eq 0 -and
    $unpushedAcceptedFinalEvidence.Count -eq 0 -and
    $missingArtifactHashAcceptedFinalEvidence.Count -eq 0 -and
    $nonFinalLabelAcceptedFinalEvidence.Count -eq 0
)
$allCloseoutEvidencePresent = ($missing.Count -eq 0)
$allCloseoutEvidenceClean = (
    $allCloseoutEvidencePresent -and
    $unmatchedAcceptedFinalEvidence.Count -eq 0 -and
    $ambiguousAcceptedFinalEvidence.Count -eq 0 -and
    $duplicateAcceptedFinalEvidence.Count -eq 0 -and
    $nonMainAcceptedFinalEvidence.Count -eq 0 -and
    $unpushedAcceptedFinalEvidence.Count -eq 0 -and
    $missingArtifactHashAcceptedFinalEvidence.Count -eq 0 -and
    $nonFinalLabelAcceptedFinalEvidence.Count -eq 0
)
$result = [pscustomobject]@{
    evidenceRoot = if ($rootPath) { $rootPath.Path } else { $EvidenceRoot }
    summaryCount = $summaryFiles.Count
    acceptedFinalEvidenceCount = $acceptedFinalSummaries.Count
    unmatchedAcceptedFinalEvidence = $unmatchedAcceptedFinalEvidence
    ambiguousAcceptedFinalEvidence = $ambiguousAcceptedFinalEvidence
    duplicateAcceptedFinalEvidence = $duplicateAcceptedFinalEvidence
    nonMainAcceptedFinalEvidence = $nonMainAcceptedFinalEvidence
    unpushedAcceptedFinalEvidence = $unpushedAcceptedFinalEvidence
    missingArtifactHashAcceptedFinalEvidence = $missingArtifactHashAcceptedFinalEvidence
    nonFinalLabelAcceptedFinalEvidence = $nonFinalLabelAcceptedFinalEvidence
    slots = $slotList
    missing = $missing
    presetVisualSlotsPresent = $presetVisualSlotsPresent
    presetDocsEvidenceClean = $presetDocsEvidenceClean
    readyForPresetDocs = $presetDocsEvidenceClean
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
    Write-Output "Non-main accepted final evidence: $(@($result.nonMainAcceptedFinalEvidence).Count)"
    Write-Output "Unpushed accepted final evidence: $(@($result.unpushedAcceptedFinalEvidence).Count)"
    Write-Output "Missing artifact-hash accepted final evidence: $(@($result.missingArtifactHashAcceptedFinalEvidence).Count)"
    Write-Output "Non-final-label accepted final evidence: $(@($result.nonFinalLabelAcceptedFinalEvidence).Count)"
    Write-Output ""
    foreach ($slot in $slotList) {
        $mark = if ($slot.satisfied) { "[x]" } else { "[ ]" }
        Write-Output "$mark $($slot.title) [$($slot.milestones -join ', ')]"
        Write-Output "    Required: $($slot.requiredEvidence)"
        Write-Output "    Protocol: $($slot.protocol)"
        if ($slot.satisfied) {
            Write-Output "    Evidence: $($slot.bundle)"
            if (-not [string]::IsNullOrWhiteSpace($slot.sourceShortCommit) -or -not [string]::IsNullOrWhiteSpace($slot.sourceBranch)) {
                Write-Output "    Source: $($slot.sourceShortCommit) on $($slot.sourceBranch)"
            }
            if (-not [string]::IsNullOrWhiteSpace($slot.screenshotSha256)) {
                Write-Output "    Screenshot SHA-256: $($slot.screenshotSha256)"
            }
            if (-not [string]::IsNullOrWhiteSpace($slot.screenrecordSha256)) {
                Write-Output "    Screenrecord SHA-256: $($slot.screenrecordSha256)"
            }
        } else {
            Write-Output "    Status: $($slot.reason)"
            Write-Output "    Next: $($slot.nextCommand)"
        }
    }
    Write-Output ""
    Write-Output "Preset visual slots present: $($result.presetVisualSlotsPresent)"
    Write-Output "Preset docs evidence clean: $($result.presetDocsEvidenceClean)"
    Write-Output "Ready for preset docs update: $($result.readyForPresetDocs)"
    Write-Output "All closeout evidence present: $($result.allCloseoutEvidencePresent)"
    Write-Output "All closeout evidence clean: $($result.allCloseoutEvidenceClean)"
    if (@($result.unmatchedAcceptedFinalEvidence).Count -gt 0) {
        Write-Output ""
        Write-Output "Unmatched accepted final evidence:"
        foreach ($evidence in @($result.unmatchedAcceptedFinalEvidence)) {
            Write-Output "- $($evidence.label): $($evidence.bundle)"
            if (-not [string]::IsNullOrWhiteSpace($evidence.sourceShortCommit) -or -not [string]::IsNullOrWhiteSpace($evidence.sourceBranch)) {
                Write-Output "    Source: $($evidence.sourceShortCommit) on $($evidence.sourceBranch)"
            }
            if (-not [string]::IsNullOrWhiteSpace($evidence.screenshotSha256)) {
                Write-Output "    Screenshot SHA-256: $($evidence.screenshotSha256)"
            }
            if (-not [string]::IsNullOrWhiteSpace($evidence.screenrecordSha256)) {
                Write-Output "    Screenrecord SHA-256: $($evidence.screenrecordSha256)"
            }
            Write-Output "    $($evidence.reason)"
        }
    }
    if (@($result.ambiguousAcceptedFinalEvidence).Count -gt 0) {
        Write-Output ""
        Write-Output "Ambiguous accepted final evidence:"
        foreach ($evidence in @($result.ambiguousAcceptedFinalEvidence)) {
            Write-Output "- $($evidence.label): $($evidence.bundle)"
            if (-not [string]::IsNullOrWhiteSpace($evidence.sourceShortCommit) -or -not [string]::IsNullOrWhiteSpace($evidence.sourceBranch)) {
                Write-Output "    Source: $($evidence.sourceShortCommit) on $($evidence.sourceBranch)"
            }
            if (-not [string]::IsNullOrWhiteSpace($evidence.screenshotSha256)) {
                Write-Output "    Screenshot SHA-256: $($evidence.screenshotSha256)"
            }
            if (-not [string]::IsNullOrWhiteSpace($evidence.screenrecordSha256)) {
                Write-Output "    Screenrecord SHA-256: $($evidence.screenrecordSha256)"
            }
            Write-Output "    $($evidence.reason): $($evidence.matchedSlots -join ', ')"
        }
    }
    if (@($result.duplicateAcceptedFinalEvidence).Count -gt 0) {
        Write-Output ""
        Write-Output "Duplicate accepted final evidence:"
        foreach ($evidence in @($result.duplicateAcceptedFinalEvidence)) {
            Write-Output "- $($evidence.label): $($evidence.bundle)"
            if (-not [string]::IsNullOrWhiteSpace($evidence.sourceShortCommit) -or -not [string]::IsNullOrWhiteSpace($evidence.sourceBranch)) {
                Write-Output "    Source: $($evidence.sourceShortCommit) on $($evidence.sourceBranch)"
            }
            if (-not [string]::IsNullOrWhiteSpace($evidence.screenshotSha256)) {
                Write-Output "    Screenshot SHA-256: $($evidence.screenshotSha256)"
            }
            if (-not [string]::IsNullOrWhiteSpace($evidence.screenrecordSha256)) {
                Write-Output "    Screenrecord SHA-256: $($evidence.screenrecordSha256)"
            }
            Write-Output "    $($evidence.reason): $($evidence.slot)"
            Write-Output "    Original: $($evidence.originalLabel): $($evidence.originalBundle)"
            if (-not [string]::IsNullOrWhiteSpace($evidence.originalSourceShortCommit) -or -not [string]::IsNullOrWhiteSpace($evidence.originalSourceBranch)) {
                Write-Output "    Original source: $($evidence.originalSourceShortCommit) on $($evidence.originalSourceBranch)"
            }
            if (-not [string]::IsNullOrWhiteSpace($evidence.originalScreenshotSha256)) {
                Write-Output "    Original screenshot SHA-256: $($evidence.originalScreenshotSha256)"
            }
            if (-not [string]::IsNullOrWhiteSpace($evidence.originalScreenrecordSha256)) {
                Write-Output "    Original screenrecord SHA-256: $($evidence.originalScreenrecordSha256)"
            }
        }
    }
    if (@($result.nonMainAcceptedFinalEvidence).Count -gt 0) {
        Write-Output ""
        Write-Output "Non-main accepted final evidence:"
        foreach ($evidence in @($result.nonMainAcceptedFinalEvidence)) {
            Write-Output "- $($evidence.label): $($evidence.bundle)"
            if (-not [string]::IsNullOrWhiteSpace($evidence.sourceShortCommit) -or -not [string]::IsNullOrWhiteSpace($evidence.sourceBranch)) {
                Write-Output "    Source: $($evidence.sourceShortCommit) on $($evidence.sourceBranch)"
            }
            if (-not [string]::IsNullOrWhiteSpace($evidence.screenshotSha256)) {
                Write-Output "    Screenshot SHA-256: $($evidence.screenshotSha256)"
            }
            if (-not [string]::IsNullOrWhiteSpace($evidence.screenrecordSha256)) {
                Write-Output "    Screenrecord SHA-256: $($evidence.screenrecordSha256)"
            }
            Write-Output "    $($evidence.reason)"
        }
    }
    if (@($result.unpushedAcceptedFinalEvidence).Count -gt 0) {
        Write-Output ""
        Write-Output "Unpushed accepted final evidence:"
        foreach ($evidence in @($result.unpushedAcceptedFinalEvidence)) {
            Write-Output "- $($evidence.label): $($evidence.bundle)"
            if (-not [string]::IsNullOrWhiteSpace($evidence.sourceShortCommit) -or -not [string]::IsNullOrWhiteSpace($evidence.sourceBranch)) {
                Write-Output "    Source: $($evidence.sourceShortCommit) on $($evidence.sourceBranch)"
            }
            if (-not [string]::IsNullOrWhiteSpace($evidence.screenshotSha256)) {
                Write-Output "    Screenshot SHA-256: $($evidence.screenshotSha256)"
            }
            if (-not [string]::IsNullOrWhiteSpace($evidence.screenrecordSha256)) {
                Write-Output "    Screenrecord SHA-256: $($evidence.screenrecordSha256)"
            }
            Write-Output "    $($evidence.reason)"
        }
    }
    if (@($result.missingArtifactHashAcceptedFinalEvidence).Count -gt 0) {
        Write-Output ""
        Write-Output "Missing artifact-hash accepted final evidence:"
        foreach ($evidence in @($result.missingArtifactHashAcceptedFinalEvidence)) {
            Write-Output "- $($evidence.label): $($evidence.bundle)"
            if (-not [string]::IsNullOrWhiteSpace($evidence.sourceShortCommit) -or -not [string]::IsNullOrWhiteSpace($evidence.sourceBranch)) {
                Write-Output "    Source: $($evidence.sourceShortCommit) on $($evidence.sourceBranch)"
            }
            if (-not [string]::IsNullOrWhiteSpace($evidence.screenshotSha256)) {
                Write-Output "    Screenshot SHA-256: $($evidence.screenshotSha256)"
            }
            if (-not [string]::IsNullOrWhiteSpace($evidence.screenrecordSha256)) {
                Write-Output "    Screenrecord SHA-256: $($evidence.screenrecordSha256)"
            }
            Write-Output "    $($evidence.reason)"
        }
    }
    if (@($result.nonFinalLabelAcceptedFinalEvidence).Count -gt 0) {
        Write-Output ""
        Write-Output "Non-final-label accepted final evidence:"
        foreach ($evidence in @($result.nonFinalLabelAcceptedFinalEvidence)) {
            Write-Output "- $($evidence.label): $($evidence.bundle)"
            if (-not [string]::IsNullOrWhiteSpace($evidence.sourceShortCommit) -or -not [string]::IsNullOrWhiteSpace($evidence.sourceBranch)) {
                Write-Output "    Source: $($evidence.sourceShortCommit) on $($evidence.sourceBranch)"
            }
            if (-not [string]::IsNullOrWhiteSpace($evidence.screenshotSha256)) {
                Write-Output "    Screenshot SHA-256: $($evidence.screenshotSha256)"
            }
            if (-not [string]::IsNullOrWhiteSpace($evidence.screenrecordSha256)) {
                Write-Output "    Screenrecord SHA-256: $($evidence.screenrecordSha256)"
            }
            Write-Output "    $($evidence.reason)"
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
if ($FailOnNonMain -and $nonMainAcceptedFinalEvidence.Count -gt 0) {
    exit 8
}
if ($FailOnUnpushedSource -and $unpushedAcceptedFinalEvidence.Count -gt 0) {
    exit 15
}
if ($FailOnMissingArtifactHashes -and $missingArtifactHashAcceptedFinalEvidence.Count -gt 0) {
    exit 16
}
if ($FailOnNonFinalLabel -and $nonFinalLabelAcceptedFinalEvidence.Count -gt 0) {
    exit 17
}
if ($FailOnCloseoutNotReady -and -not $result.allCloseoutEvidenceClean) {
    exit 7
}
if ($FailOnPresetDocsNotReady -and -not $result.presetDocsEvidenceClean) {
    exit 4
}

exit 0
