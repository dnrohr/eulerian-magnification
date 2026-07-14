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
        [switch]$FailOnMissing
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
        [bool]$Final = $true
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
    Write-Summary -Root $root -Name "setup-only" -Label "live-linear-pulse-setup" -Mode "Pulse" -RoiSource "FullFrame" -Claim "Pulse setup target visible" -Roi $false -Renderer $true -Phase $false -Final $false

    $result = & (Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1") -EvidenceRoot $root -Json | ConvertFrom-Json
    Assert-Equal -Actual $result.summaryCount -Expected 7 -Message "Summary count mismatch."
    Assert-Equal -Actual $result.acceptedFinalEvidenceCount -Expected 6 -Message "Accepted final evidence count mismatch."
    Assert-Equal -Actual @($result.missing).Count -Expected 0 -Message "All slots should be satisfied."
    Assert-Equal -Actual $result.readyForPresetDocs -Expected $true -Message "Preset docs should be ready when four preset slots pass."
    Assert-Equal -Actual $result.allCloseoutEvidencePresent -Expected $true -Message "All closeout evidence should be present."

    $partialRoot = Join-Path $root "partial"
    New-Item -ItemType Directory -Path $partialRoot -Force | Out-Null
    Write-Summary -Root $partialRoot -Name "pulse" -Label "live-linear-pulse-final" -Mode "Pulse" -RoiSource "FullFrame" -Claim "Pulse full-frame live linear visual parity" -Roi $false -Renderer $true -Phase $false

    $partial = & (Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1") -EvidenceRoot $partialRoot -Json | ConvertFrom-Json
    Assert-Equal -Actual $partial.summaryCount -Expected 1 -Message "Partial summary count mismatch."
    Assert-Equal -Actual @($partial.missing).Count -Expected 5 -Message "Partial closeout should report missing slots."
    Assert-Equal -Actual $partial.readyForPresetDocs -Expected $false -Message "Partial closeout should not be ready for preset docs."

    $partialExitCode = Invoke-Closeout -EvidenceRoot $partialRoot -FailOnMissing
    Assert-Equal -Actual $partialExitCode -Expected 2 -Message "FailOnMissing should exit 2 when closeout slots are missing."

    $completeExitCode = Invoke-Closeout -EvidenceRoot $root -FailOnMissing
    Assert-Equal -Actual $completeExitCode -Expected 0 -Message "FailOnMissing should exit 0 when all closeout slots are satisfied."
} finally {
    Remove-Item -LiteralPath $root -Recurse -Force
}

Write-Output "Pixel validation closeout self-test passed."
