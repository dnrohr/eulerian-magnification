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

function Invoke-ReadinessExitCode {
    param(
        [string]$FixtureRoot,
        [string]$OutputPath
    )

    $script = Join-Path $PSScriptRoot "export_pixel_session_readiness.ps1"
    $powerShellExe = (Get-Process -Id $PID).Path
    & $powerShellExe -NoProfile -File $script -FixtureRoot $FixtureRoot -OutputPath $OutputPath -FailOnNotReady *> $null
    return $LASTEXITCODE
}

$root = Join-Path ([System.IO.Path]::GetTempPath()) "eulerian-session-readiness-$([guid]::NewGuid().ToString('N'))"
$readyFixture = Join-Path $root "ready"
$hotFixture = Join-Path $root "hot"
New-Item -ItemType Directory -Path $readyFixture -Force | Out-Null
New-Item -ItemType Directory -Path $hotFixture -Force | Out-Null

@"
Thermal Status: 1
Temperature{mValue=34.2, mType=0, mName=skin, mStatus=1}
"@ | Out-File -LiteralPath (Join-Path $readyFixture "thermalservice.txt") -Encoding utf8
@"
Current Battery Service state:
  AC powered: false
  USB powered: true
  temperature: 325
"@ | Out-File -LiteralPath (Join-Path $readyFixture "battery.txt") -Encoding utf8
@"
mCurrentFocus=Window{abc u0 com.dnrohr.eulerianmagnification/com.dnrohr.eulerianmagnification.MainActivity}
mFocusedApp=ActivityRecord{def u0 com.dnrohr.eulerianmagnification/.MainActivity}
"@ | Out-File -LiteralPath (Join-Path $readyFixture "window_focus.txt") -Encoding utf8
@"
Total frames rendered: 120
Janky frames: 5 (4.2%)
95th percentile: 32ms
"@ | Out-File -LiteralPath (Join-Path $readyFixture "gfxinfo.txt") -Encoding utf8

$readyOutput = Join-Path $root "ready.json"
$readyText = & (Join-Path $PSScriptRoot "export_pixel_session_readiness.ps1") -FixtureRoot $readyFixture -OutputPath $readyOutput
$ready = Get-Content -LiteralPath $readyOutput -Raw | ConvertFrom-Json
Assert-True -Condition (($readyText -join "`n").Contains("Pixel session readiness: ready")) -Message "Ready fixture should print ready status."
Assert-True -Condition (($readyText -join "`n").Contains("Setup readiness: ready")) -Message "Ready fixture should print setup readiness."
Assert-Equal -Actual $ready.readyForWatchedCapture -Expected $true -Message "Ready fixture should be ready for watched capture."
Assert-Equal -Actual $ready.readyForSetupCapture -Expected $true -Message "Ready fixture should be ready for setup capture."
Assert-Equal -Actual @($ready.issues).Count -Expected 0 -Message "Ready fixture should have no issues."
Assert-Equal -Actual @($ready.setupIssues).Count -Expected 0 -Message "Ready fixture should have no setup issues."
Assert-True -Condition ("Device looks ready; proceed only when the watched target is physically set up." -in @($ready.recommendedActions)) -Message "Ready fixture should include a proceed action."
Assert-Equal -Actual $ready.thermal.status -Expected 1 -Message "Ready fixture thermal status mismatch."
Assert-Equal -Actual $ready.battery.temperatureC -Expected 32.5 -Message "Ready fixture battery temperature mismatch."
Assert-Equal -Actual $ready.focusedApp.packageVisible -Expected $true -Message "Ready fixture should report focused app."
Assert-Equal -Actual $ready.gfxinfo.jankyPercent -Expected 4.2 -Message "Ready fixture janky percent mismatch."
Assert-Equal -Actual $ready.gfxinfo.frameP95Ms -Expected 32 -Message "Ready fixture p95 frame time mismatch."

@"
Thermal Status: 2
Temperature{mValue=39.7, mType=0, mName=skin, mStatus=2}
"@ | Out-File -LiteralPath (Join-Path $hotFixture "thermalservice.txt") -Encoding utf8
@"
Current Battery Service state:
  USB powered: true
  temperature: 401
"@ | Out-File -LiteralPath (Join-Path $hotFixture "battery.txt") -Encoding utf8
@"
mCurrentFocus=Window{abc u0 com.example.other/.MainActivity}
"@ | Out-File -LiteralPath (Join-Path $hotFixture "window_focus.txt") -Encoding utf8
@"
Total frames rendered: 200
Janky frames: 50 (25.0%)
95th percentile: 80ms
"@ | Out-File -LiteralPath (Join-Path $hotFixture "gfxinfo.txt") -Encoding utf8

$hotOutput = Join-Path $root "hot.json"
$hotExitCode = Invoke-ReadinessExitCode -FixtureRoot $hotFixture -OutputPath $hotOutput
$hot = Get-Content -LiteralPath $hotOutput -Raw | ConvertFrom-Json
Assert-Equal -Actual $hotExitCode -Expected 31 -Message "FailOnNotReady should exit 31 for a not-ready fixture."
Assert-Equal -Actual $hot.readyForWatchedCapture -Expected $false -Message "Hot fixture should not be ready for watched capture."
Assert-Equal -Actual $hot.readyForSetupCapture -Expected $false -Message "Wrong focused app should still block non-final setup capture."
Assert-True -Condition ("focused app is not com.dnrohr.eulerianmagnification" -in @($hot.issues)) -Message "Hot fixture should flag wrong focused app."
Assert-True -Condition ("focused app is not com.dnrohr.eulerianmagnification" -in @($hot.setupIssues)) -Message "Hot fixture setup issues should still flag wrong focused app."
Assert-True -Condition (-not ((@($hot.setupIssues) -join "`n").Contains("setup threshold"))) -Message "Moderate thermal fixture should not add a setup thermal issue."
Assert-True -Condition ((@($hot.issues) -join "`n").Contains("is not below readiness threshold")) -Message "Hot fixture should flag thermal readiness."
Assert-True -Condition ((@($hot.warnings) -join "`n").Contains("battery temperature")) -Message "Hot fixture should warn about battery temperature."
Assert-True -Condition ((@($hot.warnings) -join "`n").Contains("95th percentile frame time")) -Message "Hot fixture should warn about frame time."
Assert-True -Condition ((@($hot.warnings) -join "`n").Contains("janky frames")) -Message "Hot fixture should warn about jank."
Assert-True -Condition ((@($hot.recommendedActions) -join "`n").Contains("Let the phone cool")) -Message "Hot fixture should recommend cooling before final visual acceptance."
Assert-True -Condition ((@($hot.recommendedActions) -join "`n").Contains("Bring com.dnrohr.eulerianmagnification to the foreground")) -Message "Hot fixture should recommend focusing the app."
Assert-True -Condition ((@($hot.recommendedActions) -join "`n").Contains("Unplug the phone")) -Message "Hot fixture should recommend reducing charging heat."

$severeFixture = Join-Path $root "severe"
New-Item -ItemType Directory -Path $severeFixture -Force | Out-Null
@"
Thermal Status: 3
Temperature{mValue=69.0, mType=0, mName=MID, mStatus=3}
"@ | Out-File -LiteralPath (Join-Path $severeFixture "thermalservice.txt") -Encoding utf8
@"
Current Battery Service state:
  USB powered: true
  temperature: 402
"@ | Out-File -LiteralPath (Join-Path $severeFixture "battery.txt") -Encoding utf8
@"
mCurrentFocus=Window{abc u0 com.dnrohr.eulerianmagnification/com.dnrohr.eulerianmagnification.MainActivity}
"@ | Out-File -LiteralPath (Join-Path $severeFixture "window_focus.txt") -Encoding utf8
@"
Total frames rendered: 200
Janky frames: 4 (2.0%)
95th percentile: 15ms
"@ | Out-File -LiteralPath (Join-Path $severeFixture "gfxinfo.txt") -Encoding utf8

$severeOutput = Join-Path $root "severe.json"
$severeExitCode = Invoke-ReadinessExitCode -FixtureRoot $severeFixture -OutputPath $severeOutput
$severe = Get-Content -LiteralPath $severeOutput -Raw | ConvertFrom-Json
Assert-Equal -Actual $severeExitCode -Expected 31 -Message "Severe thermal fixture should fail final readiness."
Assert-Equal -Actual $severe.readyForWatchedCapture -Expected $false -Message "Severe fixture should not be ready for watched capture."
Assert-Equal -Actual $severe.readyForSetupCapture -Expected $false -Message "Severe fixture should not be ready for setup capture."
Assert-True -Condition ((@($severe.setupIssues) -join "`n").Contains("setup threshold")) -Message "Severe fixture should report a setup threshold issue."
Assert-True -Condition ((@($severe.recommendedActions) -join "`n").Contains("Stop live preview")) -Message "Severe fixture should recommend stopping preview while cooling."

Write-Output "Pixel session readiness self-test passed."
