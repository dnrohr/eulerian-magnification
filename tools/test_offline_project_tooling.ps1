$ErrorActionPreference = "Stop"

$tests = @(
    "test_live_validation_tooling.ps1",
    "test_roadmap_status_summary.ps1",
    "test_github_workflows.ps1"
)

foreach ($test in $tests) {
    $testPath = Join-Path $PSScriptRoot $test
    Write-Output "Running $test..."
    & $testPath
}

Write-Output "Running summarize_roadmap_status.ps1 -FailOnMismatch..."
& (Join-Path $PSScriptRoot "summarize_roadmap_status.ps1") -FailOnMismatch

Write-Output "Offline project tooling self-tests passed."
