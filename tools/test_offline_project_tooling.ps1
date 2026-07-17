$ErrorActionPreference = "Stop"

$tests = @(
    "test_live_validation_tooling.ps1",
    "test_roadmap_status_summary.ps1",
    "test_github_workflows.ps1",
    "test_debug_install_helper.ps1",
    "test_pixel_session_readiness.ps1",
    "test_roi_bounds_converter.ps1",
    "test_roi_final_capture_command.ps1",
    "test_pixel_validation_plan.ps1",
    "test_pixel_validation_handoff.ps1",
    "test_pixel_validation_handoff_verifier.ps1",
    "test_pixel_validation_closeout.ps1"
)

foreach ($test in $tests) {
    $testPath = Join-Path $PSScriptRoot $test
    Write-Output "Running $test..."
    & $testPath
}

Write-Output "Running summarize_roadmap_status.ps1 -FailOnMismatch..."
& (Join-Path $PSScriptRoot "summarize_roadmap_status.ps1") -FailOnMismatch

Write-Output "Offline project tooling self-tests passed."
