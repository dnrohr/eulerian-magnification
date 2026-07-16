param(
    [string]$EvidenceRoot = "sample-videos\exports\live-validation",
    [string]$OutputPath = "",
    [switch]$Json,
    [switch]$FailOnMissing,
    [switch]$FailOnUnmatched,
    [switch]$FailOnAmbiguous,
    [switch]$FailOnDuplicate,
    [switch]$FailOnNonMain,
    [switch]$FailOnUnpushedSource,
    [switch]$FailOnMissingArtifactHashes,
    [switch]$FailOnNonFinalLabel,
    [switch]$FailOnWrongSlotLabel,
    [switch]$FailOnMissingOperatorNotes,
    [switch]$FailOnMissingVisualReviewText,
    [string]$ExpectedDeviceSerial = "47091JEKB05516",
    [switch]$FailOnWrongDeviceSerial,
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
        deviceSerial = $Summary.deviceSerial
        sourceBranch = Get-SourceValue -Summary $Summary -Name "branch"
        sourceCommit = Get-SourceValue -Summary $Summary -Name "commit"
        sourceShortCommit = Get-SourceValue -Summary $Summary -Name "shortCommit"
        screenshotSha256 = Get-ArtifactSha256 -Summary $Summary -Name "screenshot"
        screenrecordSha256 = Get-ArtifactSha256 -Summary $Summary -Name "screenrecord"
        reviewContactSheetSha256 = Get-ArtifactSha256 -Summary $Summary -Name "reviewContactSheet"
        targetDescription = $Summary.visualReview.targetDescription
        visualClaim = $Summary.visualReview.visualClaim
        operatorNotes = $Summary.visualReview.operatorNotes
        mode = $Summary.launch.mode
        roiSource = $Summary.launch.roiSource
    }
}

function New-CloseoutBlocker {
    param(
        [string]$Kind,
        [int]$Count,
        [string]$Message,
        $Items
    )

    return [pscustomobject][ordered]@{
        kind = $Kind
        count = $Count
        message = $Message
        items = @($Items)
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

function Test-SlotLabel {
    param(
        $Summary,
        $Slot
    )

    if ([string]::IsNullOrWhiteSpace($Summary.label)) {
        return $false
    }
    if (-not $Slot -or [string]::IsNullOrWhiteSpace($Slot.expectedFinalLabel)) {
        return $false
    }

    $label = ([string]$Summary.label).ToLowerInvariant()
    return $label -eq ([string]$Slot.expectedFinalLabel).ToLowerInvariant()
}

function Test-OperatorNotesPresent {
    param($Summary)

    return $Summary.visualReview -and -not [string]::IsNullOrWhiteSpace($Summary.visualReview.operatorNotes)
}

function Test-VisualReviewTextPresent {
    param($Summary)

    return $Summary.visualReview -and
        -not [string]::IsNullOrWhiteSpace($Summary.visualReview.targetDescription) -and
        -not [string]::IsNullOrWhiteSpace($Summary.visualReview.visualClaim)
}

function Get-ExpectedLabelsForSlots {
    param(
        [string[]]$SlotIds,
        $Slots
    )

    $labels = @()
    foreach ($slotId in @($SlotIds)) {
        if (-not $Slots.Contains($slotId)) {
            continue
        }
        $labels += [pscustomobject]@{
            slot = $slotId
            expectedFinalLabel = $Slots[$slotId].expectedFinalLabel
        }
    }
    return $labels
}

function New-ArtifactNote {
    param($Slot)

    $parts = @(
        "$($Slot.title)",
        "label $($Slot.label)",
        "bundle $($Slot.bundle)"
    )
    if (-not [string]::IsNullOrWhiteSpace($Slot.sourceShortCommit) -or -not [string]::IsNullOrWhiteSpace($Slot.sourceBranch)) {
        $parts += "source $($Slot.sourceShortCommit) on $($Slot.sourceBranch)"
    }
    if (-not [string]::IsNullOrWhiteSpace($Slot.deviceSerial)) {
        $parts += "device $($Slot.deviceSerial)"
    }
    if (-not [string]::IsNullOrWhiteSpace($Slot.screenshotSha256)) {
        $parts += "screenshot SHA-256 $($Slot.screenshotSha256)"
    }
    if (-not [string]::IsNullOrWhiteSpace($Slot.screenrecordSha256)) {
        $parts += "screenrecord SHA-256 $($Slot.screenrecordSha256)"
    }
    if (-not [string]::IsNullOrWhiteSpace($Slot.reviewContactSheetSha256)) {
        $parts += "review contact sheet SHA-256 $($Slot.reviewContactSheetSha256)"
    }

    return ($parts -join "; ")
}

function New-Slot {
    param(
        [string]$Id,
        [string]$Title,
        [string[]]$Milestones,
        [string]$RequiredEvidence,
        [string]$Protocol,
        [string]$ExpectedFinalLabel,
        [string]$NextCommand
    )

    return [ordered]@{
        id = $Id
        title = $Title
        milestones = $Milestones
        requiredEvidence = $RequiredEvidence
        protocol = $Protocol
        expectedFinalLabel = $ExpectedFinalLabel
        nextCommand = $NextCommand
        satisfied = $false
        bundle = $null
        label = $null
        deviceSerial = $null
        sourceBranch = $null
        sourceCommit = $null
        sourceShortCommit = $null
        screenshotSha256 = $null
        screenrecordSha256 = $null
        reviewContactSheetSha256 = $null
        artifactNote = $null
        reason = "missing accepted evidence"
    }
}

$slots = [ordered]@{
    manualRoi = New-Slot -Id "manualRoi" -Title "Manual ROI known-target alignment" -Milestones @("M", "U") -RequiredEvidence "visual_validated final evidence with passing ROI measurement" -Protocol "docs/testing/ROI_DEVICE_VALIDATION.md" -ExpectedFinalLabel "manual-roi-known-target-final" -NextCommand "manual-roi-known-target-setup, then manual-roi-known-target-final"
    autoRoi = New-Slot -Id "autoRoi" -Title "Automatic face/skin ROI alignment" -Milestones @("M", "U") -RequiredEvidence "visual_validated final evidence with passing ROI measurement" -Protocol "docs/testing/ROI_DEVICE_VALIDATION.md" -ExpectedFinalLabel "auto-face-roi-final" -NextCommand "auto-face-roi-setup, then auto-face-roi-final"
    pulseLinear = New-Slot -Id "pulseLinear" -Title "Pulse live linear visual parity" -Milestones @("AE", "AP", "AT") -RequiredEvidence "visual_validated final evidence with renderer diagnostics" -Protocol "docs/experiments/pixel8a_live_linear_validation.md" -ExpectedFinalLabel "live-linear-pulse-final" -NextCommand "live-linear-pulse-setup, then live-linear-pulse-final"
    breathingLinear = New-Slot -Id "breathingLinear" -Title "Breathing live linear visual parity" -Milestones @("AE", "AP", "AT") -RequiredEvidence "visual_validated final evidence with renderer diagnostics" -Protocol "docs/experiments/pixel8a_live_linear_validation.md" -ExpectedFinalLabel "live-linear-breathing-final" -NextCommand "live-linear-breathing-setup, then live-linear-breathing-final"
    objectPhase = New-Slot -Id "objectPhase" -Title "Object vibration live phase visual parity" -Milestones @("AR", "AT") -RequiredEvidence "visual_validated final evidence with phase diagnostics" -Protocol "docs/experiments/pixel8a_live_phase_validation.md" -ExpectedFinalLabel "live-phase-object-final" -NextCommand "live-phase-object-setup, then live-phase-object-final"
    fastTremorPhase = New-Slot -Id "fastTremorPhase" -Title "Fast tremor live phase visual parity" -Milestones @("AR", "AT") -RequiredEvidence "visual_validated final evidence with phase diagnostics" -Protocol "docs/experiments/pixel8a_live_phase_validation.md" -ExpectedFinalLabel "live-phase-fast-tremor-final" -NextCommand "live-phase-fast-tremor-setup, then live-phase-fast-tremor-final"
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
$wrongSlotLabelAcceptedFinalEvidence = @()
$missingOperatorNotesAcceptedFinalEvidence = @()
$missingVisualReviewTextAcceptedFinalEvidence = @()
$wrongDeviceSerialAcceptedFinalEvidence = @()
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
    if (-not (Test-OperatorNotesPresent -Summary $summary)) {
        $report = New-EvidenceReport -Summary $summary
        $report.reason = "accepted final evidence is missing operator notes"
        $missingOperatorNotesAcceptedFinalEvidence += [pscustomobject]$report
    }
    if (-not (Test-VisualReviewTextPresent -Summary $summary)) {
        $report = New-EvidenceReport -Summary $summary
        $report.reason = "accepted final evidence is missing target description or visual claim"
        $missingVisualReviewTextAcceptedFinalEvidence += [pscustomobject]$report
    }
    if (-not [string]::IsNullOrWhiteSpace($ExpectedDeviceSerial) -and $summary.deviceSerial -ne $ExpectedDeviceSerial) {
        $report = New-EvidenceReport -Summary $summary
        $report.expectedDeviceSerial = $ExpectedDeviceSerial
        $report.reason = "accepted final evidence device serial does not match the expected Pixel"
        $wrongDeviceSerialAcceptedFinalEvidence += [pscustomobject]$report
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

    $hadCandidateSlots = $matchedSlots.Count -gt 0
    $slotLabelMismatches = @()
    foreach ($slotId in @($matchedSlots)) {
        if (-not (Test-SlotLabel -Summary $summary -Slot $slots[$slotId])) {
            $slotLabelMismatches += $slotId
        }
    }
    if ($slotLabelMismatches.Count -gt 0) {
        $report = New-EvidenceReport -Summary $summary
        $report.matchedSlots = $matchedSlots
        $report.mismatchedSlots = $slotLabelMismatches
        $report.expectedFinalLabels = Get-ExpectedLabelsForSlots -SlotIds $slotLabelMismatches -Slots $slots
        $report.reason = "accepted final evidence label does not match the closeout slot"
        $wrongSlotLabelAcceptedFinalEvidence += [pscustomobject]$report
        $matchedSlots = @()
    }

    if ($matchedSlots.Count -eq 0 -and -not $hadCandidateSlots) {
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
            $slots[$slotId].deviceSerial = $summary.deviceSerial
            $slots[$slotId].sourceBranch = Get-SourceValue -Summary $summary -Name "branch"
            $slots[$slotId].sourceCommit = Get-SourceValue -Summary $summary -Name "commit"
            $slots[$slotId].sourceShortCommit = Get-SourceValue -Summary $summary -Name "shortCommit"
            $slots[$slotId].screenshotSha256 = Get-ArtifactSha256 -Summary $summary -Name "screenshot"
            $slots[$slotId].screenrecordSha256 = Get-ArtifactSha256 -Summary $summary -Name "screenrecord"
            $slots[$slotId].reviewContactSheetSha256 = Get-ArtifactSha256 -Summary $summary -Name "reviewContactSheet"
            $slots[$slotId].reason = "accepted final evidence"
            $slots[$slotId].artifactNote = New-ArtifactNote -Slot $slots[$slotId]
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
            $report.originalReviewContactSheetSha256 = $slots[$slotId].reviewContactSheetSha256
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
    $nonFinalLabelAcceptedFinalEvidence.Count -eq 0 -and
    $wrongSlotLabelAcceptedFinalEvidence.Count -eq 0 -and
    $missingOperatorNotesAcceptedFinalEvidence.Count -eq 0 -and
    $missingVisualReviewTextAcceptedFinalEvidence.Count -eq 0 -and
    $wrongDeviceSerialAcceptedFinalEvidence.Count -eq 0
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
    $nonFinalLabelAcceptedFinalEvidence.Count -eq 0 -and
    $wrongSlotLabelAcceptedFinalEvidence.Count -eq 0 -and
    $missingOperatorNotesAcceptedFinalEvidence.Count -eq 0 -and
    $missingVisualReviewTextAcceptedFinalEvidence.Count -eq 0 -and
    $wrongDeviceSerialAcceptedFinalEvidence.Count -eq 0
)
$closeoutBlockers = @()
if ($missing.Count -gt 0) {
    $missingItems = @($missing | ForEach-Object {
        [pscustomobject][ordered]@{
            id = $_.id
            title = $_.title
            milestones = $_.milestones
            requiredEvidence = $_.requiredEvidence
            protocol = $_.protocol
            expectedFinalLabel = $_.expectedFinalLabel
            nextCommand = $_.nextCommand
            reason = $_.reason
        }
    })
    $closeoutBlockers += New-CloseoutBlocker -Kind "missingSlots" -Count $missing.Count -Message "$($missing.Count) closeout slot(s) are missing accepted final evidence." -Items $missingItems
}
if ($unmatchedAcceptedFinalEvidence.Count -gt 0) {
    $closeoutBlockers += New-CloseoutBlocker -Kind "unmatchedAcceptedFinalEvidence" -Count $unmatchedAcceptedFinalEvidence.Count -Message "$($unmatchedAcceptedFinalEvidence.Count) accepted final evidence bundle(s) do not match any closeout slot." -Items $unmatchedAcceptedFinalEvidence
}
if ($ambiguousAcceptedFinalEvidence.Count -gt 0) {
    $closeoutBlockers += New-CloseoutBlocker -Kind "ambiguousAcceptedFinalEvidence" -Count $ambiguousAcceptedFinalEvidence.Count -Message "$($ambiguousAcceptedFinalEvidence.Count) accepted final evidence bundle(s) match multiple closeout slots." -Items $ambiguousAcceptedFinalEvidence
}
if ($duplicateAcceptedFinalEvidence.Count -gt 0) {
    $closeoutBlockers += New-CloseoutBlocker -Kind "duplicateAcceptedFinalEvidence" -Count $duplicateAcceptedFinalEvidence.Count -Message "$($duplicateAcceptedFinalEvidence.Count) accepted final evidence bundle(s) duplicate an already satisfied closeout slot." -Items $duplicateAcceptedFinalEvidence
}
if ($nonMainAcceptedFinalEvidence.Count -gt 0) {
    $closeoutBlockers += New-CloseoutBlocker -Kind "nonMainAcceptedFinalEvidence" -Count $nonMainAcceptedFinalEvidence.Count -Message "$($nonMainAcceptedFinalEvidence.Count) accepted final evidence bundle(s) were not captured from main." -Items $nonMainAcceptedFinalEvidence
}
if ($unpushedAcceptedFinalEvidence.Count -gt 0) {
    $closeoutBlockers += New-CloseoutBlocker -Kind "unpushedAcceptedFinalEvidence" -Count $unpushedAcceptedFinalEvidence.Count -Message "$($unpushedAcceptedFinalEvidence.Count) accepted final evidence bundle(s) use a source commit that is not reachable from origin/main." -Items $unpushedAcceptedFinalEvidence
}
if ($missingArtifactHashAcceptedFinalEvidence.Count -gt 0) {
    $closeoutBlockers += New-CloseoutBlocker -Kind "missingArtifactHashAcceptedFinalEvidence" -Count $missingArtifactHashAcceptedFinalEvidence.Count -Message "$($missingArtifactHashAcceptedFinalEvidence.Count) accepted final evidence bundle(s) lack screenshot or screenrecord SHA-256 values." -Items $missingArtifactHashAcceptedFinalEvidence
}
if ($nonFinalLabelAcceptedFinalEvidence.Count -gt 0) {
    $closeoutBlockers += New-CloseoutBlocker -Kind "nonFinalLabelAcceptedFinalEvidence" -Count $nonFinalLabelAcceptedFinalEvidence.Count -Message "$($nonFinalLabelAcceptedFinalEvidence.Count) accepted final evidence bundle(s) do not use a final capture label." -Items $nonFinalLabelAcceptedFinalEvidence
}
if ($wrongSlotLabelAcceptedFinalEvidence.Count -gt 0) {
    $closeoutBlockers += New-CloseoutBlocker -Kind "wrongSlotLabelAcceptedFinalEvidence" -Count $wrongSlotLabelAcceptedFinalEvidence.Count -Message "$($wrongSlotLabelAcceptedFinalEvidence.Count) accepted final evidence bundle(s) use a final label for a different closeout slot." -Items $wrongSlotLabelAcceptedFinalEvidence
}
if ($missingOperatorNotesAcceptedFinalEvidence.Count -gt 0) {
    $closeoutBlockers += New-CloseoutBlocker -Kind "missingOperatorNotesAcceptedFinalEvidence" -Count $missingOperatorNotesAcceptedFinalEvidence.Count -Message "$($missingOperatorNotesAcceptedFinalEvidence.Count) accepted final evidence bundle(s) lack operator notes." -Items $missingOperatorNotesAcceptedFinalEvidence
}
if ($missingVisualReviewTextAcceptedFinalEvidence.Count -gt 0) {
    $closeoutBlockers += New-CloseoutBlocker -Kind "missingVisualReviewTextAcceptedFinalEvidence" -Count $missingVisualReviewTextAcceptedFinalEvidence.Count -Message "$($missingVisualReviewTextAcceptedFinalEvidence.Count) accepted final evidence bundle(s) lack target description or visual claim text." -Items $missingVisualReviewTextAcceptedFinalEvidence
}
if ($wrongDeviceSerialAcceptedFinalEvidence.Count -gt 0) {
    $closeoutBlockers += New-CloseoutBlocker -Kind "wrongDeviceSerialAcceptedFinalEvidence" -Count $wrongDeviceSerialAcceptedFinalEvidence.Count -Message "$($wrongDeviceSerialAcceptedFinalEvidence.Count) accepted final evidence bundle(s) were not captured from expected device serial $ExpectedDeviceSerial." -Items $wrongDeviceSerialAcceptedFinalEvidence
}
$result = [pscustomobject]@{
    evidenceRoot = if ($rootPath) { $rootPath.Path } else { $EvidenceRoot }
    expectedDeviceSerial = $ExpectedDeviceSerial
    summaryCount = $summaryFiles.Count
    acceptedFinalEvidenceCount = $acceptedFinalSummaries.Count
    unmatchedAcceptedFinalEvidence = $unmatchedAcceptedFinalEvidence
    ambiguousAcceptedFinalEvidence = $ambiguousAcceptedFinalEvidence
    duplicateAcceptedFinalEvidence = $duplicateAcceptedFinalEvidence
    nonMainAcceptedFinalEvidence = $nonMainAcceptedFinalEvidence
    unpushedAcceptedFinalEvidence = $unpushedAcceptedFinalEvidence
    missingArtifactHashAcceptedFinalEvidence = $missingArtifactHashAcceptedFinalEvidence
    nonFinalLabelAcceptedFinalEvidence = $nonFinalLabelAcceptedFinalEvidence
    wrongSlotLabelAcceptedFinalEvidence = $wrongSlotLabelAcceptedFinalEvidence
    missingOperatorNotesAcceptedFinalEvidence = $missingOperatorNotesAcceptedFinalEvidence
    missingVisualReviewTextAcceptedFinalEvidence = $missingVisualReviewTextAcceptedFinalEvidence
    wrongDeviceSerialAcceptedFinalEvidence = $wrongDeviceSerialAcceptedFinalEvidence
    slots = $slotList
    missing = $missing
    presetVisualSlotsPresent = $presetVisualSlotsPresent
    presetDocsEvidenceClean = $presetDocsEvidenceClean
    readyForPresetDocs = $presetDocsEvidenceClean
    allCloseoutEvidencePresent = $allCloseoutEvidencePresent
    allCloseoutEvidenceClean = $allCloseoutEvidenceClean
    closeoutBlockers = $closeoutBlockers
}

$resultJson = $result | ConvertTo-Json -Depth 8
if (-not [string]::IsNullOrWhiteSpace($OutputPath)) {
    $outputDirectory = Split-Path -Parent $OutputPath
    if (-not [string]::IsNullOrWhiteSpace($outputDirectory)) {
        New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
    }
    $resultJson | Out-File -LiteralPath $OutputPath -Encoding utf8
}

if ($Json) {
    $resultJson
} else {
    Write-Output "Pixel validation closeout"
    if (-not [string]::IsNullOrWhiteSpace($OutputPath)) {
        Write-Output "Summary output: $OutputPath"
    }
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
    Write-Output "Wrong-slot-label accepted final evidence: $(@($result.wrongSlotLabelAcceptedFinalEvidence).Count)"
    Write-Output "Missing-operator-notes accepted final evidence: $(@($result.missingOperatorNotesAcceptedFinalEvidence).Count)"
    Write-Output "Missing-visual-review-text accepted final evidence: $(@($result.missingVisualReviewTextAcceptedFinalEvidence).Count)"
    Write-Output "Wrong-device-serial accepted final evidence: $(@($result.wrongDeviceSerialAcceptedFinalEvidence).Count)"
    Write-Output ""
    foreach ($slot in $slotList) {
        $mark = if ($slot.satisfied) { "[x]" } else { "[ ]" }
        Write-Output "$mark $($slot.title) [$($slot.milestones -join ', ')]"
        Write-Output "    Required: $($slot.requiredEvidence)"
        Write-Output "    Protocol: $($slot.protocol)"
        Write-Output "    Expected final label: $($slot.expectedFinalLabel)"
        if ($slot.satisfied) {
            Write-Output "    Evidence: $($slot.bundle)"
            if (-not [string]::IsNullOrWhiteSpace($slot.sourceShortCommit) -or -not [string]::IsNullOrWhiteSpace($slot.sourceBranch)) {
                Write-Output "    Source: $($slot.sourceShortCommit) on $($slot.sourceBranch)"
            }
            if (-not [string]::IsNullOrWhiteSpace($slot.deviceSerial)) {
                Write-Output "    Device serial: $($slot.deviceSerial)"
            }
            if (-not [string]::IsNullOrWhiteSpace($slot.screenshotSha256)) {
                Write-Output "    Screenshot SHA-256: $($slot.screenshotSha256)"
            }
            if (-not [string]::IsNullOrWhiteSpace($slot.screenrecordSha256)) {
                Write-Output "    Screenrecord SHA-256: $($slot.screenrecordSha256)"
            }
            if (-not [string]::IsNullOrWhiteSpace($slot.reviewContactSheetSha256)) {
                Write-Output "    Review contact sheet SHA-256: $($slot.reviewContactSheetSha256)"
            }
            if (-not [string]::IsNullOrWhiteSpace($slot.artifactNote)) {
                Write-Output "    Artifact note: $($slot.artifactNote)"
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
    Write-Output "Closeout blockers: $(@($result.closeoutBlockers).Count)"
    foreach ($blocker in @($result.closeoutBlockers)) {
        Write-Output "- $($blocker.kind): $($blocker.message)"
    }
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
    if (@($result.wrongSlotLabelAcceptedFinalEvidence).Count -gt 0) {
        Write-Output ""
        Write-Output "Wrong-slot-label accepted final evidence:"
        foreach ($evidence in @($result.wrongSlotLabelAcceptedFinalEvidence)) {
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
            Write-Output "    $($evidence.reason): $($evidence.mismatchedSlots -join ', ')"
            foreach ($expected in @($evidence.expectedFinalLabels)) {
                Write-Output "    Expected $($expected.slot): $($expected.expectedFinalLabel)"
            }
        }
    }
    if (@($result.missingOperatorNotesAcceptedFinalEvidence).Count -gt 0) {
        Write-Output ""
        Write-Output "Missing-operator-notes accepted final evidence:"
        foreach ($evidence in @($result.missingOperatorNotesAcceptedFinalEvidence)) {
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
    if (@($result.missingVisualReviewTextAcceptedFinalEvidence).Count -gt 0) {
        Write-Output ""
        Write-Output "Missing-visual-review-text accepted final evidence:"
        foreach ($evidence in @($result.missingVisualReviewTextAcceptedFinalEvidence)) {
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
if ($FailOnWrongSlotLabel -and $wrongSlotLabelAcceptedFinalEvidence.Count -gt 0) {
    exit 18
}
if ($FailOnMissingOperatorNotes -and $missingOperatorNotesAcceptedFinalEvidence.Count -gt 0) {
    exit 19
}
if ($FailOnMissingVisualReviewText -and $missingVisualReviewTextAcceptedFinalEvidence.Count -gt 0) {
    exit 20
}
if ($FailOnWrongDeviceSerial -and $wrongDeviceSerialAcceptedFinalEvidence.Count -gt 0) {
    exit 21
}
if ($FailOnCloseoutNotReady -and -not $result.allCloseoutEvidenceClean) {
    exit 7
}
if ($FailOnPresetDocsNotReady -and -not $result.presetDocsEvidenceClean) {
    exit 4
}

exit 0
