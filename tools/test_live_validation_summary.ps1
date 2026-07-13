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

function Write-TestScreenshot {
    param([string]$Path)

    Add-Type -AssemblyName System.Drawing
    $bitmap = [System.Drawing.Bitmap]::new(64, 96)
    try {
        for ($y = 0; $y -lt $bitmap.Height; $y++) {
            for ($x = 0; $x -lt $bitmap.Width; $x++) {
                $red = [Math]::Min(255, 40 + ($x * 3))
                $green = [Math]::Min(255, 60 + ($y * 2))
                $blue = 120
                $bitmap.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($red, $green, $blue))
            }
        }
        $bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $bitmap.Dispose()
    }
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
    Write-JsonFile -Path (Join-Path $abortedBundle "thermal_ready_wait.json") -Value ([ordered]@{
        createdAt = "2026-07-12T00:00:00.0000000-04:00"
        ready = $false
        readyBelowThermalStatus = 4
        requiredReadySamples = 2
        timeoutSeconds = 0
        pollSeconds = 1
        consecutiveReadySamples = 0
        lastThermal = [ordered]@{
            status = 4
            statusLabel = "critical"
            maxSensorStatus = 5
            maxSensorStatusLabel = "emergency"
            maxTemperatureC = 69.0
            hottestSensorName = "MID"
        }
        records = @()
    })
    Write-JsonFile -Path (Join-Path $abortedBundle "manifest.json") -Value ([ordered]@{
        createdAt = "2026-07-12T00:00:00.0000000-04:00"
        label = "aborted"
        aborted = $true
        abortReason = "thermal readiness wait exited 2 before app launch"
        launch = [ordered]@{
            skipped = $true
            requireUiText = @()
            thermalWait = [ordered]@{
                requested = $true
                readyBelowStatus = 4
                requiredReadySamples = 2
                timeoutSeconds = 0
                pollSeconds = 1
            }
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
            thermalReadyWait = Join-Path $abortedBundle "thermal_ready_wait.json"
        }
    })

    $abortedExitCode = Invoke-Summary -BundlePath $abortedBundle
    $abortedSummary = Get-Content -LiteralPath (Join-Path $abortedBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $abortedExitCode -Expected 4 -Message "Aborted summary exit code mismatch."
    Assert-Equal -Actual $abortedSummary.aborted -Expected $true -Message "Aborted summary flag mismatch."
    Assert-Equal -Actual $abortedSummary.evidenceVerdict.status -Expected "thermal_preflight_aborted" -Message "Aborted verdict mismatch."
    Assert-Equal -Actual $abortedSummary.passedRuntimeSmoke -Expected $false -Message "Aborted runtime-smoke result mismatch."
    Assert-Equal -Actual @($abortedSummary.artifacts.missingRequired).Count -Expected 0 -Message "Aborted bundle should not require runtime artifacts."
    Assert-Equal -Actual $abortedSummary.artifacts.thermalReadyWaitPresent -Expected $true -Message "Thermal wait presence mismatch."
    Assert-Equal -Actual $abortedSummary.thermalReadyWait.ready -Expected $false -Message "Thermal wait ready flag mismatch."
    Assert-Equal -Actual $abortedSummary.thermalReadyWait.requiredReadySamples -Expected 2 -Message "Thermal wait sample count mismatch."

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

    $uiMissingBundle = Join-Path $root "ui-missing"
    New-Item -ItemType Directory -Force -Path $uiMissingBundle | Out-Null
    Write-TestScreenshot -Path (Join-Path $uiMissingBundle "screenshot.png")
    @"
Total frames rendered: 120
Janky frames: 2 (1.67%)
50th percentile: 9ms
90th percentile: 12ms
Number Missed Vsync: 1
"@ | Out-File -LiteralPath (Join-Path $uiMissingBundle "gfxinfo.txt") -Encoding utf8
    "Camera FPS: 30.0" | Out-File -LiteralPath (Join-Path $uiMissingBundle "logcat_tail.txt") -Encoding utf8
    "<hierarchy><node text=`"Controls`" /><node text=`"phase: 128x96 / phase ready / amplitude gate active`" /></hierarchy>" |
        Out-File -LiteralPath (Join-Path $uiMissingBundle "ui_dump.xml") -Encoding utf8
    Write-JsonFile -Path (Join-Path $uiMissingBundle "manifest.json") -Value ([ordered]@{
        createdAt = "2026-07-12T00:00:00.0000000-04:00"
        label = "ui-missing"
        source = [ordered]@{
            commit = "abc123"
            shortCommit = "abc123"
            branch = "main"
            dirty = $true
            statusShort = @("M tools/summarize_live_validation_evidence.ps1")
        }
        launch = [ordered]@{
            skipped = $false
            requireUiText = @("GL renderer")
        }
        visualReview = [ordered]@{
            targetDescription = "synthetic UI text"
            visualClaim = ""
            targetVisible = $false
            visualValidated = $false
            operatorNotes = "tool self-test"
        }
        warnings = @()
    })

    $uiMissingExitCode = Invoke-Summary -BundlePath $uiMissingBundle
    $uiMissingSummary = Get-Content -LiteralPath (Join-Path $uiMissingBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $uiMissingExitCode -Expected 3 -Message "Missing UI summary exit code mismatch."
    Assert-Equal -Actual $uiMissingSummary.passedRuntimeSmoke -Expected $true -Message "Missing UI case should pass runtime smoke."
    Assert-Equal -Actual $uiMissingSummary.uiTextAssertions.passed -Expected $false -Message "Missing UI assertion should fail."
    Assert-Equal -Actual $uiMissingSummary.evidenceVerdict.status -Expected "ui_assertion_failed" -Message "Missing UI verdict mismatch."
    Assert-True -Condition ("source worktree was dirty during capture" -in @($uiMissingSummary.warnings)) -Message "Dirty source warning missing."
    Assert-True -Condition ("phase: 128x96 / phase ready / amplitude gate active" -in @($uiMissingSummary.uiDump.phaseLabels)) -Message "Phase label summary missing."

    Write-Output "Live validation summary self-test passed: $root"
} finally {
    if ([string]::IsNullOrWhiteSpace($OutputRoot) -and (Test-Path -LiteralPath $root)) {
        Remove-Item -LiteralPath $root -Recurse -Force
    }
}
