$ErrorActionPreference = "Stop"

$tests = @(
    "test_live_validation_capture_contract.ps1",
    "test_live_validation_summary.ps1",
    "test_live_validation_protocol_docs.ps1",
    "test_live_validation_review_sheet.ps1",
    "test_live_validation_review_queue.ps1",
    "test_live_validation_review_dashboard.ps1"
)

foreach ($test in $tests) {
    $testPath = Join-Path $PSScriptRoot $test
    Write-Output "Running $test..."
    & $testPath
}

Write-Output "Live validation tooling self-tests passed."
