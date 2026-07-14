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

function Invoke-Closeout {
    param(
        [string]$EvidenceRoot,
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
        [switch]$FailOnCloseoutNotReady,
        [switch]$FailOnPresetDocsNotReady
    )

    $script = Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1"
    $stdout = Join-Path $EvidenceRoot "closeout_stdout.txt"
    $args = @{
        EvidenceRoot = $EvidenceRoot
        Json = $true
    }
    if ($FailOnMissing) {
        $args.FailOnMissing = $true
    }
    if ($FailOnUnmatched) {
        $args.FailOnUnmatched = $true
    }
    if ($FailOnAmbiguous) {
        $args.FailOnAmbiguous = $true
    }
    if ($FailOnDuplicate) {
        $args.FailOnDuplicate = $true
    }
    if ($FailOnNonMain) {
        $args.FailOnNonMain = $true
    }
    if ($FailOnUnpushedSource) {
        $args.FailOnUnpushedSource = $true
    }
    if ($FailOnMissingArtifactHashes) {
        $args.FailOnMissingArtifactHashes = $true
    }
    if ($FailOnNonFinalLabel) {
        $args.FailOnNonFinalLabel = $true
    }
    if ($FailOnWrongSlotLabel) {
        $args.FailOnWrongSlotLabel = $true
    }
    if ($FailOnMissingOperatorNotes) {
        $args.FailOnMissingOperatorNotes = $true
    }
    if ($FailOnMissingVisualReviewText) {
        $args.FailOnMissingVisualReviewText = $true
    }
    if ($FailOnCloseoutNotReady) {
        $args.FailOnCloseoutNotReady = $true
    }
    if ($FailOnPresetDocsNotReady) {
        $args.FailOnPresetDocsNotReady = $true
    }

    & $script @args *> $stdout
    return $LASTEXITCODE
}

function Write-Summary {
    param(
        [string]$Root,
        [string]$Name,
        [string]$Label,
        [string]$Mode,
        [string]$RoiSource,
        [string]$Claim,
        [bool]$Roi,
        [bool]$Renderer,
        [bool]$Phase,
        [bool]$Final = $true,
        [string]$SourceBranch = "main",
        [string]$SourceCommit = "",
        [bool]$IncludeArtifactHashes = $true,
        [string]$OperatorNotes = "tool self-test accepted visual evidence",
        [string]$TargetDescription = $Claim,
        [string]$VisualClaim = $Claim
    )

    $dir = Join-Path $Root $Name
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
    if ([string]::IsNullOrWhiteSpace($SourceCommit)) {
        $SourceCommit = $script:CurrentHead
    }

    $gate = {
        param([bool]$Passed)
        [ordered]@{ passed = $Passed }
    }

    $summary = [ordered]@{
        label = $Label
        launch = [ordered]@{
            mode = $Mode
            roiSource = $RoiSource
        }
        source = [ordered]@{
            branch = $SourceBranch
            commit = $SourceCommit
            shortCommit = if ($SourceCommit.Length -gt 7) { $SourceCommit.Substring(0, 7) } else { $SourceCommit }
            dirty = $false
        }
        visualReview = [ordered]@{
            targetDescription = $TargetDescription
            visualClaim = $VisualClaim
            operatorNotes = $OperatorNotes
        }
        evidenceVerdict = [ordered]@{
            status = if ($Final) { "visual_validated" } else { "target_visible_unvalidated" }
        }
        uiDump = [ordered]@{
            rendererLabels = if ($Renderer) { @("Renderer: Live linear EVM reconstruction") } else { @() }
            phaseLabels = if ($Phase) { @("phase: ready") } else { @() }
        }
        requiredGates = [ordered]@{
            cleanSource = (& $gate $Final)
            visualValidation = (& $gate $Final)
            screenrecord = (& $gate $Final)
            thermalReady = (& $gate $Final)
            cameraFps = (& $gate $Final)
            focusedApp = (& $gate $Final)
            noWarnings = (& $gate $Final)
            roiMeasurement = (& $gate $Roi)
            rendererDiagnostics = (& $gate $Renderer)
            phaseDiagnostics = (& $gate $Phase)
        }
    }

    if ($IncludeArtifactHashes) {
        $summary.artifacts = [ordered]@{
            screenshot = [ordered]@{
                sha256 = "screenshot-$Name-sha256"
            }
            screenrecord = [ordered]@{
                sha256 = "screenrecord-$Name-sha256"
            }
        }
    }

    $summary | ConvertTo-Json -Depth 8 | Out-File -LiteralPath (Join-Path $dir "evidence_summary.json") -Encoding utf8
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$script:CurrentHead = (& git -C $repoRoot rev-parse origin/main).Trim()
$currentShortHead = (& git -C $repoRoot rev-parse --short origin/main).Trim()

$root = Join-Path ([System.IO.Path]::GetTempPath()) "eulerian-pixel-closeout-test-$([guid]::NewGuid().ToString('N'))"
New-Item -ItemType Directory -Path $root -Force | Out-Null

try {
    Write-Summary -Root $root -Name "manual" -Label "manual-roi-known-target-final" -Mode "Tremor" -RoiSource "Manual" -Claim "Manual ROI outline overlaps known target" -Roi $true -Renderer $false -Phase $false
    Write-Summary -Root $root -Name "auto" -Label "auto-face-roi-final" -Mode "Pulse" -RoiSource "Auto" -Claim "Automatic face skin ROI overlaps visible face" -Roi $true -Renderer $false -Phase $false
    Write-Summary -Root $root -Name "pulse" -Label "live-linear-pulse-final" -Mode "Pulse" -RoiSource "FullFrame" -Claim "Pulse full-frame live linear visual parity" -Roi $false -Renderer $true -Phase $false
    Write-Summary -Root $root -Name "breathing" -Label "live-linear-breathing-final" -Mode "Breathing" -RoiSource "FullFrame" -Claim "Breathing slow motion live linear visual parity" -Roi $false -Renderer $true -Phase $false
    Write-Summary -Root $root -Name "object" -Label "live-phase-object-final" -Mode "Tremor" -RoiSource "Manual" -Claim "Object vibration edge-localized phase visual parity" -Roi $false -Renderer $false -Phase $true
    Write-Summary -Root $root -Name "fast" -Label "live-phase-fast-tremor-final" -Mode "Tremor" -RoiSource "Manual" -Claim "Fast tremor edge-localized phase visual parity" -Roi $false -Renderer $false -Phase $true
    Write-Summary -Root $root -Name "accepted-unknown" -Label "accepted-unknown-final" -Mode "Pulse" -RoiSource "FullFrame" -Claim "Accepted final evidence with no closeout slot words" -Roi $false -Renderer $false -Phase $false
    Write-Summary -Root $root -Name "setup-only" -Label "live-linear-pulse-setup" -Mode "Pulse" -RoiSource "FullFrame" -Claim "Pulse setup target visible" -Roi $false -Renderer $true -Phase $false -Final $false

    $result = & (Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1") -EvidenceRoot $root -Json | ConvertFrom-Json
    Assert-Equal -Actual $result.summaryCount -Expected 8 -Message "Summary count mismatch."
    Assert-Equal -Actual $result.acceptedFinalEvidenceCount -Expected 7 -Message "Accepted final evidence count mismatch."
    Assert-Equal -Actual @($result.unmatchedAcceptedFinalEvidence).Count -Expected 1 -Message "Unmatched accepted final evidence count mismatch."
    Assert-Equal -Actual $result.unmatchedAcceptedFinalEvidence[0].label -Expected "accepted-unknown-final" -Message "Unmatched evidence label mismatch."
    Assert-Equal -Actual @($result.ambiguousAcceptedFinalEvidence).Count -Expected 0 -Message "Known closeout fixtures should not be ambiguous."
    Assert-Equal -Actual @($result.duplicateAcceptedFinalEvidence).Count -Expected 0 -Message "Known closeout fixtures should not duplicate slots."
    Assert-Equal -Actual @($result.nonMainAcceptedFinalEvidence).Count -Expected 0 -Message "Known closeout fixtures should come from main."
    Assert-Equal -Actual @($result.unpushedAcceptedFinalEvidence).Count -Expected 0 -Message "Known closeout fixtures should be reachable from origin/main."
    Assert-Equal -Actual @($result.missingArtifactHashAcceptedFinalEvidence).Count -Expected 0 -Message "Known closeout fixtures should expose artifact hashes."
    Assert-Equal -Actual @($result.nonFinalLabelAcceptedFinalEvidence).Count -Expected 0 -Message "Known closeout fixtures should use final capture labels."
    Assert-Equal -Actual @($result.wrongSlotLabelAcceptedFinalEvidence).Count -Expected 0 -Message "Known closeout fixtures should use slot-specific final labels."
    Assert-Equal -Actual @($result.missingOperatorNotesAcceptedFinalEvidence).Count -Expected 0 -Message "Known closeout fixtures should include operator notes."
    Assert-Equal -Actual @($result.missingVisualReviewTextAcceptedFinalEvidence).Count -Expected 0 -Message "Known closeout fixtures should include visual-review text."
    Assert-Equal -Actual @($result.missing).Count -Expected 0 -Message "All slots should be satisfied."
    Assert-Equal -Actual $result.presetVisualSlotsPresent -Expected $true -Message "Preset visual slots should be present when four preset slots pass."
    Assert-Equal -Actual $result.presetDocsEvidenceClean -Expected $false -Message "Unmatched evidence should prevent preset docs closeout."
    Assert-Equal -Actual $result.readyForPresetDocs -Expected $false -Message "Preset docs should not be ready when accepted evidence is unclean."
    Assert-Equal -Actual $result.allCloseoutEvidencePresent -Expected $true -Message "All closeout evidence should be present."
    Assert-Equal -Actual $result.allCloseoutEvidenceClean -Expected $false -Message "Unmatched evidence should prevent clean roadmap closeout."
    Assert-Equal -Actual $result.slots[0].protocol -Expected "docs/testing/ROI_DEVICE_VALIDATION.md" -Message "Closeout slots should expose protocol docs."
    Assert-Equal -Actual $result.slots[0].expectedFinalLabel -Expected "manual-roi-known-target-final" -Message "Closeout slots should expose expected final labels."
    Assert-Equal -Actual $result.slots[0].sourceBranch -Expected "main" -Message "Satisfied closeout slots should expose source branch."
    Assert-Equal -Actual $result.slots[0].sourceShortCommit -Expected $currentShortHead -Message "Satisfied closeout slots should expose source short commit."
    Assert-Equal -Actual $result.slots[0].screenshotSha256 -Expected "screenshot-manual-sha256" -Message "Satisfied closeout slots should expose screenshot hashes."
    Assert-Equal -Actual $result.slots[0].screenrecordSha256 -Expected "screenrecord-manual-sha256" -Message "Satisfied closeout slots should expose screenrecord hashes."
    Assert-Equal -Actual $result.unmatchedAcceptedFinalEvidence[0].sourceShortCommit -Expected $currentShortHead -Message "Unmatched closeout evidence should expose source short commit."
    Assert-Equal -Actual $result.unmatchedAcceptedFinalEvidence[0].screenshotSha256 -Expected "screenshot-accepted-unknown-sha256" -Message "Unmatched closeout evidence should expose screenshot hashes."
    Assert-Equal -Actual $result.unmatchedAcceptedFinalEvidence[0].screenrecordSha256 -Expected "screenrecord-accepted-unknown-sha256" -Message "Unmatched closeout evidence should expose screenrecord hashes."
    Assert-True -Condition ($result.slots[0].nextCommand.Contains("manual-roi-known-target-setup")) -Message "Manual ROI slot should expose next command hint."

    $partialRoot = Join-Path $root "partial"
    New-Item -ItemType Directory -Path $partialRoot -Force | Out-Null
    Write-Summary -Root $partialRoot -Name "pulse" -Label "live-linear-pulse-final" -Mode "Pulse" -RoiSource "FullFrame" -Claim "Pulse full-frame live linear visual parity" -Roi $false -Renderer $true -Phase $false

    $partial = & (Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1") -EvidenceRoot $partialRoot -Json | ConvertFrom-Json
    Assert-Equal -Actual $partial.summaryCount -Expected 1 -Message "Partial summary count mismatch."
    Assert-Equal -Actual @($partial.missing).Count -Expected 5 -Message "Partial closeout should report missing slots."
    Assert-Equal -Actual @($partial.unmatchedAcceptedFinalEvidence).Count -Expected 0 -Message "Partial closeout should not have unmatched accepted evidence."
    Assert-Equal -Actual $partial.readyForPresetDocs -Expected $false -Message "Partial closeout should not be ready for preset docs."
    Assert-Equal -Actual $partial.presetVisualSlotsPresent -Expected $false -Message "Partial closeout should not have all preset slots."
    Assert-Equal -Actual $partial.allCloseoutEvidenceClean -Expected $false -Message "Partial closeout should not be clean."
    Assert-True -Condition ($partial.missing[0].nextCommand.Length -gt 0) -Message "Missing closeout slots should include next command hints."
    Assert-True -Condition ($partial.missing[0].expectedFinalLabel.Length -gt 0) -Message "Missing closeout slots should include expected final labels."

    $partialExitCode = Invoke-Closeout -EvidenceRoot $partialRoot -FailOnMissing
    Assert-Equal -Actual $partialExitCode -Expected 2 -Message "FailOnMissing should exit 2 when closeout slots are missing."

    $partialPresetDocsExitCode = Invoke-Closeout -EvidenceRoot $partialRoot -FailOnPresetDocsNotReady
    Assert-Equal -Actual $partialPresetDocsExitCode -Expected 4 -Message "FailOnPresetDocsNotReady should exit 4 until all preset visual slots are satisfied."

    $partialCloseoutReadyExitCode = Invoke-Closeout -EvidenceRoot $partialRoot -FailOnCloseoutNotReady
    Assert-Equal -Actual $partialCloseoutReadyExitCode -Expected 7 -Message "FailOnCloseoutNotReady should exit 7 until all closeout slots are satisfied and clean."

    $objectOnlyRoot = Join-Path $root "object-only"
    New-Item -ItemType Directory -Path $objectOnlyRoot -Force | Out-Null
    Write-Summary -Root $objectOnlyRoot -Name "object" -Label "live-phase-object-final" -Mode "Tremor" -RoiSource "Manual" -Claim "Object vibration edge-localized phase visual parity" -Roi $false -Renderer $false -Phase $true
    $objectOnly = & (Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1") -EvidenceRoot $objectOnlyRoot -Json | ConvertFrom-Json
    Assert-Equal -Actual ($objectOnly.slots | Where-Object { $_.id -eq "objectPhase" } | Select-Object -ExpandProperty satisfied) -Expected $true -Message "Object phase evidence should satisfy the object slot."
    Assert-Equal -Actual ($objectOnly.slots | Where-Object { $_.id -eq "fastTremorPhase" } | Select-Object -ExpandProperty satisfied) -Expected $false -Message "Object phase evidence must not satisfy fast tremor just because Mode is Tremor."

    $fastOnlyRoot = Join-Path $root "fast-only"
    New-Item -ItemType Directory -Path $fastOnlyRoot -Force | Out-Null
    Write-Summary -Root $fastOnlyRoot -Name "fast" -Label "live-phase-fast-tremor-final" -Mode "Tremor" -RoiSource "Manual" -Claim "Fast tremor edge-localized phase visual parity" -Roi $false -Renderer $false -Phase $true
    $fastOnly = & (Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1") -EvidenceRoot $fastOnlyRoot -Json | ConvertFrom-Json
    Assert-Equal -Actual ($fastOnly.slots | Where-Object { $_.id -eq "fastTremorPhase" } | Select-Object -ExpandProperty satisfied) -Expected $true -Message "Fast tremor evidence should satisfy the fast tremor slot."
    Assert-Equal -Actual ($fastOnly.slots | Where-Object { $_.id -eq "objectPhase" } | Select-Object -ExpandProperty satisfied) -Expected $false -Message "Fast tremor evidence must not satisfy the object slot."

    $completeExitCode = Invoke-Closeout -EvidenceRoot $root -FailOnMissing
    Assert-Equal -Actual $completeExitCode -Expected 0 -Message "FailOnMissing should exit 0 when all closeout slots are satisfied."

    $uncleanPresetDocsExitCode = Invoke-Closeout -EvidenceRoot $root -FailOnPresetDocsNotReady
    Assert-Equal -Actual $uncleanPresetDocsExitCode -Expected 4 -Message "FailOnPresetDocsNotReady should fail when preset slots pass but accepted evidence is unclean."

    $unmatchedExitCode = Invoke-Closeout -EvidenceRoot $root -FailOnUnmatched
    Assert-Equal -Actual $unmatchedExitCode -Expected 3 -Message "FailOnUnmatched should exit 3 when accepted final evidence is not mapped to a closeout slot."

    $ambiguousRoot = Join-Path $root "ambiguous"
    New-Item -ItemType Directory -Path $ambiguousRoot -Force | Out-Null
    Write-Summary -Root $ambiguousRoot -Name "pulse-breathing" -Label "live-linear-pulse-breathing-final" -Mode "Pulse" -RoiSource "FullFrame" -Claim "Pulse and breathing full-frame live linear visual parity" -Roi $false -Renderer $true -Phase $false
    $ambiguous = & (Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1") -EvidenceRoot $ambiguousRoot -Json | ConvertFrom-Json
    Assert-Equal -Actual @($ambiguous.ambiguousAcceptedFinalEvidence).Count -Expected 0 -Message "Ambiguous evidence should not survive wrong-slot label filtering."
    Assert-Equal -Actual @($ambiguous.wrongSlotLabelAcceptedFinalEvidence).Count -Expected 1 -Message "Ambiguous wrong final label should be reported as a slot-label mismatch."
    Assert-Equal -Actual @($ambiguous.wrongSlotLabelAcceptedFinalEvidence[0].mismatchedSlots).Count -Expected 2 -Message "Wrong-slot evidence should report both mismatched slots."
    Assert-Equal -Actual @($ambiguous.wrongSlotLabelAcceptedFinalEvidence[0].expectedFinalLabels).Count -Expected 2 -Message "Wrong-slot evidence should report expected labels for both mismatched slots."
    Assert-Equal -Actual ($ambiguous.wrongSlotLabelAcceptedFinalEvidence[0].expectedFinalLabels | Where-Object { $_.slot -eq "pulseLinear" } | Select-Object -ExpandProperty expectedFinalLabel) -Expected "live-linear-pulse-final" -Message "Wrong-slot evidence should report the Pulse expected label."
    Assert-Equal -Actual ($ambiguous.wrongSlotLabelAcceptedFinalEvidence[0].expectedFinalLabels | Where-Object { $_.slot -eq "breathingLinear" } | Select-Object -ExpandProperty expectedFinalLabel) -Expected "live-linear-breathing-final" -Message "Wrong-slot evidence should report the Breathing expected label."
    $ambiguousExitCode = Invoke-Closeout -EvidenceRoot $ambiguousRoot -FailOnAmbiguous
    Assert-Equal -Actual $ambiguousExitCode -Expected 0 -Message "FailOnAmbiguous should not fire after wrong-slot label filtering removes all ambiguous matches."
    $wrongSlotLabelExitCode = Invoke-Closeout -EvidenceRoot $ambiguousRoot -FailOnWrongSlotLabel
    Assert-Equal -Actual $wrongSlotLabelExitCode -Expected 18 -Message "FailOnWrongSlotLabel should fail when the final label does not match the candidate slot."

    $duplicateRoot = Join-Path $root "duplicate"
    New-Item -ItemType Directory -Path $duplicateRoot -Force | Out-Null
    Write-Summary -Root $duplicateRoot -Name "pulse-a" -Label "live-linear-pulse-final" -Mode "Pulse" -RoiSource "FullFrame" -Claim "Pulse full-frame live linear visual parity" -Roi $false -Renderer $true -Phase $false
    Write-Summary -Root $duplicateRoot -Name "pulse-b" -Label "live-linear-pulse-final" -Mode "Pulse" -RoiSource "FullFrame" -Claim "Pulse full-frame live linear visual parity" -Roi $false -Renderer $true -Phase $false
    $duplicate = & (Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1") -EvidenceRoot $duplicateRoot -Json | ConvertFrom-Json
    Assert-Equal -Actual @($duplicate.duplicateAcceptedFinalEvidence).Count -Expected 1 -Message "Duplicate closeout should report extra accepted evidence for a satisfied slot."
    Assert-Equal -Actual $duplicate.duplicateAcceptedFinalEvidence[0].slot -Expected "pulseLinear" -Message "Duplicate evidence slot mismatch."
    Assert-Equal -Actual $duplicate.duplicateAcceptedFinalEvidence[0].sourceShortCommit -Expected $currentShortHead -Message "Duplicate evidence should expose source short commit."
    Assert-Equal -Actual $duplicate.duplicateAcceptedFinalEvidence[0].originalSourceShortCommit -Expected $currentShortHead -Message "Duplicate evidence should expose original source short commit."
    Assert-Equal -Actual $duplicate.duplicateAcceptedFinalEvidence[0].screenshotSha256 -Expected "screenshot-pulse-b-sha256" -Message "Duplicate evidence should expose screenshot hash."
    Assert-Equal -Actual $duplicate.duplicateAcceptedFinalEvidence[0].originalScreenshotSha256 -Expected "screenshot-pulse-a-sha256" -Message "Duplicate evidence should expose original screenshot hash."
    $duplicateExitCode = Invoke-Closeout -EvidenceRoot $duplicateRoot -FailOnDuplicate
    Assert-Equal -Actual $duplicateExitCode -Expected 6 -Message "FailOnDuplicate should exit 6 when multiple accepted evidence bundles match the same slot."

    $classifiedRoot = Join-Path $root "classified"
    New-Item -ItemType Directory -Path $classifiedRoot -Force | Out-Null
    foreach ($name in @("manual", "auto", "pulse", "breathing", "object", "fast")) {
        Copy-Item -LiteralPath (Join-Path $root $name) -Destination (Join-Path $classifiedRoot $name) -Recurse
    }
    $classifiedExitCode = Invoke-Closeout -EvidenceRoot $classifiedRoot -FailOnMissing -FailOnUnmatched -FailOnAmbiguous -FailOnDuplicate -FailOnNonMain -FailOnUnpushedSource -FailOnMissingArtifactHashes -FailOnNonFinalLabel -FailOnWrongSlotLabel -FailOnMissingOperatorNotes -FailOnMissingVisualReviewText
    Assert-Equal -Actual $classifiedExitCode -Expected 0 -Message "Combined closeout gates should pass when all accepted evidence maps to slots on pushed main."
    $classified = & (Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1") -EvidenceRoot $classifiedRoot -Json | ConvertFrom-Json
    Assert-Equal -Actual $classified.readyForPresetDocs -Expected $true -Message "Preset docs should be ready when preset slots are satisfied and evidence is clean."
    Assert-Equal -Actual $classified.presetVisualSlotsPresent -Expected $true -Message "Preset visual slots should be present in classified closeout."
    Assert-Equal -Actual $classified.presetDocsEvidenceClean -Expected $true -Message "Preset docs evidence should be clean in classified closeout."
    $classifiedPresetDocsExitCode = Invoke-Closeout -EvidenceRoot $classifiedRoot -FailOnPresetDocsNotReady
    Assert-Equal -Actual $classifiedPresetDocsExitCode -Expected 0 -Message "FailOnPresetDocsNotReady should pass when preset slots are satisfied and evidence is clean."
    $classifiedCloseoutReadyExitCode = Invoke-Closeout -EvidenceRoot $classifiedRoot -FailOnCloseoutNotReady
    Assert-Equal -Actual $classifiedCloseoutReadyExitCode -Expected 0 -Message "FailOnCloseoutNotReady should pass when all accepted evidence maps cleanly to slots."

    $missingHashesRoot = Join-Path $root "missing-hashes"
    New-Item -ItemType Directory -Path $missingHashesRoot -Force | Out-Null
    Write-Summary -Root $missingHashesRoot -Name "pulse" -Label "live-linear-pulse-final" -Mode "Pulse" -RoiSource "FullFrame" -Claim "Pulse full-frame live linear visual parity" -Roi $false -Renderer $true -Phase $false -IncludeArtifactHashes $false
    $missingHashes = & (Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1") -EvidenceRoot $missingHashesRoot -Json | ConvertFrom-Json
    Assert-Equal -Actual @($missingHashes.missingArtifactHashAcceptedFinalEvidence).Count -Expected 1 -Message "Accepted final evidence without artifact hashes should be reported."
    Assert-Equal -Actual $missingHashes.missingArtifactHashAcceptedFinalEvidence[0].label -Expected "live-linear-pulse-final" -Message "Missing artifact-hash evidence label mismatch."
    Assert-Equal -Actual $missingHashes.allCloseoutEvidenceClean -Expected $false -Message "Missing artifact hashes should prevent clean roadmap closeout."
    Assert-Equal -Actual $missingHashes.readyForPresetDocs -Expected $false -Message "Missing artifact hashes should prevent preset docs readiness."
    $missingHashesExitCode = Invoke-Closeout -EvidenceRoot $missingHashesRoot -FailOnMissingArtifactHashes
    Assert-Equal -Actual $missingHashesExitCode -Expected 16 -Message "FailOnMissingArtifactHashes should fail on accepted evidence without screenshot or screenrecord hashes."

    $nonFinalLabelRoot = Join-Path $root "non-final-label"
    New-Item -ItemType Directory -Path $nonFinalLabelRoot -Force | Out-Null
    Write-Summary -Root $nonFinalLabelRoot -Name "pulse-setup-accepted" -Label "live-linear-pulse-setup" -Mode "Pulse" -RoiSource "FullFrame" -Claim "Pulse full-frame live linear visual parity" -Roi $false -Renderer $true -Phase $false
    $nonFinalLabel = & (Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1") -EvidenceRoot $nonFinalLabelRoot -Json | ConvertFrom-Json
    Assert-Equal -Actual @($nonFinalLabel.nonFinalLabelAcceptedFinalEvidence).Count -Expected 1 -Message "Accepted final evidence with a setup label should be reported."
    Assert-Equal -Actual @($nonFinalLabel.slots | Where-Object { $_.satisfied }).Count -Expected 0 -Message "Accepted setup-labeled evidence must not satisfy closeout slots."
    Assert-Equal -Actual $nonFinalLabel.allCloseoutEvidenceClean -Expected $false -Message "Non-final labels should prevent clean roadmap closeout."
    Assert-Equal -Actual $nonFinalLabel.readyForPresetDocs -Expected $false -Message "Non-final labels should prevent preset docs readiness."
    $nonFinalLabelExitCode = Invoke-Closeout -EvidenceRoot $nonFinalLabelRoot -FailOnNonFinalLabel
    Assert-Equal -Actual $nonFinalLabelExitCode -Expected 17 -Message "FailOnNonFinalLabel should fail on accepted evidence whose label is not a final capture label."

    $wrongSlotLabelRoot = Join-Path $root "wrong-slot-label"
    New-Item -ItemType Directory -Path $wrongSlotLabelRoot -Force | Out-Null
    Write-Summary -Root $wrongSlotLabelRoot -Name "pulse-label-breathing-claim" -Label "live-linear-pulse-final" -Mode "Breathing" -RoiSource "FullFrame" -Claim "Breathing slow motion live linear visual parity" -Roi $false -Renderer $true -Phase $false
    $wrongSlotLabel = & (Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1") -EvidenceRoot $wrongSlotLabelRoot -Json | ConvertFrom-Json
    Assert-Equal -Actual @($wrongSlotLabel.wrongSlotLabelAcceptedFinalEvidence).Count -Expected 1 -Message "Evidence whose final label targets a different slot should be reported."
    Assert-Equal -Actual $wrongSlotLabel.wrongSlotLabelAcceptedFinalEvidence[0].expectedFinalLabels[0].expectedFinalLabel -Expected "live-linear-breathing-final" -Message "Wrong-slot evidence should include the expected final label."
    Assert-Equal -Actual @($wrongSlotLabel.slots | Where-Object { $_.satisfied }).Count -Expected 0 -Message "Wrong-slot final labels must not satisfy closeout slots."
    Assert-Equal -Actual $wrongSlotLabel.readyForPresetDocs -Expected $false -Message "Wrong-slot final labels should prevent preset docs readiness."
    $wrongSlotLabelGateExitCode = Invoke-Closeout -EvidenceRoot $wrongSlotLabelRoot -FailOnWrongSlotLabel
    Assert-Equal -Actual $wrongSlotLabelGateExitCode -Expected 18 -Message "FailOnWrongSlotLabel should fail on accepted evidence whose label targets the wrong slot."

    $missingOperatorNotesRoot = Join-Path $root "missing-operator-notes"
    New-Item -ItemType Directory -Path $missingOperatorNotesRoot -Force | Out-Null
    Write-Summary -Root $missingOperatorNotesRoot -Name "pulse" -Label "live-linear-pulse-final" -Mode "Pulse" -RoiSource "FullFrame" -Claim "Pulse full-frame live linear visual parity" -Roi $false -Renderer $true -Phase $false -OperatorNotes ""
    $missingOperatorNotes = & (Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1") -EvidenceRoot $missingOperatorNotesRoot -Json | ConvertFrom-Json
    Assert-Equal -Actual @($missingOperatorNotes.missingOperatorNotesAcceptedFinalEvidence).Count -Expected 1 -Message "Accepted final evidence without operator notes should be reported."
    Assert-Equal -Actual $missingOperatorNotes.allCloseoutEvidenceClean -Expected $false -Message "Missing operator notes should prevent clean roadmap closeout."
    Assert-Equal -Actual $missingOperatorNotes.readyForPresetDocs -Expected $false -Message "Missing operator notes should prevent preset docs readiness."
    $missingOperatorNotesExitCode = Invoke-Closeout -EvidenceRoot $missingOperatorNotesRoot -FailOnMissingOperatorNotes
    Assert-Equal -Actual $missingOperatorNotesExitCode -Expected 19 -Message "FailOnMissingOperatorNotes should fail on accepted evidence without operator notes."

    $missingVisualReviewTextRoot = Join-Path $root "missing-visual-review-text"
    New-Item -ItemType Directory -Path $missingVisualReviewTextRoot -Force | Out-Null
    Write-Summary -Root $missingVisualReviewTextRoot -Name "pulse" -Label "live-linear-pulse-final" -Mode "Pulse" -RoiSource "FullFrame" -Claim "Pulse full-frame live linear visual parity" -Roi $false -Renderer $true -Phase $false -TargetDescription "" -VisualClaim ""
    $missingVisualReviewText = & (Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1") -EvidenceRoot $missingVisualReviewTextRoot -Json | ConvertFrom-Json
    Assert-Equal -Actual @($missingVisualReviewText.missingVisualReviewTextAcceptedFinalEvidence).Count -Expected 1 -Message "Accepted final evidence without visual-review text should be reported."
    Assert-Equal -Actual $missingVisualReviewText.missingVisualReviewTextAcceptedFinalEvidence[0].label -Expected "live-linear-pulse-final" -Message "Missing visual-review text evidence label mismatch."
    Assert-Equal -Actual $missingVisualReviewText.allCloseoutEvidenceClean -Expected $false -Message "Missing visual-review text should prevent clean roadmap closeout."
    Assert-Equal -Actual $missingVisualReviewText.readyForPresetDocs -Expected $false -Message "Missing visual-review text should prevent preset docs readiness."
    $missingVisualReviewTextExitCode = Invoke-Closeout -EvidenceRoot $missingVisualReviewTextRoot -FailOnMissingVisualReviewText
    Assert-Equal -Actual $missingVisualReviewTextExitCode -Expected 20 -Message "FailOnMissingVisualReviewText should fail on accepted evidence without target description or visual claim."

    $offMainRoot = Join-Path $root "off-main"
    New-Item -ItemType Directory -Path $offMainRoot -Force | Out-Null
    Write-Summary -Root $offMainRoot -Name "manual" -Label "manual-roi-known-target-final" -Mode "Tremor" -RoiSource "Manual" -Claim "Manual ROI outline overlaps known target" -Roi $true -Renderer $false -Phase $false
    Write-Summary -Root $offMainRoot -Name "auto" -Label "auto-face-roi-final" -Mode "Pulse" -RoiSource "Auto" -Claim "Automatic face skin ROI overlaps visible face" -Roi $true -Renderer $false -Phase $false
    Write-Summary -Root $offMainRoot -Name "pulse" -Label "live-linear-pulse-final" -Mode "Pulse" -RoiSource "FullFrame" -Claim "Pulse full-frame live linear visual parity" -Roi $false -Renderer $true -Phase $false -SourceBranch "codex/test"
    Write-Summary -Root $offMainRoot -Name "breathing" -Label "live-linear-breathing-final" -Mode "Breathing" -RoiSource "FullFrame" -Claim "Breathing slow motion live linear visual parity" -Roi $false -Renderer $true -Phase $false
    Write-Summary -Root $offMainRoot -Name "object" -Label "live-phase-object-final" -Mode "Tremor" -RoiSource "Manual" -Claim "Object vibration edge-localized phase visual parity" -Roi $false -Renderer $false -Phase $true
    Write-Summary -Root $offMainRoot -Name "fast" -Label "live-phase-fast-tremor-final" -Mode "Tremor" -RoiSource "Manual" -Claim "Fast tremor edge-localized phase visual parity" -Roi $false -Renderer $false -Phase $true
    $offMain = & (Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1") -EvidenceRoot $offMainRoot -Json | ConvertFrom-Json
    Assert-Equal -Actual @($offMain.nonMainAcceptedFinalEvidence).Count -Expected 1 -Message "Off-main accepted final evidence should be reported."
    Assert-Equal -Actual $offMain.nonMainAcceptedFinalEvidence[0].sourceBranch -Expected "codex/test" -Message "Off-main evidence should report source branch."
    Assert-Equal -Actual $offMain.allCloseoutEvidencePresent -Expected $true -Message "Off-main fixture should still have all closeout slots present."
    Assert-Equal -Actual $offMain.allCloseoutEvidenceClean -Expected $false -Message "Off-main evidence should prevent clean roadmap closeout."
    Assert-Equal -Actual $offMain.readyForPresetDocs -Expected $false -Message "Off-main evidence should prevent preset docs readiness."
    $offMainCloseoutReadyExitCode = Invoke-Closeout -EvidenceRoot $offMainRoot -FailOnCloseoutNotReady
    Assert-Equal -Actual $offMainCloseoutReadyExitCode -Expected 7 -Message "FailOnCloseoutNotReady should fail on off-main accepted evidence."
    $offMainPresetDocsExitCode = Invoke-Closeout -EvidenceRoot $offMainRoot -FailOnPresetDocsNotReady
    Assert-Equal -Actual $offMainPresetDocsExitCode -Expected 4 -Message "FailOnPresetDocsNotReady should fail on off-main accepted preset evidence."
    $offMainExitCode = Invoke-Closeout -EvidenceRoot $offMainRoot -FailOnNonMain
    Assert-Equal -Actual $offMainExitCode -Expected 8 -Message "FailOnNonMain should fail on off-main accepted evidence."

    $unpushedRoot = Join-Path $root "unpushed"
    New-Item -ItemType Directory -Path $unpushedRoot -Force | Out-Null
    Write-Summary -Root $unpushedRoot -Name "pulse" -Label "live-linear-pulse-final" -Mode "Pulse" -RoiSource "FullFrame" -Claim "Pulse full-frame live linear visual parity" -Roi $false -Renderer $true -Phase $false -SourceCommit "0000000000000000000000000000000000000000"
    $unpushed = & (Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1") -EvidenceRoot $unpushedRoot -Json | ConvertFrom-Json
    Assert-Equal -Actual @($unpushed.unpushedAcceptedFinalEvidence).Count -Expected 1 -Message "Unpushed or unknown accepted final evidence should be reported."
    Assert-Equal -Actual $unpushed.unpushedAcceptedFinalEvidence[0].sourceCommit -Expected "0000000000000000000000000000000000000000" -Message "Unpushed evidence should report source commit."
    Assert-Equal -Actual $unpushed.allCloseoutEvidenceClean -Expected $false -Message "Unpushed evidence should prevent clean roadmap closeout."
    Assert-Equal -Actual $unpushed.readyForPresetDocs -Expected $false -Message "Unpushed evidence should prevent preset docs readiness."
    $unpushedCloseoutReadyExitCode = Invoke-Closeout -EvidenceRoot $unpushedRoot -FailOnCloseoutNotReady
    Assert-Equal -Actual $unpushedCloseoutReadyExitCode -Expected 7 -Message "FailOnCloseoutNotReady should fail on unpushed accepted evidence."
    $unpushedPresetDocsExitCode = Invoke-Closeout -EvidenceRoot $unpushedRoot -FailOnPresetDocsNotReady
    Assert-Equal -Actual $unpushedPresetDocsExitCode -Expected 4 -Message "FailOnPresetDocsNotReady should fail on unpushed accepted preset evidence."
    $unpushedExitCode = Invoke-Closeout -EvidenceRoot $unpushedRoot -FailOnUnpushedSource
    Assert-Equal -Actual $unpushedExitCode -Expected 15 -Message "FailOnUnpushedSource should fail on accepted evidence whose commit is not on origin/main."
} finally {
    Remove-Item -LiteralPath $root -Recurse -Force
}

Write-Output "Pixel validation closeout self-test passed."
