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

function Assert-DocContains {
    param(
        [string]$Path,
        [string]$Expected,
        [string]$Message
    )

    $content = Get-Content -LiteralPath $Path -Raw
    Assert-True -Condition $content.Contains($Expected) -Message $Message
}

function Get-PowershellBlocks {
    param([string]$Path)

    $content = Get-Content -LiteralPath $Path -Raw
    $blocks = @()
    foreach ($match in [regex]::Matches($content, '(?ms)^```powershell\s*$\n(.*?)^```\s*$')) {
        $blocks += $match.Groups[1].Value.Trim()
    }
    return $blocks
}

function Assert-CaptureCommandBlocksParse {
    param(
        [string]$Path,
        [System.Collections.Generic.HashSet[string]]$CaptureParameters
    )

    $captureBlockCount = 0
    foreach ($block in Get-PowershellBlocks -Path $Path) {
        if (-not $block.StartsWith(".\tools\capture_live_validation_evidence.ps1")) {
            continue
        }

        $captureBlockCount += 1
        $parseErrors = $null
        $tokens = [System.Management.Automation.PSParser]::Tokenize($block, [ref]$parseErrors)
        Assert-Equal -Actual @($parseErrors).Count -Expected 0 -Message "Documented capture command should parse as PowerShell in $Path."

        $executable = @($tokens | Where-Object { $_.Type -eq "Command" } | Select-Object -First 1).Content
        Assert-Equal -Actual $executable -Expected ".\tools\capture_live_validation_evidence.ps1" -Message "Documented capture command should call the live validation capture script in $Path."

        foreach ($token in @($tokens | Where-Object { $_.Type -eq "CommandParameter" })) {
            $parameterName = $token.Content.TrimStart("-")
            Assert-True -Condition ($CaptureParameters.Contains($parameterName)) -Message "Unknown capture command parameter '$($token.Content)' in documented command in $Path."
        }
    }

    return $captureBlockCount
}

$repoRoot = Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")

$readme = Join-Path $repoRoot "README.md"
$taskReadme = Join-Path $repoRoot "docs\tasks\README.md"
$roiDoc = Join-Path $repoRoot "docs\testing\ROI_DEVICE_VALIDATION.md"
$liveGuide = Join-Path $repoRoot "docs\testing\LIVE_VISUAL_VALIDATION_CAPTURE.md"
$parityDoc = Join-Path $repoRoot "docs\testing\MIT_PARITY_TARGETS.md"
$linearDoc = Join-Path $repoRoot "docs\experiments\pixel8a_live_linear_validation.md"
$phaseDoc = Join-Path $repoRoot "docs\experiments\pixel8a_live_phase_validation.md"
$captureScript = Join-Path $PSScriptRoot "capture_live_validation_evidence.ps1"
$captureParameters = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
$captureScriptContent = Get-Content -LiteralPath $captureScript -Raw
if ($captureScriptContent -notmatch '(?s)^param\((.*?)\)\s*\$ErrorActionPreference') {
    throw "Could not parse capture script param block."
}

foreach ($match in [regex]::Matches($Matches[1], '\$(\w+)')) {
    [void]$captureParameters.Add($match.Groups[1].Value)
}

foreach ($path in @($readme, $taskReadme, $roiDoc, $liveGuide, $parityDoc, $linearDoc, $phaseDoc)) {
    Assert-True -Condition (Test-Path -LiteralPath $path) -Message "Missing protocol doc: $path"
}

$plan = & (Join-Path $PSScriptRoot "show_next_pixel_validation_plan.ps1") -Json | ConvertFrom-Json
foreach ($group in @($plan.validationGroups)) {
    $protocolPath = Join-Path $repoRoot $group.protocol
    Assert-True -Condition (Test-Path -LiteralPath $protocolPath) -Message "Missing planner protocol doc: $protocolPath"
    foreach ($command in @($group.commands)) {
        if (-not $command.command.StartsWith(".\tools\capture_live_validation_evidence.ps1")) {
            continue
        }
        Assert-DocContains -Path $protocolPath -Expected $command.name -Message "Protocol doc '$($group.protocol)' must include planner command '$($command.name)'."
    }
}

foreach ($path in @($roiDoc, $linearDoc, $phaseDoc)) {
    $captureBlockCount = Assert-CaptureCommandBlocksParse -Path $path -CaptureParameters $captureParameters
    Assert-True -Condition ($captureBlockCount -gt 0) -Message "Protocol doc must include parseable capture command blocks: $path"
}

foreach ($path in @($readme, $taskReadme)) {
    Assert-DocContains -Path $path -Expected "summarize_pixel_validation_closeout.ps1" -Message "Operator docs must document closeout summary."
    Assert-DocContains -Path $path -Expected "-FailOnMissing" -Message "Operator docs must document missing-evidence closeout gate."
    Assert-DocContains -Path $path -Expected "-FailOnUnmatched" -Message "Operator docs must document unmatched-evidence closeout gate."
    Assert-DocContains -Path $path -Expected "-FailOnAmbiguous" -Message "Operator docs must document ambiguous-evidence closeout gate."
    Assert-DocContains -Path $path -Expected "-FailOnDuplicate" -Message "Operator docs must document duplicate-evidence closeout gate."
    Assert-DocContains -Path $path -Expected "-FailOnNonMain" -Message "Operator docs must document non-main-evidence closeout gate."
    Assert-DocContains -Path $path -Expected "-FailOnUnpushedSource" -Message "Operator docs must document pushed-source closeout gate."
    Assert-DocContains -Path $path -Expected "-FailOnCloseoutNotReady" -Message "Operator docs must document one-shot closeout readiness gate."
    Assert-DocContains -Path $path -Expected "-FailOnPresetDocsNotReady" -Message "Operator docs must document preset-doc readiness gate."
}

Assert-DocContains -Path $roiDoc -Expected "-RequireRoiMeasurement" -Message "ROI protocol must require ROI measurement."
Assert-DocContains -Path $roiDoc -Expected "-RequireFinalVisualEvidence" -Message "ROI protocol must include final visual evidence gates."
Assert-DocContains -Path $roiDoc -Expected "-MeasureRoiKind Manual" -Message "ROI protocol must document manual overlay measurement."
Assert-DocContains -Path $roiDoc -Expected "-MeasureRoiKind Auto" -Message "ROI protocol must document automatic overlay measurement."
Assert-DocContains -Path $roiDoc -Expected "manual-roi-known-target-final" -Message "ROI protocol must include explicit manual ROI final evidence."
Assert-DocContains -Path $roiDoc -Expected "auto-face-roi-final" -Message "ROI protocol must include explicit automatic ROI final evidence."
Assert-DocContains -Path $roiDoc -Expected "-RequireEvidenceVerdict target_visible_unvalidated" -Message "ROI setup evidence should stop at target-visible review."
Assert-DocContains -Path $roiDoc -Expected "source.commitReachableFromOriginMain" -Message "ROI protocol must require final evidence from a pushed source commit."

Assert-DocContains -Path $liveGuide -Expected "-RequireFinalVisualEvidence" -Message "Live guide must document the final visual evidence profile."
Assert-DocContains -Path $liveGuide -Expected "-RequireScreenrecord" -Message "Live guide must document screenrecord gating."
Assert-DocContains -Path $liveGuide -Expected "-RequireThermalReady" -Message "Live guide must document thermal readiness gating."
Assert-DocContains -Path $liveGuide -Expected "-RequireCameraFps" -Message "Live guide must document camera FPS gating."
Assert-DocContains -Path $liveGuide -Expected "-RequireFocusedApp" -Message "Live guide must document focused-app gating."
Assert-DocContains -Path $liveGuide -Expected "-RequireEvidenceVerdict" -Message "Live guide must document verdict gating."

Assert-DocContains -Path $parityDoc -Expected "summarize_pixel_validation_closeout.ps1" -Message "Parity targets must document closeout summary."
Assert-DocContains -Path $parityDoc -Expected "-FailOnPresetDocsNotReady" -Message "Parity targets must document the preset docs readiness gate."
Assert-DocContains -Path $parityDoc -Expected "-FailOnAmbiguous" -Message "Parity targets must document ambiguous accepted evidence gate."
Assert-DocContains -Path $parityDoc -Expected "-FailOnDuplicate" -Message "Parity targets must document duplicate accepted evidence gate."
Assert-DocContains -Path $parityDoc -Expected "-FailOnNonMain" -Message "Parity targets must document non-main accepted evidence gate."
Assert-DocContains -Path $parityDoc -Expected "-FailOnUnpushedSource" -Message "Parity targets must document pushed-source accepted evidence gate."
Assert-DocContains -Path $parityDoc -Expected "-FailOnCloseoutNotReady" -Message "Parity targets must document one-shot closeout readiness gate."

Assert-DocContains -Path $linearDoc -Expected "-Mode Pulse" -Message "Live linear protocol must include the Pulse command."
Assert-DocContains -Path $linearDoc -Expected "-Mode Breathing" -Message "Live linear protocol must include the Breathing variant."
Assert-DocContains -Path $linearDoc -Expected "-RoiSource FullFrame" -Message "Live linear protocol must validate full-frame ROI."
Assert-DocContains -Path $linearDoc -Expected "-RequireRendererDiagnostics" -Message "Live linear protocol must require renderer diagnostics."
Assert-DocContains -Path $linearDoc -Expected "-RequireFinalVisualEvidence" -Message "Live linear protocol must include final visual evidence gates."
Assert-DocContains -Path $linearDoc -Expected "live-linear-breathing-final" -Message "Live linear protocol must include explicit Breathing final evidence."
Assert-DocContains -Path $linearDoc -Expected "target_visible_unvalidated" -Message "Live linear setup should stop at target-visible review."
Assert-DocContains -Path $linearDoc -Expected "source.commitReachableFromOriginMain" -Message "Live linear protocol must require final evidence from a pushed source commit."

Assert-DocContains -Path $phaseDoc -Expected "-Mode Tremor" -Message "Live phase protocol must use the public Motion/Tremor path."
Assert-DocContains -Path $phaseDoc -Expected "-RoiSource Manual" -Message "Live phase protocol must validate manual ROI phase motion."
Assert-DocContains -Path $phaseDoc -Expected "-ManualRoi" -Message "Live phase protocol must provide a manual ROI command."
Assert-DocContains -Path $phaseDoc -Expected "-RequirePhaseDiagnostics" -Message "Live phase protocol must require phase diagnostics."
Assert-DocContains -Path $phaseDoc -Expected "-RequireFinalVisualEvidence" -Message "Live phase protocol must include final visual evidence gates."
Assert-DocContains -Path $phaseDoc -Expected "live-phase-fast-tremor-final" -Message "Live phase protocol must include explicit Fast tremor final evidence."
Assert-DocContains -Path $phaseDoc -Expected "target_visible_unvalidated" -Message "Live phase setup should stop at target-visible review."
Assert-DocContains -Path $phaseDoc -Expected "source.commitReachableFromOriginMain" -Message "Live phase protocol must require final evidence from a pushed source commit."

Write-Output "Live validation protocol docs self-test passed."
