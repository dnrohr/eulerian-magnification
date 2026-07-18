param(
    [string]$Package = "com.dnrohr.eulerianmagnification",
    [string]$Activity = ".MainActivity",
    [string]$AdbPath = "",
    [string]$DeviceSerial = "",
    [int]$Attempts = 3,
    [int]$SettleSeconds = 5,
    [string]$OutputRoot = "sample-videos\exports\live-validation",
    [switch]$RequireFinalReady,
    [switch]$LeaveRunningOnFailure,
    [switch]$DryRun,
    [switch]$Json
)

$ErrorActionPreference = "Stop"

function Find-Adb {
    param([string]$ExplicitPath)

    if (-not [string]::IsNullOrWhiteSpace($ExplicitPath)) {
        if (Test-Path -LiteralPath $ExplicitPath) {
            return (Resolve-Path -LiteralPath $ExplicitPath).Path
        }
        throw "Requested adb path does not exist: $ExplicitPath"
    }

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

function Invoke-CommandCapture {
    param(
        [string]$FilePath,
        [string[]]$Arguments
    )

    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        $output = & $FilePath @Arguments 2>&1
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    return [pscustomobject]@{
        exitCode = $exitCode
        output = @($output)
        command = "$FilePath $($Arguments -join ' ')"
    }
}

function Get-ConnectedDevices {
    param([string]$Adb)

    $devices = Invoke-CommandCapture -FilePath $Adb -Arguments @("devices", "-l")
    if ($devices.exitCode -ne 0) {
        throw "adb devices failed: $($devices.output -join "`n")"
    }

    $result = @()
    foreach ($line in @($devices.output)) {
        if ($line -match '^(\S+)\s+device\b(.*)$') {
            $result += [pscustomobject]@{
                serial = $Matches[1]
                details = $Matches[2].Trim()
            }
        }
    }
    return $result
}

function Get-SelectedDevice {
    param(
        [object[]]$Devices,
        [string]$Serial
    )

    if (-not [string]::IsNullOrWhiteSpace($Serial)) {
        $match = @($Devices | Where-Object { $_.serial -eq $Serial })
        if ($match.Count -eq 1) {
            return $match[0]
        }
        throw "Requested device serial was not connected: $Serial"
    }

    if ($Devices.Count -eq 1) {
        return $Devices[0]
    }
    if ($Devices.Count -eq 0) {
        throw "No connected adb device is available."
    }
    throw "Multiple adb devices are connected. Pass -DeviceSerial."
}

if ($Attempts -lt 1) {
    throw "Attempts must be at least 1."
}
if ($SettleSeconds -lt 0) {
    throw "SettleSeconds must be 0 or greater."
}

$adb = Find-Adb -ExplicitPath $AdbPath
$outputRootPath = if ([System.IO.Path]::IsPathRooted($OutputRoot)) {
    $OutputRoot
} else {
    Join-Path (Resolve-Path -LiteralPath ".").Path $OutputRoot
}
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$outputPath = Join-Path $outputRootPath "pixel_preview_recovery_$timestamp.json"
$readinessOutputPath = Join-Path $outputRootPath "pixel_session_readiness_probe.json"

$commands = @()
for ($index = 1; $index -le $Attempts; $index++) {
    $session = "recovery-$timestamp-$index"
    $commands += "$adb shell am force-stop $Package"
    $commands += "$adb logcat -c"
    $commands += "$adb shell am start -n $Package/$Activity --es validation.cameraSession $session --ez validation.glPreview true --ez validation.controls false --ez validation.clean true"
    $commands += ".\tools\export_pixel_session_readiness.ps1 -DeviceSerial $DeviceSerial -OutputPath $readinessOutputPath -RequireFreshCameraFrames -FreshCameraLogSeconds 3 -MinimumFps 15 -MaximumJankyPercent 25 -Json"
}

if ($DryRun) {
    $result = [pscustomobject]@{
        dryRun = $true
        adb = $adb
        package = $Package
        activity = $Activity
        deviceSerial = $DeviceSerial
        attempts = $Attempts
        settleSeconds = $SettleSeconds
        outputPath = $outputPath
        readinessOutputPath = $readinessOutputPath
        requireFinalReady = [bool]$RequireFinalReady
        leaveRunningOnFailure = [bool]$LeaveRunningOnFailure
        commands = $commands
    }
    if ($Json) {
        $result | ConvertTo-Json -Depth 6
    } else {
        Write-Output "Pixel preview recovery dry run"
        Write-Output "ADB: $($result.adb)"
        Write-Output "Package: $Package"
        Write-Output "Attempts: $Attempts"
        foreach ($command in $commands) {
            Write-Output "Command: $command"
        }
    }
    exit 0
}

New-Item -ItemType Directory -Path $outputRootPath -Force | Out-Null
$devices = @(Get-ConnectedDevices -Adb $adb)
$device = Get-SelectedDevice -Devices $devices -Serial $DeviceSerial
$serialArgs = @("-s", $device.serial)
$attemptResults = @()
$ready = $false
$setupReady = $false
$finalReady = $false
$stoppedAfterFailure = $false

for ($index = 1; $index -le $Attempts; $index++) {
    $session = "recovery-$timestamp-$index"
    $forceStop = Invoke-CommandCapture -FilePath $adb -Arguments ($serialArgs + @("shell", "am", "force-stop", $Package))
    $clearLogcat = Invoke-CommandCapture -FilePath $adb -Arguments ($serialArgs + @("logcat", "-c"))
    $launch = Invoke-CommandCapture -FilePath $adb -Arguments (
        $serialArgs + @(
            "shell", "am", "start",
            "-n", "$Package/$Activity",
            "--es", "validation.cameraSession", $session,
            "--ez", "validation.glPreview", "true",
            "--ez", "validation.controls", "false",
            "--ez", "validation.clean", "true"
        )
    )
    if ($SettleSeconds -gt 0) {
        Start-Sleep -Seconds $SettleSeconds
    }

    & (Join-Path $PSScriptRoot "export_pixel_session_readiness.ps1") `
        -DeviceSerial $device.serial `
        -OutputPath $readinessOutputPath `
        -RequireFreshCameraFrames `
        -FreshCameraLogSeconds 3 `
        -MinimumFps 15 `
        -MaximumJankyPercent 25 `
        -Json | Out-Null
    $readinessJson = Get-Content -LiteralPath $readinessOutputPath -Raw | ConvertFrom-Json

    $setupReady = [bool]$readinessJson.readyForSetupCapture
    $finalReady = [bool]$readinessJson.readyForWatchedCapture
    $ready = if ($RequireFinalReady) { $finalReady } else { $setupReady }
    $attemptResults += [pscustomobject]@{
        attempt = $index
        cameraSession = $session
        forceStopExitCode = $forceStop.exitCode
        clearLogcatExitCode = $clearLogcat.exitCode
        launchExitCode = $launch.exitCode
        readyForSetupCapture = $setupReady
        readyForWatchedCapture = $finalReady
        issues = @($readinessJson.issues)
        setupIssues = @($readinessJson.setupIssues)
        warnings = @($readinessJson.warnings)
        cameraSyncWarningLineCount = $readinessJson.cameraLogHealth.syncWarningLineCount
    }
    if ($ready) {
        break
    }
}

if (-not $ready -and -not $LeaveRunningOnFailure) {
    $stopAfterFailure = Invoke-CommandCapture -FilePath $adb -Arguments ($serialArgs + @("shell", "am", "force-stop", $Package))
    $stoppedAfterFailure = $stopAfterFailure.exitCode -eq 0
}

$result = [pscustomobject]@{
    dryRun = $false
    createdAt = (Get-Date).ToString("o")
    adb = $adb
    package = $Package
    activity = $Activity
    deviceSerial = $device.serial
    deviceDetails = $device.details
    attemptsRequested = $Attempts
    attemptsRun = @($attemptResults).Count
    requireFinalReady = [bool]$RequireFinalReady
    leaveRunningOnFailure = [bool]$LeaveRunningOnFailure
    stoppedAfterFailure = $stoppedAfterFailure
    recovered = $ready
    readyForSetupCapture = $setupReady
    readyForWatchedCapture = $finalReady
    outputPath = $outputPath
    readinessOutputPath = $readinessOutputPath
    attempts = $attemptResults
}
$result | ConvertTo-Json -Depth 8 | Out-File -LiteralPath $outputPath -Encoding utf8

if ($Json) {
    $result | ConvertTo-Json -Depth 8
} else {
    Write-Output "Pixel preview recovery: $(if ($ready) { "ready" } else { "not ready" })"
    Write-Output "Attempts: $($result.attemptsRun) / $Attempts"
    Write-Output "Setup readiness: $setupReady"
    Write-Output "Final readiness: $finalReady"
    Write-Output "Output: $outputPath"
}

if (-not $ready) {
    exit 33
}
