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
    [Nullable[double]]$Amplification = $null,
    [Nullable[bool]]$GlPreview = $null,
    [Nullable[bool]]$Controls = $null,
    [Nullable[bool]]$Clean = $null,
    [Nullable[bool]]$LockAeAwb = $null,
    [switch]$PersistLaunchSettings
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

$adb = Find-Adb
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$safeLabel = if ([string]::IsNullOrWhiteSpace($Label)) { "capture" } else { $Label -replace "[^A-Za-z0-9._-]", "-" }
$outputDir = Join-Path $OutputRoot "$timestamp-$safeLabel"
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

$artifacts = [ordered]@{}

$devicesPath = Join-Path $outputDir "adb_devices.txt"
Run-AdbText -Adb $adb -Arguments @("devices") -OutputPath $devicesPath
$artifacts.devices = $devicesPath

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

$focusPath = Join-Path $outputDir "window_focus.txt"
Run-AdbText -Adb $adb -Arguments @(
    "shell",
    "dumpsys",
    "window"
) -OutputPath $focusPath
$artifacts.windowFocus = $focusPath

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

$logcatPath = Join-Path $outputDir "logcat_tail.txt"
Run-AdbText -Adb $adb -Arguments @("logcat", "-d", "-t", "800") -OutputPath $logcatPath
$artifacts.logcat = $logcatPath

$manifestPath = Join-Path $outputDir "manifest.json"
$manifest = [ordered]@{
    createdAt = (Get-Date).ToString("o")
    label = $safeLabel
    package = $Package
    launch = [ordered]@{
        skipped = [bool]$SkipLaunch
        mode = $Mode
        view = $View
        roiSource = $RoiSource
        manualRoi = $ManualRoi
        amplification = if ($PSBoundParameters.ContainsKey("Amplification")) { $Amplification } else { $null }
        glPreview = if ($PSBoundParameters.ContainsKey("GlPreview")) { $GlPreview } else { $null }
        controls = if ($PSBoundParameters.ContainsKey("Controls")) { $Controls } else { $null }
        clean = if ($PSBoundParameters.ContainsKey("Clean")) { $Clean } else { $null }
        lockAeAwb = if ($PSBoundParameters.ContainsKey("LockAeAwb")) { $LockAeAwb } else { $null }
        persistSettings = [bool]$PersistLaunchSettings
    }
    adb = $adb
    outputDir = (Resolve-Path -LiteralPath $outputDir).Path
    screenRecordSeconds = $ScreenRecordSeconds
    launchedApp = (-not $SkipLaunch)
    artifacts = $artifacts
    notes = @(
        "This bundle captures runtime evidence only.",
        "It does not prove visual parity unless a known target is visible and inspected."
    )
}
$manifest | ConvertTo-Json -Depth 5 | Out-File -LiteralPath $manifestPath -Encoding utf8

Write-Output "Live validation evidence captured:"
Write-Output (Resolve-Path -LiteralPath $outputDir).Path
