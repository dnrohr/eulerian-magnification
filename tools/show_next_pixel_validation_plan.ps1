param(
    [string]$EvidenceRoot = "sample-videos\exports\live-validation",
    [string]$DeviceSerial = "47091JEKB05516",
    [string[]]$Slot = @(),
    [ValidateSet("All", "Setup", "Final")]
    [string]$CaptureStage = "All",
    [switch]$NextOnly,
    [switch]$CommandsOnly,
    [switch]$AllowOperatorCommands,
    [switch]$AllowFinalCommands,
    [switch]$FailOnInvalidSlot,
    [switch]$FailOnEmptyQueue,
    [string]$OutputPath = "",
    [switch]$Json
)

$ErrorActionPreference = "Stop"

$roadmap = (& (Join-Path $PSScriptRoot "summarize_roadmap_status.ps1") -Json | ConvertFrom-Json)
$closeout = (& (Join-Path $PSScriptRoot "summarize_pixel_validation_closeout.ps1") -EvidenceRoot $EvidenceRoot -Json | ConvertFrom-Json)
$setupThermalReadyBelowStatus = 3
$finalThermalReadyBelowStatus = 2

$validationGroups = @(
    [pscustomobject]@{
        order = 1
        id = "roi-mapping"
        title = "ROI mapping and device validation"
        milestones = @("M", "U")
        protocol = "docs/testing/ROI_DEVICE_VALIDATION.md"
        setupEvidence = "Manual known target and automatic visible face/skin setup captures should stop at target_visible_unvalidated."
        finalEvidence = "Final runs require -RequireRoiMeasurement plus -RequireFinalVisualEvidence from a clean source tree whose commit is reachable from origin/main."
        closes = @(
            "portrait/front-camera ROI mapping confidence",
            "manual ROI known-target alignment",
            "automatic face/skin ROI alignment"
        )
        commands = @(
            [pscustomobject]@{
                name = "manual-roi-known-target-setup"
                purpose = "Pre-inspection manual ROI alignment capture."
                command = ".\tools\capture_live_validation_evidence.ps1 -Label `"manual-roi-known-target`" -WaitForThermalReady -ThermalReadyBelowStatus 4 -ThermalReadySamples 2 -ThermalReadyTimeoutSeconds 900 -ThermalReadyPollSeconds 30 -Mode Tremor -View Raw -RoiSource Manual -ManualRoi `"0.25,0.25,0.75,0.75`" -GlPreview `$true -Controls `$false -Clean `$true -ScreenRecordSeconds 10 -RequireScreenrecord -RequireThermalReady -RequireCameraFps -RequireFocusedApp -RequireEvidenceVerdict target_visible_unvalidated -TargetDescription `"known high-contrast target inside manually selected ROI`" -VisualClaim `"Manual ROI outline overlaps the same visible target that was selected`" -TargetVisible `$true -VisualValidated `$false -OperatorNotes `"Set VisualValidated true only after inspecting the screenshot/recording and deriving target bounds for the final ROI measurement.`" -Summarize"
            },
            [pscustomobject]@{
                name = "auto-face-roi-setup"
                purpose = "Pre-inspection automatic face/skin ROI alignment capture."
                command = ".\tools\capture_live_validation_evidence.ps1 -Label `"auto-face-roi`" -WaitForThermalReady -ThermalReadyBelowStatus 4 -ThermalReadySamples 2 -ThermalReadyTimeoutSeconds 900 -ThermalReadyPollSeconds 30 -Mode Pulse -RoiSource Auto -GlPreview `$true -Controls `$false -Clean `$true -ScreenRecordSeconds 10 -RequireScreenrecord -RequireThermalReady -RequireCameraFps -RequireFocusedApp -RequireEvidenceVerdict target_visible_unvalidated -TargetDescription `"visible face or skin target tracked by automatic ROI`" -VisualClaim `"Automatic ROI outline overlaps the visible face or skin region being tracked`" -TargetVisible `$true -VisualValidated `$false -OperatorNotes `"Set VisualValidated true only after the automatic ROI is inspected and face/skin target bounds are derived for the final ROI measurement.`" -Summarize"
            },
            [pscustomobject]@{
                name = "manual-roi-known-target-final"
                purpose = "Closing manual ROI alignment evidence after inspection."
                command = ".\tools\capture_live_validation_evidence.ps1 -Label `"manual-roi-known-target-final`" -WaitForThermalReady -ThermalReadyBelowStatus 4 -ThermalReadySamples 2 -ThermalReadyTimeoutSeconds 900 -ThermalReadyPollSeconds 30 -Mode Tremor -View Raw -RoiSource Manual -ManualRoi `"0.25,0.25,0.75,0.75`" -GlPreview `$true -Controls `$false -Clean `$true -MeasureRoiExpected `"<visible-target-bounds-in-screenshot-space>`" -MeasureRoiKind Manual -RequireRoiMeasurement -ScreenRecordSeconds 10 -RequireScreenrecord -RequireThermalReady -RequireCameraFps -RequireFocusedApp -TargetDescription `"known high-contrast target inside manually selected ROI`" -VisualClaim `"Manual ROI outline overlaps the same visible target that was selected`" -TargetVisible `$true -VisualValidated `$true -OperatorNotes `"Accepted only if the screenshot/recording and measurement JSON show one manual ROI outline aligned to the visible target.`" -RequireFinalVisualEvidence -Summarize"
            },
            [pscustomobject]@{
                name = "auto-face-roi-final"
                purpose = "Closing automatic face/skin ROI alignment evidence after inspection."
                command = ".\tools\capture_live_validation_evidence.ps1 -Label `"auto-face-roi-final`" -WaitForThermalReady -ThermalReadyBelowStatus 4 -ThermalReadySamples 2 -ThermalReadyTimeoutSeconds 900 -ThermalReadyPollSeconds 30 -Mode Pulse -RoiSource Auto -GlPreview `$true -Controls `$false -Clean `$true -MeasureRoiExpected `"<visible-face-or-skin-target-bounds-in-screenshot-space>`" -MeasureRoiKind Auto -RequireRoiMeasurement -ScreenRecordSeconds 10 -RequireScreenrecord -RequireThermalReady -RequireCameraFps -RequireFocusedApp -TargetDescription `"visible face or skin target tracked by automatic ROI`" -VisualClaim `"Automatic ROI outline overlaps the visible face or skin region being tracked`" -TargetVisible `$true -VisualValidated `$true -OperatorNotes `"Accepted only if the screenshot/recording and measurement JSON show one automatic ROI outline aligned to the visible face or skin target, not center or frozen fallback.`" -RequireFinalVisualEvidence -Summarize"
            }
        )
    },
    [pscustomobject]@{
        order = 2
        id = "live-linear"
        title = "Live full-frame linear EVM"
        milestones = @("AE", "AP")
        protocol = "docs/experiments/pixel8a_live_linear_validation.md"
        setupEvidence = "Pulse and Breathing setup captures should show renderer diagnostics and stop at target_visible_unvalidated."
        finalEvidence = "Final Pulse and Breathing runs require -RequireRendererDiagnostics, explicit runtime gates, and -RequireFinalVisualEvidence from a clean source tree whose commit is reachable from origin/main."
        closes = @(
            "portrait full-frame live EVM preview validation",
            "live linear reconstruction visual evidence",
            "Pulse and Breathing preset visual parity inputs"
        )
        commands = @(
            [pscustomobject]@{
                name = "live-linear-pulse-setup"
                purpose = "Pre-inspection Pulse full-frame live linear capture."
                command = ".\tools\capture_live_validation_evidence.ps1 -Label `"live-linear-pulse-setup`" -WaitForThermalReady -ThermalReadyBelowStatus 4 -ThermalReadySamples 2 -ThermalReadyTimeoutSeconds 900 -ThermalReadyPollSeconds 30 -Mode Pulse -View Split -RoiSource FullFrame -GlPreview `$true -Controls `$true -Panel Debug -ScreenRecordSeconds 15 -RequireScreenrecord -RequireThermalReady -RequireCameraFps -RequireFocusedApp -RequireRendererDiagnostics -RequireEvidenceVerdict target_visible_unvalidated -RequireUiText `"Preview: Full-frame linear EVM preview`",`"Renderer: Live linear EVM reconstruction`",`"GL renderer: Live reconstruction`" -TargetDescription `"watched pulse target under steady lighting`" -VisualClaim `"Live Pulse Split view shows full-frame linear reconstruction rather than ROI tint`" -TargetVisible `$true -VisualValidated `$false -OperatorNotes `"Set VisualValidated true only after inspecting the recording against the pass criteria.`" -Summarize"
            },
            [pscustomobject]@{
                name = "live-linear-pulse-final"
                purpose = "Closing Pulse full-frame live linear evidence after inspection."
                command = ".\tools\capture_live_validation_evidence.ps1 -Label `"live-linear-pulse-final`" -WaitForThermalReady -ThermalReadyBelowStatus 4 -ThermalReadySamples 2 -ThermalReadyTimeoutSeconds 900 -ThermalReadyPollSeconds 30 -Mode Pulse -View Split -RoiSource FullFrame -GlPreview `$true -Controls `$false -ScreenRecordSeconds 15 -RequireScreenrecord -RequireThermalReady -RequireCameraFps -RequireFocusedApp -RequireRendererDiagnostics -RequireUiText `"Preview: Full-frame linear EVM preview`",`"Renderer: Live linear EVM reconstruction`",`"GL renderer: Live reconstruction`" -TargetDescription `"watched pulse target under steady lighting`" -VisualClaim `"Live Pulse Split view shows accepted full-frame linear reconstruction rather than ROI tint`" -TargetVisible `$true -VisualValidated `$true -OperatorNotes `"Accepted only if the recording shows target-visible magnification without ROI-only tint or full-frame flashing.`" -RequireFinalVisualEvidence -Summarize"
            },
            [pscustomobject]@{
                name = "live-linear-breathing-setup"
                purpose = "Pre-inspection Breathing full-frame live linear capture."
                command = ".\tools\capture_live_validation_evidence.ps1 -Label `"live-linear-breathing-setup`" -WaitForThermalReady -ThermalReadyBelowStatus 4 -ThermalReadySamples 2 -ThermalReadyTimeoutSeconds 900 -ThermalReadyPollSeconds 30 -Mode Breathing -View Split -RoiSource FullFrame -GlPreview `$true -Controls `$true -Panel Debug -ScreenRecordSeconds 15 -RequireScreenrecord -RequireThermalReady -RequireCameraFps -RequireFocusedApp -RequireRendererDiagnostics -RequireEvidenceVerdict target_visible_unvalidated -RequireUiText `"Preview: Full-frame linear EVM preview`",`"Renderer: Live linear EVM reconstruction`",`"GL renderer: Live reconstruction`" -TargetDescription `"watched slow-motion edge or breathing target`" -VisualClaim `"Live Breathing Split view shows full-frame reconstructed motion on the watched target rather than ROI tint`" -TargetVisible `$true -VisualValidated `$false -OperatorNotes `"Set VisualValidated true only after inspecting the recording against the Breathing pass criteria.`" -Summarize"
            },
            [pscustomobject]@{
                name = "live-linear-breathing-final"
                purpose = "Closing Breathing full-frame live linear evidence after inspection."
                command = ".\tools\capture_live_validation_evidence.ps1 -Label `"live-linear-breathing-final`" -WaitForThermalReady -ThermalReadyBelowStatus 4 -ThermalReadySamples 2 -ThermalReadyTimeoutSeconds 900 -ThermalReadyPollSeconds 30 -Mode Breathing -View Split -RoiSource FullFrame -GlPreview `$true -Controls `$false -ScreenRecordSeconds 15 -RequireScreenrecord -RequireThermalReady -RequireCameraFps -RequireFocusedApp -RequireRendererDiagnostics -RequireUiText `"Preview: Full-frame linear EVM preview`",`"Renderer: Live linear EVM reconstruction`",`"GL renderer: Live reconstruction`" -TargetDescription `"watched slow-motion edge or breathing target`" -VisualClaim `"Live Breathing Split view shows accepted full-frame reconstructed motion on the watched target rather than ROI tint`" -TargetVisible `$true -VisualValidated `$true -OperatorNotes `"Accepted only if the recording shows target-visible Breathing motion magnification without ROI-only tint, whole-frame flashing, or a frozen camera stream.`" -RequireFinalVisualEvidence -Summarize"
            }
        )
    },
    [pscustomobject]@{
        order = 3
        id = "live-phase"
        title = "Live phase motion EVM"
        milestones = @("AR")
        protocol = "docs/experiments/pixel8a_live_phase_validation.md"
        setupEvidence = "Controlled high-contrast moving-edge setup should stop at target_visible_unvalidated."
        finalEvidence = "Final Motion/Fast tremor runs require -RequirePhaseDiagnostics, explicit runtime gates, and -RequireFinalVisualEvidence from a clean source tree whose commit is reachable from origin/main."
        closes = @(
            "controlled object-motion live phase validation",
            "Object vibration and Fast tremor preset visual parity inputs"
        )
        commands = @(
            [pscustomobject]@{
                name = "live-phase-object-setup"
                purpose = "Pre-inspection controlled motion phase capture."
                command = ".\tools\capture_live_validation_evidence.ps1 -Label `"live-phase-object`" -WaitForThermalReady -ThermalReadyBelowStatus 4 -ThermalReadySamples 2 -ThermalReadyTimeoutSeconds 900 -ThermalReadyPollSeconds 30 -Mode Tremor -View Split -RoiSource Manual -ManualRoi `"0.25,0.25,0.75,0.75`" -GlPreview `$true -Controls `$true -Panel Debug -ScreenRecordSeconds 15 -RequireScreenrecord -RequireThermalReady -RequireCameraFps -RequireFocusedApp -RequirePhaseDiagnostics -RequireEvidenceVerdict target_visible_unvalidated -RequireUiText `"Renderer: Live phase motion`",`"GL renderer: Live phase motion`",`"phase:`" -TargetDescription `"high-contrast edge or small object moving subtly inside manual ROI`" -VisualClaim `"Live phase Split view shows edge-localized amplified motion inside the manual ROI`" -TargetVisible `$true -VisualValidated `$false -OperatorNotes `"Set VisualValidated true only after inspecting the recording against the pass criteria.`" -Summarize"
            },
            [pscustomobject]@{
                name = "live-phase-object-final"
                purpose = "Closing controlled motion phase evidence after inspection."
                command = ".\tools\capture_live_validation_evidence.ps1 -Label `"live-phase-object-final`" -WaitForThermalReady -ThermalReadyBelowStatus 4 -ThermalReadySamples 2 -ThermalReadyTimeoutSeconds 900 -ThermalReadyPollSeconds 30 -Mode Tremor -View Split -RoiSource Manual -ManualRoi `"0.25,0.25,0.75,0.75`" -GlPreview `$true -Controls `$false -ScreenRecordSeconds 15 -RequireScreenrecord -RequireThermalReady -RequireCameraFps -RequireFocusedApp -RequirePhaseDiagnostics -RequireUiText `"Renderer: Live phase motion`",`"GL renderer: Live phase motion`",`"phase:`" -TargetDescription `"high-contrast edge or small object moving subtly inside manual ROI`" -VisualClaim `"Live phase Split view shows edge-localized amplified motion inside the manual ROI`" -TargetVisible `$true -VisualValidated `$true -OperatorNotes `"Accepted only if the recording shows edge-localized amplified motion, not uniform ROI flashing.`" -RequireFinalVisualEvidence -Summarize"
            },
            [pscustomobject]@{
                name = "live-phase-fast-tremor-setup"
                purpose = "Pre-inspection Fast tremor phase capture."
                command = ".\tools\capture_live_validation_evidence.ps1 -Label `"live-phase-fast-tremor-setup`" -WaitForThermalReady -ThermalReadyBelowStatus 4 -ThermalReadySamples 2 -ThermalReadyTimeoutSeconds 900 -ThermalReadyPollSeconds 30 -Mode Tremor -View Split -RoiSource Manual -ManualRoi `"0.25,0.25,0.75,0.75`" -GlPreview `$true -Controls `$true -Panel Debug -ScreenRecordSeconds 15 -RequireScreenrecord -RequireThermalReady -RequireCameraFps -RequireFocusedApp -RequirePhaseDiagnostics -RequireEvidenceVerdict target_visible_unvalidated -RequireUiText `"Renderer: Live phase motion`",`"GL renderer: Live phase motion`",`"phase:`" -TargetDescription `"fast tremor target with a high-contrast edge inside manual ROI`" -VisualClaim `"Live phase Split view shows edge-localized amplified fast tremor without uniform ROI flashing`" -TargetVisible `$true -VisualValidated `$false -OperatorNotes `"Set VisualValidated true only after inspecting the recording against the Fast tremor pass criteria.`" -Summarize"
            },
            [pscustomobject]@{
                name = "live-phase-fast-tremor-final"
                purpose = "Closing Fast tremor phase evidence after inspection."
                command = ".\tools\capture_live_validation_evidence.ps1 -Label `"live-phase-fast-tremor-final`" -WaitForThermalReady -ThermalReadyBelowStatus 4 -ThermalReadySamples 2 -ThermalReadyTimeoutSeconds 900 -ThermalReadyPollSeconds 30 -Mode Tremor -View Split -RoiSource Manual -ManualRoi `"0.25,0.25,0.75,0.75`" -GlPreview `$true -Controls `$false -ScreenRecordSeconds 15 -RequireScreenrecord -RequireThermalReady -RequireCameraFps -RequireFocusedApp -RequirePhaseDiagnostics -RequireUiText `"Renderer: Live phase motion`",`"GL renderer: Live phase motion`",`"phase:`" -TargetDescription `"fast tremor target with a high-contrast edge inside manual ROI`" -VisualClaim `"Live phase Split view shows accepted edge-localized amplified fast tremor without uniform ROI flashing`" -TargetVisible `$true -VisualValidated `$true -OperatorNotes `"Accepted only if the recording shows edge-localized amplified fast tremor, not uniform ROI flashing or color-only change.`" -RequireFinalVisualEvidence -Summarize"
            }
        )
    },
    [pscustomobject]@{
        order = 4
        id = "preset-parity"
        title = "Preset parity acceptance"
        milestones = @("AT")
        protocol = "docs/testing/MIT_PARITY_TARGETS.md"
        setupEvidence = "Review accepted Pulse, Breathing, Object vibration, and Fast tremor bundles against preset pass criteria."
        finalEvidence = "Update README and parity docs only after all watched visual artifacts are accepted."
        closes = @(
            "AT watched visual parity checklist",
            "README and parity-table visual validation status"
        )
        commands = @(
            [pscustomobject]@{
                name = "preset-parity-closeout"
                purpose = "Documentation closeout after accepted visual artifacts exist."
                command = "Run .\tools\summarize_pixel_validation_closeout.ps1 -OutputPath sample-videos\exports\live-validation\pixel_closeout_summary.json -FailOnNonMain -FailOnUnpushedSource -FailOnMissingArtifactHashes -FailOnNonFinalLabel -FailOnWrongSlotLabel -FailOnMissingOperatorNotes -FailOnMissingVisualReviewText -FailOnWrongDeviceSerial -FailOnReviewContactSheetIssues, .\tools\summarize_pixel_validation_closeout.ps1 -OutputPath sample-videos\exports\live-validation\pixel_closeout_summary.json -FailOnCloseoutNotReady, and .\tools\summarize_pixel_validation_closeout.ps1 -OutputPath sample-videos\exports\live-validation\pixel_closeout_summary.json -FailOnPresetDocsNotReady. If all pass, update README.md and docs/testing/MIT_PARITY_TARGETS.md with visual-validation status and artifact notes from pixel_closeout_summary.json, then rerun .\tools\test_offline_project_tooling.ps1."
            }
        )
    }
)

if (-not [string]::IsNullOrWhiteSpace($DeviceSerial)) {
    $escapedDeviceSerial = $DeviceSerial -replace '"', '\"'
    $captureCommandPrefix = ".\tools\capture_live_validation_evidence.ps1"
    $deviceSerialArguments = " -DeviceSerial `"$escapedDeviceSerial`" -RequireDeviceSerial `"$escapedDeviceSerial`""
    foreach ($group in $validationGroups) {
        foreach ($command in @($group.commands)) {
            if ($command.command.StartsWith($captureCommandPrefix)) {
                $command.command = "$captureCommandPrefix$deviceSerialArguments$($command.command.Substring($captureCommandPrefix.Length))"
            }
        }
    }
}
foreach ($group in $validationGroups) {
    foreach ($command in @($group.commands)) {
        if ($command.command.StartsWith(".\tools\capture_live_validation_evidence.ps1")) {
            $thermalReadyBelowStatus = if ($command.name.EndsWith("-final")) {
                $finalThermalReadyBelowStatus
            } else {
                $setupThermalReadyBelowStatus
            }
            $command.command = $command.command -replace "-ThermalReadyBelowStatus 4", "-ThermalReadyBelowStatus $thermalReadyBelowStatus"
        }
    }
}

$thermalReadinessOutputPath = Join-Path $EvidenceRoot "thermal_ready_wait_preflight.json"
$thermalReadinessCommandParts = @(
    ".\tools\wait_for_device_thermal_ready.ps1",
    "-ReadyBelowThermalStatus $finalThermalReadyBelowStatus",
    "-RequiredReadySamples 2",
    "-TimeoutSeconds 900",
    "-PollSeconds 30",
    ('-OutputPath "{0}"' -f $thermalReadinessOutputPath)
)
if (-not [string]::IsNullOrWhiteSpace($DeviceSerial)) {
    $escapedDeviceSerial = $DeviceSerial -replace '"', '\"'
    $thermalReadinessCommandParts = @($thermalReadinessCommandParts[0], "-DeviceSerial `"$escapedDeviceSerial`"") + @($thermalReadinessCommandParts | Select-Object -Skip 1)
}
$thermalReadiness = [pscustomobject]@{
    readyBelowThermalStatus = $finalThermalReadyBelowStatus
    requiredReadySamples = 2
    timeoutSeconds = 900
    pollSeconds = 30
    outputPath = $thermalReadinessOutputPath
    command = ($thermalReadinessCommandParts -join " ")
    note = "Run before a watched final validation session when the device may be warm or the camera preview appears slow; status must be below moderate so final no-warning captures are not blocked by thermal warnings."
}

$inProgressMilestones = @($roadmap.inProgress | ForEach-Object { $_.milestone })
$coveredMilestones = @($validationGroups | ForEach-Object { $_.milestones } | Select-Object -Unique)
$missingMilestones = @($inProgressMilestones | Where-Object { $_ -notin $coveredMilestones })
$commandLookup = @{}
foreach ($group in $validationGroups) {
    foreach ($command in @($group.commands)) {
        $stage = if ($command.name.EndsWith("-final")) { "Final" } else { "Setup" }
        $isCaptureCommand = $command.command.StartsWith(".\tools\capture_live_validation_evidence.ps1")
        $commandLookup[$command.name] = [pscustomobject]@{
            groupId = $group.id
            groupTitle = $group.title
            protocol = $group.protocol
            name = $command.name
            captureStage = $stage
            operatorRequired = $isCaptureCommand
            finalVisualAcceptanceRequired = ($isCaptureCommand -and $stage -eq "Final")
            purpose = $command.purpose
            command = $command.command
        }
    }
}

$slotGuidance = @{
    manualRoi = [pscustomobject]@{
        operatorSetup = @(
            "Use a stable, high-contrast target with clear rectangular bounds.",
            "Place the manual ROI once, then keep the phone and target still for the capture.",
            "Use tools\convert_roi_bounds_to_normalized.ps1 to convert visible target pixel bounds from the setup screenshot into MeasureRoiExpected."
        )
        acceptanceChecks = @(
            "Exactly one manual ROI outline is visible.",
            "The ROI outline overlaps the selected target bounds.",
            "The capture has valid screenrecord, thermal-ready, camera-FPS, focused-app, and ROI-measurement gates."
        )
    }
    autoRoi = [pscustomobject]@{
        operatorSetup = @(
            "Use a visible face or skin target under steady lighting.",
            "Leave ROI Source on Auto ROI and avoid manually dragging the ROI.",
            "Use tools\convert_roi_bounds_to_normalized.ps1 to convert visible face or skin pixel bounds from the setup screenshot into MeasureRoiExpected."
        )
        acceptanceChecks = @(
            "Exactly one automatic ROI outline is visible.",
            "The ROI outline follows the visible face or skin target, not the center fallback.",
            "The capture has valid screenrecord, thermal-ready, camera-FPS, focused-app, and ROI-measurement gates."
        )
    }
    pulseLinear = [pscustomobject]@{
        operatorSetup = @(
            "Use a steady pulse/skin-color target with fixed lighting and minimal camera motion.",
            "Run Pulse in Split view with Full frame ROI and GL preview.",
            "Let the preview settle after thermal readiness before judging the effect."
        )
        acceptanceChecks = @(
            "Split view shows full-frame live linear reconstruction, not an ROI-only tint.",
            "The target-visible change is localized enough to read as magnification, not whole-frame flashing.",
            "Renderer diagnostics, valid screenrecord, thermal-ready, camera-FPS, and focused-app gates are present."
        )
    }
    breathingLinear = [pscustomobject]@{
        operatorSetup = @(
            "Use a slow moving edge, torso, or breathing target with the phone held still.",
            "Run Breathing in Split view with Full frame ROI and GL preview.",
            "Avoid judging while the preview is thermally throttled or visibly frozen."
        )
        acceptanceChecks = @(
            "Split view shows reconstructed motion on the watched target rather than ROI-only tint.",
            "The recording does not show whole-frame flashing or a frozen camera stream.",
            "Renderer diagnostics, valid screenrecord, thermal-ready, camera-FPS, and focused-app gates are present."
        )
    }
    objectPhase = [pscustomobject]@{
        operatorSetup = @(
            "Use a high-contrast edge or small object moving subtly inside the manual ROI.",
            "Run Tremor in Split view with Manual ROI and GL preview.",
            "Keep the manual ROI fixed around the moving edge for the full capture."
        )
        acceptanceChecks = @(
            "Split view shows edge-localized amplified motion inside the ROI.",
            "The result is not uniform ROI flashing or a color-only change.",
            "Phase diagnostics, valid screenrecord, thermal-ready, camera-FPS, and focused-app gates are present."
        )
    }
    fastTremorPhase = [pscustomobject]@{
        operatorSetup = @(
            "Use a fast tremor target with a high-contrast edge inside the manual ROI.",
            "Run Tremor in Split view with Manual ROI and GL preview.",
            "Keep lighting steady so fast motion is not confused with flicker."
        )
        acceptanceChecks = @(
            "Split view shows edge-localized amplified fast tremor.",
            "The result is not uniform ROI flashing, whole-frame flashing, or color-only change.",
            "Phase diagnostics, valid screenrecord, thermal-ready, camera-FPS, and focused-app gates are present."
        )
    }
}

$recommendedCaptures = @()
$missingSlotBlocker = @($closeout.closeoutBlockers | Where-Object { $_.kind -eq "missingSlots" } | Select-Object -First 1)
$availableSlots = @($missingSlotBlocker.items | ForEach-Object { $_.id })
$requestedSlots = @($Slot | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
$invalidRequestedSlots = @($requestedSlots | Where-Object { $_ -notin $availableSlots })
$invalidSlotExitCode = 21
$emptyQueueExitCode = 22
$requestedStage = $CaptureStage
foreach ($missingSlot in @($missingSlotBlocker.items)) {
    if ($requestedSlots.Count -gt 0 -and $missingSlot.id -notin $requestedSlots) {
        continue
    }
    $commandNames = @($missingSlot.nextCommand -split "\s*,\s*then\s*|\s*,\s*" | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    $commands = @($commandNames | ForEach-Object {
        if ($commandLookup.ContainsKey($_)) {
            $commandLookup[$_]
        } else {
            [pscustomobject]@{
                groupId = $null
                groupTitle = $null
                protocol = $missingSlot.protocol
                name = $_
                captureStage = $null
                operatorRequired = $false
                finalVisualAcceptanceRequired = $false
                purpose = "Missing command template."
                command = $null
            }
        }
    })
    if ($requestedStage -ne "All") {
        $commands = @($commands | Where-Object { $_.captureStage -eq $requestedStage })
    }
    if ($commands.Count -eq 0) {
        continue
    }
    $guidance = if ($slotGuidance.ContainsKey($missingSlot.id)) {
        $slotGuidance[$missingSlot.id]
    } else {
        [pscustomobject]@{
            operatorSetup = @()
            acceptanceChecks = @()
        }
    }
    $recommendedCaptures += [pscustomobject]@{
        slot = $missingSlot.id
        title = $missingSlot.title
        milestones = $missingSlot.milestones
        expectedFinalLabel = $missingSlot.expectedFinalLabel
        protocol = $missingSlot.protocol
        operatorSetup = @($guidance.operatorSetup)
        acceptanceChecks = @($guidance.acceptanceChecks)
        commands = $commands
    }
}

$result = [pscustomobject]@{
    roadmap = [pscustomobject]@{
        total = $roadmap.total
        complete = $roadmap.statusCounts.Complete
        inProgress = $roadmap.statusCounts.'In Progress'
    }
    coveredMilestones = $coveredMilestones
    missingMilestones = $missingMilestones
    currentCloseout = [pscustomobject]@{
        evidenceRoot = $closeout.evidenceRoot
        acceptedFinalEvidenceCount = $closeout.acceptedFinalEvidenceCount
        allCloseoutEvidencePresent = $closeout.allCloseoutEvidencePresent
        allCloseoutEvidenceClean = $closeout.allCloseoutEvidenceClean
        readyForPresetDocs = $closeout.readyForPresetDocs
        blockerCount = @($closeout.closeoutBlockers).Count
        blockers = $closeout.closeoutBlockers
    }
    availableSlots = $availableSlots
    requestedSlots = $requestedSlots
    invalidRequestedSlots = $invalidRequestedSlots
    captureStage = $requestedStage
    deviceSerial = $DeviceSerial
    thermalReadiness = $thermalReadiness
    closeoutBlockerCount = @($closeout.closeoutBlockers).Count
    recommendedCaptureCount = @($recommendedCaptures).Count
    commandCount = @($recommendedCaptures | ForEach-Object { $_.commands } | Where-Object { -not [string]::IsNullOrWhiteSpace($_.command) }).Count
    operatorRequiredCommandCount = @($recommendedCaptures | ForEach-Object { $_.commands } | Where-Object { $_.operatorRequired }).Count
    finalVisualAcceptanceCommandCount = @($recommendedCaptures | ForEach-Object { $_.commands } | Where-Object { $_.finalVisualAcceptanceRequired }).Count
    allowOperatorCommands = [bool]$AllowOperatorCommands
    allowFinalCommands = [bool]$AllowFinalCommands
    recommendedCaptures = $recommendedCaptures
    validationGroups = $validationGroups
}

$jsonOutput = $result | ConvertTo-Json -Depth 6
if (-not [string]::IsNullOrWhiteSpace($OutputPath)) {
    $outputParent = Split-Path -Parent $OutputPath
    if (-not [string]::IsNullOrWhiteSpace($outputParent) -and -not (Test-Path -LiteralPath $outputParent)) {
        New-Item -ItemType Directory -Path $outputParent | Out-Null
    }
    Set-Content -LiteralPath $OutputPath -Value $jsonOutput -Encoding utf8
}

function Exit-ForRequestedPlanFailures {
    if ($FailOnInvalidSlot -and @($result.invalidRequestedSlots).Count -gt 0) {
        exit $invalidSlotExitCode
    }
    if ($FailOnEmptyQueue -and @($result.recommendedCaptures).Count -eq 0) {
        exit $emptyQueueExitCode
    }
}

if ($Json) {
    $jsonOutput
    Exit-ForRequestedPlanFailures
    exit 0
}

if ($CommandsOnly) {
    Exit-ForRequestedPlanFailures
    foreach ($capture in @($result.recommendedCaptures)) {
        foreach ($command in @($capture.commands)) {
            if (-not [string]::IsNullOrWhiteSpace($command.command)) {
                if ($command.operatorRequired -and -not $AllowOperatorCommands) {
                    Write-Output "# OPERATOR REQUIRED: $($command.command)"
                } elseif ($command.finalVisualAcceptanceRequired -and -not $AllowFinalCommands) {
                    Write-Output "# OPERATOR REQUIRED FINAL: $($command.command)"
                } else {
                    Write-Output $command.command
                }
            }
        }
    }
    exit 0
}

Write-Output "Next Pixel validation plan"
Write-Output "Roadmap: $($result.roadmap.complete)/$($result.roadmap.total) complete; $($result.roadmap.inProgress) in progress."
Write-Output "Evidence root: $($result.currentCloseout.evidenceRoot)"
Write-Output "Capture stage: $($result.captureStage)"
Write-Output "Current closeout blockers: $($result.closeoutBlockerCount)"
Write-Output "Recommended captures: $($result.recommendedCaptureCount)"
Write-Output "Command templates: $($result.commandCount)"
Write-Output "Operator-required commands: $($result.operatorRequiredCommandCount)"
Write-Output "Final visual-acceptance commands: $($result.finalVisualAcceptanceCommandCount)"
if ($result.finalVisualAcceptanceCommandCount -gt 0) {
    Write-Output "Operator command guard: -CommandsOnly comments target-visible capture commands unless -AllowOperatorCommands is passed."
    Write-Output "Final command guard: final visual-acceptance commands also require -AllowFinalCommands."
}
Write-Output "Thermal readiness command: $($result.thermalReadiness.command)"
if (@($result.availableSlots).Count -gt 0) {
    Write-Output "Available missing slots: $($result.availableSlots -join ', ')"
}
if (@($result.invalidRequestedSlots).Count -gt 0) {
    Write-Output "Warning: requested slot(s) not currently missing or unknown: $($result.invalidRequestedSlots -join ', ')"
}
foreach ($blocker in @($result.currentCloseout.blockers)) {
    Write-Output "- $($blocker.kind): $($blocker.message)"
    if ($blocker.kind -eq "missingSlots") {
        foreach ($item in @($blocker.items)) {
            Write-Output "    Next $($item.id): $($item.nextCommand)"
        }
    }
}
if (@($result.recommendedCaptures).Count -gt 0) {
    Write-Output ""
    Write-Output "Recommended captures:"
    foreach ($capture in @($result.recommendedCaptures)) {
        Write-Output "- $($capture.title) [$($capture.milestones -join ', ')]"
        Write-Output "    Expected final label: $($capture.expectedFinalLabel)"
        Write-Output "    Protocol: $($capture.protocol)"
        foreach ($setupStep in @($capture.operatorSetup)) {
            Write-Output "    Setup: $setupStep"
        }
        foreach ($acceptanceCheck in @($capture.acceptanceChecks)) {
            Write-Output "    Accept: $acceptanceCheck"
        }
        foreach ($command in @($capture.commands)) {
            Write-Output "    $($command.name): $($command.purpose)"
            if ($command.finalVisualAcceptanceRequired) {
                Write-Output "      Operator required: final visual acceptance must be based on inspected target-visible video; do not run unattended."
            } elseif ($command.operatorRequired) {
                Write-Output "      Operator required: target setup must be physically present and visually inspectable."
            }
            if (-not [string]::IsNullOrWhiteSpace($command.command)) {
                Write-Output "      $($command.command)"
            }
        }
    }
}
if (@($result.recommendedCaptures).Count -eq 0) {
    Write-Output "Warning: no recommended captures match the current filters."
}

if ($NextOnly) {
    Exit-ForRequestedPlanFailures
    exit 0
}

if ($missingMilestones.Count -gt 0) {
    Write-Output "Warning: missing validation plan coverage for milestone(s): $($missingMilestones -join ', ')"
}

foreach ($group in $validationGroups | Sort-Object order) {
    Write-Output ""
    Write-Output "$($group.order). $($group.title) [$($group.milestones -join ', ')]"
    Write-Output "   Protocol: $($group.protocol)"
    Write-Output "   Setup: $($group.setupEvidence)"
    Write-Output "   Final: $($group.finalEvidence)"
    Write-Output "   Closes: $($group.closes -join '; ')"
    Write-Output "   Commands:"
    foreach ($command in $group.commands) {
        Write-Output "   - $($command.name): $($command.purpose)"
        Write-Output "     $($command.command)"
    }
}

Exit-ForRequestedPlanFailures
