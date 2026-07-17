param(
    [string]$Package = "com.dnrohr.eulerianmagnification",
    [string]$AdbPath = "",
    [string]$DeviceSerial = "",
    [string]$OutputPath = "",
    [string]$FixtureRoot = "",
    [int]$ReadyBelowThermalStatus = 2,
    [int]$WarnThermalStatus = 2,
    [double]$WarnBatteryTemperatureC = 38.0,
    [double]$WarnFrameP95Ms = 45.0,
    [double]$WarnJankyPercent = 10.0,
    [switch]$FailOnNotReady
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

function Thermal-StatusLabel {
    param([Nullable[int]]$Status)
    switch ($Status) {
        0 { "none" }
        1 { "light" }
        2 { "moderate" }
        3 { "severe" }
        4 { "critical" }
        5 { "emergency" }
        6 { "shutdown" }
        default { $null }
    }
}

function Read-DeviceText {
    param(
        [string]$Name,
        [string[]]$AdbArguments
    )

    if (-not [string]::IsNullOrWhiteSpace($FixtureRoot)) {
        $fixturePath = Join-Path $FixtureRoot $Name
        if (Test-Path -LiteralPath $fixturePath) {
            return Get-Content -LiteralPath $fixturePath -Raw
        }
        return ""
    }

    $adb = Find-Adb -ExplicitPath $AdbPath
    $deviceArgs = if ([string]::IsNullOrWhiteSpace($DeviceSerial)) { @() } else { @("-s", $DeviceSerial) }
    $output = & $adb @deviceArgs @AdbArguments
    if ($LASTEXITCODE -ne 0) {
        throw "adb command failed with exit code ${LASTEXITCODE}: $($AdbArguments -join ' ')"
    }
    return (($output | Out-String).Trim())
}

function Parse-ThermalSummary {
    param([string]$Text)

    $thermalStatus = $null
    $statusMatch = [regex]::Match($Text, 'Thermal Status:\s+(\d+)')
    if ($statusMatch.Success) {
        $thermalStatus = [int]::Parse($statusMatch.Groups[1].Value, [Globalization.CultureInfo]::InvariantCulture)
    }

    $maxSensorStatus = $null
    $maxTemperatureC = $null
    $hottestSensorName = $null
    $temperaturePattern = 'Temperature\{mValue=([-+]?[0-9]+(?:\.[0-9]+)?),\s*mType=[^,]+,\s*mName=([^,}]+),\s*mStatus=(\d+)\}'
    foreach ($match in [regex]::Matches($Text, $temperaturePattern)) {
        $temperature = [double]::Parse($match.Groups[1].Value, [Globalization.CultureInfo]::InvariantCulture)
        $name = $match.Groups[2].Value
        $sensorStatus = [int]::Parse($match.Groups[3].Value, [Globalization.CultureInfo]::InvariantCulture)
        if ($null -eq $maxSensorStatus -or $sensorStatus -gt $maxSensorStatus) {
            $maxSensorStatus = $sensorStatus
        }
        if ($null -eq $maxTemperatureC -or $temperature -gt $maxTemperatureC) {
            $maxTemperatureC = $temperature
            $hottestSensorName = $name
        }
    }

    return [ordered]@{
        status = $thermalStatus
        statusLabel = Thermal-StatusLabel $thermalStatus
        maxSensorStatus = $maxSensorStatus
        maxSensorStatusLabel = Thermal-StatusLabel $maxSensorStatus
        maxTemperatureC = $maxTemperatureC
        hottestSensorName = $hottestSensorName
    }
}

function Get-MaxThermalStatus {
    param([hashtable]$Summary)

    $values = @($Summary.status, $Summary.maxSensorStatus) | Where-Object { $null -ne $_ }
    if ($values.Count -eq 0) {
        return $null
    }
    return ($values | Measure-Object -Maximum).Maximum
}

function Parse-BatterySummary {
    param([string]$Text)

    $temperatureC = $null
    $temperatureMatch = [regex]::Match($Text, '(?m)^\s*temperature:\s*([-+]?\d+)')
    if ($temperatureMatch.Success) {
        $temperatureC = [double]::Parse($temperatureMatch.Groups[1].Value, [Globalization.CultureInfo]::InvariantCulture) / 10.0
    }

    return [ordered]@{
        temperatureC = $temperatureC
        usbPowered = $Text -match '(?m)^\s*USB powered:\s*true'
        acPowered = $Text -match '(?m)^\s*AC powered:\s*true'
    }
}

function Parse-FocusedAppSummary {
    param(
        [string]$Text,
        [string]$ExpectedPackage
    )

    $focusLines = @()
    foreach ($line in ($Text -split "`r?`n")) {
        if ($line -match 'mCurrentFocus|mFocusedApp|mFocusedWindow') {
            $focusLines += $line.Trim()
        }
    }

    return [ordered]@{
        present = -not [string]::IsNullOrWhiteSpace($Text)
        packageVisible = $Text.Contains($ExpectedPackage)
        focusLines = $focusLines
    }
}

function Parse-GfxInfoSummary {
    param([string]$Text)

    $totalFrames = $null
    $totalMatch = [regex]::Match($Text, 'Total frames rendered:\s*(\d+)')
    if ($totalMatch.Success) {
        $totalFrames = [int]::Parse($totalMatch.Groups[1].Value, [Globalization.CultureInfo]::InvariantCulture)
    }

    $jankyFrames = $null
    $jankyPercent = $null
    $jankyMatch = [regex]::Match($Text, 'Janky frames:\s*(\d+)\s*\(([0-9.]+)%\)')
    if ($jankyMatch.Success) {
        $jankyFrames = [int]::Parse($jankyMatch.Groups[1].Value, [Globalization.CultureInfo]::InvariantCulture)
        $jankyPercent = [double]::Parse($jankyMatch.Groups[2].Value, [Globalization.CultureInfo]::InvariantCulture)
    }

    $p95Ms = $null
    $p95Match = [regex]::Match($Text, '95th percentile:\s*([0-9.]+)ms')
    if ($p95Match.Success) {
        $p95Ms = [double]::Parse($p95Match.Groups[1].Value, [Globalization.CultureInfo]::InvariantCulture)
    }

    return [ordered]@{
        present = -not [string]::IsNullOrWhiteSpace($Text)
        totalFramesRendered = $totalFrames
        jankyFrames = $jankyFrames
        jankyPercent = $jankyPercent
        frameP95Ms = $p95Ms
    }
}

$thermalText = Read-DeviceText -Name "thermalservice.txt" -AdbArguments @("shell", "dumpsys", "thermalservice")
$batteryText = Read-DeviceText -Name "battery.txt" -AdbArguments @("shell", "dumpsys", "battery")
$focusText = Read-DeviceText -Name "window_focus.txt" -AdbArguments @("shell", "dumpsys", "window")
$gfxText = Read-DeviceText -Name "gfxinfo.txt" -AdbArguments @("shell", "dumpsys", "gfxinfo", $Package)

$thermal = Parse-ThermalSummary $thermalText
$battery = Parse-BatterySummary $batteryText
$focusedApp = Parse-FocusedAppSummary -Text $focusText -ExpectedPackage $Package
$gfxinfo = Parse-GfxInfoSummary $gfxText
$maxThermalStatus = Get-MaxThermalStatus $thermal

$issues = @()
$warnings = @()
if ($null -eq $maxThermalStatus) {
    $issues += "thermal status unavailable"
} elseif ($maxThermalStatus -ge $ReadyBelowThermalStatus) {
    $issues += "thermal status $maxThermalStatus ($((Thermal-StatusLabel $maxThermalStatus))) is not below readiness threshold $ReadyBelowThermalStatus"
} elseif ($maxThermalStatus -ge $WarnThermalStatus) {
    $warnings += "thermal status $maxThermalStatus ($((Thermal-StatusLabel $maxThermalStatus))) is at or above warning threshold $WarnThermalStatus"
}

if (-not $focusedApp.present) {
    $issues += "focused-app state unavailable"
} elseif (-not $focusedApp.packageVisible) {
    $issues += "focused app is not $Package"
}

if ($null -ne $battery.temperatureC -and $battery.temperatureC -ge $WarnBatteryTemperatureC) {
    $warnings += "battery temperature $($battery.temperatureC) C is at or above warning threshold $WarnBatteryTemperatureC C"
}

if ($gfxinfo.present) {
    if ($null -ne $gfxinfo.frameP95Ms -and $gfxinfo.frameP95Ms -ge $WarnFrameP95Ms) {
        $warnings += "gfxinfo 95th percentile frame time $($gfxinfo.frameP95Ms) ms is at or above warning threshold $WarnFrameP95Ms ms"
    }
    if ($null -ne $gfxinfo.jankyPercent -and $gfxinfo.jankyPercent -ge $WarnJankyPercent) {
        $warnings += "gfxinfo janky frames $($gfxinfo.jankyPercent)% is at or above warning threshold $WarnJankyPercent%"
    }
} else {
    $warnings += "gfxinfo output unavailable; frame jank could not be checked"
}

$result = [ordered]@{
    createdAt = (Get-Date).ToString("o")
    package = $Package
    deviceSerial = $DeviceSerial
    fixtureRoot = $FixtureRoot
    thresholds = [ordered]@{
        readyBelowThermalStatus = $ReadyBelowThermalStatus
        warnThermalStatus = $WarnThermalStatus
        warnBatteryTemperatureC = $WarnBatteryTemperatureC
        warnFrameP95Ms = $WarnFrameP95Ms
        warnJankyPercent = $WarnJankyPercent
    }
    readyForWatchedCapture = (@($issues).Count -eq 0)
    issues = @($issues)
    warnings = @($warnings)
    thermal = $thermal
    battery = $battery
    focusedApp = $focusedApp
    gfxinfo = $gfxinfo
}

if (-not [string]::IsNullOrWhiteSpace($OutputPath)) {
    $parent = Split-Path -Parent $OutputPath
    if (-not [string]::IsNullOrWhiteSpace($parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }
    $result | ConvertTo-Json -Depth 8 | Out-File -LiteralPath $OutputPath -Encoding utf8
}

$statusText = if ($result.readyForWatchedCapture) { "ready" } else { "not ready" }
Write-Output "Pixel session readiness: $statusText"
Write-Output "Issues: $(@($issues).Count)"
Write-Output "Warnings: $(@($warnings).Count)"
if (-not [string]::IsNullOrWhiteSpace($OutputPath)) {
    Write-Output "Output: $OutputPath"
}

if ($FailOnNotReady -and -not $result.readyForWatchedCapture) {
    exit 31
}
