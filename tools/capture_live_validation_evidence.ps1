param(
    [string]$OutputRoot = "sample-videos\exports\live-validation",
    [string]$Label = "",
    [string]$Package = "com.dnrohr.eulerianmagnification",
    [int]$ScreenRecordSeconds = 0,
    [switch]$SkipLaunch,
    [string]$Mode = "",
    [string]$View = "",
    [string]$RoiSource = "",
    [string]$ManualRoi = "",
    [string]$Panel = "",
    [Nullable[double]]$Amplification = $null,
    [Nullable[bool]]$GlPreview = $null,
    [Nullable[bool]]$Controls = $null,
    [Nullable[bool]]$Clean = $null,
    [Nullable[bool]]$LockAeAwb = $null,
    [string]$MeasureRoiExpected = "",
    [ValidateSet("", "Manual", "Auto")]
    [string]$MeasureRoiKind = "",
    [int]$MeasureRoiColorTolerance = 42,
    [double]$MeasureRoiSearchMargin = 0.08,
    [double]$MeasureRoiMaxEdgeError = 0.04,
    [int]$MeasureRoiMinimumMatchedPixels = 24,
    [switch]$MeasureRoiAllowMultipleComponents,
    [string[]]$RequireUiText = @(),
    [switch]$PreserveLogcat,
    [switch]$PersistLaunchSettings,
    [switch]$Summarize
)

$ErrorActionPreference = "Stop"

function Find-Adb {
    $sdkAdb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
    if (Test-Path -LiteralPath $sdkAdb) {
        return $sdkAdb
    }

    $pathAdb = Get-Command adb -ErrorAction SilentlyContinue
    if ($pathAdb) {
        return $pathAdb.Source
    }

    throw "adb.exe was not found in LOCALAPPDATA Android SDK or PATH."
}

function Run-AdbText {
    param(
        [string]$Adb,
        [string[]]$Arguments,
        [string]$OutputPath,
        [int]$TimeoutSeconds = 12
    )

    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $Adb
    $startInfo.Arguments = ($Arguments | ForEach-Object {
        if ($_ -match '[\s"]') {
            '"' + ($_ -replace '"', '\"') + '"'
        } else {
            $_
        }
    }) -join " "
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    [void]$process.Start()
    $stdoutTask = $process.StandardOutput.ReadToEndAsync()
    $stderrTask = $process.StandardError.ReadToEndAsync()

    if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
        $process.Kill($true)
        $process.WaitForExit()
        $text = @(
            "adb command timed out after $TimeoutSeconds seconds:",
            ($Arguments -join " ")
        )
    } else {
        $text = @(
            $stdoutTask.Result,
            $stderrTask.Result,
            "adb exit code: $($process.ExitCode)"
        )
    }

    $text | Out-File -LiteralPath $OutputPath -Encoding utf8
}

function Find-PowerShell {
    $pwsh = Get-Command pwsh -ErrorAction SilentlyContinue
    if ($pwsh) {
        return $pwsh.Source
    }
    $powershell = Get-Command powershell -ErrorAction SilentlyContinue
    if ($powershell) {
        return $powershell.Source
    }
    throw "No PowerShell executable was found for child validation scripts."
}

function Get-GitValue {
    param([string[]]$Arguments)

    $git = Get-Command git -ErrorAction SilentlyContinue
    if (-not $git) {
        return $null
    }
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $output = & $git.Source @Arguments 2>$null
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = $previousErrorActionPreference
    if ($exitCode -ne 0) {
        return $null
    }
    return (($output | Out-String).Trim())
}

function Get-GitMetadata {
    $commit = Get-GitValue -Arguments @("rev-parse", "HEAD")
    $shortCommit = Get-GitValue -Arguments @("rev-parse", "--short", "HEAD")
    $branch = Get-GitValue -Arguments @("branch", "--show-current")
    $status = Get-GitValue -Arguments @("status", "--short")
    $statusLines = @()
    if (-not [string]::IsNullOrWhiteSpace($status)) {
        $statusLines = @($status -split "`r?`n")
    }
    return [ordered]@{
        commit = $commit
        shortCommit = $shortCommit
        branch = $branch
        dirty = -not [string]::IsNullOrWhiteSpace($status)
        statusShort = $statusLines
    }
}

$adb = Find-Adb
$source = Get-GitMetadata
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$safeLabel = if ([string]::IsNullOrWhiteSpace($Label)) { "capture" } else { $Label -replace "[^A-Za-z0-9._-]", "-" }
$outputDir = Join-Path $OutputRoot "$timestamp-$safeLabel"
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

$artifacts = [ordered]@{}
$warnings = @()

if ($Controls -eq $true -and $Panel -eq "Debug") {
    $warnings += "Debug controls are useful for renderer diagnostics but can add UI jank; use hidden controls or Clean mode for visual-quality captures."
}

$devicesPath = Join-Path $outputDir "adb_devices.txt"
Run-AdbText -Adb $adb -Arguments @("devices") -OutputPath $devicesPath
$artifacts.devices = $devicesPath

if (-not $PreserveLogcat) {
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    & $adb logcat -c *> $null
    $ErrorActionPreference = $previousErrorActionPreference
}

if (-not $SkipLaunch) {
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    & $adb shell am force-stop $Package *> $null
    Start-Sleep -Milliseconds 500
    $launchArgs = @(
        "shell",
        "am",
        "start",
        "-n",
        "$Package/.MainActivity"
    )
    if (-not [string]::IsNullOrWhiteSpace($Mode)) {
        $launchArgs += @("--es", "validation.mode", $Mode)
    }
    if (-not [string]::IsNullOrWhiteSpace($View)) {
        $launchArgs += @("--es", "validation.view", $View)
    }
    if (-not [string]::IsNullOrWhiteSpace($RoiSource)) {
        $launchArgs += @("--es", "validation.roiSource", $RoiSource)
    }
    if (-not [string]::IsNullOrWhiteSpace($ManualRoi)) {
        $launchArgs += @("--es", "validation.manualRoi", $ManualRoi)
    }
    if (-not [string]::IsNullOrWhiteSpace($Panel)) {
        $launchArgs += @("--es", "validation.panel", $Panel)
    }
    if ($PSBoundParameters.ContainsKey("Amplification")) {
        $launchArgs += @("--es", "validation.amplification", $Amplification.ToString([Globalization.CultureInfo]::InvariantCulture))
    }
    if ($PSBoundParameters.ContainsKey("GlPreview")) {
        $launchArgs += @("--ez", "validation.glPreview", $GlPreview.ToString().ToLowerInvariant())
    }
    if ($PSBoundParameters.ContainsKey("Controls")) {
        $launchArgs += @("--ez", "validation.controls", $Controls.ToString().ToLowerInvariant())
    }
    if ($PSBoundParameters.ContainsKey("Clean")) {
        $launchArgs += @("--ez", "validation.clean", $Clean.ToString().ToLowerInvariant())
    }
    if ($PSBoundParameters.ContainsKey("LockAeAwb")) {
        $launchArgs += @("--ez", "validation.lockAeAwb", $LockAeAwb.ToString().ToLowerInvariant())
    }
    if ($PersistLaunchSettings) {
        $launchArgs += @("--ez", "validation.persist", "true")
    }
    & $adb @launchArgs *> $null
    $ErrorActionPreference = $previousErrorActionPreference
    Start-Sleep -Seconds 3
}

$propsPath = Join-Path $outputDir "device_props.txt"
Run-AdbText -Adb $adb -Arguments @(
    "shell",
    "sh",
    "-c",
    "date; getprop ro.product.model; getprop ro.build.version.release; getprop ro.build.fingerprint"
) -OutputPath $propsPath
$artifacts.deviceProps = $propsPath

$packageInfoPath = Join-Path $outputDir "app_package.txt"
Run-AdbText -Adb $adb -Arguments @(
    "shell",
    "dumpsys",
    "package",
    $Package
) -OutputPath $packageInfoPath
$artifacts.packageInfo = $packageInfoPath

$focusPath = Join-Path $outputDir "window_focus.txt"
Run-AdbText -Adb $adb -Arguments @(
    "shell",
    "dumpsys",
    "window"
) -OutputPath $focusPath
$artifacts.windowFocus = $focusPath

$remoteUiDump = "/sdcard/Download/eulerian-live-validation-$timestamp-ui.xml"
$uiDumpPath = Join-Path $outputDir "ui_dump.xml"
Run-AdbText -Adb $adb -Arguments @(
    "shell",
    "uiautomator",
    "dump",
    "--compressed",
    $remoteUiDump
) -OutputPath (Join-Path $outputDir "ui_dump_stdout.txt")
& $adb pull $remoteUiDump $uiDumpPath | Out-Null
& $adb shell rm $remoteUiDump | Out-Null
if (Test-Path -LiteralPath $uiDumpPath) {
    $artifacts.uiDump = $uiDumpPath
}

$gfxPath = Join-Path $outputDir "gfxinfo.txt"
Run-AdbText -Adb $adb -Arguments @("shell", "dumpsys", "gfxinfo", $Package) -OutputPath $gfxPath
$artifacts.gfxinfo = $gfxPath

$thermalPath = Join-Path $outputDir "thermalservice.txt"
Run-AdbText -Adb $adb -Arguments @("shell", "dumpsys", "thermalservice") -OutputPath $thermalPath
$artifacts.thermal = $thermalPath

$batteryPath = Join-Path $outputDir "battery.txt"
Run-AdbText -Adb $adb -Arguments @("shell", "dumpsys", "battery") -OutputPath $batteryPath
$artifacts.battery = $batteryPath

$remoteScreenshot = "/sdcard/Download/eulerian-live-validation-$timestamp.png"
$screenshotPath = Join-Path $outputDir "screenshot.png"
& $adb shell screencap -p $remoteScreenshot | Out-Null
& $adb pull $remoteScreenshot $screenshotPath | Out-Null
& $adb shell rm $remoteScreenshot | Out-Null
$artifacts.screenshot = $screenshotPath

if ($ScreenRecordSeconds -gt 0) {
    $duration = [Math]::Min($ScreenRecordSeconds, 180)
    $remoteVideo = "/sdcard/Download/eulerian-live-validation-$timestamp.mp4"
    $videoPath = Join-Path $outputDir "screenrecord.mp4"
    & $adb shell screenrecord --time-limit $duration $remoteVideo
    & $adb pull $remoteVideo $videoPath | Out-Null
    & $adb shell rm $remoteVideo | Out-Null
    $artifacts.screenrecord = $videoPath
}

if (-not [string]::IsNullOrWhiteSpace($MeasureRoiExpected)) {
    $measurementScript = Join-Path $PSScriptRoot "measure_roi_overlay_screenshot.ps1"
    $measurementPath = Join-Path $outputDir "roi_overlay_measurement.json"
    $measurementLogPath = Join-Path $outputDir "roi_overlay_measurement_stdout.txt"
    if (-not (Test-Path -LiteralPath $measurementScript)) {
        $warnings += "ROI measurement requested, but measure_roi_overlay_screenshot.ps1 was not found."
    } else {
        $resolvedMeasureKind = if ([string]::IsNullOrWhiteSpace($MeasureRoiKind)) {
            if ($RoiSource -eq "Auto") { "Auto" } else { "Manual" }
        } else {
            $MeasureRoiKind
        }
        $measurementArgs = @(
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            $measurementScript,
            "-ScreenshotPath",
            $screenshotPath,
            "-ExpectedRoi",
            $MeasureRoiExpected,
            "-OverlayKind",
            $resolvedMeasureKind,
            "-OutputPath",
            $measurementPath,
            "-ColorTolerance",
            $MeasureRoiColorTolerance.ToString([Globalization.CultureInfo]::InvariantCulture),
            "-SearchMargin",
            $MeasureRoiSearchMargin.ToString([Globalization.CultureInfo]::InvariantCulture),
            "-MaxEdgeError",
            $MeasureRoiMaxEdgeError.ToString([Globalization.CultureInfo]::InvariantCulture),
            "-MinimumMatchedPixels",
            $MeasureRoiMinimumMatchedPixels.ToString([Globalization.CultureInfo]::InvariantCulture)
        )
        if ($MeasureRoiAllowMultipleComponents) {
            $measurementArgs += "-AllowMultipleComponents"
        }

        $powerShell = Find-PowerShell
        & $powerShell @measurementArgs *> $measurementLogPath
        $measurementExitCode = $LASTEXITCODE
        if (Test-Path -LiteralPath $measurementPath) {
            $artifacts.roiMeasurement = $measurementPath
        }
        $artifacts.roiMeasurementLog = $measurementLogPath
        if ($measurementExitCode -ne 0) {
            $warnings += "ROI overlay measurement failed with exit code $measurementExitCode."
        }
    }
}

$logcatPath = Join-Path $outputDir "logcat_tail.txt"
Run-AdbText -Adb $adb -Arguments @("logcat", "-d", "-t", "800") -OutputPath $logcatPath
$artifacts.logcat = $logcatPath

$manifestPath = Join-Path $outputDir "manifest.json"
$manifest = [ordered]@{
    createdAt = (Get-Date).ToString("o")
    label = $safeLabel
    package = $Package
    source = $source
    launch = [ordered]@{
        skipped = [bool]$SkipLaunch
        mode = $Mode
        view = $View
        roiSource = $RoiSource
        manualRoi = $ManualRoi
        panel = $Panel
        amplification = if ($PSBoundParameters.ContainsKey("Amplification")) { $Amplification } else { $null }
        glPreview = if ($PSBoundParameters.ContainsKey("GlPreview")) { $GlPreview } else { $null }
        controls = if ($PSBoundParameters.ContainsKey("Controls")) { $Controls } else { $null }
        clean = if ($PSBoundParameters.ContainsKey("Clean")) { $Clean } else { $null }
        lockAeAwb = if ($PSBoundParameters.ContainsKey("LockAeAwb")) { $LockAeAwb } else { $null }
        measureRoiExpected = $MeasureRoiExpected
        measureRoiKind = $MeasureRoiKind
        requireUiText = @($RequireUiText)
        persistSettings = [bool]$PersistLaunchSettings
    }
    adb = $adb
    outputDir = (Resolve-Path -LiteralPath $outputDir).Path
    screenRecordSeconds = $ScreenRecordSeconds
    launchedApp = (-not $SkipLaunch)
    logcatCleared = (-not [bool]$PreserveLogcat)
    artifacts = $artifacts
    warnings = $warnings
    notes = @(
        "This bundle captures runtime evidence only.",
        "It does not prove visual parity unless a known target is visible and inspected."
    )
}
$manifest | ConvertTo-Json -Depth 5 | Out-File -LiteralPath $manifestPath -Encoding utf8

if ($Summarize) {
    $summaryScript = Join-Path $PSScriptRoot "summarize_live_validation_evidence.ps1"
    if (-not (Test-Path -LiteralPath $summaryScript)) {
        Write-Warning "Summary requested, but summarize_live_validation_evidence.ps1 was not found."
    } else {
        $requiredUiTextArgs = @($RequireUiText | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
        if ($requiredUiTextArgs.Count -gt 0) {
            & $summaryScript -BundlePath $outputDir -RequireUiText $requiredUiTextArgs
        } else {
            & $summaryScript -BundlePath $outputDir
        }
    }
}

Write-Output "Live validation evidence captured:"
Write-Output (Resolve-Path -LiteralPath $outputDir).Path
