$ErrorActionPreference = "Stop"

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Assert-Equal {
    param(
        $Actual,
        $Expected,
        [string]$Message
    )

    if ($Actual -ne $Expected) {
        throw "$Message Expected '$Expected' but got '$Actual'."
    }
}

function Invoke-ConverterExitCode {
    param([string[]]$Arguments)

    $script = Join-Path $PSScriptRoot "convert_roi_bounds_to_normalized.ps1"
    $powerShellExe = (Get-Process -Id $PID).Path
    $oldErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        & $powerShellExe @("-NoProfile", "-File", $script) @Arguments *> $null
        return $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $oldErrorActionPreference
    }
}

$root = Join-Path ([System.IO.Path]::GetTempPath()) "eulerian-roi-bounds-converter-$([guid]::NewGuid().ToString('N'))"
New-Item -ItemType Directory -Path $root -Force | Out-Null

try {
    $result = & (Join-Path $PSScriptRoot "convert_roi_bounds_to_normalized.ps1") `
        -ImageWidth 1080 `
        -ImageHeight 2400 `
        -PixelBounds "90,600,990,1800" `
        -Json | ConvertFrom-Json

    Assert-Equal -Actual $result.measureRoiExpected -Expected "0.083333,0.25,0.916667,0.75" -Message "Normalized ROI string mismatch."
    Assert-Equal -Actual $result.imageSize.width -Expected 1080 -Message "Image width mismatch."
    Assert-Equal -Actual $result.imageSize.height -Expected 2400 -Message "Image height mismatch."
    Assert-Equal -Actual $result.pixelBounds.left -Expected 90 -Message "Pixel left mismatch."
    Assert-True -Condition ($result.note.Contains("visible target")) -Message "Converter should preserve target-derived warning."

    Add-Type -AssemblyName System.Drawing
    $imagePath = Join-Path $root "screenshot.png"
    $bitmap = [System.Drawing.Bitmap]::new(200, 100)
    try {
        $bitmap.Save($imagePath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $bitmap.Dispose()
    }

    $outputPath = Join-Path $root "roi_bounds.json"
    $fromImage = & (Join-Path $PSScriptRoot "convert_roi_bounds_to_normalized.ps1") `
        -ImagePath $imagePath `
        -PixelBounds "50,25,150,75" `
        -OutputPath $outputPath `
        -Json | ConvertFrom-Json

    Assert-Equal -Actual $fromImage.measureRoiExpected -Expected "0.25,0.25,0.75,0.75" -Message "Image-derived normalized ROI mismatch."
    Assert-True -Condition (Test-Path -LiteralPath $outputPath) -Message "Converter should write OutputPath JSON."
    $written = Get-Content -LiteralPath $outputPath -Raw | ConvertFrom-Json
    Assert-Equal -Actual $written.measureRoiExpected -Expected "0.25,0.25,0.75,0.75" -Message "Written normalized ROI mismatch."

    $text = & (Join-Path $PSScriptRoot "convert_roi_bounds_to_normalized.ps1") `
        -ImageWidth 200 `
        -ImageHeight 100 `
        -PixelBounds "50,25,150,75"
    Assert-True -Condition (($text -join "`n").Contains("MeasureRoiExpected: 0.25,0.25,0.75,0.75")) -Message "Text output should print the paste-ready ROI string."

    $badShapeExitCode = Invoke-ConverterExitCode -Arguments @("-ImageWidth", "200", "-ImageHeight", "100", "-PixelBounds", "50,25,150")
    $outsideExitCode = Invoke-ConverterExitCode -Arguments @("-ImageWidth", "200", "-ImageHeight", "100", "-PixelBounds", "-1,25,150,75")
    Assert-Equal -Actual $badShapeExitCode -Expected 1 -Message "Malformed bounds should fail."
    Assert-Equal -Actual $outsideExitCode -Expected 1 -Message "Out-of-image bounds should fail."
} finally {
    if (Test-Path -LiteralPath $root) {
        Remove-Item -LiteralPath $root -Recurse -Force
    }
}

Write-Output "ROI bounds converter self-test passed."
