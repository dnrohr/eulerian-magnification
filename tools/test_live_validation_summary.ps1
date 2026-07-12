param(
    [string]$OutputRoot = ""
)

$ErrorActionPreference = "Stop"

function Assert-Equal {
    param(
        [object]$Actual,
        [object]$Expected,
        [string]$Message
    )

    if ($Actual -ne $Expected) {
        throw "$Message Expected '$Expected' but got '$Actual'."
    }
}

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Invoke-Summary {
    param([string]$BundlePath)

    $summaryScript = Join-Path $PSScriptRoot "summarize_live_validation_evidence.ps1"
    $stdoutPath = Join-Path $BundlePath "summary_stdout.txt"
    & $summaryScript -BundlePath $BundlePath *> $stdoutPath
    return $LASTEXITCODE
}

function Write-JsonFile {
    param(
        [string]$Path,
        [object]$Value
    )

    $Value | ConvertTo-Json -Depth 8 | Out-File -LiteralPath $Path -Encoding utf8
}

$root = if ([string]::IsNullOrWhiteSpace($OutputRoot)) {
    Join-Path ([System.IO.Path]::GetTempPath()) ("eulerian-live-summary-test-" + [Guid]::NewGuid().ToString("N"))
} else {
    $OutputRoot
}
New-Item -ItemType Directory -Force -Path $root | Out-Null

try {
    $abortedBundle = Join-Path $root "aborted"
    New-Item -ItemType Directory -Force -Path $abortedBundle | Out-Null
    "List of devices attached`nFAKEDEVICE`tdevice" | Out-File -LiteralPath (Join-Path $abortedBundle "adb_devices.txt") -Encoding utf8
    "Thermal Status: 4`nTemperature{mValue=69.0, mType=3, mName=MID, mStatus=5}" |
        Out-File -LiteralPath (Join-Path $abortedBundle "thermalservice_preflight.txt") -Encoding utf8
    Write-JsonFile -Path (Join-Path $abortedBundle "manifest.json") -Value ([ordered]@{
        createdAt = "2026-07-12T00:00:00.0000000-04:00"
        label = "aborted"
        aborted = $true
        abortReason = "thermal preflight status 5 at or above abort threshold 4"
        launch = [ordered]@{
            skipped = $true
            requireUiText = @()
        }
        visualReview = [ordered]@{
            targetDescription = "synthetic abort"
            visualClaim = "summary classifies abort"
            targetVisible = $false
            visualValidated = $false
            operatorNotes = "tool self-test"
        }
        thermalPreflight = [ordered]@{
            status = 4
            statusLabel = "critical"
            maxSensorStatus = 5
            maxSensorStatusLabel = "emergency"
            maxTemperatureC = 69.0
            hottestSensorName = "MID"
        }
        warnings = @("capture aborted before app launch: synthetic")
        artifacts = [ordered]@{
            devices = Join-Path $abortedBundle "adb_devices.txt"
            thermalPreflight = Join-Path $abortedBundle "thermalservice_preflight.txt"
        }
    })

    $abortedExitCode = Invoke-Summary -BundlePath $abortedBundle
    $abortedSummary = Get-Content -LiteralPath (Join-Path $abortedBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $abortedExitCode -Expected 4 -Message "Aborted summary exit code mismatch."
    Assert-Equal -Actual $abortedSummary.aborted -Expected $true -Message "Aborted summary flag mismatch."
    Assert-Equal -Actual $abortedSummary.evidenceVerdict.status -Expected "thermal_preflight_aborted" -Message "Aborted verdict mismatch."
    Assert-Equal -Actual $abortedSummary.passedRuntimeSmoke -Expected $false -Message "Aborted runtime-smoke result mismatch."
    Assert-Equal -Actual @($abortedSummary.artifacts.missingRequired).Count -Expected 0 -Message "Aborted bundle should not require runtime artifacts."

    $incompleteBundle = Join-Path $root "incomplete"
    New-Item -ItemType Directory -Force -Path $incompleteBundle | Out-Null
    Write-JsonFile -Path (Join-Path $incompleteBundle "manifest.json") -Value ([ordered]@{
        createdAt = "2026-07-12T00:00:00.0000000-04:00"
        label = "incomplete"
        launch = [ordered]@{
            skipped = $false
            requireUiText = @()
        }
        visualReview = [ordered]@{
            targetDescription = "synthetic incomplete"
            visualClaim = ""
            targetVisible = $false
            visualValidated = $false
            operatorNotes = "tool self-test"
        }
        warnings = @()
    })

    $incompleteExitCode = Invoke-Summary -BundlePath $incompleteBundle
    $incompleteSummary = Get-Content -LiteralPath (Join-Path $incompleteBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $incompleteExitCode -Expected 2 -Message "Incomplete summary exit code mismatch."
    Assert-Equal -Actual $incompleteSummary.aborted -Expected $false -Message "Incomplete summary should not be marked aborted."
    Assert-Equal -Actual $incompleteSummary.evidenceVerdict.status -Expected "runtime_failed" -Message "Incomplete verdict mismatch."
    Assert-True -Condition (@($incompleteSummary.artifacts.missingRequired).Count -ge 3) -Message "Incomplete bundle should report missing runtime artifacts."

    Write-Output "Live validation summary self-test passed: $root"
} finally {
    if ([string]::IsNullOrWhiteSpace($OutputRoot) -and (Test-Path -LiteralPath $root)) {
        Remove-Item -LiteralPath $root -Recurse -Force
    }
}
