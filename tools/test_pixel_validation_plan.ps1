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
Assert-True -Condition ($linear.finalEvidence.Contains("-RequireRendererDiagnostics")) -Message "Linear final evidence should require renderer diagnostics."
Assert-True -Condition ($phase.finalEvidence.Contains("-RequirePhaseDiagnostics")) -Message "Phase final evidence should require phase diagnostics."
Assert-True -Condition ($preset.finalEvidence.Contains("Update README")) -Message "Preset parity plan should keep docs updates after accepted artifacts."

foreach ($group in $groups) {
    Assert-True -Condition (@($group.commands).Count -gt 0) -Message "Validation group '$($group.id)' should include command templates."
}

$allCommands = @($groups | ForEach-Object { $_.commands } | ForEach-Object { $_.command })
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

Write-Output "Pixel validation plan self-test passed."
