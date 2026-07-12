param(
    [Parameter(Mandatory = $true)]
    [string]$ScreenshotPath,

    [Parameter(Mandatory = $true)]
    [string]$ExpectedRoi,

    [string]$OutputPath = "",
    [string]$TargetColor = "#FFC857",
    [int]$ColorTolerance = 42,
    [double]$SearchMargin = 0.08,
    [double]$MaxEdgeError = 0.04,
    [int]$MinimumMatchedPixels = 24
)

$ErrorActionPreference = "Stop"

function Parse-NormalizedRect {
    param([string]$Value)
    $parts = $Value.Split(",") | ForEach-Object { [double]::Parse($_.Trim(), [Globalization.CultureInfo]::InvariantCulture) }
    if ($parts.Count -ne 4) {
        throw "Expected ROI must be four comma-separated normalized values: left,top,right,bottom."
    }
    if ($parts[0] -lt 0.0 -or $parts[1] -lt 0.0 -or $parts[2] -gt 1.0 -or $parts[3] -gt 1.0 -or $parts[2] -le $parts[0] -or $parts[3] -le $parts[1]) {
        throw "Expected ROI must be normalized to 0..1 with right > left and bottom > top."
    }
    return [ordered]@{
        left = $parts[0]
        top = $parts[1]
        right = $parts[2]
        bottom = $parts[3]
    }
}

function Parse-HexColor {
    param([string]$Value)
    $hex = $Value.Trim().TrimStart("#")
    if ($hex.Length -ne 6) {
        throw "TargetColor must be #RRGGBB."
    }
    return [ordered]@{
        r = [Convert]::ToInt32($hex.Substring(0, 2), 16)
        g = [Convert]::ToInt32($hex.Substring(2, 2), 16)
        b = [Convert]::ToInt32($hex.Substring(4, 2), 16)
    }
}

function Clamp-Unit {
    param([double]$Value)
    return [Math]::Min(1.0, [Math]::Max(0.0, $Value))
}

Add-Type -AssemblyName System.Drawing

$resolvedScreenshot = (Resolve-Path -LiteralPath $ScreenshotPath).Path
$expected = Parse-NormalizedRect $ExpectedRoi
$target = Parse-HexColor $TargetColor
$bitmap = [System.Drawing.Bitmap]::new($resolvedScreenshot)

try {
    $width = $bitmap.Width
    $height = $bitmap.Height
    $search = [ordered]@{
        left = Clamp-Unit($expected.left - $SearchMargin)
        top = Clamp-Unit($expected.top - $SearchMargin)
        right = Clamp-Unit($expected.right + $SearchMargin)
        bottom = Clamp-Unit($expected.bottom + $SearchMargin)
    }

    $startX = [Math]::Floor($search.left * $width)
    $endX = [Math]::Ceiling($search.right * $width) - 1
    $startY = [Math]::Floor($search.top * $height)
    $endY = [Math]::Ceiling($search.bottom * $height) - 1

    $matched = 0
    $minX = $width
    $minY = $height
    $maxX = -1
    $maxY = -1

    for ($y = $startY; $y -le $endY; $y++) {
        for ($x = $startX; $x -le $endX; $x++) {
            $pixel = $bitmap.GetPixel($x, $y)
            $distance = [Math]::Abs($pixel.R - $target.r) +
                [Math]::Abs($pixel.G - $target.g) +
                [Math]::Abs($pixel.B - $target.b)
            if ($distance -le $ColorTolerance) {
                $matched += 1
                if ($x -lt $minX) { $minX = $x }
                if ($x -gt $maxX) { $maxX = $x }
                if ($y -lt $minY) { $minY = $y }
                if ($y -gt $maxY) { $maxY = $y }
            }
        }
    }

    $measured = $null
    $edgeError = $null
    $passed = $false
    if ($matched -ge $MinimumMatchedPixels) {
        $measured = [ordered]@{
            left = $minX / [double]$width
            top = $minY / [double]$height
            right = ($maxX + 1) / [double]$width
            bottom = ($maxY + 1) / [double]$height
        }
        $edgeError = [ordered]@{
            left = [Math]::Abs($measured.left - $expected.left)
            top = [Math]::Abs($measured.top - $expected.top)
            right = [Math]::Abs($measured.right - $expected.right)
            bottom = [Math]::Abs($measured.bottom - $expected.bottom)
        }
        $passed = @($edgeError.left, $edgeError.top, $edgeError.right, $edgeError.bottom |
            Where-Object { $_ -gt $MaxEdgeError }).Count -eq 0
    }

    $result = [ordered]@{
        screenshot = $resolvedScreenshot
        imageSize = [ordered]@{ width = $width; height = $height }
        expectedRoi = $expected
        searchMargin = $SearchMargin
        targetColor = $TargetColor
        colorTolerance = $ColorTolerance
        minimumMatchedPixels = $MinimumMatchedPixels
        matchedPixels = $matched
        measuredRoi = $measured
        edgeError = $edgeError
        maxEdgeError = $MaxEdgeError
        passed = $passed
        notes = @(
            "Use clean or hidden controls when possible; yellow UI text can match the manual ROI color.",
            "This measures the visible screenshot overlay only. It does not prove target alignment unless the expected rectangle was derived from a visible known target."
        )
    }

    $json = $result | ConvertTo-Json -Depth 5
    if ([string]::IsNullOrWhiteSpace($OutputPath)) {
        Write-Output $json
    } else {
        $json | Out-File -LiteralPath $OutputPath -Encoding utf8
        Write-Output "ROI overlay measurement written: $OutputPath"
        Write-Output "passed=$passed matchedPixels=$matched"
    }

    if (-not $passed) {
        exit 2
    }
} finally {
    $bitmap.Dispose()
}
