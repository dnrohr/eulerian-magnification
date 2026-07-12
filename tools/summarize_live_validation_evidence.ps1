param(
    [Parameter(Mandatory = $true)]
    [string]$BundlePath,

    [string]$OutputPath = "",
    [double]$WarnJankyPercent = 50.0,
    [int]$WarnMedianFrameMillis = 33,
    [int]$WarnThermalStatus = 2
)

$ErrorActionPreference = "Stop"

function Read-TextIfExists {
    param([string]$Path)
    if (Test-Path -LiteralPath $Path) {
        return Get-Content -LiteralPath $Path -Raw
    }
    return ""
}

function Match-FirstNumber {
    param(
        [string]$Text,
        [string]$Pattern
    )
    $match = [regex]::Match($Text, $Pattern)
    if (-not $match.Success) {
        return $null
    }
    return [double]::Parse($match.Groups[1].Value, [Globalization.CultureInfo]::InvariantCulture)
}

function Get-RequiredPath {
    param(
        [string]$Root,
        [string]$Name
    )
    return Join-Path $Root $Name
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

$bundle = (Resolve-Path -LiteralPath $BundlePath).Path
$manifestPath = Get-RequiredPath $bundle "manifest.json"
$gfxPath = Get-RequiredPath $bundle "gfxinfo.txt"
$logcatPath = Get-RequiredPath $bundle "logcat_tail.txt"
$screenshotPath = Get-RequiredPath $bundle "screenshot.png"
$roiMeasurementPath = Get-RequiredPath $bundle "roi_overlay_measurement.json"
$thermalPath = Get-RequiredPath $bundle "thermalservice.txt"

$missing = @()
foreach ($required in @($manifestPath, $gfxPath, $logcatPath, $screenshotPath)) {
    if (-not (Test-Path -LiteralPath $required)) {
        $missing += $required
    }
}

$manifest = $null
if (Test-Path -LiteralPath $manifestPath) {
    $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
}

$gfx = Read-TextIfExists $gfxPath
$logcat = Read-TextIfExists $logcatPath
$thermalText = Read-TextIfExists $thermalPath
$thermalSummary = Parse-ThermalSummary $thermalText

$jankyPercent = Match-FirstNumber $gfx 'Janky frames:\s+\d+\s+\(([0-9.]+)%\)'
$totalFrames = Match-FirstNumber $gfx 'Total frames rendered:\s+(\d+)'
$medianFrameMs = Match-FirstNumber $gfx '50th percentile:\s+(\d+)ms'
$p90FrameMs = Match-FirstNumber $gfx '90th percentile:\s+(\d+)ms'
$missedVsync = Match-FirstNumber $gfx 'Number Missed Vsync:\s+(\d+)'

$runtimePatterns = [ordered]@{
    fatalException = 'FATAL EXCEPTION'
    androidRuntime = 'AndroidRuntime'
    anr = '\bANR\b'
    glError = 'GL error|CameraOesRenderer.*error|RendererError'
}

$runtimeFindings = [ordered]@{}
foreach ($entry in $runtimePatterns.GetEnumerator()) {
    $runtimeFindings[$entry.Key] = [regex]::IsMatch($logcat, $entry.Value, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
}

$warnings = @()
if ($manifest -and $manifest.PSObject.Properties.Name -contains "warnings") {
    foreach ($warning in @($manifest.warnings)) {
        if (-not [string]::IsNullOrWhiteSpace($warning)) {
            $warnings += $warning
        }
    }
}
if ($missing.Count -gt 0) {
    $warnings += "missing required artifacts"
}
if ($null -ne $jankyPercent -and $jankyPercent -gt $WarnJankyPercent) {
    $warnings += "janky frames above $WarnJankyPercent percent"
}
if ($null -ne $medianFrameMs -and $medianFrameMs -gt $WarnMedianFrameMillis) {
    $warnings += "median frame time above $WarnMedianFrameMillis ms"
}
if ($null -ne $thermalSummary.status -and $thermalSummary.status -ge $WarnThermalStatus) {
    $warnings += "thermal status $($thermalSummary.status) ($($thermalSummary.statusLabel)) at or above warning threshold $WarnThermalStatus"
}
if ($null -ne $thermalSummary.maxSensorStatus -and $thermalSummary.maxSensorStatus -ge $WarnThermalStatus) {
    $warnings += "thermal sensor status $($thermalSummary.maxSensorStatus) ($($thermalSummary.maxSensorStatusLabel)) at or above warning threshold $WarnThermalStatus"
}
foreach ($entry in $runtimeFindings.GetEnumerator()) {
    if ($entry.Value) {
        $warnings += "runtime finding: $($entry.Key)"
    }
}

$roiMeasurement = $null
if (Test-Path -LiteralPath $roiMeasurementPath) {
    $roiMeasurement = Get-Content -LiteralPath $roiMeasurementPath -Raw | ConvertFrom-Json
    if ($roiMeasurement.passed -ne $true) {
        $warnings += "ROI overlay measurement failed"
    }
}

$screenshotInfo = $null
if (Test-Path -LiteralPath $screenshotPath) {
    Add-Type -AssemblyName System.Drawing
    $bitmap = [System.Drawing.Bitmap]::new($screenshotPath)
    try {
        $screenshotInfo = [ordered]@{
            path = $screenshotPath
            width = $bitmap.Width
            height = $bitmap.Height
            bytes = (Get-Item -LiteralPath $screenshotPath).Length
        }
    } finally {
        $bitmap.Dispose()
    }
}

$result = [ordered]@{
    bundle = $bundle
    createdAt = if ($manifest) { $manifest.createdAt } else { $null }
    label = if ($manifest) { $manifest.label } else { $null }
    source = if ($manifest -and $manifest.PSObject.Properties.Name -contains "source") { $manifest.source } else { $null }
    launch = if ($manifest) { $manifest.launch } else { $null }
    artifacts = [ordered]@{
        missingRequired = $missing
        screenshot = $screenshotInfo
        screenrecordPresent = Test-Path -LiteralPath (Join-Path $bundle "screenrecord.mp4")
        roiMeasurementPresent = $null -ne $roiMeasurement
        packageInfoPresent = Test-Path -LiteralPath (Join-Path $bundle "app_package.txt")
    }
    gfx = [ordered]@{
        totalFrames = $totalFrames
        jankyPercent = $jankyPercent
        medianFrameMs = $medianFrameMs
        p90FrameMs = $p90FrameMs
        missedVsync = $missedVsync
    }
    runtimeFindings = $runtimeFindings
    thermal = $thermalSummary
    roiMeasurement = $roiMeasurement
    warnings = $warnings
    passedRuntimeSmoke = ($missing.Count -eq 0) -and
        (-not $runtimeFindings.fatalException) -and
        (-not $runtimeFindings.androidRuntime) -and
        (-not $runtimeFindings.anr) -and
        (-not $runtimeFindings.glError)
    notes = @(
        "This summarizes runtime evidence only.",
        "It does not prove visual validation unless the screenshot or recording contains the intended target and was inspected against the relevant pass criteria."
    )
}

$json = $result | ConvertTo-Json -Depth 8
$destination = if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    Join-Path $bundle "evidence_summary.json"
} else {
    $OutputPath
}
$json | Out-File -LiteralPath $destination -Encoding utf8

Write-Output "Live validation evidence summary written: $destination"
Write-Output "passedRuntimeSmoke=$($result.passedRuntimeSmoke) warnings=$($warnings.Count)"

if (-not $result.passedRuntimeSmoke) {
    exit 2
}
