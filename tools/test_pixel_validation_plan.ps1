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

$plan = & (Join-Path $PSScriptRoot "show_next_pixel_validation_plan.ps1") -Json | ConvertFrom-Json
$closeout = & (Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1") -EvidenceRoot (Join-Path ([System.IO.Path]::GetTempPath()) "eulerian-missing-closeout-$([guid]::NewGuid().ToString('N'))") -Json | ConvertFrom-Json

Assert-Equal -Actual $plan.roadmap.total -Expected 47 -Message "Roadmap total mismatch."
Assert-Equal -Actual $plan.roadmap.complete -Expected 41 -Message "Complete milestone count mismatch."
Assert-Equal -Actual $plan.roadmap.inProgress -Expected 6 -Message "In-progress milestone count mismatch."
Assert-Equal -Actual @($plan.missingMilestones).Count -Expected 0 -Message "Every in-progress milestone should have validation-plan coverage."

$covered = @($plan.coveredMilestones)
foreach ($milestone in @("M", "U", "AE", "AP", "AR", "AT")) {
    Assert-True -Condition ($milestone -in $covered) -Message "Missing covered milestone $milestone."
}

$groups = @($plan.validationGroups)
Assert-Equal -Actual $groups.Count -Expected 4 -Message "Validation group count mismatch."

$roi = $groups | Where-Object { $_.id -eq "roi-mapping" } | Select-Object -First 1
$linear = $groups | Where-Object { $_.id -eq "live-linear" } | Select-Object -First 1
$phase = $groups | Where-Object { $_.id -eq "live-phase" } | Select-Object -First 1
$preset = $groups | Where-Object { $_.id -eq "preset-parity" } | Select-Object -First 1

Assert-True -Condition ($null -ne $roi) -Message "ROI validation group missing."
Assert-True -Condition ($null -ne $linear) -Message "Live linear validation group missing."
Assert-True -Condition ($null -ne $phase) -Message "Live phase validation group missing."
Assert-True -Condition ($null -ne $preset) -Message "Preset parity validation group missing."

Assert-True -Condition ($roi.finalEvidence.Contains("-RequireRoiMeasurement")) -Message "ROI final evidence should require ROI measurement."
Assert-True -Condition ($roi.finalEvidence.Contains("-RequireFinalVisualEvidence")) -Message "ROI final evidence should require final visual evidence."
Assert-True -Condition ($roi.finalEvidence.Contains("origin/main")) -Message "ROI final evidence should require a source commit reachable from origin/main."
Assert-True -Condition ($linear.finalEvidence.Contains("-RequireRendererDiagnostics")) -Message "Linear final evidence should require renderer diagnostics."
Assert-True -Condition ($linear.finalEvidence.Contains("origin/main")) -Message "Linear final evidence should require a source commit reachable from origin/main."
Assert-True -Condition ($phase.finalEvidence.Contains("-RequirePhaseDiagnostics")) -Message "Phase final evidence should require phase diagnostics."
Assert-True -Condition ($phase.finalEvidence.Contains("origin/main")) -Message "Phase final evidence should require a source commit reachable from origin/main."
Assert-True -Condition ($preset.finalEvidence.Contains("Update README")) -Message "Preset parity plan should keep docs updates after accepted artifacts."
$presetCloseoutCommand = @($preset.commands | Where-Object { $_.name -eq "preset-parity-closeout" } | Select-Object -First 1).command
Assert-True -Condition ($presetCloseoutCommand.Contains("-FailOnCloseoutNotReady")) -Message "Preset parity closeout should require the roadmap closeout readiness gate."
Assert-True -Condition ($presetCloseoutCommand.Contains("-FailOnPresetDocsNotReady")) -Message "Preset parity closeout should require the preset docs readiness gate."
Assert-True -Condition ($presetCloseoutCommand.Contains("-FailOnNonMain")) -Message "Preset parity closeout should require the non-main evidence gate."
Assert-True -Condition ($presetCloseoutCommand.Contains("-FailOnUnpushedSource")) -Message "Preset parity closeout should require the source commit containment gate."
Assert-True -Condition ($presetCloseoutCommand.IndexOf("-FailOnCloseoutNotReady") -lt $presetCloseoutCommand.IndexOf("update README.md")) -Message "Preset parity docs should only be updated after closeout readiness gates."
Assert-True -Condition (@($roi.commands | Where-Object { $_.name -eq "manual-roi-known-target-final" }).Count -eq 1) -Message "ROI plan should include explicit manual ROI final command."
Assert-True -Condition (@($roi.commands | Where-Object { $_.name -eq "auto-face-roi-final" }).Count -eq 1) -Message "ROI plan should include explicit automatic ROI final command."
Assert-True -Condition (@($linear.commands | Where-Object { $_.name -eq "live-linear-breathing-setup" }).Count -eq 1) -Message "Linear plan should include explicit Breathing setup command."
Assert-True -Condition (@($linear.commands | Where-Object { $_.name -eq "live-linear-breathing-final" }).Count -eq 1) -Message "Linear plan should include explicit Breathing final command."
Assert-True -Condition (@($phase.commands | Where-Object { $_.name -eq "live-phase-fast-tremor-setup" }).Count -eq 1) -Message "Phase plan should include explicit Fast tremor setup command."
Assert-True -Condition (@($phase.commands | Where-Object { $_.name -eq "live-phase-fast-tremor-final" }).Count -eq 1) -Message "Phase plan should include explicit Fast tremor final command."

foreach ($group in $groups) {
    Assert-True -Condition (@($group.commands).Count -gt 0) -Message "Validation group '$($group.id)' should include command templates."
}

$allCommands = @($groups | ForEach-Object { $_.commands } | ForEach-Object { $_.command })
$allCommandNames = @($groups | ForEach-Object { $_.commands } | ForEach-Object { $_.name })
foreach ($expected in @(
    "-RequireEvidenceVerdict target_visible_unvalidated",
    "-RequireFinalVisualEvidence",
    "-RequireRoiMeasurement",
    "-RequireRendererDiagnostics",
    "-RequirePhaseDiagnostics",
    "-RequireCameraFps",
    "-RequireFocusedApp",
    "-RequireThermalReady",
    "docs/testing/MIT_PARITY_TARGETS.md"
)) {
    Assert-True -Condition (($allCommands -join "`n").Contains($expected)) -Message "Validation commands should include '$expected'."
}

$phaseCommandText = @($phase.commands | ForEach-Object { $_.command }) -join "`n"
Assert-True -Condition ($phaseCommandText.Contains("live-phase-object-final")) -Message "Phase commands should include explicit Object final label."
Assert-True -Condition ($phaseCommandText.Contains("live-phase-fast-tremor-final")) -Message "Phase commands should include explicit Fast tremor final label."
Assert-True -Condition ($phaseCommandText.Contains("fast tremor target")) -Message "Fast tremor command should carry a distinct target description."

$roiCommandText = @($roi.commands | ForEach-Object { $_.command }) -join "`n"
Assert-True -Condition ($roiCommandText.Contains("manual-roi-known-target-final")) -Message "ROI commands should include explicit manual final label."
Assert-True -Condition ($roiCommandText.Contains("auto-face-roi-final")) -Message "ROI commands should include explicit automatic final label."
$linearCommandText = @($linear.commands | ForEach-Object { $_.command }) -join "`n"
Assert-True -Condition ($linearCommandText.Contains("live-linear-breathing-final")) -Message "Linear commands should include explicit Breathing final label."
Assert-True -Condition ($linearCommandText.Contains("watched slow-motion edge or breathing target")) -Message "Breathing commands should carry a distinct target description."

foreach ($slot in @($closeout.slots)) {
    foreach ($commandName in @($slot.nextCommand -split "\s*,\s*then\s*|\s*,\s*")) {
        if ([string]::IsNullOrWhiteSpace($commandName)) {
            continue
        }
        Assert-True -Condition ($commandName -in $allCommandNames) -Message "Closeout slot '$($slot.id)' references unknown planner command '$commandName'."
    }
}

$capturePlanCommands = @($groups | ForEach-Object { $_.commands } | Where-Object { $_.command.StartsWith(".\tools\capture_live_validation_evidence.ps1") })
foreach ($command in $capturePlanCommands) {
    $isSetup = $command.name.EndsWith("-setup") -or $command.name.EndsWith("-target")
    $isFinal = $command.name.EndsWith("-final")
    if ($isSetup) {
        Assert-True -Condition ($command.command.Contains("-RequireEvidenceVerdict target_visible_unvalidated")) -Message "Setup command '$($command.name)' should stop at target_visible_unvalidated."
        Assert-True -Condition (-not $command.command.Contains("-RequireFinalVisualEvidence")) -Message "Setup command '$($command.name)' must not use final visual evidence gates."
    }
    if ($isFinal) {
        Assert-True -Condition ($command.command.Contains("-RequireFinalVisualEvidence")) -Message "Final command '$($command.name)' should use final visual evidence gates."
        Assert-True -Condition (-not $command.command.Contains("-RequireEvidenceVerdict target_visible_unvalidated")) -Message "Final command '$($command.name)' must not use the setup-only verdict gate."
        Assert-True -Condition ($command.command.Contains('-VisualValidated $true')) -Message "Final command '$($command.name)' should mark operator visual validation true."
    }
}

$captureScript = Join-Path $PSScriptRoot "capture_live_validation_evidence.ps1"
$captureParameters = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
$captureScriptContent = Get-Content -LiteralPath $captureScript -Raw
if ($captureScriptContent -notmatch '(?s)^param\((.*?)\)\s*\$ErrorActionPreference') {
    throw "Could not parse capture script param block."
}

foreach ($match in [regex]::Matches($Matches[1], '\$(\w+)')) {
    [void]$captureParameters.Add($match.Groups[1].Value)
}

$captureCommands = @($allCommands | Where-Object { $_.StartsWith(".\tools\capture_live_validation_evidence.ps1") })
Assert-True -Condition ($captureCommands.Count -ge 4) -Message "Expected capture command templates in the Pixel validation plan."

foreach ($command in $captureCommands) {
    $parseErrors = $null
    $tokens = [System.Management.Automation.PSParser]::Tokenize($command, [ref]$parseErrors)
    Assert-Equal -Actual @($parseErrors).Count -Expected 0 -Message "Capture command template should parse as PowerShell."

    $executable = @($tokens | Where-Object { $_.Type -eq "Command" } | Select-Object -First 1).Content
    Assert-Equal -Actual $executable -Expected ".\tools\capture_live_validation_evidence.ps1" -Message "Capture command should call the live validation capture script."

    foreach ($token in @($tokens | Where-Object { $_.Type -eq "CommandParameter" })) {
        $parameterName = $token.Content.TrimStart("-")
        Assert-True -Condition ($captureParameters.Contains($parameterName)) -Message "Unknown capture command parameter '$($token.Content)' in template: $command"
    }
}

Write-Output "Pixel validation plan self-test passed."
