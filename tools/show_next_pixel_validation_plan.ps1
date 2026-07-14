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
        finalEvidence = "Final runs require -RequireRoiMeasurement plus -RequireFinalVisualEvidence from a clean source tree."
        closes = @(
            "portrait/front-camera ROI mapping confidence",
            "manual ROI known-target alignment",
            "automatic face/skin ROI alignment"
        )
    },
    [pscustomobject]@{
        order = 2
        id = "live-linear"
        title = "Live full-frame linear EVM"
        milestones = @("AE", "AP")
        protocol = "docs/experiments/pixel8a_live_linear_validation.md"
        setupEvidence = "Pulse and Breathing setup captures should show renderer diagnostics and stop at target_visible_unvalidated."
        finalEvidence = "Final Pulse and Breathing runs require -RequireRendererDiagnostics plus -RequireFinalVisualEvidence."
        closes = @(
            "portrait full-frame live EVM preview validation",
            "live linear reconstruction visual evidence",
            "Pulse and Breathing preset visual parity inputs"
        )
    },
    [pscustomobject]@{
        order = 3
        id = "live-phase"
        title = "Live phase motion EVM"
        milestones = @("AR")
        protocol = "docs/experiments/pixel8a_live_phase_validation.md"
        setupEvidence = "Controlled high-contrast moving-edge setup should stop at target_visible_unvalidated."
        finalEvidence = "Final Motion/Fast tremor runs require -RequirePhaseDiagnostics plus -RequireFinalVisualEvidence."
        closes = @(
            "controlled object-motion live phase validation",
            "Object vibration and Fast tremor preset visual parity inputs"
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
}
