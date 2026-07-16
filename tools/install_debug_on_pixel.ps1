param(
    [string]$ApkPath = "app\build\outputs\apk\debug\app-debug.apk",
    [string]$Package = "com.dnrohr.eulerianmagnification",
    [string]$AdbPath = "",
    [string]$DeviceSerial = "",
    [switch]$Build,
    [switch]$Launch,
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

function Get-PackageVersionText {
    param(
        [string]$Adb,
        [string]$Serial,
        [string]$PackageName
    )

    $packageDump = Invoke-CommandCapture -FilePath $Adb -Arguments @("-s", $Serial, "shell", "dumpsys", "package", $PackageName)
    if ($packageDump.exitCode -ne 0) {
        return ""
    }
    $lines = @($packageDump.output | Where-Object { $_ -match 'version(Name|Code)' } | Select-Object -First 4)
    return ($lines -join "; ").Trim()
}

$resolvedApkPath = if ([System.IO.Path]::IsPathRooted($ApkPath)) {
    $ApkPath
} else {
    Join-Path (Resolve-Path -LiteralPath ".").Path $ApkPath
}

$adb = Find-Adb -ExplicitPath $AdbPath
$commands = @()
if ($Build) {
    $commands += ".\gradlew.bat assembleDebug"
}
$commands += "$adb install -r `"$resolvedApkPath`""
if ($Launch) {
    $commands += "$adb shell monkey -p $Package 1"
}

if ($DryRun) {
    $result = [pscustomobject]@{
        dryRun = $true
        adb = $adb
        apkPath = $resolvedApkPath
        package = $Package
        deviceSerial = $DeviceSerial
        commands = $commands
    }
    if ($Json) {
        $result | ConvertTo-Json -Depth 5
    } else {
        Write-Output "Debug install dry run"
        Write-Output "ADB: $($result.adb)"
        Write-Output "APK: $($result.apkPath)"
        Write-Output "Package: $($result.package)"
        foreach ($command in $result.commands) {
            Write-Output "Command: $command"
        }
    }
    exit 0
}

if ($Build) {
    $build = Invoke-CommandCapture -FilePath ".\gradlew.bat" -Arguments @("assembleDebug")
    if ($build.exitCode -ne 0) {
        throw "assembleDebug failed: $($build.output -join "`n")"
    }
}

if (-not (Test-Path -LiteralPath $resolvedApkPath)) {
    throw "Debug APK not found. Run with -Build or build it first: $resolvedApkPath"
}

$devices = @(Get-ConnectedDevices -Adb $adb)
$device = Get-SelectedDevice -Devices $devices -Serial $DeviceSerial
$adbDeviceArgs = @("-s", $device.serial)
$install = Invoke-CommandCapture -FilePath $adb -Arguments ($adbDeviceArgs + @("install", "-r", $resolvedApkPath))
if ($install.exitCode -ne 0) {
    throw "adb install failed: $($install.output -join "`n")"
}

$grantCamera = Invoke-CommandCapture -FilePath $adb -Arguments ($adbDeviceArgs + @("shell", "pm", "grant", $Package, "android.permission.CAMERA"))
$launchResult = $null
if ($Launch) {
    $launchResult = Invoke-CommandCapture -FilePath $adb -Arguments ($adbDeviceArgs + @("shell", "monkey", "-p", $Package, "1"))
    if ($launchResult.exitCode -ne 0) {
        throw "adb launch failed: $($launchResult.output -join "`n")"
    }
}

$versionText = Get-PackageVersionText -Adb $adb -Serial $device.serial -PackageName $Package
$result = [pscustomobject]@{
    dryRun = $false
    adb = $adb
    apkPath = $resolvedApkPath
    package = $Package
    deviceSerial = $device.serial
    deviceDetails = $device.details
    installOutput = @($install.output)
    cameraGrantExitCode = $grantCamera.exitCode
    cameraGrantOutput = @($grantCamera.output)
    launched = [bool]$Launch
    launchOutput = if ($launchResult) { @($launchResult.output) } else { @() }
    packageVersion = $versionText
}

if ($Json) {
    $result | ConvertTo-Json -Depth 6
} else {
    Write-Output "Debug app installed on Pixel/ADB device"
    Write-Output "Device: $($result.deviceSerial) $($result.deviceDetails)"
    Write-Output "APK: $($result.apkPath)"
    Write-Output "Package: $($result.package)"
    if (-not [string]::IsNullOrWhiteSpace($result.packageVersion)) {
        Write-Output "Version: $($result.packageVersion)"
    }
    if ($result.launched) {
        Write-Output "Launched: true"
    }
}
