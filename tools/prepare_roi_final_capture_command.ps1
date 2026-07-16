param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("manualRoi", "autoRoi")]
    [string]$Slot,
    [Parameter(Mandatory = $true)]
    [string]$SetupBundle,
    [Parameter(Mandatory = $true)]
    [string]$PixelBounds,
    [string]$EvidenceRoot = "sample-videos\exports\live-validation",
    [string]$DeviceSerial = "47091JEKB05516",
    [string]$OutputPath = "",
    [switch]$Json
)

$ErrorActionPreference = "Stop"

$resolvedBundle = Resolve-Path -LiteralPath $SetupBundle -ErrorAction SilentlyContinue
if (-not $resolvedBundle) {
    throw "Setup bundle not found: $SetupBundle"
}

$screenshotPath = Join-Path $resolvedBundle.Path "screenshot.png"
if (-not (Test-Path -LiteralPath $screenshotPath)) {
    throw "Setup bundle does not contain screenshot.png: $SetupBundle"
}

$converter = Join-Path $PSScriptRoot "convert_roi_bounds_to_normalized.ps1"
$planner = Join-Path $PSScriptRoot "show_next_pixel_validation_plan.ps1"
$converted = & $converter -ImagePath $screenshotPath -PixelBounds $PixelBounds -Json | ConvertFrom-Json
$commands = @(& $planner -EvidenceRoot $EvidenceRoot -DeviceSerial $DeviceSerial -Slot $Slot -CaptureStage Final -CommandsOnly)
$command = @($commands | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -First 1)
if ([string]::IsNullOrWhiteSpace($command)) {
    throw "No final capture command is available for slot '$Slot'."
}

$placeholderPattern = if ($Slot -eq "manualRoi") {
    '<visible-target-bounds-in-screenshot-space>'
} else {
    '<visible-face-or-skin-target-bounds-in-screenshot-space>'
}
$finalCommand = $command.Replace($placeholderPattern, $converted.measureRoiExpected)
if ($finalCommand -eq $command) {
    throw "Final command for slot '$Slot' did not contain the expected ROI placeholder."
}

$result = [pscustomobject][ordered]@{
    slot = $Slot
    setupBundle = $resolvedBundle.Path
    screenshotPath = (Resolve-Path -LiteralPath $screenshotPath).Path
    pixelBounds = $PixelBounds
    measureRoiExpected = $converted.measureRoiExpected
    finalCommand = $finalCommand
    note = "Run this final command only after the setup screenshot target is visible and the pixel bounds were measured from that target."
}

if (-not [string]::IsNullOrWhiteSpace($OutputPath)) {
    $outputDirectory = Split-Path -Parent $OutputPath
    if (-not [string]::IsNullOrWhiteSpace($outputDirectory) -and -not (Test-Path -LiteralPath $outputDirectory)) {
        New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
    }
    $result | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $OutputPath -Encoding utf8
}

if ($Json) {
    $result | ConvertTo-Json -Depth 4
} else {
    Write-Output "MeasureRoiExpected: $($result.measureRoiExpected)"
    Write-Output "Final command:"
    Write-Output $result.finalCommand
    if (-not [string]::IsNullOrWhiteSpace($OutputPath)) {
        Write-Output "Output: $OutputPath"
    }
}
