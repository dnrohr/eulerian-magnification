param(
    [switch]$Json
)

$ErrorActionPreference = "Stop"

$roadmap = (& (Join-Path $PSScriptRoot "summarize_roadmap_status.ps1") -Json | ConvertFrom-Json)

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
                command = ".\tools\capture_live_validation_evidence.ps1 -Label `"manual-roi-known-target`" -WaitForThermalReady -ThermalReadyBelowStatus 4 -ThermalReadySamples 2 -ThermalReadyTimeoutSeconds 900 -ThermalReadyPollSeconds 30 -Mode Tremor -View Raw -RoiSource Manual -ManualRoi `"0.25,0.25,0.75,0.75`" -GlPreview `$true -Controls `$false -Clean `$true -MeasureRoiExpected `"<visible-target-bounds-in-screenshot-space>`" -MeasureRoiKind Manual -RequireRoiMeasurement -ScreenRecordSeconds 10 -RequireScreenrecord -RequireThermalReady -RequireCameraFps -RequireFocusedApp -RequireEvidenceVerdict target_visible_unvalidated -TargetDescription `"known high-contrast target inside manually selected ROI`" -VisualClaim `"Manual ROI outline overlaps the same visible target that was selected`" -TargetVisible `$true -VisualValidated `$false -OperatorNotes `"Set VisualValidated true only after inspecting the screenshot/recording and measurement JSON.`" -Summarize"
            },
            [pscustomobject]@{
                name = "auto-face-roi-setup"
                purpose = "Pre-inspection automatic face/skin ROI alignment capture."
                command = ".\tools\capture_live_validation_evidence.ps1 -Label `"auto-face-roi`" -WaitForThermalReady -ThermalReadyBelowStatus 4 -ThermalReadySamples 2 -ThermalReadyTimeoutSeconds 900 -ThermalReadyPollSeconds 30 -Mode Pulse -RoiSource Auto -GlPreview `$true -Controls `$false -Clean `$true -MeasureRoiExpected `"<visible-face-or-skin-target-bounds-in-screenshot-space>`" -MeasureRoiKind Auto -RequireRoiMeasurement -ScreenRecordSeconds 10 -RequireScreenrecord -RequireThermalReady -RequireCameraFps -RequireFocusedApp -RequireEvidenceVerdict target_visible_unvalidated -TargetDescription `"visible face or skin target tracked by automatic ROI`" -VisualClaim `"Automatic ROI outline overlaps the visible face or skin region being tracked`" -TargetVisible `$true -VisualValidated `$false -OperatorNotes `"Set VisualValidated true only after the automatic ROI is inspected against the visible face/skin target.`" -Summarize"
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
        finalEvidence = "Final Pulse and Breathing runs require -RequireRendererDiagnostics plus -RequireFinalVisualEvidence from a clean source tree whose commit is reachable from origin/main."
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
                command = ".\tools\capture_live_validation_evidence.ps1 -Label `"live-linear-pulse-final`" -WaitForThermalReady -ThermalReadyBelowStatus 4 -ThermalReadySamples 2 -ThermalReadyTimeoutSeconds 900 -ThermalReadyPollSeconds 30 -Mode Pulse -View Split -RoiSource FullFrame -GlPreview `$true -Controls `$false -ScreenRecordSeconds 15 -RequireRendererDiagnostics -RequireUiText `"Preview: Full-frame linear EVM preview`",`"Renderer: Live linear EVM reconstruction`",`"GL renderer: Live reconstruction`" -TargetDescription `"watched pulse target under steady lighting`" -VisualClaim `"Live Pulse Split view shows accepted full-frame linear reconstruction rather than ROI tint`" -TargetVisible `$true -VisualValidated `$true -OperatorNotes `"Accepted only if the recording shows target-visible magnification without ROI-only tint or full-frame flashing.`" -RequireFinalVisualEvidence -Summarize"
            },
            [pscustomobject]@{
                name = "live-linear-breathing-setup"
                purpose = "Pre-inspection Breathing full-frame live linear capture."
                command = ".\tools\capture_live_validation_evidence.ps1 -Label `"live-linear-breathing-setup`" -WaitForThermalReady -ThermalReadyBelowStatus 4 -ThermalReadySamples 2 -ThermalReadyTimeoutSeconds 900 -ThermalReadyPollSeconds 30 -Mode Breathing -View Split -RoiSource FullFrame -GlPreview `$true -Controls `$true -Panel Debug -ScreenRecordSeconds 15 -RequireScreenrecord -RequireThermalReady -RequireCameraFps -RequireFocusedApp -RequireRendererDiagnostics -RequireEvidenceVerdict target_visible_unvalidated -RequireUiText `"Preview: Full-frame linear EVM preview`",`"Renderer: Live linear EVM reconstruction`",`"GL renderer: Live reconstruction`" -TargetDescription `"watched slow-motion edge or breathing target`" -VisualClaim `"Live Breathing Split view shows full-frame reconstructed motion on the watched target rather than ROI tint`" -TargetVisible `$true -VisualValidated `$false -OperatorNotes `"Set VisualValidated true only after inspecting the recording against the Breathing pass criteria.`" -Summarize"
            },
            [pscustomobject]@{
                name = "live-linear-breathing-final"
                purpose = "Closing Breathing full-frame live linear evidence after inspection."
                command = ".\tools\capture_live_validation_evidence.ps1 -Label `"live-linear-breathing-final`" -WaitForThermalReady -ThermalReadyBelowStatus 4 -ThermalReadySamples 2 -ThermalReadyTimeoutSeconds 900 -ThermalReadyPollSeconds 30 -Mode Breathing -View Split -RoiSource FullFrame -GlPreview `$true -Controls `$false -ScreenRecordSeconds 15 -RequireRendererDiagnostics -RequireUiText `"Preview: Full-frame linear EVM preview`",`"Renderer: Live linear EVM reconstruction`",`"GL renderer: Live reconstruction`" -TargetDescription `"watched slow-motion edge or breathing target`" -VisualClaim `"Live Breathing Split view shows accepted full-frame reconstructed motion on the watched target rather than ROI tint`" -TargetVisible `$true -VisualValidated `$true -OperatorNotes `"Accepted only if the recording shows target-visible Breathing motion magnification without ROI-only tint, whole-frame flashing, or a frozen camera stream.`" -RequireFinalVisualEvidence -Summarize"
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
        finalEvidence = "Final Motion/Fast tremor runs require -RequirePhaseDiagnostics plus -RequireFinalVisualEvidence from a clean source tree whose commit is reachable from origin/main."
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
                command = ".\tools\capture_live_validation_evidence.ps1 -Label `"live-phase-object-final`" -WaitForThermalReady -ThermalReadyBelowStatus 4 -ThermalReadySamples 2 -ThermalReadyTimeoutSeconds 900 -ThermalReadyPollSeconds 30 -Mode Tremor -View Split -RoiSource Manual -ManualRoi `"0.25,0.25,0.75,0.75`" -GlPreview `$true -Controls `$false -ScreenRecordSeconds 15 -RequirePhaseDiagnostics -RequireUiText `"Renderer: Live phase motion`",`"GL renderer: Live phase motion`",`"phase:`" -TargetDescription `"high-contrast edge or small object moving subtly inside manual ROI`" -VisualClaim `"Live phase Split view shows edge-localized amplified motion inside the manual ROI`" -TargetVisible `$true -VisualValidated `$true -OperatorNotes `"Accepted only if the recording shows edge-localized amplified motion, not uniform ROI flashing.`" -RequireFinalVisualEvidence -Summarize"
            },
            [pscustomobject]@{
                name = "live-phase-fast-tremor-setup"
                purpose = "Pre-inspection Fast tremor phase capture."
                command = ".\tools\capture_live_validation_evidence.ps1 -Label `"live-phase-fast-tremor-setup`" -WaitForThermalReady -ThermalReadyBelowStatus 4 -ThermalReadySamples 2 -ThermalReadyTimeoutSeconds 900 -ThermalReadyPollSeconds 30 -Mode Tremor -View Split -RoiSource Manual -ManualRoi `"0.25,0.25,0.75,0.75`" -GlPreview `$true -Controls `$true -Panel Debug -ScreenRecordSeconds 15 -RequireScreenrecord -RequireThermalReady -RequireCameraFps -RequireFocusedApp -RequirePhaseDiagnostics -RequireEvidenceVerdict target_visible_unvalidated -RequireUiText `"Renderer: Live phase motion`",`"GL renderer: Live phase motion`",`"phase:`" -TargetDescription `"fast tremor target with a high-contrast edge inside manual ROI`" -VisualClaim `"Live phase Split view shows edge-localized amplified fast tremor without uniform ROI flashing`" -TargetVisible `$true -VisualValidated `$false -OperatorNotes `"Set VisualValidated true only after inspecting the recording against the Fast tremor pass criteria.`" -Summarize"
            },
            [pscustomobject]@{
                name = "live-phase-fast-tremor-final"
                purpose = "Closing Fast tremor phase evidence after inspection."
                command = ".\tools\capture_live_validation_evidence.ps1 -Label `"live-phase-fast-tremor-final`" -WaitForThermalReady -ThermalReadyBelowStatus 4 -ThermalReadySamples 2 -ThermalReadyTimeoutSeconds 900 -ThermalReadyPollSeconds 30 -Mode Tremor -View Split -RoiSource Manual -ManualRoi `"0.25,0.25,0.75,0.75`" -GlPreview `$true -Controls `$false -ScreenRecordSeconds 15 -RequirePhaseDiagnostics -RequireUiText `"Renderer: Live phase motion`",`"GL renderer: Live phase motion`",`"phase:`" -TargetDescription `"fast tremor target with a high-contrast edge inside manual ROI`" -VisualClaim `"Live phase Split view shows accepted edge-localized amplified fast tremor without uniform ROI flashing`" -TargetVisible `$true -VisualValidated `$true -OperatorNotes `"Accepted only if the recording shows edge-localized amplified fast tremor, not uniform ROI flashing or color-only change.`" -RequireFinalVisualEvidence -Summarize"
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
                command = "Run .\tools\summarize_pixel_validation_closeout.ps1 -OutputPath sample-videos\exports\live-validation\pixel_closeout_summary.json -FailOnNonMain -FailOnUnpushedSource -FailOnMissingArtifactHashes -FailOnNonFinalLabel -FailOnWrongSlotLabel -FailOnMissingOperatorNotes -FailOnMissingVisualReviewText, .\tools\summarize_pixel_validation_closeout.ps1 -OutputPath sample-videos\exports\live-validation\pixel_closeout_summary.json -FailOnCloseoutNotReady, and .\tools\summarize_pixel_validation_closeout.ps1 -OutputPath sample-videos\exports\live-validation\pixel_closeout_summary.json -FailOnPresetDocsNotReady. If all pass, update README.md and docs/testing/MIT_PARITY_TARGETS.md with visual-validation status and artifact notes from pixel_closeout_summary.json, then rerun .\tools\test_offline_project_tooling.ps1."
            }
        )
    }
)

$inProgressMilestones = @($roadmap.inProgress | ForEach-Object { $_.milestone })
$coveredMilestones = @($validationGroups | ForEach-Object { $_.milestones } | Select-Object -Unique)
$missingMilestones = @($inProgressMilestones | Where-Object { $_ -notin $coveredMilestones })

$result = [pscustomobject]@{
    roadmap = [pscustomobject]@{
        total = $roadmap.total
        complete = $roadmap.statusCounts.Complete
        inProgress = $roadmap.statusCounts.'In Progress'
    }
    coveredMilestones = $coveredMilestones
    missingMilestones = $missingMilestones
    validationGroups = $validationGroups
}

if ($Json) {
    $result | ConvertTo-Json -Depth 6
    exit 0
}

Write-Output "Next Pixel validation plan"
Write-Output "Roadmap: $($result.roadmap.complete)/$($result.roadmap.total) complete; $($result.roadmap.inProgress) in progress."

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
