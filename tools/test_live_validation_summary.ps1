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
    param(
        [string]$BundlePath,
        [switch]$RequireFinalVisualEvidence,
        [switch]$RequireCleanSource,
        [switch]$RequireVisualValidation,
        [switch]$RequireNoWarnings,
        [switch]$RequireRoiMeasurement,
        [switch]$RequireScreenrecord,
        [switch]$RequireThermalReady,
        [switch]$RequireCameraFps,
        [switch]$RequireFocusedApp,
        [switch]$RequireRendererDiagnostics,
        [switch]$RequirePhaseDiagnostics,
        [switch]$RequireReviewContactSheet,
        [string]$RequireDeviceSerial = "",
        [string]$RequireEvidenceVerdict = ""
    )

    $summaryScript = Join-Path $PSScriptRoot "summarize_live_validation_evidence.ps1"
    $stdoutPath = Join-Path $BundlePath "summary_stdout.txt"
    $summaryArgs = @{
        BundlePath = $BundlePath
    }
    if ($RequireFinalVisualEvidence) { $summaryArgs.RequireFinalVisualEvidence = $true }
    if ($RequireCleanSource) { $summaryArgs.RequireCleanSource = $true }
    if ($RequireVisualValidation) { $summaryArgs.RequireVisualValidation = $true }
    if ($RequireNoWarnings) { $summaryArgs.RequireNoWarnings = $true }
    if ($RequireRoiMeasurement) { $summaryArgs.RequireRoiMeasurement = $true }
    if ($RequireScreenrecord) { $summaryArgs.RequireScreenrecord = $true }
    if ($RequireThermalReady) { $summaryArgs.RequireThermalReady = $true }
    if ($RequireCameraFps) { $summaryArgs.RequireCameraFps = $true }
    if ($RequireFocusedApp) { $summaryArgs.RequireFocusedApp = $true }
    if ($RequireRendererDiagnostics) { $summaryArgs.RequireRendererDiagnostics = $true }
    if ($RequirePhaseDiagnostics) { $summaryArgs.RequirePhaseDiagnostics = $true }
    if ($RequireReviewContactSheet) { $summaryArgs.RequireReviewContactSheet = $true }
    if (-not [string]::IsNullOrWhiteSpace($RequireDeviceSerial)) { $summaryArgs.RequireDeviceSerial = $RequireDeviceSerial }
    if (-not [string]::IsNullOrWhiteSpace($RequireEvidenceVerdict)) { $summaryArgs.RequireEvidenceVerdict = $RequireEvidenceVerdict }
    & $summaryScript @summaryArgs *> $stdoutPath
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

function Write-TestLandscapeScreenshot {
    param([string]$Path)

    Add-Type -AssemblyName System.Drawing
    $bitmap = [System.Drawing.Bitmap]::new(96, 64)
    try {
        for ($y = 0; $y -lt $bitmap.Height; $y++) {
            for ($x = 0; $x -lt $bitmap.Width; $x++) {
                $red = [Math]::Min(255, 30 + ($x * 2))
                $green = [Math]::Min(255, 80 + ($y * 3))
                $blue = 150
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

    $gatedAbortedExitCode = Invoke-Summary -BundlePath $abortedBundle -RequireCleanSource -RequireVisualValidation
    $gatedAbortedSummary = Get-Content -LiteralPath (Join-Path $abortedBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $gatedAbortedExitCode -Expected 4 -Message "Gated aborted summary exit code mismatch."
    Assert-Equal -Actual $gatedAbortedSummary.requiredGates.cleanSource.required -Expected $true -Message "Gated abort should record clean-source requirement."
    Assert-Equal -Actual $gatedAbortedSummary.requiredGates.visualValidation.required -Expected $true -Message "Gated abort should record visual-validation requirement."
    Assert-Equal -Actual $gatedAbortedSummary.requiredGates.cleanSource.passed -Expected $false -Message "Gated abort without source metadata should not pass clean-source gate."
    Assert-Equal -Actual $gatedAbortedSummary.requiredGates.visualValidation.passed -Expected $false -Message "Gated abort should not pass visual-validation gate."

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

    $phaseDiagnosticsBundle = Join-Path $root "phase-diagnostics"
    Copy-Item -LiteralPath $uiMissingBundle -Destination $phaseDiagnosticsBundle -Recurse
    $phaseDiagnosticsManifestPath = Join-Path $phaseDiagnosticsBundle "manifest.json"
    $phaseDiagnosticsManifest = Get-Content -LiteralPath $phaseDiagnosticsManifestPath -Raw | ConvertFrom-Json
    $phaseDiagnosticsManifest.launch.requireUiText = @()
    $phaseDiagnosticsManifest.source.dirty = $false
    $phaseDiagnosticsManifest.source.statusShort = @()
    $phaseDiagnosticsManifest | ConvertTo-Json -Depth 8 | Out-File -LiteralPath $phaseDiagnosticsManifestPath -Encoding utf8

    $phaseDiagnosticsExitCode = Invoke-Summary -BundlePath $phaseDiagnosticsBundle -RequireCleanSource -RequirePhaseDiagnostics
    $phaseDiagnosticsSummary = Get-Content -LiteralPath (Join-Path $phaseDiagnosticsBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $phaseDiagnosticsExitCode -Expected 0 -Message "Phase diagnostics gate exit code mismatch."
    Assert-Equal -Actual $phaseDiagnosticsSummary.requiredGates.phaseDiagnostics.required -Expected $true -Message "Phase diagnostics gate should be required."
    Assert-Equal -Actual $phaseDiagnosticsSummary.requiredGates.phaseDiagnostics.passed -Expected $true -Message "Phase diagnostics gate should pass."
    Assert-Equal -Actual $phaseDiagnosticsSummary.requiredGates.phaseDiagnostics.labelCount -Expected 1 -Message "Phase diagnostics label count mismatch."

    $visualGateBundle = Join-Path $root "visual-gate"
    New-Item -ItemType Directory -Force -Path $visualGateBundle | Out-Null
    Write-TestScreenshot -Path (Join-Path $visualGateBundle "screenshot.png")
    [System.IO.File]::WriteAllBytes((Join-Path $visualGateBundle "review_contact_sheet.jpg"), [byte[]](255, 216, 255, 217))
    @"
Total frames rendered: 120
Janky frames: 0 (0.00%)
50th percentile: 9ms
90th percentile: 12ms
Number Missed Vsync: 0
"@ | Out-File -LiteralPath (Join-Path $visualGateBundle "gfxinfo.txt") -Encoding utf8
    "Camera FPS: 30.0" | Out-File -LiteralPath (Join-Path $visualGateBundle "logcat_tail.txt") -Encoding utf8
    "<hierarchy><node text=`"Renderer: Live full-frame EVM`" /></hierarchy>" |
        Out-File -LiteralPath (Join-Path $visualGateBundle "ui_dump.xml") -Encoding utf8
    Write-JsonFile -Path (Join-Path $visualGateBundle "manifest.json") -Value ([ordered]@{
        createdAt = "2026-07-12T00:00:00.0000000-04:00"
        label = "visual-gate"
        deviceSerial = "47091JEKB05516"
        source = [ordered]@{
            commit = "def456"
            shortCommit = "def456"
            branch = "main"
            dirty = $false
            statusShort = @()
        }
        launch = [ordered]@{
            skipped = $false
            requireUiText = @()
        }
        visualReview = [ordered]@{
            targetDescription = "synthetic watched target"
            visualClaim = "known target visibly magnified"
            targetVisible = $true
            visualValidated = $false
            operatorNotes = "tool self-test"
        }
        warnings = @()
    })
    Write-JsonFile -Path (Join-Path $visualGateBundle "review_contact_sheet_manifest.json") -Value ([ordered]@{
        screenrecordSha256 = "screenrecord-for-review-sheet-sha256"
        contactSheetSha256 = "review-contact-sheet-sha256"
        columns = 3
        rows = 3
        frameWidth = 360
    })

    $visualGateExitCode = Invoke-Summary -BundlePath $visualGateBundle -RequireCleanSource -RequireVisualValidation
    $visualGateSummary = Get-Content -LiteralPath (Join-Path $visualGateBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $visualGateExitCode -Expected 5 -Message "Visual gate exit code mismatch."
    Assert-Equal -Actual $visualGateSummary.passedRuntimeSmoke -Expected $true -Message "Visual gate should pass runtime smoke."
    Assert-Equal -Actual $visualGateSummary.artifacts.reviewContactSheet.present -Expected $true -Message "Review contact sheet should be detected."
    Assert-Equal -Actual $visualGateSummary.artifacts.reviewContactSheet.manifestPresent -Expected $true -Message "Review contact sheet manifest should be detected."
    Assert-True -Condition (-not [string]::IsNullOrWhiteSpace($visualGateSummary.artifacts.reviewContactSheet.sha256)) -Message "Review contact sheet hash should be reported."
    Assert-Equal -Actual $visualGateSummary.artifacts.reviewContactSheet.screenrecordSha256 -Expected "screenrecord-for-review-sheet-sha256" -Message "Review contact sheet should report manifest screenrecord hash."
    Assert-Equal -Actual $visualGateSummary.artifacts.reviewContactSheet.screenrecordSha256Matches -Expected $null -Message "Review contact sheet match status should be unknown without screenrecord hash."
    Assert-Equal -Actual $visualGateSummary.requiredGates.cleanSource.passed -Expected $true -Message "Clean source gate should pass."
    Assert-Equal -Actual $visualGateSummary.requiredGates.visualValidation.passed -Expected $false -Message "Visual validation gate should fail."
    Assert-True -Condition ("visual validation required but evidence verdict does not count as visual validation" -in @($visualGateSummary.warnings)) -Message "Visual gate warning missing."

    $cameraFpsExitCode = Invoke-Summary -BundlePath $visualGateBundle -RequireCleanSource -RequireCameraFps
    $cameraFpsSummary = Get-Content -LiteralPath (Join-Path $visualGateBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $cameraFpsExitCode -Expected 0 -Message "Camera FPS gate exit code mismatch."
    Assert-Equal -Actual $cameraFpsSummary.requiredGates.cameraFps.required -Expected $true -Message "Camera FPS gate should be required."
    Assert-Equal -Actual $cameraFpsSummary.requiredGates.cameraFps.sampleCount -Expected 1 -Message "Camera FPS sample count mismatch."
    Assert-Equal -Actual $cameraFpsSummary.requiredGates.cameraFps.passed -Expected $true -Message "Camera FPS gate should pass."

    $deviceSerialExitCode = Invoke-Summary -BundlePath $visualGateBundle -RequireCleanSource -RequireDeviceSerial "47091JEKB05516"
    $deviceSerialSummary = Get-Content -LiteralPath (Join-Path $visualGateBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $deviceSerialExitCode -Expected 0 -Message "Device serial gate exit code mismatch."
    Assert-Equal -Actual $deviceSerialSummary.deviceSerial -Expected "47091JEKB05516" -Message "Summary should expose the capture device serial."
    Assert-Equal -Actual $deviceSerialSummary.requiredGates.deviceSerial.required -Expected $true -Message "Device serial gate should be required."
    Assert-Equal -Actual $deviceSerialSummary.requiredGates.deviceSerial.expected -Expected "47091JEKB05516" -Message "Device serial expected value mismatch."
    Assert-Equal -Actual $deviceSerialSummary.requiredGates.deviceSerial.actual -Expected "47091JEKB05516" -Message "Device serial actual value mismatch."
    Assert-Equal -Actual $deviceSerialSummary.requiredGates.deviceSerial.passed -Expected $true -Message "Device serial gate should pass."

    $wrongDeviceSerialExitCode = Invoke-Summary -BundlePath $visualGateBundle -RequireCleanSource -RequireDeviceSerial "WRONG-SERIAL"
    $wrongDeviceSerialSummary = Get-Content -LiteralPath (Join-Path $visualGateBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $wrongDeviceSerialExitCode -Expected 21 -Message "Wrong device serial gate exit code mismatch."
    Assert-Equal -Actual $wrongDeviceSerialSummary.requiredGates.deviceSerial.passed -Expected $false -Message "Wrong device serial gate should fail."
    Assert-True -Condition ("device serial required WRONG-SERIAL but was 47091JEKB05516" -in @($wrongDeviceSerialSummary.warnings)) -Message "Wrong device serial warning missing."

    $missingCameraFpsBundle = Join-Path $root "missing-camera-fps"
    Copy-Item -LiteralPath $visualGateBundle -Destination $missingCameraFpsBundle -Recurse
    "Camera opened without cadence line" | Out-File -LiteralPath (Join-Path $missingCameraFpsBundle "logcat_tail.txt") -Encoding utf8
    $missingCameraFpsExitCode = Invoke-Summary -BundlePath $missingCameraFpsBundle -RequireCleanSource -RequireCameraFps
    $missingCameraFpsSummary = Get-Content -LiteralPath (Join-Path $missingCameraFpsBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $missingCameraFpsExitCode -Expected 13 -Message "Missing camera FPS gate exit code mismatch."
    Assert-Equal -Actual $missingCameraFpsSummary.requiredGates.cameraFps.sampleCount -Expected 0 -Message "Missing camera FPS sample count mismatch."
    Assert-Equal -Actual $missingCameraFpsSummary.requiredGates.cameraFps.passed -Expected $false -Message "Missing camera FPS gate should fail."
    Assert-True -Condition ("camera HAL FPS required but no FPS samples were found" -in @($missingCameraFpsSummary.warnings)) -Message "Missing camera FPS warning missing."

    $lowCameraFpsBundle = Join-Path $root "low-camera-fps"
    Copy-Item -LiteralPath $visualGateBundle -Destination $lowCameraFpsBundle -Recurse
    "Camera FPS: 12.5" | Out-File -LiteralPath (Join-Path $lowCameraFpsBundle "logcat_tail.txt") -Encoding utf8
    $lowCameraFpsExitCode = Invoke-Summary -BundlePath $lowCameraFpsBundle -RequireCleanSource -RequireCameraFps
    $lowCameraFpsSummary = Get-Content -LiteralPath (Join-Path $lowCameraFpsBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $lowCameraFpsExitCode -Expected 13 -Message "Low camera FPS gate exit code mismatch."
    Assert-Equal -Actual $lowCameraFpsSummary.requiredGates.cameraFps.sampleCount -Expected 1 -Message "Low camera FPS sample count mismatch."
    Assert-Equal -Actual $lowCameraFpsSummary.requiredGates.cameraFps.passed -Expected $false -Message "Low camera FPS gate should fail."
    Assert-True -Condition ("camera HAL FPS required but minimum FPS was below 23.5 fps" -in @($lowCameraFpsSummary.warnings)) -Message "Low camera FPS required warning missing."

    $missingFocusedAppExitCode = Invoke-Summary -BundlePath $visualGateBundle -RequireCleanSource -RequireFocusedApp
    $missingFocusedAppSummary = Get-Content -LiteralPath (Join-Path $visualGateBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $missingFocusedAppExitCode -Expected 14 -Message "Missing focused-app gate exit code mismatch."
    Assert-Equal -Actual $missingFocusedAppSummary.requiredGates.focusedApp.required -Expected $true -Message "Focused-app gate should be required."
    Assert-Equal -Actual $missingFocusedAppSummary.requiredGates.focusedApp.present -Expected $false -Message "Focused-app artifact should be missing."
    Assert-Equal -Actual $missingFocusedAppSummary.requiredGates.focusedApp.passed -Expected $false -Message "Missing focused-app artifact should not pass."
    Assert-True -Condition ("focused app required but window_focus.txt is missing" -in @($missingFocusedAppSummary.warnings)) -Message "Missing focused-app warning missing."

    $wrongFocusedAppBundle = Join-Path $root "wrong-focused-app"
    Copy-Item -LiteralPath $visualGateBundle -Destination $wrongFocusedAppBundle -Recurse
    "mCurrentFocus=Window{123 u0 com.android.launcher/.Launcher}" |
        Out-File -LiteralPath (Join-Path $wrongFocusedAppBundle "window_focus.txt") -Encoding utf8
    $wrongFocusedAppExitCode = Invoke-Summary -BundlePath $wrongFocusedAppBundle -RequireCleanSource -RequireFocusedApp
    $wrongFocusedAppSummary = Get-Content -LiteralPath (Join-Path $wrongFocusedAppBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $wrongFocusedAppExitCode -Expected 14 -Message "Wrong focused-app gate exit code mismatch."
    Assert-Equal -Actual $wrongFocusedAppSummary.requiredGates.focusedApp.present -Expected $true -Message "Wrong focused-app artifact should be present."
    Assert-Equal -Actual $wrongFocusedAppSummary.requiredGates.focusedApp.packageVisible -Expected $false -Message "Wrong focused-app package should not be visible."
    Assert-Equal -Actual $wrongFocusedAppSummary.requiredGates.focusedApp.passed -Expected $false -Message "Wrong focused-app gate should fail."
    Assert-True -Condition ("focused app required but com.dnrohr.eulerianmagnification was not found in window_focus.txt" -in @($wrongFocusedAppSummary.warnings)) -Message "Wrong focused-app warning missing."

    $passingFocusedAppBundle = Join-Path $root "passing-focused-app"
    Copy-Item -LiteralPath $visualGateBundle -Destination $passingFocusedAppBundle -Recurse
    "mCurrentFocus=Window{456 u0 com.dnrohr.eulerianmagnification/.MainActivity}" |
        Out-File -LiteralPath (Join-Path $passingFocusedAppBundle "window_focus.txt") -Encoding utf8
    $passingFocusedAppExitCode = Invoke-Summary -BundlePath $passingFocusedAppBundle -RequireCleanSource -RequireFocusedApp
    $passingFocusedAppSummary = Get-Content -LiteralPath (Join-Path $passingFocusedAppBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $passingFocusedAppExitCode -Expected 0 -Message "Passing focused-app gate exit code mismatch."
    Assert-Equal -Actual $passingFocusedAppSummary.requiredGates.focusedApp.present -Expected $true -Message "Passing focused-app artifact should be present."
    Assert-Equal -Actual $passingFocusedAppSummary.requiredGates.focusedApp.packageVisible -Expected $true -Message "Passing focused-app package should be visible."
    Assert-Equal -Actual $passingFocusedAppSummary.requiredGates.focusedApp.passed -Expected $true -Message "Focused-app gate should pass."

    $missingFinalEvidenceExitCode = Invoke-Summary -BundlePath $visualGateBundle -RequireFinalVisualEvidence
    $missingFinalEvidenceSummary = Get-Content -LiteralPath (Join-Path $visualGateBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $missingFinalEvidenceExitCode -Expected 5 -Message "Missing final visual evidence exit code mismatch."
    Assert-Equal -Actual $missingFinalEvidenceSummary.requiredGates.cleanSource.required -Expected $true -Message "Final evidence should require clean source."
    Assert-Equal -Actual $missingFinalEvidenceSummary.requiredGates.visualValidation.required -Expected $true -Message "Final evidence should require visual validation."
    Assert-Equal -Actual $missingFinalEvidenceSummary.requiredGates.screenrecord.required -Expected $true -Message "Final evidence should require screenrecord."
    Assert-Equal -Actual $missingFinalEvidenceSummary.requiredGates.thermalReady.required -Expected $true -Message "Final evidence should require thermal readiness."
    Assert-Equal -Actual $missingFinalEvidenceSummary.requiredGates.cameraFps.required -Expected $true -Message "Final evidence should require camera FPS."
    Assert-Equal -Actual $missingFinalEvidenceSummary.requiredGates.focusedApp.required -Expected $true -Message "Final evidence should require focused app."
    Assert-Equal -Actual $missingFinalEvidenceSummary.requiredGates.noWarnings.required -Expected $true -Message "Final evidence should require no warnings."

    $passingFinalEvidenceBundle = Join-Path $root "passing-final-evidence"
    Copy-Item -LiteralPath $visualGateBundle -Destination $passingFinalEvidenceBundle -Recurse
    $passingFinalManifestPath = Join-Path $passingFinalEvidenceBundle "manifest.json"
    $passingFinalManifest = Get-Content -LiteralPath $passingFinalManifestPath -Raw | ConvertFrom-Json
    $passingFinalManifest.visualReview.visualValidated = $true
    $passingFinalManifest | ConvertTo-Json -Depth 8 | Out-File -LiteralPath $passingFinalManifestPath -Encoding utf8
    [System.IO.File]::WriteAllBytes(
        (Join-Path $passingFinalEvidenceBundle "screenrecord.mp4"),
        [byte[]](0, 0, 0, 24, 102, 116, 121, 112, 105, 115, 111, 109, 0, 0, 2, 0, 105, 115, 111, 109, 105, 115, 111, 50)
    )
    $passingFinalScreenrecordHash = (Get-FileHash -LiteralPath (Join-Path $passingFinalEvidenceBundle "screenrecord.mp4") -Algorithm SHA256).Hash
    Write-JsonFile -Path (Join-Path $passingFinalEvidenceBundle "review_contact_sheet_manifest.json") -Value ([ordered]@{
        screenrecordSha256 = $passingFinalScreenrecordHash
        contactSheetSha256 = "review-contact-sheet-sha256"
        columns = 3
        rows = 3
        frameWidth = 360
    })
    Write-JsonFile -Path (Join-Path $passingFinalEvidenceBundle "thermal_ready_wait.json") -Value ([ordered]@{
        ready = $true
        readyBelowThermalStatus = 4
        requiredReadySamples = 2
        consecutiveReadySamples = 2
    })
    "mCurrentFocus=Window{456 u0 com.dnrohr.eulerianmagnification/.MainActivity}" |
        Out-File -LiteralPath (Join-Path $passingFinalEvidenceBundle "window_focus.txt") -Encoding utf8

    $passingFinalEvidenceExitCode = Invoke-Summary -BundlePath $passingFinalEvidenceBundle -RequireFinalVisualEvidence
    $passingFinalEvidenceSummary = Get-Content -LiteralPath (Join-Path $passingFinalEvidenceBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $passingFinalEvidenceExitCode -Expected 0 -Message "Passing final visual evidence exit code mismatch."
    Assert-Equal -Actual $passingFinalEvidenceSummary.requiredGates.visualValidation.passed -Expected $true -Message "Final evidence visual gate should pass."
    Assert-Equal -Actual $passingFinalEvidenceSummary.requiredGates.screenrecord.passed -Expected $true -Message "Final evidence screenrecord gate should pass."
    Assert-Equal -Actual $passingFinalEvidenceSummary.requiredGates.thermalReady.passed -Expected $true -Message "Final evidence thermal-ready gate should pass."
    Assert-Equal -Actual $passingFinalEvidenceSummary.requiredGates.cameraFps.passed -Expected $true -Message "Final evidence camera FPS gate should pass."
    Assert-Equal -Actual $passingFinalEvidenceSummary.requiredGates.focusedApp.passed -Expected $true -Message "Final evidence focused-app gate should pass."
    Assert-Equal -Actual $passingFinalEvidenceSummary.requiredGates.noWarnings.passed -Expected $true -Message "Final evidence no-warnings gate should pass."
    Assert-Equal -Actual $passingFinalEvidenceSummary.artifacts.screenshot.sha256 -Expected (Get-FileHash -LiteralPath (Join-Path $passingFinalEvidenceBundle "screenshot.png") -Algorithm SHA256).Hash -Message "Final evidence screenshot hash mismatch."
    Assert-Equal -Actual $passingFinalEvidenceSummary.artifacts.screenrecord.sha256 -Expected (Get-FileHash -LiteralPath (Join-Path $passingFinalEvidenceBundle "screenrecord.mp4") -Algorithm SHA256).Hash -Message "Final evidence screenrecord hash mismatch."

    $missingOperatorNotesBundle = Join-Path $root "missing-operator-notes"
    Copy-Item -LiteralPath $passingFinalEvidenceBundle -Destination $missingOperatorNotesBundle -Recurse
    $missingOperatorNotesManifestPath = Join-Path $missingOperatorNotesBundle "manifest.json"
    $missingOperatorNotesManifest = Get-Content -LiteralPath $missingOperatorNotesManifestPath -Raw | ConvertFrom-Json
    $missingOperatorNotesManifest.visualReview.operatorNotes = ""
    $missingOperatorNotesManifest | ConvertTo-Json -Depth 8 | Out-File -LiteralPath $missingOperatorNotesManifestPath -Encoding utf8
    $missingOperatorNotesExitCode = Invoke-Summary -BundlePath $missingOperatorNotesBundle -RequireFinalVisualEvidence
    $missingOperatorNotesSummary = Get-Content -LiteralPath (Join-Path $missingOperatorNotesBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $missingOperatorNotesExitCode -Expected 7 -Message "Missing operator notes final evidence exit code mismatch."
    Assert-Equal -Actual $missingOperatorNotesSummary.requiredGates.visualValidation.passed -Expected $true -Message "Missing notes should still have accepted visual validation."
    Assert-Equal -Actual $missingOperatorNotesSummary.requiredGates.noWarnings.passed -Expected $false -Message "Missing notes should fail the final no-warnings gate."
    Assert-True -Condition ("visualValidated=true requires non-empty operator notes" -in @($missingOperatorNotesSummary.warnings)) -Message "Missing operator notes warning missing."

    $missingVisualReviewTextBundle = Join-Path $root "missing-visual-review-text"
    Copy-Item -LiteralPath $passingFinalEvidenceBundle -Destination $missingVisualReviewTextBundle -Recurse
    $missingVisualReviewTextManifestPath = Join-Path $missingVisualReviewTextBundle "manifest.json"
    $missingVisualReviewTextManifest = Get-Content -LiteralPath $missingVisualReviewTextManifestPath -Raw | ConvertFrom-Json
    $missingVisualReviewTextManifest.visualReview.targetDescription = ""
    $missingVisualReviewTextManifest.visualReview.visualClaim = ""
    $missingVisualReviewTextManifest | ConvertTo-Json -Depth 8 | Out-File -LiteralPath $missingVisualReviewTextManifestPath -Encoding utf8
    $missingVisualReviewTextExitCode = Invoke-Summary -BundlePath $missingVisualReviewTextBundle -RequireFinalVisualEvidence
    $missingVisualReviewTextSummary = Get-Content -LiteralPath (Join-Path $missingVisualReviewTextBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $missingVisualReviewTextExitCode -Expected 7 -Message "Missing visual-review text final evidence exit code mismatch."
    Assert-Equal -Actual $missingVisualReviewTextSummary.requiredGates.visualValidation.passed -Expected $true -Message "Missing visual-review text should still have accepted visual validation."
    Assert-Equal -Actual $missingVisualReviewTextSummary.requiredGates.noWarnings.passed -Expected $false -Message "Missing visual-review text should fail the final no-warnings gate."
    Assert-True -Condition ("visualValidated=true requires non-empty target description" -in @($missingVisualReviewTextSummary.warnings)) -Message "Missing target description warning missing."
    Assert-True -Condition ("visualValidated=true requires non-empty visual claim" -in @($missingVisualReviewTextSummary.warnings)) -Message "Missing visual claim warning missing."

    $rendererDiagnosticsExitCode = Invoke-Summary -BundlePath $visualGateBundle -RequireCleanSource -RequireRendererDiagnostics
    $rendererDiagnosticsSummary = Get-Content -LiteralPath (Join-Path $visualGateBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $rendererDiagnosticsExitCode -Expected 0 -Message "Renderer diagnostics gate exit code mismatch."
    Assert-Equal -Actual $rendererDiagnosticsSummary.requiredGates.rendererDiagnostics.required -Expected $true -Message "Renderer diagnostics gate should be required."
    Assert-Equal -Actual $rendererDiagnosticsSummary.requiredGates.rendererDiagnostics.passed -Expected $true -Message "Renderer diagnostics gate should pass."
    Assert-Equal -Actual $rendererDiagnosticsSummary.requiredGates.rendererDiagnostics.labelCount -Expected 1 -Message "Renderer diagnostics label count mismatch."

    $missingPhaseDiagnosticsExitCode = Invoke-Summary -BundlePath $visualGateBundle -RequireCleanSource -RequirePhaseDiagnostics
    $missingPhaseDiagnosticsSummary = Get-Content -LiteralPath (Join-Path $visualGateBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $missingPhaseDiagnosticsExitCode -Expected 10 -Message "Missing phase diagnostics gate exit code mismatch."
    Assert-Equal -Actual $missingPhaseDiagnosticsSummary.requiredGates.phaseDiagnostics.passed -Expected $false -Message "Missing phase diagnostics gate should fail."
    Assert-True -Condition ("phase diagnostics required but no phase labels were found in the UI dump" -in @($missingPhaseDiagnosticsSummary.warnings)) -Message "Missing phase diagnostics warning missing."

    $missingReviewSheetExitCode = Invoke-Summary -BundlePath $visualGateBundle -RequireCleanSource -RequireReviewContactSheet
    $missingReviewSheetSummary = Get-Content -LiteralPath (Join-Path $visualGateBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $missingReviewSheetExitCode -Expected 22 -Message "Review contact sheet gate should fail when screenrecord hash cannot be matched."
    Assert-Equal -Actual $missingReviewSheetSummary.requiredGates.reviewContactSheet.required -Expected $true -Message "Review contact sheet gate should be required."
    Assert-Equal -Actual $missingReviewSheetSummary.requiredGates.reviewContactSheet.present -Expected $true -Message "Review contact sheet should be present."
    Assert-Equal -Actual $missingReviewSheetSummary.requiredGates.reviewContactSheet.manifestPresent -Expected $true -Message "Review contact sheet manifest should be present."
    Assert-Equal -Actual $missingReviewSheetSummary.requiredGates.reviewContactSheet.screenrecordSha256Matches -Expected $null -Message "Review contact sheet match status should be unknown without screenrecord."
    Assert-Equal -Actual $missingReviewSheetSummary.requiredGates.reviewContactSheet.passed -Expected $false -Message "Unmatched review contact sheet should fail the gate."
    Assert-True -Condition ("review contact sheet required but manifest does not match screenrecord.mp4" -in @($missingReviewSheetSummary.warnings)) -Message "Missing review sheet match warning missing."

    $passingReviewSheetBundle = Join-Path $root "passing-review-sheet"
    Copy-Item -LiteralPath $visualGateBundle -Destination $passingReviewSheetBundle -Recurse
    [System.IO.File]::WriteAllBytes(
        (Join-Path $passingReviewSheetBundle "screenrecord.mp4"),
        [byte[]](0, 0, 0, 24, 102, 116, 121, 112, 105, 115, 111, 109, 0, 0, 2, 0, 105, 115, 111, 109, 105, 115, 111, 50)
    )
    $passingScreenrecordHash = (Get-FileHash -LiteralPath (Join-Path $passingReviewSheetBundle "screenrecord.mp4") -Algorithm SHA256).Hash
    Write-JsonFile -Path (Join-Path $passingReviewSheetBundle "review_contact_sheet_manifest.json") -Value ([ordered]@{
        screenrecordSha256 = $passingScreenrecordHash
        contactSheetSha256 = "review-contact-sheet-sha256"
        columns = 3
        rows = 3
        frameWidth = 360
    })
    $passingReviewSheetExitCode = Invoke-Summary -BundlePath $passingReviewSheetBundle -RequireCleanSource -RequireReviewContactSheet
    $passingReviewSheetSummary = Get-Content -LiteralPath (Join-Path $passingReviewSheetBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $passingReviewSheetExitCode -Expected 0 -Message "Matching review contact sheet gate should pass."
    Assert-Equal -Actual $passingReviewSheetSummary.artifacts.reviewContactSheet.screenrecordSha256Matches -Expected $true -Message "Matching review contact sheet should report true."
    Assert-Equal -Actual $passingReviewSheetSummary.requiredGates.reviewContactSheet.passed -Expected $true -Message "Matching review contact sheet should pass the required gate."

    $mismatchedReviewSheetBundle = Join-Path $root "mismatched-review-sheet"
    Copy-Item -LiteralPath $passingReviewSheetBundle -Destination $mismatchedReviewSheetBundle -Recurse
    Write-JsonFile -Path (Join-Path $mismatchedReviewSheetBundle "review_contact_sheet_manifest.json") -Value ([ordered]@{
        screenrecordSha256 = "wrong-screenrecord-sha256"
        contactSheetSha256 = "review-contact-sheet-sha256"
        columns = 3
        rows = 3
        frameWidth = 360
    })
    $mismatchedReviewSheetExitCode = Invoke-Summary -BundlePath $mismatchedReviewSheetBundle -RequireCleanSource -RequireReviewContactSheet
    $mismatchedReviewSheetSummary = Get-Content -LiteralPath (Join-Path $mismatchedReviewSheetBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $mismatchedReviewSheetExitCode -Expected 22 -Message "Mismatched review contact sheet gate should fail."
    Assert-Equal -Actual $mismatchedReviewSheetSummary.artifacts.reviewContactSheet.screenrecordSha256Matches -Expected $false -Message "Mismatched review contact sheet should report false."
    Assert-Equal -Actual $mismatchedReviewSheetSummary.requiredGates.reviewContactSheet.passed -Expected $false -Message "Mismatched review contact sheet should fail the required gate."
    Assert-True -Condition ("review contact sheet manifest screenrecord SHA-256 does not match screenrecord.mp4" -in @($mismatchedReviewSheetSummary.warnings)) -Message "Mismatched review sheet warning missing."

    $matchingVerdictExitCode = Invoke-Summary -BundlePath $visualGateBundle -RequireCleanSource -RequireEvidenceVerdict "target_visible_unvalidated"
    $matchingVerdictSummary = Get-Content -LiteralPath (Join-Path $visualGateBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $matchingVerdictExitCode -Expected 0 -Message "Matching evidence verdict gate exit code mismatch."
    Assert-Equal -Actual $matchingVerdictSummary.requiredGates.evidenceVerdict.required -Expected $true -Message "Evidence verdict gate should be required."
    Assert-Equal -Actual $matchingVerdictSummary.requiredGates.evidenceVerdict.expected -Expected "target_visible_unvalidated" -Message "Evidence verdict expected value mismatch."
    Assert-Equal -Actual $matchingVerdictSummary.requiredGates.evidenceVerdict.actual -Expected "target_visible_unvalidated" -Message "Evidence verdict actual value mismatch."
    Assert-Equal -Actual $matchingVerdictSummary.requiredGates.evidenceVerdict.passed -Expected $true -Message "Matching evidence verdict gate should pass."

    $mismatchedVerdictExitCode = Invoke-Summary -BundlePath $visualGateBundle -RequireCleanSource -RequireEvidenceVerdict "visual_validated"
    $mismatchedVerdictSummary = Get-Content -LiteralPath (Join-Path $visualGateBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $mismatchedVerdictExitCode -Expected 9 -Message "Mismatched evidence verdict gate exit code mismatch."
    Assert-Equal -Actual $mismatchedVerdictSummary.requiredGates.evidenceVerdict.passed -Expected $false -Message "Mismatched evidence verdict gate should fail."
    Assert-True -Condition ("evidence verdict required visual_validated but was target_visible_unvalidated" -in @($mismatchedVerdictSummary.warnings)) -Message "Mismatched evidence verdict warning missing."

    $missingScreenrecordExitCode = Invoke-Summary -BundlePath $visualGateBundle -RequireCleanSource -RequireScreenrecord
    $missingScreenrecordSummary = Get-Content -LiteralPath (Join-Path $visualGateBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $missingScreenrecordExitCode -Expected 11 -Message "Missing screenrecord gate exit code mismatch."
    Assert-Equal -Actual $missingScreenrecordSummary.requiredGates.screenrecord.required -Expected $true -Message "Screenrecord gate should be required."
    Assert-Equal -Actual $missingScreenrecordSummary.requiredGates.screenrecord.present -Expected $false -Message "Screenrecord should be missing."
    Assert-Equal -Actual $missingScreenrecordSummary.requiredGates.screenrecord.passed -Expected $false -Message "Missing screenrecord should not pass."
    Assert-True -Condition ("screenrecord required but screenrecord.mp4 is missing" -in @($missingScreenrecordSummary.warnings)) -Message "Missing screenrecord warning missing."

    $invalidScreenrecordBundle = Join-Path $root "invalid-screenrecord"
    Copy-Item -LiteralPath $visualGateBundle -Destination $invalidScreenrecordBundle -Recurse
    "synthetic non-mp4 placeholder" | Out-File -LiteralPath (Join-Path $invalidScreenrecordBundle "screenrecord.mp4") -Encoding utf8
    $invalidScreenrecordExitCode = Invoke-Summary -BundlePath $invalidScreenrecordBundle -RequireCleanSource -RequireScreenrecord
    $invalidScreenrecordSummary = Get-Content -LiteralPath (Join-Path $invalidScreenrecordBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $invalidScreenrecordExitCode -Expected 11 -Message "Invalid screenrecord gate exit code mismatch."
    Assert-Equal -Actual $invalidScreenrecordSummary.requiredGates.screenrecord.present -Expected $true -Message "Invalid screenrecord should be present."
    Assert-Equal -Actual $invalidScreenrecordSummary.requiredGates.screenrecord.mp4Signature -Expected $false -Message "Invalid screenrecord should not have an MP4 signature."
    Assert-Equal -Actual $invalidScreenrecordSummary.requiredGates.screenrecord.passed -Expected $false -Message "Invalid screenrecord should not pass."
    Assert-True -Condition ("screenrecord required but screenrecord.mp4 does not look like an MP4 file" -in @($invalidScreenrecordSummary.warnings)) -Message "Invalid screenrecord warning missing."

    $passingScreenrecordBundle = Join-Path $root "passing-screenrecord"
    Copy-Item -LiteralPath $visualGateBundle -Destination $passingScreenrecordBundle -Recurse
    [System.IO.File]::WriteAllBytes(
        (Join-Path $passingScreenrecordBundle "screenrecord.mp4"),
        [byte[]](0, 0, 0, 24, 102, 116, 121, 112, 105, 115, 111, 109, 0, 0, 2, 0, 105, 115, 111, 109, 105, 115, 111, 50)
    )
    $passingScreenrecordExitCode = Invoke-Summary -BundlePath $passingScreenrecordBundle -RequireCleanSource -RequireScreenrecord
    $passingScreenrecordSummary = Get-Content -LiteralPath (Join-Path $passingScreenrecordBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $passingScreenrecordExitCode -Expected 0 -Message "Passing screenrecord gate exit code mismatch."
    Assert-Equal -Actual $passingScreenrecordSummary.requiredGates.screenrecord.present -Expected $true -Message "Screenrecord should be present."
    Assert-True -Condition ($passingScreenrecordSummary.requiredGates.screenrecord.bytes -gt 0) -Message "Screenrecord bytes should be positive."
    Assert-Equal -Actual $passingScreenrecordSummary.artifacts.screenrecord.sha256 -Expected (Get-FileHash -LiteralPath (Join-Path $passingScreenrecordBundle "screenrecord.mp4") -Algorithm SHA256).Hash -Message "Passing screenrecord hash mismatch."
    Assert-Equal -Actual $passingScreenrecordSummary.requiredGates.screenrecord.mp4Signature -Expected $true -Message "Screenrecord should have an MP4 signature."
    Assert-Equal -Actual $passingScreenrecordSummary.requiredGates.screenrecord.passed -Expected $true -Message "Screenrecord gate should pass."

    $missingThermalReadyExitCode = Invoke-Summary -BundlePath $visualGateBundle -RequireCleanSource -RequireThermalReady
    $missingThermalReadySummary = Get-Content -LiteralPath (Join-Path $visualGateBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $missingThermalReadyExitCode -Expected 12 -Message "Missing thermal-ready gate exit code mismatch."
    Assert-Equal -Actual $missingThermalReadySummary.requiredGates.thermalReady.required -Expected $true -Message "Thermal-ready gate should be required."
    Assert-Equal -Actual $missingThermalReadySummary.requiredGates.thermalReady.present -Expected $false -Message "Thermal-ready artifact should be missing."
    Assert-Equal -Actual $missingThermalReadySummary.requiredGates.thermalReady.passed -Expected $false -Message "Missing thermal-ready artifact should not pass."
    Assert-True -Condition ("thermal readiness required but thermal_ready_wait.json is missing" -in @($missingThermalReadySummary.warnings)) -Message "Missing thermal-ready warning missing."

    $failedThermalReadyBundle = Join-Path $root "failed-thermal-ready"
    Copy-Item -LiteralPath $visualGateBundle -Destination $failedThermalReadyBundle -Recurse
    Write-JsonFile -Path (Join-Path $failedThermalReadyBundle "thermal_ready_wait.json") -Value ([ordered]@{
        ready = $false
        readyBelowThermalStatus = 4
        requiredReadySamples = 2
        consecutiveReadySamples = 0
    })
    $failedThermalReadyExitCode = Invoke-Summary -BundlePath $failedThermalReadyBundle -RequireCleanSource -RequireThermalReady
    $failedThermalReadySummary = Get-Content -LiteralPath (Join-Path $failedThermalReadyBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $failedThermalReadyExitCode -Expected 12 -Message "Failed thermal-ready gate exit code mismatch."
    Assert-Equal -Actual $failedThermalReadySummary.requiredGates.thermalReady.present -Expected $true -Message "Failed thermal-ready artifact should be present."
    Assert-Equal -Actual $failedThermalReadySummary.requiredGates.thermalReady.ready -Expected $false -Message "Failed thermal-ready artifact should report ready=false."
    Assert-Equal -Actual $failedThermalReadySummary.requiredGates.thermalReady.passed -Expected $false -Message "Failed thermal-ready artifact should not pass."
    Assert-True -Condition ("thermal readiness required but wait result was not ready" -in @($failedThermalReadySummary.warnings)) -Message "Failed thermal-ready warning missing."

    $passingThermalReadyBundle = Join-Path $root "passing-thermal-ready"
    Copy-Item -LiteralPath $visualGateBundle -Destination $passingThermalReadyBundle -Recurse
    Write-JsonFile -Path (Join-Path $passingThermalReadyBundle "thermal_ready_wait.json") -Value ([ordered]@{
        ready = $true
        readyBelowThermalStatus = 4
        requiredReadySamples = 2
        consecutiveReadySamples = 2
    })
    $passingThermalReadyExitCode = Invoke-Summary -BundlePath $passingThermalReadyBundle -RequireCleanSource -RequireThermalReady
    $passingThermalReadySummary = Get-Content -LiteralPath (Join-Path $passingThermalReadyBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $passingThermalReadyExitCode -Expected 0 -Message "Passing thermal-ready gate exit code mismatch."
    Assert-Equal -Actual $passingThermalReadySummary.requiredGates.thermalReady.present -Expected $true -Message "Passing thermal-ready artifact should be present."
    Assert-Equal -Actual $passingThermalReadySummary.requiredGates.thermalReady.ready -Expected $true -Message "Passing thermal-ready artifact should report ready=true."
    Assert-Equal -Actual $passingThermalReadySummary.requiredGates.thermalReady.passed -Expected $true -Message "Thermal-ready gate should pass."

    $dirtyGateBundle = Join-Path $root "dirty-gate"
    Copy-Item -LiteralPath $visualGateBundle -Destination $dirtyGateBundle -Recurse
    $dirtyManifestPath = Join-Path $dirtyGateBundle "manifest.json"
    $dirtyManifest = Get-Content -LiteralPath $dirtyManifestPath -Raw | ConvertFrom-Json
    $dirtyManifest.source.dirty = $true
    $dirtyManifest.visualReview.visualValidated = $true
    $dirtyManifest | ConvertTo-Json -Depth 8 | Out-File -LiteralPath $dirtyManifestPath -Encoding utf8

    $dirtyGateExitCode = Invoke-Summary -BundlePath $dirtyGateBundle -RequireCleanSource -RequireVisualValidation
    $dirtyGateSummary = Get-Content -LiteralPath (Join-Path $dirtyGateBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $dirtyGateExitCode -Expected 6 -Message "Clean source gate exit code mismatch."
    Assert-Equal -Actual $dirtyGateSummary.requiredGates.cleanSource.passed -Expected $false -Message "Clean source gate should fail."
    Assert-Equal -Actual $dirtyGateSummary.requiredGates.visualValidation.passed -Expected $true -Message "Visual gate should pass for accepted target."
    Assert-True -Condition ("clean source required but worktree was dirty during capture" -in @($dirtyGateSummary.warnings)) -Message "Clean source gate warning missing."

    $warningGateBundle = Join-Path $root "warning-gate"
    Copy-Item -LiteralPath $visualGateBundle -Destination $warningGateBundle -Recurse
    $warningManifestPath = Join-Path $warningGateBundle "manifest.json"
    $warningManifest = Get-Content -LiteralPath $warningManifestPath -Raw | ConvertFrom-Json
    $warningManifest.visualReview.visualValidated = $true
    $warningManifest.warnings = @("synthetic advisory warning")
    $warningManifest | ConvertTo-Json -Depth 8 | Out-File -LiteralPath $warningManifestPath -Encoding utf8

    $warningGateExitCode = Invoke-Summary -BundlePath $warningGateBundle -RequireCleanSource -RequireVisualValidation -RequireNoWarnings
    $warningGateSummary = Get-Content -LiteralPath (Join-Path $warningGateBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $warningGateExitCode -Expected 7 -Message "No-warnings gate exit code mismatch."
    Assert-Equal -Actual $warningGateSummary.requiredGates.cleanSource.passed -Expected $true -Message "Warning gate should pass clean source."
    Assert-Equal -Actual $warningGateSummary.requiredGates.visualValidation.passed -Expected $true -Message "Warning gate should pass visual validation."
    Assert-Equal -Actual $warningGateSummary.requiredGates.noWarnings.passed -Expected $false -Message "No-warnings gate should fail."
    Assert-Equal -Actual $warningGateSummary.requiredGates.noWarnings.warningCount -Expected 1 -Message "No-warnings gate warning count mismatch."
    Assert-True -Condition ("no warnings required but summary has 1 warning(s)" -in @($warningGateSummary.warnings)) -Message "No-warnings gate warning missing."

    $unpushedSourceBundle = Join-Path $root "unpushed-source-gate"
    Copy-Item -LiteralPath $visualGateBundle -Destination $unpushedSourceBundle -Recurse
    $unpushedSourceManifestPath = Join-Path $unpushedSourceBundle "manifest.json"
    $unpushedSourceManifest = Get-Content -LiteralPath $unpushedSourceManifestPath -Raw | ConvertFrom-Json
    $unpushedSourceManifest.source | Add-Member -NotePropertyName commitReachableFromOriginMain -NotePropertyValue $false -Force
    $unpushedSourceManifest.visualReview.visualValidated = $true
    $unpushedSourceManifest | ConvertTo-Json -Depth 8 | Out-File -LiteralPath $unpushedSourceManifestPath -Encoding utf8

    $unpushedSourceExitCode = Invoke-Summary -BundlePath $unpushedSourceBundle -RequireCleanSource -RequireVisualValidation -RequireNoWarnings
    $unpushedSourceSummary = Get-Content -LiteralPath (Join-Path $unpushedSourceBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $unpushedSourceExitCode -Expected 7 -Message "Unpushed-source warning gate exit code mismatch."
    Assert-Equal -Actual $unpushedSourceSummary.requiredGates.cleanSource.passed -Expected $true -Message "Unpushed-source bundle should still pass clean-source gate."
    Assert-Equal -Actual $unpushedSourceSummary.requiredGates.cleanSource.commitReachableFromOriginMain -Expected $false -Message "Clean-source gate should report source reachability."
    Assert-Equal -Actual $unpushedSourceSummary.requiredGates.visualValidation.passed -Expected $true -Message "Unpushed-source bundle should pass visual validation."
    Assert-Equal -Actual $unpushedSourceSummary.requiredGates.noWarnings.passed -Expected $false -Message "Unpushed-source warning should fail no-warnings gate."
    Assert-True -Condition ("source commit was not reachable from origin/main during capture" -in @($unpushedSourceSummary.warnings)) -Message "Unpushed-source warning missing."

    $lateWarningBundle = Join-Path $root "late-warning-gate"
    Copy-Item -LiteralPath $warningGateBundle -Destination $lateWarningBundle -Recurse
    Write-TestLandscapeScreenshot -Path (Join-Path $lateWarningBundle "screenshot.png")
    $lateWarningManifestPath = Join-Path $lateWarningBundle "manifest.json"
    $lateWarningManifest = Get-Content -LiteralPath $lateWarningManifestPath -Raw | ConvertFrom-Json
    $lateWarningManifest.warnings = @()
    $lateWarningManifest | ConvertTo-Json -Depth 8 | Out-File -LiteralPath $lateWarningManifestPath -Encoding utf8

    $lateWarningExitCode = Invoke-Summary -BundlePath $lateWarningBundle -RequireCleanSource -RequireNoWarnings
    $lateWarningSummary = Get-Content -LiteralPath (Join-Path $lateWarningBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $lateWarningExitCode -Expected 7 -Message "Late no-warnings gate exit code mismatch."
    Assert-Equal -Actual $lateWarningSummary.evidenceVerdict.status -Expected "wrong_orientation" -Message "Late warning verdict mismatch."
    Assert-Equal -Actual $lateWarningSummary.requiredGates.noWarnings.warningCount -Expected 1 -Message "Late no-warnings gate warning count mismatch."
    Assert-True -Condition ("screenshot is not portrait-oriented" -in @($lateWarningSummary.warnings)) -Message "Late screenshot warning missing."

    $acceptedWrongOrientationBundle = Join-Path $root "accepted-wrong-orientation"
    Copy-Item -LiteralPath $lateWarningBundle -Destination $acceptedWrongOrientationBundle -Recurse
    $acceptedWrongOrientationExitCode = Invoke-Summary -BundlePath $acceptedWrongOrientationBundle -RequireCleanSource -RequireVisualValidation
    $acceptedWrongOrientationSummary = Get-Content -LiteralPath (Join-Path $acceptedWrongOrientationBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $acceptedWrongOrientationExitCode -Expected 5 -Message "Accepted wrong-orientation visual gate exit code mismatch."
    Assert-Equal -Actual $acceptedWrongOrientationSummary.visualReview.countsAsVisualValidation -Expected $true -Message "Operator visual review should be accepted."
    Assert-Equal -Actual $acceptedWrongOrientationSummary.evidenceVerdict.status -Expected "wrong_orientation" -Message "Accepted wrong-orientation verdict mismatch."
    Assert-Equal -Actual $acceptedWrongOrientationSummary.requiredGates.visualValidation.passed -Expected $false -Message "Final verdict should fail visual gate."
    Assert-True -Condition ("visual validation required but evidence verdict does not count as visual validation" -in @($acceptedWrongOrientationSummary.warnings)) -Message "Verdict-based visual gate warning missing."

    $missingRoiMeasurementExitCode = Invoke-Summary -BundlePath $visualGateBundle -RequireCleanSource -RequireRoiMeasurement
    $missingRoiMeasurementSummary = Get-Content -LiteralPath (Join-Path $visualGateBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $missingRoiMeasurementExitCode -Expected 8 -Message "Missing ROI measurement gate exit code mismatch."
    Assert-Equal -Actual $missingRoiMeasurementSummary.requiredGates.roiMeasurement.required -Expected $true -Message "ROI measurement gate should be required."
    Assert-Equal -Actual $missingRoiMeasurementSummary.requiredGates.roiMeasurement.present -Expected $false -Message "ROI measurement should be missing."
    Assert-Equal -Actual $missingRoiMeasurementSummary.requiredGates.roiMeasurement.passed -Expected $false -Message "Missing ROI measurement should not pass."
    Assert-True -Condition ("ROI measurement required but roi_overlay_measurement.json is missing" -in @($missingRoiMeasurementSummary.warnings)) -Message "Missing ROI measurement warning missing."

    $passingRoiMeasurementBundle = Join-Path $root "passing-roi-measurement"
    Copy-Item -LiteralPath $visualGateBundle -Destination $passingRoiMeasurementBundle -Recurse
    Write-JsonFile -Path (Join-Path $passingRoiMeasurementBundle "roi_overlay_measurement.json") -Value ([ordered]@{
        passed = $true
        overlayKind = "Manual"
        expectedRoi = [ordered]@{
            left = 0.25
            top = 0.25
            right = 0.75
            bottom = 0.75
        }
    })
    $passingRoiMeasurementExitCode = Invoke-Summary -BundlePath $passingRoiMeasurementBundle -RequireCleanSource -RequireRoiMeasurement
    $passingRoiMeasurementSummary = Get-Content -LiteralPath (Join-Path $passingRoiMeasurementBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $passingRoiMeasurementExitCode -Expected 0 -Message "Passing ROI measurement gate exit code mismatch."
    Assert-Equal -Actual $passingRoiMeasurementSummary.requiredGates.roiMeasurement.present -Expected $true -Message "ROI measurement should be present."
    Assert-Equal -Actual $passingRoiMeasurementSummary.requiredGates.roiMeasurement.passed -Expected $true -Message "ROI measurement gate should pass."

    Write-Output "Live validation summary self-test passed: $root"
} finally {
    if ([string]::IsNullOrWhiteSpace($OutputRoot) -and (Test-Path -LiteralPath $root)) {
        Remove-Item -LiteralPath $root -Recurse -Force
    }
}
