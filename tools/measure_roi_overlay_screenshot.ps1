param(
    [Parameter(Mandatory = $true)]
    [string]$ScreenshotPath,

    [Parameter(Mandatory = $true)]
    [string]$ExpectedRoi,

    [string]$OutputPath = "",
    [ValidateSet("Manual", "Auto")]
    [string]$OverlayKind = "Manual",
    [string]$TargetColor = "",
    [int]$ColorTolerance = 42,
    [double]$SearchMargin = 0.08,
    [double]$MaxEdgeError = 0.04,
    [int]$MinimumMatchedPixels = 24,
    [switch]$AllowMultipleComponents
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

function Default-OverlayColor {
    param([string]$Kind)
    if ($Kind -eq "Auto") {
        return "#00BFA5"
    }
    return "#FFC857"
}

function Measure-Components {
    param(
        [System.Collections.Generic.HashSet[string]]$MatchedPixels
    )

    $unvisited = [System.Collections.Generic.HashSet[string]]::new($MatchedPixels)
    $componentCount = 0
    $largestSize = 0
    $largestBounds = $null

    while ($unvisited.Count -gt 0) {
        $seed = $null
        foreach ($item in $unvisited) {
            $seed = $item
            break
        }
        if ($null -eq $seed) {
            break
        }

        $componentCount += 1
        $queue = [System.Collections.Generic.Queue[string]]::new()
        $queue.Enqueue($seed)
        [void]$unvisited.Remove($seed)

        $size = 0
        $minX = [int]::MaxValue
        $minY = [int]::MaxValue
        $maxX = -1
        $maxY = -1

        while ($queue.Count -gt 0) {
            $current = $queue.Dequeue()
            $parts = $current.Split(",")
            $x = [int]$parts[0]
            $y = [int]$parts[1]
            $size += 1
            if ($x -lt $minX) { $minX = $x }
            if ($x -gt $maxX) { $maxX = $x }
            if ($y -lt $minY) { $minY = $y }
            if ($y -gt $maxY) { $maxY = $y }

            $neighbors = @(
                "$($x - 1),$y",
                "$($x + 1),$y",
                "$x,$($y - 1)",
                "$x,$($y + 1)"
            )
            foreach ($neighbor in $neighbors) {
                if ($unvisited.Remove($neighbor)) {
                    $queue.Enqueue($neighbor)
                }
            }
        }

        if ($size -gt $largestSize) {
            $largestSize = $size
            $largestBounds = [ordered]@{
                leftPx = $minX
                topPx = $minY
                rightPx = $maxX + 1
                bottomPx = $maxY + 1
            }
        }
    }

    return [ordered]@{
        count = $componentCount
        largestSize = $largestSize
        largestBounds = $largestBounds
    }
}

Add-Type -AssemblyName System.Drawing

$resolvedScreenshot = (Resolve-Path -LiteralPath $ScreenshotPath).Path
$expected = Parse-NormalizedRect $ExpectedRoi
$resolvedTargetColor = if ([string]::IsNullOrWhiteSpace($TargetColor)) {
    Default-OverlayColor $OverlayKind
} else {
    $TargetColor
}
$target = Parse-HexColor $resolvedTargetColor
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
    $matchedPixelKeys = [System.Collections.Generic.HashSet[string]]::new()

    for ($y = $startY; $y -le $endY; $y++) {
        for ($x = $startX; $x -le $endX; $x++) {
            $pixel = $bitmap.GetPixel($x, $y)
            $distance = [Math]::Abs($pixel.R - $target.r) +
                [Math]::Abs($pixel.G - $target.g) +
                [Math]::Abs($pixel.B - $target.b)
            if ($distance -le $ColorTolerance) {
                $matched += 1
                [void]$matchedPixelKeys.Add("$x,$y")
                if ($x -lt $minX) { $minX = $x }
                if ($x -gt $maxX) { $maxX = $x }
                if ($y -lt $minY) { $minY = $y }
                if ($y -gt $maxY) { $maxY = $y }
            }
        }
    }

    $measured = $null
    $edgeError = $null
    $components = Measure-Components $matchedPixelKeys
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
        if (-not $AllowMultipleComponents -and $components.count -ne 1) {
            $passed = $false
        }
    }

    $result = [ordered]@{
        screenshot = $resolvedScreenshot
        imageSize = [ordered]@{ width = $width; height = $height }
        overlayKind = $OverlayKind
        expectedRoi = $expected
        searchMargin = $SearchMargin
        targetColor = $resolvedTargetColor
        colorTolerance = $ColorTolerance
        minimumMatchedPixels = $MinimumMatchedPixels
        matchedPixels = $matched
        components = $components
        requireSingleComponent = (-not $AllowMultipleComponents)
        measuredRoi = $measured
        edgeError = $edgeError
        maxEdgeError = $MaxEdgeError
        passed = $passed
        notes = @(
            "Use clean or hidden controls when possible; UI text can match ROI overlay colors.",
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
