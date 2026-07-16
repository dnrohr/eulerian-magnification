param(
    [Parameter(Mandatory = $true)]
    [string]$BundlePath,

    [string]$OutputPath = "",
    [double]$WarnJankyPercent = 50.0,
    [int]$WarnMedianFrameMillis = 33,
    [int]$WarnThermalStatus = 2,
    [double]$WarnCameraFps = 23.5,
    [double]$WarnBatteryTemperatureC = 40.0,
    [string[]]$RequireUiText = @(),
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
    [ValidateSet("", "runtime_smoke_only", "visual_validated", "target_visible_unvalidated", "visual_claim_without_target", "ui_assertion_failed", "screenshot_blank", "wrong_orientation", "runtime_failed", "thermal_preflight_aborted")]
    [string]$RequireEvidenceVerdict = ""
)

$ErrorActionPreference = "Stop"

if ($RequireFinalVisualEvidence) {
    $RequireCleanSource = $true
    $RequireVisualValidation = $true
    $RequireNoWarnings = $true
    $RequireScreenrecord = $true
    $RequireThermalReady = $true
    $RequireCameraFps = $true
    $RequireFocusedApp = $true
}

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

function Get-FileSha256IfExists {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return $null
    }

    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash
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

function Parse-CameraFpsSummary {
    param([string]$Text)

    $values = @()
    foreach ($match in [regex]::Matches($Text, '\bFPS:\s+([0-9]+(?:\.[0-9]+)?)')) {
        $values += [double]::Parse($match.Groups[1].Value, [Globalization.CultureInfo]::InvariantCulture)
    }
    if ($values.Count -eq 0) {
        return [ordered]@{
            sampleCount = 0
            averageFps = $null
            minFps = $null
            maxFps = $null
        }
    }
    return [ordered]@{
        sampleCount = $values.Count
        averageFps = ($values | Measure-Object -Average).Average
        minFps = ($values | Measure-Object -Minimum).Minimum
        maxFps = ($values | Measure-Object -Maximum).Maximum
    }
}

function Match-FirstText {
    param(
        [string]$Text,
        [string]$Pattern
    )
    $match = [regex]::Match(
        $Text,
        $Pattern,
        [System.Text.RegularExpressions.RegexOptions]::IgnoreCase -bor [System.Text.RegularExpressions.RegexOptions]::Multiline
    )
    if (-not $match.Success) {
        return $null
    }
    return $match.Groups[1].Value.Trim()
}

function Parse-BatterySummary {
    param([string]$Text)

    $levelText = Match-FirstText $Text '^\s*level:\s+(\d+)'
    $temperatureText = Match-FirstText $Text '^\s*temperature:\s+(-?\d+)'
    $statusText = Match-FirstText $Text '^\s*status:\s+(\d+)'
    $chargingStateText = Match-FirstText $Text '^\s*Charging state:\s+(\d+)'
    $acPowered = [regex]::IsMatch($Text, '^\s*AC powered:\s+true', [System.Text.RegularExpressions.RegexOptions]::IgnoreCase -bor [System.Text.RegularExpressions.RegexOptions]::Multiline)
    $usbPowered = [regex]::IsMatch($Text, '^\s*USB powered:\s+true', [System.Text.RegularExpressions.RegexOptions]::IgnoreCase -bor [System.Text.RegularExpressions.RegexOptions]::Multiline)
    $wirelessPowered = [regex]::IsMatch($Text, '^\s*Wireless powered:\s+true', [System.Text.RegularExpressions.RegexOptions]::IgnoreCase -bor [System.Text.RegularExpressions.RegexOptions]::Multiline)
    $dockPowered = [regex]::IsMatch($Text, '^\s*Dock powered:\s+true', [System.Text.RegularExpressions.RegexOptions]::IgnoreCase -bor [System.Text.RegularExpressions.RegexOptions]::Multiline)

    $powerSources = @()
    if ($acPowered) { $powerSources += "AC" }
    if ($usbPowered) { $powerSources += "USB" }
    if ($wirelessPowered) { $powerSources += "Wireless" }
    if ($dockPowered) { $powerSources += "Dock" }

    $level = if ($null -ne $levelText) { [int]::Parse($levelText, [Globalization.CultureInfo]::InvariantCulture) } else { $null }
    $temperatureC = if ($null -ne $temperatureText) {
        [int]::Parse($temperatureText, [Globalization.CultureInfo]::InvariantCulture) / 10.0
    } else {
        $null
    }
    $status = if ($null -ne $statusText) { [int]::Parse($statusText, [Globalization.CultureInfo]::InvariantCulture) } else { $null }
    $chargingState = if ($null -ne $chargingStateText) { [int]::Parse($chargingStateText, [Globalization.CultureInfo]::InvariantCulture) } else { $null }

    return [ordered]@{
        levelPercent = $level
        temperatureC = $temperatureC
        status = $status
        chargingState = $chargingState
        powered = $powerSources.Count -gt 0
        powerSources = $powerSources
    }
}

function Parse-UiDumpSummary {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return [ordered]@{
            present = $false
            text = @()
            qualityLabels = @()
            rendererLabels = @()
            roiLabels = @()
            phaseLabels = @()
        }
    }

    $texts = @()
    try {
        [xml]$xml = Get-Content -LiteralPath $Path -Raw
        foreach ($node in $xml.SelectNodes("//*[@text]")) {
            $text = $node.GetAttribute("text")
            if (-not [string]::IsNullOrWhiteSpace($text) -and $texts -notcontains $text) {
                $texts += $text
            }
        }
    } catch {
        return [ordered]@{
            present = $true
            parseError = $_.Exception.Message
            text = @()
            qualityLabels = @()
            rendererLabels = @()
            roiLabels = @()
            phaseLabels = @()
        }
    }

    $qualityPattern = 'Good|Face missing|Too dark|Thermal high|Low FPS|Full frame slow|Camera FPS low|Timing unstable|Lighting flicker|Exposure unstable|ROI motion|Mode motion risk|Amplification risk|Signal weak'
    $phasePattern = 'phase:|phase fallback|phase warmup|phase ready'
    return [ordered]@{
        present = $true
        text = $texts
        qualityLabels = @($texts | Where-Object { $_ -match $qualityPattern })
        rendererLabels = @($texts | Where-Object { $_ -match 'Renderer:|Preview:|GL renderer:|Pyramid:|Benchmark:' })
        roiLabels = @($texts | Where-Object { $_ -match 'Auto ROI|Full frame|Manual ROI|Tracking|Center ROI|Frozen' })
        phaseLabels = @($texts | Where-Object { $_ -match $phasePattern })
    }
}

function Test-RequiredUiText {
    param(
        [string[]]$Texts,
        [string[]]$RequiredText
    )

    $checks = @()
    foreach ($required in @($RequiredText)) {
        if ([string]::IsNullOrWhiteSpace($required)) {
            continue
        }
        $found = $false
        foreach ($text in @($Texts)) {
            if ($text.IndexOf($required, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
                $found = $true
                break
            }
        }
        $checks += [ordered]@{
            text = $required
            found = $found
        }
    }
    return $checks
}

function Test-Mp4Signature {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return $false
    }
    $bytes = [System.IO.File]::ReadAllBytes($Path)
    if ($bytes.Length -lt 12) {
        return $false
    }
    $headerLength = [Math]::Min($bytes.Length, 32)
    $header = [System.Text.Encoding]::ASCII.GetString($bytes, 0, $headerLength)
    return $header.IndexOf("ftyp", [System.StringComparison]::Ordinal) -ge 0
}

function Measure-ScreenshotContent {
    param([System.Drawing.Bitmap]$Bitmap)

    $stepX = [Math]::Max(1, [Math]::Floor($Bitmap.Width / 80))
    $stepY = [Math]::Max(1, [Math]::Floor($Bitmap.Height / 80))
    $count = 0
    $sum = 0.0
    $sumSquares = 0.0
    $darkCount = 0
    $lightCount = 0

    for ($y = 0; $y -lt $Bitmap.Height; $y += $stepY) {
        for ($x = 0; $x -lt $Bitmap.Width; $x += $stepX) {
            $pixel = $Bitmap.GetPixel($x, $y)
            $luma = (0.2126 * $pixel.R) + (0.7152 * $pixel.G) + (0.0722 * $pixel.B)
            $count += 1
            $sum += $luma
            $sumSquares += $luma * $luma
            if ($luma -lt 8.0) {
                $darkCount += 1
            }
            if ($luma -gt 247.0) {
                $lightCount += 1
            }
        }
    }

    if ($count -eq 0) {
        return [ordered]@{
            sampleCount = 0
            meanLuma = $null
            lumaStdDev = $null
            darkFraction = $null
            lightFraction = $null
            nonBlank = $false
            portrait = $Bitmap.Height -gt $Bitmap.Width
        }
    }

    $mean = $sum / $count
    $variance = [Math]::Max(0.0, ($sumSquares / $count) - ($mean * $mean))
    $stdDev = [Math]::Sqrt($variance)
    return [ordered]@{
        sampleCount = $count
        meanLuma = [Math]::Round($mean, 2)
        lumaStdDev = [Math]::Round($stdDev, 2)
        darkFraction = [Math]::Round($darkCount / $count, 4)
        lightFraction = [Math]::Round($lightCount / $count, 4)
        nonBlank = $stdDev -ge 3.0
        portrait = $Bitmap.Height -gt $Bitmap.Width
    }
}

function Get-EvidenceVerdict {
    param(
        [bool]$Aborted,
        [string]$AbortReason,
        [bool]$PassedRuntimeSmoke,
        [bool]$UiTextAssertionsPassed,
        [bool]$HasRequiredUiText,
        [string]$VisualClaim,
        [Nullable[bool]]$TargetVisible,
        [Nullable[bool]]$VisualValidated,
        [bool]$ScreenshotNonBlank,
        [bool]$ScreenshotPortrait
    )

    if ($Aborted) {
        return [ordered]@{
            status = "thermal_preflight_aborted"
            countsAsVisualValidation = $false
            reason = $AbortReason
        }
    }
    if (-not $PassedRuntimeSmoke) {
        return [ordered]@{
            status = "runtime_failed"
            countsAsVisualValidation = $false
            reason = "Runtime smoke failed."
        }
    }
    if ($HasRequiredUiText -and -not $UiTextAssertionsPassed) {
        return [ordered]@{
            status = "ui_assertion_failed"
            countsAsVisualValidation = $false
            reason = "Required UI text was not found."
        }
    }
    if (-not $ScreenshotNonBlank) {
        return [ordered]@{
            status = "screenshot_blank"
            countsAsVisualValidation = $false
            reason = "Screenshot is blank or near-uniform."
        }
    }
    if (-not $ScreenshotPortrait) {
        return [ordered]@{
            status = "wrong_orientation"
            countsAsVisualValidation = $false
            reason = "Screenshot is not portrait-oriented."
        }
    }
    if ($TargetVisible -eq $true -and $VisualValidated -eq $true) {
        return [ordered]@{
            status = "visual_validated"
            countsAsVisualValidation = $true
            reason = "Target was visible and the operator accepted the visual claim."
        }
    }
    if ($TargetVisible -eq $true) {
        return [ordered]@{
            status = "target_visible_unvalidated"
            countsAsVisualValidation = $false
            reason = "Target was visible, but the visual claim was not accepted."
        }
    }
    if (-not [string]::IsNullOrWhiteSpace($VisualClaim)) {
        return [ordered]@{
            status = "visual_claim_without_target"
            countsAsVisualValidation = $false
            reason = "A visual claim was provided without a visible target."
        }
    }
    return [ordered]@{
        status = "runtime_smoke_only"
        countsAsVisualValidation = $false
        reason = "Runtime smoke passed, but no watched visual target was validated."
    }
}

$bundle = (Resolve-Path -LiteralPath $BundlePath).Path
$manifestPath = Get-RequiredPath $bundle "manifest.json"
$gfxPath = Get-RequiredPath $bundle "gfxinfo.txt"
$logcatPath = Get-RequiredPath $bundle "logcat_tail.txt"
$screenshotPath = Get-RequiredPath $bundle "screenshot.png"
$screenrecordPath = Get-RequiredPath $bundle "screenrecord.mp4"
$reviewContactSheetPath = Get-RequiredPath $bundle "review_contact_sheet.jpg"
$reviewContactSheetManifestPath = Get-RequiredPath $bundle "review_contact_sheet_manifest.json"
$roiMeasurementPath = Get-RequiredPath $bundle "roi_overlay_measurement.json"
$thermalPath = Get-RequiredPath $bundle "thermalservice.txt"
$batteryPath = Get-RequiredPath $bundle "battery.txt"
$uiDumpPath = Get-RequiredPath $bundle "ui_dump.xml"
$thermalReadyWaitPath = Get-RequiredPath $bundle "thermal_ready_wait.json"
$windowFocusPath = Get-RequiredPath $bundle "window_focus.txt"

$manifest = $null
if (Test-Path -LiteralPath $manifestPath) {
    $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
}
$aborted = $manifest -and
    ($manifest.PSObject.Properties.Name -contains "aborted") -and
    ($manifest.aborted -eq $true)
$abortReason = if ($aborted -and $manifest.PSObject.Properties.Name -contains "abortReason") {
    $manifest.abortReason
} else {
    $null
}
$expectedPackage = if ($manifest -and $manifest.PSObject.Properties.Name -contains "package" -and -not [string]::IsNullOrWhiteSpace($manifest.package)) {
    $manifest.package
} else {
    "com.dnrohr.eulerianmagnification"
}
$deviceSerial = if ($manifest -and $manifest.PSObject.Properties.Name -contains "deviceSerial" -and -not [string]::IsNullOrWhiteSpace($manifest.deviceSerial)) {
    [string]$manifest.deviceSerial
} else {
    $null
}
$deviceSerialRequirementPassed = [string]::IsNullOrWhiteSpace($RequireDeviceSerial) -or ($deviceSerial -eq $RequireDeviceSerial)

$missing = @()
$requiredArtifactPaths = if ($aborted) {
    @($manifestPath)
} else {
    @($manifestPath, $gfxPath, $logcatPath, $screenshotPath)
}
foreach ($required in $requiredArtifactPaths) {
    if (-not (Test-Path -LiteralPath $required)) {
        $missing += $required
    }
}

$requiredUiText = @()
foreach ($required in @($RequireUiText)) {
    if (-not [string]::IsNullOrWhiteSpace($required)) {
        $requiredUiText += $required
    }
}
if ($requiredUiText.Count -eq 0 -and $manifest -and $manifest.launch -and $manifest.launch.PSObject.Properties.Name -contains "requireUiText") {
    foreach ($required in @($manifest.launch.requireUiText)) {
        if (-not [string]::IsNullOrWhiteSpace($required)) {
            $requiredUiText += $required
        }
    }
}

$visualReview = [ordered]@{
    targetDescription = $null
    visualClaim = $null
    targetVisible = $null
    visualValidated = $null
    operatorNotes = $null
    countsAsVisualValidation = $false
}
if ($manifest -and $manifest.PSObject.Properties.Name -contains "visualReview") {
    $visualReview.targetDescription = $manifest.visualReview.targetDescription
    $visualReview.visualClaim = $manifest.visualReview.visualClaim
    $visualReview.targetVisible = $manifest.visualReview.targetVisible
    $visualReview.visualValidated = $manifest.visualReview.visualValidated
    $visualReview.operatorNotes = $manifest.visualReview.operatorNotes
    $visualReview.countsAsVisualValidation = ($manifest.visualReview.targetVisible -eq $true) -and
        ($manifest.visualReview.visualValidated -eq $true)
}

$gfx = Read-TextIfExists $gfxPath
$logcat = Read-TextIfExists $logcatPath
$windowFocusText = Read-TextIfExists $windowFocusPath
$focusedAppSummary = [ordered]@{
    present = Test-Path -LiteralPath $windowFocusPath
    expectedPackage = $expectedPackage
    packageVisible = -not [string]::IsNullOrWhiteSpace($windowFocusText) -and
        $windowFocusText.IndexOf($expectedPackage, [System.StringComparison]::OrdinalIgnoreCase) -ge 0
}
$focusedAppPassed = (-not [bool]$RequireFocusedApp) -or ($focusedAppSummary.present -and $focusedAppSummary.packageVisible)
$thermalText = Read-TextIfExists $thermalPath
$thermalSummary = Parse-ThermalSummary $thermalText
$thermalPreflightSummary = if ($manifest -and $manifest.PSObject.Properties.Name -contains "thermalPreflight") {
    $manifest.thermalPreflight
} else {
    $preflightPath = Get-RequiredPath $bundle "thermalservice_preflight.txt"
    Parse-ThermalSummary (Read-TextIfExists $preflightPath)
}
$thermalReadyWait = if (Test-Path -LiteralPath $thermalReadyWaitPath) {
    Get-Content -LiteralPath $thermalReadyWaitPath -Raw | ConvertFrom-Json
} else {
    $null
}
$thermalReadyPassed = (-not [bool]$RequireThermalReady) -or ($null -ne $thermalReadyWait -and $thermalReadyWait.ready -eq $true)
$cameraFpsSummary = Parse-CameraFpsSummary $logcat
$cameraFpsPassed = (-not [bool]$RequireCameraFps) -or (
    $cameraFpsSummary.sampleCount -gt 0 -and
    $null -ne $cameraFpsSummary.minFps -and
    $cameraFpsSummary.minFps -ge $WarnCameraFps
)
$batteryText = Read-TextIfExists $batteryPath
$batterySummary = Parse-BatterySummary $batteryText
$uiDumpSummary = Parse-UiDumpSummary $uiDumpPath
$uiTextAssertions = if ($aborted) {
    @()
} else {
    Test-RequiredUiText -Texts @($uiDumpSummary.text) -RequiredText $requiredUiText
}

$jankyPercent = Match-FirstNumber $gfx 'Janky frames:\s+\d+\s+\(([0-9.]+)%\)'
$totalFrames = Match-FirstNumber $gfx 'Total frames rendered:\s+(\d+)'
$medianFrameMs = Match-FirstNumber $gfx '50th percentile:\s+(\d+)ms'
$p90FrameMs = Match-FirstNumber $gfx '90th percentile:\s+(\d+)ms'
$missedVsync = Match-FirstNumber $gfx 'Number Missed Vsync:\s+(\d+)'

$runtimePatterns = [ordered]@{
    fatalException = 'FATAL EXCEPTION'
    androidRuntime = '\s[EF]\s+AndroidRuntime\b'
    anr = '\bANR\b'
    glError = 'GL error|CameraOesRenderer.*error|RendererError'
}

$runtimeFindings = [ordered]@{}
foreach ($entry in $runtimePatterns.GetEnumerator()) {
    $runtimeFindings[$entry.Key] = [regex]::IsMatch($logcat, $entry.Value, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
}

$warnings = @()
$sourcePresent = $manifest -and ($manifest.PSObject.Properties.Name -contains "source") -and $manifest.source
$sourceDirty = $false
$sourceCommitReachableFromOriginMain = $null
if ($sourcePresent -and ($manifest.source.PSObject.Properties.Name -contains "dirty")) {
    $sourceDirty = $manifest.source.dirty -eq $true
}
if ($sourcePresent -and ($manifest.source.PSObject.Properties.Name -contains "commitReachableFromOriginMain")) {
    $sourceCommitReachableFromOriginMain = $manifest.source.commitReachableFromOriginMain
}
if ($manifest -and $manifest.PSObject.Properties.Name -contains "warnings") {
    foreach ($warning in @($manifest.warnings)) {
        if (-not [string]::IsNullOrWhiteSpace($warning)) {
            $warnings += $warning
        }
    }
}
if ($sourceDirty) {
    $warnings += "source worktree was dirty during capture"
}
if ($sourceCommitReachableFromOriginMain -eq $false) {
    $warnings += "source commit was not reachable from origin/main during capture"
}
if ($RequireCleanSource -and (-not $sourcePresent)) {
    $warnings += "clean source required but source metadata is missing"
}
if ($RequireCleanSource -and $sourceDirty) {
    $warnings += "clean source required but worktree was dirty during capture"
}
if ($missing.Count -gt 0 -and -not $aborted) {
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
if ($cameraFpsSummary.sampleCount -gt 0 -and $null -ne $cameraFpsSummary.minFps -and $cameraFpsSummary.minFps -lt $WarnCameraFps) {
    $warnings += "camera HAL FPS below $WarnCameraFps fps"
}
if ($RequireCameraFps -and $cameraFpsSummary.sampleCount -eq 0) {
    $warnings += "camera HAL FPS required but no FPS samples were found"
}
if ($RequireCameraFps -and $cameraFpsSummary.sampleCount -gt 0 -and $null -ne $cameraFpsSummary.minFps -and $cameraFpsSummary.minFps -lt $WarnCameraFps) {
    $warnings += "camera HAL FPS required but minimum FPS was below $WarnCameraFps fps"
}
if ($RequireFocusedApp -and -not $focusedAppSummary.present) {
    $warnings += "focused app required but window_focus.txt is missing"
}
if ($RequireFocusedApp -and $focusedAppSummary.present -and -not $focusedAppSummary.packageVisible) {
    $warnings += "focused app required but $expectedPackage was not found in window_focus.txt"
}
if (-not $deviceSerialRequirementPassed) {
    $actualDeviceSerial = if ([string]::IsNullOrWhiteSpace($deviceSerial)) { "<missing>" } else { $deviceSerial }
    $warnings += "device serial required $RequireDeviceSerial but was $actualDeviceSerial"
}
if ($batterySummary.powered) {
    $warnings += "device is externally powered during capture"
}
if ($null -ne $batterySummary.temperatureC -and $batterySummary.temperatureC -ge $WarnBatteryTemperatureC) {
    $warnings += "battery temperature $($batterySummary.temperatureC) C at or above warning threshold $WarnBatteryTemperatureC C"
}
if ($RequireThermalReady -and $null -eq $thermalReadyWait) {
    $warnings += "thermal readiness required but thermal_ready_wait.json is missing"
}
if ($RequireThermalReady -and $null -ne $thermalReadyWait -and $thermalReadyWait.ready -ne $true) {
    $warnings += "thermal readiness required but wait result was not ready"
}
foreach ($entry in $runtimeFindings.GetEnumerator()) {
    if ($entry.Value) {
        $warnings += "runtime finding: $($entry.Key)"
    }
}
foreach ($assertion in @($uiTextAssertions)) {
    if ($assertion.found -ne $true) {
        $warnings += "required UI text missing: $($assertion.text)"
    }
}
if (-not [string]::IsNullOrWhiteSpace($visualReview.visualClaim) -and $visualReview.targetVisible -ne $true) {
    $warnings += "visual claim provided without targetVisible=true"
}
if ($visualReview.targetVisible -eq $true -and $visualReview.visualValidated -ne $true) {
    $warnings += "target visible but visualValidated is not true"
}
if ($visualReview.visualValidated -eq $true -and [string]::IsNullOrWhiteSpace($visualReview.operatorNotes)) {
    $warnings += "visualValidated=true requires non-empty operator notes"
}
if ($visualReview.visualValidated -eq $true -and [string]::IsNullOrWhiteSpace($visualReview.targetDescription)) {
    $warnings += "visualValidated=true requires non-empty target description"
}
if ($visualReview.visualValidated -eq $true -and [string]::IsNullOrWhiteSpace($visualReview.visualClaim)) {
    $warnings += "visualValidated=true requires non-empty visual claim"
}

$roiMeasurement = $null
if (Test-Path -LiteralPath $roiMeasurementPath) {
    $roiMeasurement = Get-Content -LiteralPath $roiMeasurementPath -Raw | ConvertFrom-Json
    if ($roiMeasurement.passed -ne $true) {
        $warnings += "ROI overlay measurement failed"
    }
}
if ($RequireRoiMeasurement -and $null -eq $roiMeasurement) {
    $warnings += "ROI measurement required but roi_overlay_measurement.json is missing"
}
if ($RequireRoiMeasurement -and $null -ne $roiMeasurement -and $roiMeasurement.passed -ne $true) {
    $warnings += "ROI measurement required but measurement did not pass"
}

$screenrecordInfo = [ordered]@{
    path = $screenrecordPath
    present = Test-Path -LiteralPath $screenrecordPath
    bytes = $null
    sha256 = $null
    nonEmpty = $false
    mp4Signature = $false
}
if ($screenrecordInfo.present) {
    $screenrecordInfo.bytes = (Get-Item -LiteralPath $screenrecordPath).Length
    $screenrecordInfo.sha256 = Get-FileSha256IfExists $screenrecordPath
    $screenrecordInfo.nonEmpty = $screenrecordInfo.bytes -gt 0
    $screenrecordInfo.mp4Signature = Test-Mp4Signature $screenrecordPath
}
if ($RequireScreenrecord -and -not $screenrecordInfo.present) {
    $warnings += "screenrecord required but screenrecord.mp4 is missing"
}
if ($RequireScreenrecord -and $screenrecordInfo.present -and -not $screenrecordInfo.nonEmpty) {
    $warnings += "screenrecord required but screenrecord.mp4 is empty"
}
if ($RequireScreenrecord -and $screenrecordInfo.present -and $screenrecordInfo.nonEmpty -and -not $screenrecordInfo.mp4Signature) {
    $warnings += "screenrecord required but screenrecord.mp4 does not look like an MP4 file"
}

$reviewContactSheetManifest = if (Test-Path -LiteralPath $reviewContactSheetManifestPath) {
    Get-Content -LiteralPath $reviewContactSheetManifestPath -Raw | ConvertFrom-Json
} else {
    $null
}
$reviewContactSheetInfo = [ordered]@{
    path = $reviewContactSheetPath
    manifestPath = $reviewContactSheetManifestPath
    present = Test-Path -LiteralPath $reviewContactSheetPath
    manifestPresent = $null -ne $reviewContactSheetManifest
    bytes = $null
    sha256 = $null
    screenrecordSha256 = if ($null -ne $reviewContactSheetManifest -and $reviewContactSheetManifest.PSObject.Properties.Name -contains "screenrecordSha256") { $reviewContactSheetManifest.screenrecordSha256 } else { $null }
    screenrecordSha256Matches = $null
    manifest = $reviewContactSheetManifest
}
if ($reviewContactSheetInfo.present) {
    $reviewContactSheetInfo.bytes = (Get-Item -LiteralPath $reviewContactSheetPath).Length
    $reviewContactSheetInfo.sha256 = Get-FileSha256IfExists $reviewContactSheetPath
}
if ($reviewContactSheetInfo.present -and $reviewContactSheetInfo.manifestPresent -and
    -not [string]::IsNullOrWhiteSpace($reviewContactSheetInfo.screenrecordSha256) -and
    -not [string]::IsNullOrWhiteSpace($screenrecordInfo.sha256)) {
    $reviewContactSheetInfo.screenrecordSha256Matches = ($reviewContactSheetInfo.screenrecordSha256 -eq $screenrecordInfo.sha256)
}
$reviewContactSheetPassed = (-not [bool]$RequireReviewContactSheet) -or (
    $reviewContactSheetInfo.present -and
    $reviewContactSheetInfo.manifestPresent -and
    -not [string]::IsNullOrWhiteSpace($reviewContactSheetInfo.sha256) -and
    ($reviewContactSheetInfo.screenrecordSha256Matches -eq $true)
)
if ($reviewContactSheetInfo.present -and -not $reviewContactSheetInfo.manifestPresent) {
    $warnings += "review contact sheet is present but review_contact_sheet_manifest.json is missing"
}
if ($reviewContactSheetInfo.present -and $reviewContactSheetInfo.manifestPresent -and $reviewContactSheetInfo.screenrecordSha256Matches -eq $false) {
    $warnings += "review contact sheet manifest screenrecord SHA-256 does not match screenrecord.mp4"
}
if ($RequireReviewContactSheet -and -not $reviewContactSheetInfo.present) {
    $warnings += "review contact sheet required but review_contact_sheet.jpg is missing"
}
if ($RequireReviewContactSheet -and $reviewContactSheetInfo.present -and -not $reviewContactSheetInfo.manifestPresent) {
    $warnings += "review contact sheet required but review_contact_sheet_manifest.json is missing"
}
if ($RequireReviewContactSheet -and $reviewContactSheetInfo.present -and $reviewContactSheetInfo.manifestPresent -and $reviewContactSheetInfo.screenrecordSha256Matches -ne $true) {
    $warnings += "review contact sheet required but manifest does not match screenrecord.mp4"
}

$screenshotInfo = $null
if (Test-Path -LiteralPath $screenshotPath) {
    Add-Type -AssemblyName System.Drawing
    $bitmap = [System.Drawing.Bitmap]::new($screenshotPath)
    try {
        $content = Measure-ScreenshotContent $bitmap
        $screenshotInfo = [ordered]@{
            path = $screenshotPath
            width = $bitmap.Width
            height = $bitmap.Height
            bytes = (Get-Item -LiteralPath $screenshotPath).Length
            sha256 = Get-FileSha256IfExists $screenshotPath
            content = $content
        }
    } finally {
        $bitmap.Dispose()
    }
}

if ($screenshotInfo -and $screenshotInfo.content.nonBlank -ne $true) {
    $warnings += "screenshot appears blank or near-uniform"
}
if ($screenshotInfo -and $screenshotInfo.content.portrait -ne $true) {
    $warnings += "screenshot is not portrait-oriented"
}

$passedRuntimeSmoke = if ($aborted) {
    $false
} else {
    ($missing.Count -eq 0) -and
        (-not $runtimeFindings.fatalException) -and
        (-not $runtimeFindings.androidRuntime) -and
        (-not $runtimeFindings.anr) -and
        (-not $runtimeFindings.glError)
}
$uiTextAssertionsPassed = $aborted -or (@($uiTextAssertions | Where-Object { $_.found -ne $true }).Count -eq 0)
$evidenceVerdict = Get-EvidenceVerdict `
    -Aborted $aborted `
    -AbortReason $abortReason `
    -PassedRuntimeSmoke $passedRuntimeSmoke `
    -UiTextAssertionsPassed $uiTextAssertionsPassed `
    -HasRequiredUiText ($requiredUiText.Count -gt 0) `
    -VisualClaim $visualReview.visualClaim `
    -TargetVisible $visualReview.targetVisible `
    -VisualValidated $visualReview.visualValidated `
    -ScreenshotNonBlank ($screenshotInfo -and $screenshotInfo.content.nonBlank -eq $true) `
    -ScreenshotPortrait ($screenshotInfo -and $screenshotInfo.content.portrait -eq $true)

if ($RequireVisualValidation -and $evidenceVerdict.countsAsVisualValidation -ne $true) {
    $warnings += "visual validation required but evidence verdict does not count as visual validation"
}
$evidenceVerdictRequirementPassed = [string]::IsNullOrWhiteSpace($RequireEvidenceVerdict) -or ($evidenceVerdict.status -eq $RequireEvidenceVerdict)
if (-not $evidenceVerdictRequirementPassed) {
    $warnings += "evidence verdict required $RequireEvidenceVerdict but was $($evidenceVerdict.status)"
}
$rendererDiagnosticsPassed = (-not [bool]$RequireRendererDiagnostics) -or (@($uiDumpSummary.rendererLabels).Count -gt 0)
if (-not $rendererDiagnosticsPassed) {
    $warnings += "renderer diagnostics required but no renderer labels were found in the UI dump"
}
$phaseDiagnosticsPassed = (-not [bool]$RequirePhaseDiagnostics) -or (@($uiDumpSummary.phaseLabels).Count -gt 0)
if (-not $phaseDiagnosticsPassed) {
    $warnings += "phase diagnostics required but no phase labels were found in the UI dump"
}
$screenrecordPassed = (-not [bool]$RequireScreenrecord) -or ($screenrecordInfo.present -and $screenrecordInfo.nonEmpty -and $screenrecordInfo.mp4Signature)
$warningCountBeforeNoWarningsGate = $warnings.Count
if ($RequireNoWarnings -and $warningCountBeforeNoWarningsGate -gt 0) {
    $warnings += "no warnings required but summary has $warningCountBeforeNoWarningsGate warning(s)"
}

$result = [ordered]@{
    bundle = $bundle
    createdAt = if ($manifest) { $manifest.createdAt } else { $null }
    label = if ($manifest) { $manifest.label } else { $null }
    deviceSerial = $deviceSerial
    source = if ($manifest -and $manifest.PSObject.Properties.Name -contains "source") { $manifest.source } else { $null }
    aborted = $aborted
    abortReason = $abortReason
    launch = if ($manifest) { $manifest.launch } else { $null }
    artifacts = [ordered]@{
        missingRequired = $missing
        screenshot = $screenshotInfo
        screenrecord = $screenrecordInfo
        reviewContactSheet = $reviewContactSheetInfo
        screenrecordPresent = $screenrecordInfo.present
        reviewContactSheetPresent = $reviewContactSheetInfo.present
        roiMeasurementPresent = $null -ne $roiMeasurement
        packageInfoPresent = Test-Path -LiteralPath (Join-Path $bundle "app_package.txt")
        windowFocusPresent = $focusedAppSummary.present
        uiDumpPresent = Test-Path -LiteralPath $uiDumpPath
        thermalReadyWaitPresent = $null -ne $thermalReadyWait
    }
    gfx = [ordered]@{
        totalFrames = $totalFrames
        jankyPercent = $jankyPercent
        medianFrameMs = $medianFrameMs
        p90FrameMs = $p90FrameMs
        missedVsync = $missedVsync
    }
    runtimeFindings = $runtimeFindings
    cameraHal = $cameraFpsSummary
    focusedApp = $focusedAppSummary
    thermalReadyWait = $thermalReadyWait
    thermalPreflight = $thermalPreflightSummary
    thermal = $thermalSummary
    battery = $batterySummary
    uiDump = $uiDumpSummary
    uiTextAssertions = [ordered]@{
        required = $requiredUiText
        checks = $uiTextAssertions
        passed = $uiTextAssertionsPassed
    }
    visualReview = $visualReview
    evidenceVerdict = $evidenceVerdict
    requiredGates = [ordered]@{
        cleanSource = [ordered]@{
            required = [bool]$RequireCleanSource
            sourceMetadataPresent = [bool]$sourcePresent
            commitReachableFromOriginMain = $sourceCommitReachableFromOriginMain
            passed = (-not [bool]$RequireCleanSource) -or ($sourcePresent -and (-not $sourceDirty))
        }
        visualValidation = [ordered]@{
            required = [bool]$RequireVisualValidation
            passed = (-not [bool]$RequireVisualValidation) -or ($evidenceVerdict.countsAsVisualValidation -eq $true)
        }
        roiMeasurement = [ordered]@{
            required = [bool]$RequireRoiMeasurement
            present = $null -ne $roiMeasurement
            passed = (-not [bool]$RequireRoiMeasurement) -or ($null -ne $roiMeasurement -and $roiMeasurement.passed -eq $true)
        }
        screenrecord = [ordered]@{
            required = [bool]$RequireScreenrecord
            present = $screenrecordInfo.present
            bytes = $screenrecordInfo.bytes
            mp4Signature = $screenrecordInfo.mp4Signature
            passed = $screenrecordPassed
        }
        thermalReady = [ordered]@{
            required = [bool]$RequireThermalReady
            present = $null -ne $thermalReadyWait
            ready = if ($null -ne $thermalReadyWait) { $thermalReadyWait.ready } else { $null }
            passed = $thermalReadyPassed
        }
        cameraFps = [ordered]@{
            required = [bool]$RequireCameraFps
            sampleCount = $cameraFpsSummary.sampleCount
            minFps = $cameraFpsSummary.minFps
            thresholdFps = $WarnCameraFps
            passed = $cameraFpsPassed
        }
        focusedApp = [ordered]@{
            required = [bool]$RequireFocusedApp
            present = $focusedAppSummary.present
            expectedPackage = $expectedPackage
            packageVisible = $focusedAppSummary.packageVisible
            passed = $focusedAppPassed
        }
        deviceSerial = [ordered]@{
            required = -not [string]::IsNullOrWhiteSpace($RequireDeviceSerial)
            expected = if ([string]::IsNullOrWhiteSpace($RequireDeviceSerial)) { $null } else { $RequireDeviceSerial }
            actual = $deviceSerial
            passed = $deviceSerialRequirementPassed
        }
        evidenceVerdict = [ordered]@{
            required = -not [string]::IsNullOrWhiteSpace($RequireEvidenceVerdict)
            expected = if ([string]::IsNullOrWhiteSpace($RequireEvidenceVerdict)) { $null } else { $RequireEvidenceVerdict }
            actual = $evidenceVerdict.status
            passed = $evidenceVerdictRequirementPassed
        }
        rendererDiagnostics = [ordered]@{
            required = [bool]$RequireRendererDiagnostics
            labelCount = @($uiDumpSummary.rendererLabels).Count
            passed = $rendererDiagnosticsPassed
        }
        phaseDiagnostics = [ordered]@{
            required = [bool]$RequirePhaseDiagnostics
            labelCount = @($uiDumpSummary.phaseLabels).Count
            passed = $phaseDiagnosticsPassed
        }
        reviewContactSheet = [ordered]@{
            required = [bool]$RequireReviewContactSheet
            present = $reviewContactSheetInfo.present
            manifestPresent = $reviewContactSheetInfo.manifestPresent
            screenrecordSha256Matches = $reviewContactSheetInfo.screenrecordSha256Matches
            passed = $reviewContactSheetPassed
        }
        noWarnings = [ordered]@{
            required = [bool]$RequireNoWarnings
            warningCount = $warningCountBeforeNoWarningsGate
            passed = (-not [bool]$RequireNoWarnings) -or ($warningCountBeforeNoWarningsGate -eq 0)
        }
    }
    roiMeasurement = $roiMeasurement
    warnings = $warnings
    passedRuntimeSmoke = $passedRuntimeSmoke
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

if ($result.aborted) {
    exit 4
}
if (-not $result.passedRuntimeSmoke) {
    exit 2
}
if ($result.uiTextAssertions.required.Count -gt 0 -and -not $result.uiTextAssertions.passed) {
    exit 3
}
if ($result.requiredGates.cleanSource.required -and -not $result.requiredGates.cleanSource.passed) {
    exit 6
}
if ($result.requiredGates.visualValidation.required -and -not $result.requiredGates.visualValidation.passed) {
    exit 5
}
if ($result.requiredGates.roiMeasurement.required -and -not $result.requiredGates.roiMeasurement.passed) {
    exit 8
}
if ($result.requiredGates.screenrecord.required -and -not $result.requiredGates.screenrecord.passed) {
    exit 11
}
if ($result.requiredGates.thermalReady.required -and -not $result.requiredGates.thermalReady.passed) {
    exit 12
}
if ($result.requiredGates.cameraFps.required -and -not $result.requiredGates.cameraFps.passed) {
    exit 13
}
if ($result.requiredGates.focusedApp.required -and -not $result.requiredGates.focusedApp.passed) {
    exit 14
}
if ($result.requiredGates.deviceSerial.required -and -not $result.requiredGates.deviceSerial.passed) {
    exit 21
}
if ($result.requiredGates.evidenceVerdict.required -and -not $result.requiredGates.evidenceVerdict.passed) {
    exit 9
}
if (($result.requiredGates.rendererDiagnostics.required -and -not $result.requiredGates.rendererDiagnostics.passed) -or
    ($result.requiredGates.phaseDiagnostics.required -and -not $result.requiredGates.phaseDiagnostics.passed)) {
    exit 10
}
if ($result.requiredGates.reviewContactSheet.required -and -not $result.requiredGates.reviewContactSheet.passed) {
    exit 22
}
if ($result.requiredGates.noWarnings.required -and -not $result.requiredGates.noWarnings.passed) {
    exit 7
}
exit 0
