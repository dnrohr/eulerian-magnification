param(
    [string]$ImagePath = "",
    [int]$ImageWidth = 0,
    [int]$ImageHeight = 0,
    [Parameter(Mandatory = $true)]
    [string]$PixelBounds,
    [string]$OutputPath = "",
    [switch]$Json
)

$ErrorActionPreference = "Stop"

function Parse-PixelBounds {
    param([string]$Value)

    $parts = $Value.Split(",") | ForEach-Object {
        [double]::Parse($_.Trim(), [Globalization.CultureInfo]::InvariantCulture)
    }
    if ($parts.Count -ne 4) {
        throw "PixelBounds must be four comma-separated values: left,top,right,bottom."
    }
    if ($parts[2] -le $parts[0] -or $parts[3] -le $parts[1]) {
        throw "PixelBounds must have right > left and bottom > top."
    }
    return [ordered]@{
        left = $parts[0]
        top = $parts[1]
        right = $parts[2]
        bottom = $parts[3]
    }
}

function Format-UnitValue {
    param([double]$Value)

    return $Value.ToString("0.######", [Globalization.CultureInfo]::InvariantCulture)
}

if (-not [string]::IsNullOrWhiteSpace($ImagePath)) {
    Add-Type -AssemblyName System.Drawing
    $resolvedImagePath = (Resolve-Path -LiteralPath $ImagePath).Path
    $bitmap = [System.Drawing.Bitmap]::new($resolvedImagePath)
    try {
        $ImageWidth = $bitmap.Width
        $ImageHeight = $bitmap.Height
    } finally {
        $bitmap.Dispose()
    }
} else {
    $resolvedImagePath = $null
}

if ($ImageWidth -le 0 -or $ImageHeight -le 0) {
    throw "Provide -ImagePath or positive -ImageWidth and -ImageHeight values."
}

$bounds = Parse-PixelBounds $PixelBounds
if ($bounds.left -lt 0 -or $bounds.top -lt 0 -or $bounds.right -gt $ImageWidth -or $bounds.bottom -gt $ImageHeight) {
    throw "PixelBounds must be inside the image size."
}

$normalized = [ordered]@{
    left = $bounds.left / [double]$ImageWidth
    top = $bounds.top / [double]$ImageHeight
    right = $bounds.right / [double]$ImageWidth
    bottom = $bounds.bottom / [double]$ImageHeight
}
$measureRoiExpected = @(
    Format-UnitValue $normalized.left
    Format-UnitValue $normalized.top
    Format-UnitValue $normalized.right
    Format-UnitValue $normalized.bottom
) -join ","

$result = [pscustomobject][ordered]@{
    imagePath = $resolvedImagePath
    imageSize = [pscustomobject][ordered]@{
        width = $ImageWidth
        height = $ImageHeight
    }
    pixelBounds = [pscustomobject]$bounds
    normalizedBounds = [pscustomobject]$normalized
    measureRoiExpected = $measureRoiExpected
    note = "Use this value only when PixelBounds came from the visible target in the captured app screenshot."
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
    Write-Output "MeasureRoiExpected: $measureRoiExpected"
    Write-Output "Image size: ${ImageWidth}x${ImageHeight}"
    Write-Output "Pixel bounds: $PixelBounds"
    if (-not [string]::IsNullOrWhiteSpace($OutputPath)) {
        Write-Output "Output: $OutputPath"
    }
}
