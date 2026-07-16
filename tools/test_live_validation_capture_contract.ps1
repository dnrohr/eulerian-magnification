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

function Assert-SequenceEqual {
    param(
        [string[]]$Actual,
        [string[]]$Expected,
        [string]$Message
    )

    Assert-Equal -Actual $Actual.Count -Expected $Expected.Count -Message "$Message Count mismatch."
    for ($i = 0; $i -lt $Expected.Count; $i++) {
        Assert-Equal -Actual $Actual[$i] -Expected $Expected[$i] -Message "$Message Item $i mismatch."
    }
}

function Get-ValidateSetValues {
    param(
        [System.Management.Automation.CommandInfo]$Command,
        [string]$ParameterName
    )

    $parameter = $Command.Parameters[$ParameterName]
    if ($null -eq $parameter) {
        throw "Parameter '$ParameterName' was not found."
    }

    $attribute = $parameter.Attributes |
        Where-Object { $_ -is [System.Management.Automation.ValidateSetAttribute] } |
        Select-Object -First 1
    if ($null -eq $attribute) {
        throw "Parameter '$ParameterName' does not have a ValidateSet attribute."
    }

    return [string[]]$attribute.ValidValues
}

function Assert-InvalidParameterValue {
    param(
        [string]$ScriptPath,
        [string]$ParameterName,
        [string]$InvalidValue
    )

    $arguments = @{
        SkipLaunch = $true
        ScreenRecordSeconds = 0
    }
    $arguments[$ParameterName] = $InvalidValue

    try {
        & $ScriptPath @arguments *> $null
    } catch [System.Management.Automation.ParameterBindingException] {
        if ($_.FullyQualifiedErrorId -like "ParameterArgumentValidationError*") {
            return
        }
        throw
    }

    throw "Parameter '$ParameterName' accepted invalid value '$InvalidValue'."
}

$captureScript = Join-Path $PSScriptRoot "capture_live_validation_evidence.ps1"
$thermalScript = Join-Path $PSScriptRoot "wait_for_device_thermal_ready.ps1"
$command = Get-Command $captureScript
$thermalCommand = Get-Command $thermalScript
$captureScriptContent = Get-Content -LiteralPath $captureScript -Raw
$thermalScriptContent = Get-Content -LiteralPath $thermalScript -Raw

foreach ($expectedParameter in @("AdbPath", "DeviceSerial")) {
    if (-not $command.Parameters.ContainsKey($expectedParameter)) {
        throw "Capture script must expose -$expectedParameter."
    }
    if (-not $thermalCommand.Parameters.ContainsKey($expectedParameter)) {
        throw "Thermal wait helper must expose -$expectedParameter."
    }
}

Assert-SequenceEqual `
    -Actual (Get-ValidateSetValues -Command $command -ParameterName "Mode") `
    -Expected @("", "Pulse", "Breathing", "Tremor", "ObjectVibration") `
    -Message "Mode ValidateSet"

Assert-SequenceEqual `
    -Actual (Get-ValidateSetValues -Command $command -ParameterName "View") `
    -Expected @("", "Raw", "Amplified", "Difference", "Split") `
    -Message "View ValidateSet"

Assert-SequenceEqual `
    -Actual (Get-ValidateSetValues -Command $command -ParameterName "RoiSource") `
    -Expected @("", "Auto", "FullFrame", "Manual") `
    -Message "RoiSource ValidateSet"

Assert-SequenceEqual `
    -Actual (Get-ValidateSetValues -Command $command -ParameterName "Panel") `
    -Expected @("", "Controls", "Setup", "Recording", "Debug") `
    -Message "Panel ValidateSet"

Assert-SequenceEqual `
    -Actual (Get-ValidateSetValues -Command $command -ParameterName "MeasureRoiKind") `
    -Expected @("", "Manual", "Auto") `
    -Message "MeasureRoiKind ValidateSet"

Assert-SequenceEqual `
    -Actual (Get-ValidateSetValues -Command $command -ParameterName "RequireEvidenceVerdict") `
    -Expected @(
        "",
        "runtime_smoke_only",
        "visual_validated",
        "target_visible_unvalidated",
        "visual_claim_without_target",
        "ui_assertion_failed",
        "screenshot_blank",
        "wrong_orientation",
        "runtime_failed",
        "thermal_preflight_aborted"
    ) `
    -Message "RequireEvidenceVerdict ValidateSet"

Assert-InvalidParameterValue -ScriptPath $captureScript -ParameterName "Mode" -InvalidValue "NotAMode"
Assert-InvalidParameterValue -ScriptPath $captureScript -ParameterName "View" -InvalidValue "Sideways"
Assert-InvalidParameterValue -ScriptPath $captureScript -ParameterName "RoiSource" -InvalidValue "Face"
Assert-InvalidParameterValue -ScriptPath $captureScript -ParameterName "Panel" -InvalidValue "Overlay"
Assert-InvalidParameterValue -ScriptPath $captureScript -ParameterName "MeasureRoiKind" -InvalidValue "Blue"
Assert-InvalidParameterValue -ScriptPath $captureScript -ParameterName "RequireEvidenceVerdict" -InvalidValue "maybe"

foreach ($expectedSourceContract in @(
    "commitReachableFromOriginMain",
    "merge-base",
    "--is-ancestor",
    "origin/main",
    "deviceSerial",
    "adbDeviceArgs",
    "-DeviceSerial"
)) {
    if (-not $captureScriptContent.Contains($expectedSourceContract)) {
        throw "Capture script must preserve source reachability contract: missing '$expectedSourceContract'."
    }
}

foreach ($expectedThermalContract in @(
    "deviceSerial",
    "adbDeviceArgs",
    "@adbDeviceArgs shell dumpsys thermalservice"
)) {
    if (-not $thermalScriptContent.Contains($expectedThermalContract)) {
        throw "Thermal wait helper must preserve device targeting contract: missing '$expectedThermalContract'."
    }
}

Write-Output "Live validation capture contract self-test passed."
