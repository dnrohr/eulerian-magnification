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

function Invoke-PlanExitCode {
    param(
        [string]$EvidenceRoot,
        [string[]]$Slot = @(),
        [switch]$FailOnInvalidSlot,
        [switch]$FailOnEmptyQueue
    )

    $script = Join-Path $PSScriptRoot "show_next_pixel_validation_plan.ps1"
    $powerShellExe = (Get-Process -Id $PID).Path
    $arguments = @("-NoProfile", "-File", $script, "-EvidenceRoot", $EvidenceRoot)
    foreach ($slotId in $Slot) {
        $arguments += @("-Slot", $slotId)
    }
    if ($FailOnInvalidSlot) {
        $arguments += "-FailOnInvalidSlot"
    }
    if ($FailOnEmptyQueue) {
        $arguments += "-FailOnEmptyQueue"
    }

    & $powerShellExe @arguments *> $null
    return $LASTEXITCODE
}

$missingCloseoutRoot = Join-Path ([System.IO.Path]::GetTempPath()) "eulerian-missing-closeout-$([guid]::NewGuid().ToString('N'))"
$plan = & (Join-Path $PSScriptRoot "show_next_pixel_validation_plan.ps1") -EvidenceRoot $missingCloseoutRoot -Json | ConvertFrom-Json
$planText = & (Join-Path $PSScriptRoot "show_next_pixel_validation_plan.ps1") -EvidenceRoot $missingCloseoutRoot
$nextOnlyText = & (Join-Path $PSScriptRoot "show_next_pixel_validation_plan.ps1") -EvidenceRoot $missingCloseoutRoot -NextOnly
$pulseOnlyPlan = & (Join-Path $PSScriptRoot "show_next_pixel_validation_plan.ps1") -EvidenceRoot $missingCloseoutRoot -Slot pulseLinear -Json | ConvertFrom-Json
$pulseOnlyText = & (Join-Path $PSScriptRoot "show_next_pixel_validation_plan.ps1") -EvidenceRoot $missingCloseoutRoot -Slot pulseLinear -NextOnly
$setupOnlyPlan = & (Join-Path $PSScriptRoot "show_next_pixel_validation_plan.ps1") -EvidenceRoot $missingCloseoutRoot -CaptureStage Setup -Json | ConvertFrom-Json
$finalOnlyPlan = & (Join-Path $PSScriptRoot "show_next_pixel_validation_plan.ps1") -EvidenceRoot $missingCloseoutRoot -CaptureStage Final -Json | ConvertFrom-Json
$finalOnlyText = & (Join-Path $PSScriptRoot "show_next_pixel_validation_plan.ps1") -EvidenceRoot $missingCloseoutRoot -CaptureStage Final -NextOnly
$pulseFinalCommandsOnly = @(& (Join-Path $PSScriptRoot "show_next_pixel_validation_plan.ps1") -EvidenceRoot $missingCloseoutRoot -Slot pulseLinear -CaptureStage Final -CommandsOnly)
$invalidSlotPlan = & (Join-Path $PSScriptRoot "show_next_pixel_validation_plan.ps1") -EvidenceRoot $missingCloseoutRoot -Slot notARealSlot -Json | ConvertFrom-Json
$invalidSlotText = & (Join-Path $PSScriptRoot "show_next_pixel_validation_plan.ps1") -EvidenceRoot $missingCloseoutRoot -Slot notARealSlot -NextOnly
$validSlotExitCode = Invoke-PlanExitCode -EvidenceRoot $missingCloseoutRoot -Slot pulseLinear -FailOnInvalidSlot
$invalidSlotExitCode = Invoke-PlanExitCode -EvidenceRoot $missingCloseoutRoot -Slot notARealSlot -FailOnInvalidSlot
$emptyQueueExitCode = Invoke-PlanExitCode -EvidenceRoot $missingCloseoutRoot -Slot notARealSlot -FailOnEmptyQueue
$validQueueExitCode = Invoke-PlanExitCode -EvidenceRoot $missingCloseoutRoot -Slot pulseLinear -FailOnEmptyQueue
$savedPlanPath = Join-Path $missingCloseoutRoot "pixel_validation_plan.json"
$savedPlanText = & (Join-Path $PSScriptRoot "show_next_pixel_validation_plan.ps1") -EvidenceRoot $missingCloseoutRoot -OutputPath $savedPlanPath -NextOnly
$savedPlan = Get-Content -LiteralPath $savedPlanPath -Raw | ConvertFrom-Json
$closeout = & (Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1") -EvidenceRoot $missingCloseoutRoot -Json | ConvertFrom-Json

Assert-Equal -Actual $plan.roadmap.total -Expected 47 -Message "Roadmap total mismatch."
Assert-Equal -Actual $plan.roadmap.complete -Expected 41 -Message "Complete milestone count mismatch."
Assert-Equal -Actual $plan.roadmap.inProgress -Expected 6 -Message "In-progress milestone count mismatch."
Assert-Equal -Actual $plan.deviceSerial -Expected "47091JEKB05516" -Message "Pixel validation plan should default to the connected Pixel 8a serial."
Assert-Equal -Actual @($plan.missingMilestones).Count -Expected 0 -Message "Every in-progress milestone should have validation-plan coverage."
Assert-Equal -Actual $plan.currentCloseout.evidenceRoot -Expected $missingCloseoutRoot -Message "Validation plan should pass through the evidence root used for closeout."
Assert-Equal -Actual $plan.currentCloseout.acceptedFinalEvidenceCount -Expected 0 -Message "Missing evidence root should have no accepted final evidence."
Assert-Equal -Actual $plan.currentCloseout.allCloseoutEvidencePresent -Expected $false -Message "Missing evidence root should not have all closeout evidence."
Assert-Equal -Actual $plan.currentCloseout.allCloseoutEvidenceClean -Expected $false -Message "Missing evidence root should not be clean."
Assert-Equal -Actual $plan.currentCloseout.readyForPresetDocs -Expected $false -Message "Missing evidence root should not be ready for preset docs."
Assert-Equal -Actual $plan.currentCloseout.blockerCount -Expected 1 -Message "Missing evidence root should expose one closeout blocker."
Assert-Equal -Actual $plan.currentCloseout.blockers[0].kind -Expected "missingSlots" -Message "Missing evidence root should expose the missing-slots blocker."
Assert-Equal -Actual $plan.currentCloseout.blockers[0].count -Expected 6 -Message "Missing-slots blocker should include every closeout slot."
Assert-Equal -Actual @($plan.availableSlots).Count -Expected 6 -Message "Plan should expose every currently missing slot id."
Assert-True -Condition ("pulseLinear" -in @($plan.availableSlots)) -Message "Available slots should include pulseLinear."
Assert-Equal -Actual @($plan.invalidRequestedSlots).Count -Expected 0 -Message "Unfiltered plan should not report invalid requested slots."
Assert-Equal -Actual @($plan.recommendedCaptures).Count -Expected 6 -Message "Missing evidence root should recommend one capture sequence per closeout slot."
Assert-Equal -Actual $plan.recommendedCaptures[0].slot -Expected "manualRoi" -Message "First recommended capture should map to the manual ROI slot."
Assert-Equal -Actual $plan.recommendedCaptures[0].commands[0].name -Expected "manual-roi-known-target-setup" -Message "Recommended manual ROI capture should start with setup evidence."
Assert-Equal -Actual $plan.recommendedCaptures[0].commands[0].captureStage -Expected "Setup" -Message "Setup command should expose capture stage."
Assert-Equal -Actual $plan.recommendedCaptures[0].commands[1].name -Expected "manual-roi-known-target-final" -Message "Recommended manual ROI capture should end with final evidence."
Assert-Equal -Actual $plan.recommendedCaptures[0].commands[1].captureStage -Expected "Final" -Message "Final command should expose capture stage."
Assert-True -Condition (-not [string]::IsNullOrWhiteSpace($plan.recommendedCaptures[0].commands[0].command)) -Message "Recommended capture commands should include command templates."
Assert-True -Condition (($planText -join "`n").Contains("Current closeout blockers: 1")) -Message "Text plan should print current closeout blocker count."
Assert-True -Condition (($planText -join "`n").Contains("Next manualRoi: manual-roi-known-target-setup")) -Message "Text plan should print next commands from missing closeout slots."
Assert-True -Condition (($planText -join "`n").Contains("Recommended captures:")) -Message "Text plan should print recommended captures."
Assert-True -Condition (($nextOnlyText -join "`n").Contains("Recommended captures:")) -Message "NextOnly plan should print recommended captures."
Assert-True -Condition (-not (($nextOnlyText -join "`n").Contains("1. ROI mapping and device validation"))) -Message "NextOnly plan should omit the full validation group listing."
Assert-Equal -Actual @($pulseOnlyPlan.recommendedCaptures).Count -Expected 1 -Message "Slot-filtered plan should recommend only the requested slot."
Assert-Equal -Actual $pulseOnlyPlan.recommendedCaptures[0].slot -Expected "pulseLinear" -Message "Slot-filtered plan should keep the requested slot."
Assert-Equal -Actual $pulseOnlyPlan.recommendedCaptures[0].commands[0].name -Expected "live-linear-pulse-setup" -Message "Pulse slot should start with Pulse setup evidence."
Assert-Equal -Actual $pulseOnlyPlan.recommendedCaptures[0].commands[1].name -Expected "live-linear-pulse-final" -Message "Pulse slot should end with Pulse final evidence."
Assert-Equal -Actual @($pulseOnlyPlan.requestedSlots).Count -Expected 1 -Message "Slot-filtered plan should expose requested slots."
Assert-Equal -Actual $pulseOnlyPlan.requestedSlots[0] -Expected "pulseLinear" -Message "Slot-filtered plan should preserve the requested slot id."
Assert-Equal -Actual @($pulseOnlyPlan.invalidRequestedSlots).Count -Expected 0 -Message "Known slot filters should not be reported invalid."
Assert-True -Condition (($pulseOnlyText -join "`n").Contains("Pulse live linear visual parity")) -Message "Slot-filtered text should include the requested capture."
Assert-True -Condition (-not (($pulseOnlyText -join "`n").Contains("Manual ROI known-target alignment"))) -Message "Slot-filtered text should omit unrequested captures."
Assert-Equal -Actual $setupOnlyPlan.captureStage -Expected "Setup" -Message "Setup-only plan should expose the requested capture stage."
Assert-Equal -Actual @($setupOnlyPlan.recommendedCaptures).Count -Expected 6 -Message "Setup-only plan should preserve missing slot count."
Assert-Equal -Actual @($setupOnlyPlan.recommendedCaptures[0].commands).Count -Expected 1 -Message "Setup-only captures should include one command per slot."
Assert-Equal -Actual $setupOnlyPlan.recommendedCaptures[0].commands[0].captureStage -Expected "Setup" -Message "Setup-only commands should be setup stage."
Assert-Equal -Actual $finalOnlyPlan.captureStage -Expected "Final" -Message "Final-only plan should expose the requested capture stage."
Assert-Equal -Actual @($finalOnlyPlan.recommendedCaptures).Count -Expected 6 -Message "Final-only plan should preserve missing slot count."
Assert-Equal -Actual @($finalOnlyPlan.recommendedCaptures[0].commands).Count -Expected 1 -Message "Final-only captures should include one command per slot."
Assert-Equal -Actual $finalOnlyPlan.recommendedCaptures[0].commands[0].captureStage -Expected "Final" -Message "Final-only commands should be final stage."
Assert-True -Condition (($finalOnlyText -join "`n").Contains("Capture stage: Final")) -Message "Text plan should print requested capture stage."
Assert-True -Condition (-not (($finalOnlyText -join "`n").Contains("manual-roi-known-target-setup:"))) -Message "Final-only text should omit setup commands."
Assert-True -Condition (($finalOnlyText -join "`n").Contains("manual-roi-known-target-final:")) -Message "Final-only text should include final commands."
Assert-Equal -Actual @($pulseFinalCommandsOnly).Count -Expected 1 -Message "CommandsOnly should respect slot and stage filters."
Assert-True -Condition ($pulseFinalCommandsOnly[0].StartsWith(".\tools\capture_live_validation_evidence.ps1")) -Message "CommandsOnly output should contain command templates only."
Assert-True -Condition ($pulseFinalCommandsOnly[0].Contains('-DeviceSerial "47091JEKB05516"')) -Message "CommandsOnly output should target the Pixel 8a by serial."
Assert-True -Condition ($pulseFinalCommandsOnly[0].Contains('-RequireDeviceSerial "47091JEKB05516"')) -Message "CommandsOnly output should require evidence from the Pixel 8a serial."
Assert-True -Condition ($pulseFinalCommandsOnly[0].Contains('live-linear-pulse-final')) -Message "CommandsOnly output should include the requested final command."
Assert-True -Condition (-not ($pulseFinalCommandsOnly[0].Contains("Recommended captures"))) -Message "CommandsOnly output should omit headings."
Assert-Equal -Actual @($invalidSlotPlan.recommendedCaptures).Count -Expected 0 -Message "Unknown slot filter should not recommend captures."
Assert-Equal -Actual @($invalidSlotPlan.invalidRequestedSlots).Count -Expected 1 -Message "Unknown slot filter should be reported as invalid."
Assert-Equal -Actual $invalidSlotPlan.invalidRequestedSlots[0] -Expected "notARealSlot" -Message "Invalid slot report should preserve the requested slot id."
Assert-True -Condition (($invalidSlotText -join "`n").Contains("Available missing slots:")) -Message "Invalid slot text should print available slots."
Assert-True -Condition (($invalidSlotText -join "`n").Contains("Warning: requested slot(s) not currently missing or unknown: notARealSlot")) -Message "Invalid slot text should warn about unknown slots."
Assert-True -Condition (($invalidSlotText -join "`n").Contains("Warning: no recommended captures match the current filters.")) -Message "Empty filtered plan should warn when no captures match."
Assert-Equal -Actual $validSlotExitCode -Expected 0 -Message "FailOnInvalidSlot should allow known missing slot filters."
Assert-Equal -Actual $invalidSlotExitCode -Expected 21 -Message "FailOnInvalidSlot should fail with exit code 21 for unknown slot filters."
Assert-Equal -Actual $validQueueExitCode -Expected 0 -Message "FailOnEmptyQueue should allow filters with recommended captures."
Assert-Equal -Actual $emptyQueueExitCode -Expected 22 -Message "FailOnEmptyQueue should fail with exit code 22 when no captures match."
Assert-True -Condition (Test-Path -LiteralPath $savedPlanPath) -Message "OutputPath should write a Pixel validation plan JSON artifact."
Assert-Equal -Actual $savedPlan.currentCloseout.evidenceRoot -Expected $missingCloseoutRoot -Message "Saved plan artifact should use the requested evidence root."
Assert-Equal -Actual @($savedPlan.recommendedCaptures).Count -Expected 6 -Message "Saved plan artifact should preserve recommended captures."
Assert-True -Condition (($savedPlanText -join "`n").Contains("Recommended captures:")) -Message "OutputPath should not suppress text output."

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
Assert-True -Condition ($presetCloseoutCommand.Contains("-FailOnMissingArtifactHashes")) -Message "Preset parity closeout should require artifact hashes for accepted evidence."
Assert-True -Condition ($presetCloseoutCommand.Contains("-FailOnNonFinalLabel")) -Message "Preset parity closeout should require final capture labels for accepted evidence."
Assert-True -Condition ($presetCloseoutCommand.Contains("-FailOnWrongSlotLabel")) -Message "Preset parity closeout should require final labels to match closeout slots."
Assert-True -Condition ($presetCloseoutCommand.Contains("-FailOnMissingOperatorNotes")) -Message "Preset parity closeout should require operator notes for accepted evidence."
Assert-True -Condition ($presetCloseoutCommand.Contains("-FailOnMissingVisualReviewText")) -Message "Preset parity closeout should require target description and visual claim for accepted evidence."
Assert-True -Condition ($presetCloseoutCommand.Contains("-OutputPath")) -Message "Preset parity closeout should write a reusable closeout summary artifact."
Assert-True -Condition ($presetCloseoutCommand.Contains("pixel_closeout_summary.json")) -Message "Preset parity closeout should name the reusable closeout summary artifact."
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
    Assert-True -Condition (-not [string]::IsNullOrWhiteSpace($slot.expectedFinalLabel)) -Message "Closeout slot '$($slot.id)' should expose an expected final label."
    Assert-True -Condition ($slot.expectedFinalLabel -in $allCommandNames) -Message "Closeout slot '$($slot.id)' expected final label '$($slot.expectedFinalLabel)' should map to a planner command."
    foreach ($commandName in @($slot.nextCommand -split "\s*,\s*then\s*|\s*,\s*")) {
        if ([string]::IsNullOrWhiteSpace($commandName)) {
            continue
        }
        Assert-True -Condition ($commandName -in $allCommandNames) -Message "Closeout slot '$($slot.id)' references unknown planner command '$commandName'."
    }
}

$capturePlanCommands = @($groups | ForEach-Object { $_.commands } | Where-Object { $_.command.StartsWith(".\tools\capture_live_validation_evidence.ps1") })
foreach ($command in $capturePlanCommands) {
    Assert-True -Condition ($command.command.Contains('-DeviceSerial "47091JEKB05516"')) -Message "Capture command '$($command.name)' should target the Pixel 8a by serial."
    Assert-True -Condition ($command.command.Contains('-RequireDeviceSerial "47091JEKB05516"')) -Message "Capture command '$($command.name)' should require summaries from the Pixel 8a serial."
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
