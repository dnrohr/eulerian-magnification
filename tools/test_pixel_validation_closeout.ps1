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
        [string]$SourceCommit = "abcdef1234567890"
    )

    $dir = Join-Path $Root $Name
    New-Item -ItemType Directory -Path $dir -Force | Out-Null

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
            targetDescription = $Claim
            visualClaim = $Claim
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

    $summary | ConvertTo-Json -Depth 8 | Out-File -LiteralPath (Join-Path $dir "evidence_summary.json") -Encoding utf8
}

$root = Join-Path ([System.IO.Path]::GetTempPath()) "eulerian-pixel-closeout-test-$([guid]::NewGuid().ToString('N'))"
New-Item -ItemType Directory -Path $root -Force | Out-Null

try {
    Write-Summary -Root $root -Name "manual" -Label "manual-roi-final" -Mode "Tremor" -RoiSource "Manual" -Claim "Manual ROI outline overlaps known target" -Roi $true -Renderer $false -Phase $false
    Write-Summary -Root $root -Name "auto" -Label "auto-face-final" -Mode "Pulse" -RoiSource "Auto" -Claim "Automatic face skin ROI overlaps visible face" -Roi $true -Renderer $false -Phase $false
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
    Assert-Equal -Actual @($result.missing).Count -Expected 0 -Message "All slots should be satisfied."
    Assert-Equal -Actual $result.presetVisualSlotsPresent -Expected $true -Message "Preset visual slots should be present when four preset slots pass."
    Assert-Equal -Actual $result.presetDocsEvidenceClean -Expected $false -Message "Unmatched evidence should prevent preset docs closeout."
    Assert-Equal -Actual $result.readyForPresetDocs -Expected $false -Message "Preset docs should not be ready when accepted evidence is unclean."
    Assert-Equal -Actual $result.allCloseoutEvidencePresent -Expected $true -Message "All closeout evidence should be present."
    Assert-Equal -Actual $result.allCloseoutEvidenceClean -Expected $false -Message "Unmatched evidence should prevent clean roadmap closeout."
    Assert-Equal -Actual $result.slots[0].protocol -Expected "docs/testing/ROI_DEVICE_VALIDATION.md" -Message "Closeout slots should expose protocol docs."
    Assert-Equal -Actual $result.slots[0].sourceBranch -Expected "main" -Message "Satisfied closeout slots should expose source branch."
    Assert-Equal -Actual $result.slots[0].sourceShortCommit -Expected "abcdef1" -Message "Satisfied closeout slots should expose source short commit."
    Assert-Equal -Actual $result.unmatchedAcceptedFinalEvidence[0].sourceShortCommit -Expected "abcdef1" -Message "Unmatched closeout evidence should expose source short commit."
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
    Assert-Equal -Actual @($ambiguous.ambiguousAcceptedFinalEvidence).Count -Expected 1 -Message "Ambiguous closeout should report evidence matching multiple slots."
    Assert-Equal -Actual @($ambiguous.ambiguousAcceptedFinalEvidence[0].matchedSlots).Count -Expected 2 -Message "Ambiguous evidence should report both matched slots."
    $ambiguousExitCode = Invoke-Closeout -EvidenceRoot $ambiguousRoot -FailOnAmbiguous
    Assert-Equal -Actual $ambiguousExitCode -Expected 5 -Message "FailOnAmbiguous should exit 5 when accepted final evidence matches multiple slots."

    $duplicateRoot = Join-Path $root "duplicate"
    New-Item -ItemType Directory -Path $duplicateRoot -Force | Out-Null
    Write-Summary -Root $duplicateRoot -Name "pulse-a" -Label "live-linear-pulse-final-a" -Mode "Pulse" -RoiSource "FullFrame" -Claim "Pulse full-frame live linear visual parity" -Roi $false -Renderer $true -Phase $false
    Write-Summary -Root $duplicateRoot -Name "pulse-b" -Label "live-linear-pulse-final-b" -Mode "Pulse" -RoiSource "FullFrame" -Claim "Pulse full-frame live linear visual parity" -Roi $false -Renderer $true -Phase $false
    $duplicate = & (Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1") -EvidenceRoot $duplicateRoot -Json | ConvertFrom-Json
    Assert-Equal -Actual @($duplicate.duplicateAcceptedFinalEvidence).Count -Expected 1 -Message "Duplicate closeout should report extra accepted evidence for a satisfied slot."
    Assert-Equal -Actual $duplicate.duplicateAcceptedFinalEvidence[0].slot -Expected "pulseLinear" -Message "Duplicate evidence slot mismatch."
    Assert-Equal -Actual $duplicate.duplicateAcceptedFinalEvidence[0].sourceShortCommit -Expected "abcdef1" -Message "Duplicate evidence should expose source short commit."
    Assert-Equal -Actual $duplicate.duplicateAcceptedFinalEvidence[0].originalSourceShortCommit -Expected "abcdef1" -Message "Duplicate evidence should expose original source short commit."
    $duplicateExitCode = Invoke-Closeout -EvidenceRoot $duplicateRoot -FailOnDuplicate
    Assert-Equal -Actual $duplicateExitCode -Expected 6 -Message "FailOnDuplicate should exit 6 when multiple accepted evidence bundles match the same slot."

    $classifiedRoot = Join-Path $root "classified"
    New-Item -ItemType Directory -Path $classifiedRoot -Force | Out-Null
    foreach ($name in @("manual", "auto", "pulse", "breathing", "object", "fast")) {
        Copy-Item -LiteralPath (Join-Path $root $name) -Destination (Join-Path $classifiedRoot $name) -Recurse
    }
    $classifiedExitCode = Invoke-Closeout -EvidenceRoot $classifiedRoot -FailOnMissing -FailOnUnmatched -FailOnAmbiguous -FailOnDuplicate
    Assert-Equal -Actual $classifiedExitCode -Expected 0 -Message "Combined closeout gates should pass when all accepted evidence maps to slots."
    $classified = & (Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1") -EvidenceRoot $classifiedRoot -Json | ConvertFrom-Json
    Assert-Equal -Actual $classified.readyForPresetDocs -Expected $true -Message "Preset docs should be ready when preset slots are satisfied and evidence is clean."
    Assert-Equal -Actual $classified.presetVisualSlotsPresent -Expected $true -Message "Preset visual slots should be present in classified closeout."
    Assert-Equal -Actual $classified.presetDocsEvidenceClean -Expected $true -Message "Preset docs evidence should be clean in classified closeout."
    $classifiedPresetDocsExitCode = Invoke-Closeout -EvidenceRoot $classifiedRoot -FailOnPresetDocsNotReady
    Assert-Equal -Actual $classifiedPresetDocsExitCode -Expected 0 -Message "FailOnPresetDocsNotReady should pass when preset slots are satisfied and evidence is clean."
    $classifiedCloseoutReadyExitCode = Invoke-Closeout -EvidenceRoot $classifiedRoot -FailOnCloseoutNotReady
    Assert-Equal -Actual $classifiedCloseoutReadyExitCode -Expected 0 -Message "FailOnCloseoutNotReady should pass when all accepted evidence maps cleanly to slots."

    $offMainRoot = Join-Path $root "off-main"
    New-Item -ItemType Directory -Path $offMainRoot -Force | Out-Null
    Write-Summary -Root $offMainRoot -Name "manual" -Label "manual-roi-final" -Mode "Tremor" -RoiSource "Manual" -Claim "Manual ROI outline overlaps known target" -Roi $true -Renderer $false -Phase $false
    Write-Summary -Root $offMainRoot -Name "auto" -Label "auto-face-final" -Mode "Pulse" -RoiSource "Auto" -Claim "Automatic face skin ROI overlaps visible face" -Roi $true -Renderer $false -Phase $false
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
} finally {
    Remove-Item -LiteralPath $root -Recurse -Force
}

Write-Output "Pixel validation closeout self-test passed."
