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
        [switch]$RequireCleanSource,
        [switch]$RequireVisualValidation,
        [switch]$RequireNoWarnings,
        [switch]$RequireRoiMeasurement,
        [switch]$RequireRendererDiagnostics,
        [switch]$RequirePhaseDiagnostics,
        [string]$RequireEvidenceVerdict = ""
    )

    $summaryScript = Join-Path $PSScriptRoot "summarize_live_validation_evidence.ps1"
    $stdoutPath = Join-Path $BundlePath "summary_stdout.txt"
    $summaryArgs = @{
        BundlePath = $BundlePath
    }
    if ($RequireCleanSource) { $summaryArgs.RequireCleanSource = $true }
    if ($RequireVisualValidation) { $summaryArgs.RequireVisualValidation = $true }
    if ($RequireNoWarnings) { $summaryArgs.RequireNoWarnings = $true }
    if ($RequireRoiMeasurement) { $summaryArgs.RequireRoiMeasurement = $true }
    if ($RequireRendererDiagnostics) { $summaryArgs.RequireRendererDiagnostics = $true }
    if ($RequirePhaseDiagnostics) { $summaryArgs.RequirePhaseDiagnostics = $true }
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

    $visualGateExitCode = Invoke-Summary -BundlePath $visualGateBundle -RequireCleanSource -RequireVisualValidation
    $visualGateSummary = Get-Content -LiteralPath (Join-Path $visualGateBundle "evidence_summary.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $visualGateExitCode -Expected 5 -Message "Visual gate exit code mismatch."
    Assert-Equal -Actual $visualGateSummary.passedRuntimeSmoke -Expected $true -Message "Visual gate should pass runtime smoke."
    Assert-Equal -Actual $visualGateSummary.requiredGates.cleanSource.passed -Expected $true -Message "Clean source gate should pass."
    Assert-Equal -Actual $visualGateSummary.requiredGates.visualValidation.passed -Expected $false -Message "Visual validation gate should fail."
    Assert-True -Condition ("visual validation required but evidence verdict does not count as visual validation" -in @($visualGateSummary.warnings)) -Message "Visual gate warning missing."

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
