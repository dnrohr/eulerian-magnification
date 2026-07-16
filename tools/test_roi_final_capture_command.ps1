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

function Invoke-HelperExitCode {
    param([string[]]$Arguments)

    $script = Join-Path $PSScriptRoot "prepare_roi_final_capture_command.ps1"
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

$root = Join-Path ([System.IO.Path]::GetTempPath()) "eulerian-roi-final-command-$([guid]::NewGuid().ToString('N'))"
$bundle = Join-Path $root "setup-bundle"
New-Item -ItemType Directory -Path $bundle -Force | Out-Null

try {
    Add-Type -AssemblyName System.Drawing
    $screenshotPath = Join-Path $bundle "screenshot.png"
    $bitmap = [System.Drawing.Bitmap]::new(200, 100)
    try {
        $bitmap.Save($screenshotPath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $bitmap.Dispose()
    }

    $manual = & (Join-Path $PSScriptRoot "prepare_roi_final_capture_command.ps1") `
        -Slot manualRoi `
        -SetupBundle $bundle `
        -PixelBounds "50,25,150,75" `
        -DeviceSerial "PIXEL-FINAL-TEST" `
        -EvidenceRoot $root `
        -Json | ConvertFrom-Json

    Assert-Equal -Actual $manual.measureRoiExpected -Expected "0.25,0.25,0.75,0.75" -Message "Manual final ROI value mismatch."
    Assert-True -Condition ($manual.finalCommand.Contains('manual-roi-known-target-final')) -Message "Manual command should use the manual final label."
    Assert-True -Condition ($manual.finalCommand.Contains('-DeviceSerial "PIXEL-FINAL-TEST"')) -Message "Manual command should preserve device serial."
    Assert-True -Condition ($manual.finalCommand.Contains('-MeasureRoiExpected "0.25,0.25,0.75,0.75"')) -Message "Manual command should replace the ROI placeholder."
    Assert-True -Condition (-not $manual.finalCommand.Contains("<visible-target-bounds-in-screenshot-space>")) -Message "Manual command should not keep the ROI placeholder."

    $autoOutputPath = Join-Path $root "auto-final-command.json"
    $auto = & (Join-Path $PSScriptRoot "prepare_roi_final_capture_command.ps1") `
        -Slot autoRoi `
        -SetupBundle $bundle `
        -PixelBounds "20,10,100,60" `
        -DeviceSerial "PIXEL-FINAL-TEST" `
        -EvidenceRoot $root `
        -OutputPath $autoOutputPath `
        -Json | ConvertFrom-Json

    Assert-Equal -Actual $auto.measureRoiExpected -Expected "0.1,0.1,0.5,0.6" -Message "Auto final ROI value mismatch."
    Assert-True -Condition ($auto.finalCommand.Contains('auto-face-roi-final')) -Message "Auto command should use the auto final label."
    Assert-True -Condition ($auto.finalCommand.Contains('-MeasureRoiExpected "0.1,0.1,0.5,0.6"')) -Message "Auto command should replace the ROI placeholder."
    Assert-True -Condition (-not $auto.finalCommand.Contains("<visible-face-or-skin-target-bounds-in-screenshot-space>")) -Message "Auto command should not keep the ROI placeholder."
    Assert-True -Condition (Test-Path -LiteralPath $autoOutputPath) -Message "Helper should write OutputPath JSON."

    $text = & (Join-Path $PSScriptRoot "prepare_roi_final_capture_command.ps1") `
        -Slot manualRoi `
        -SetupBundle $bundle `
        -PixelBounds "50,25,150,75" `
        -DeviceSerial "PIXEL-FINAL-TEST" `
        -EvidenceRoot $root
    Assert-True -Condition (($text -join "`n").Contains("Final command:")) -Message "Text output should print a final command heading."
    Assert-True -Condition (($text -join "`n").Contains("manual-roi-known-target-final")) -Message "Text output should print the final command."

    $missingBundleExitCode = Invoke-HelperExitCode -Arguments @("-Slot", "manualRoi", "-SetupBundle", (Join-Path $root "missing"), "-PixelBounds", "50,25,150,75")
    $badBoundsExitCode = Invoke-HelperExitCode -Arguments @("-Slot", "manualRoi", "-SetupBundle", $bundle, "-PixelBounds", "50,25,150")
    Assert-Equal -Actual $missingBundleExitCode -Expected 1 -Message "Missing setup bundle should fail."
    Assert-Equal -Actual $badBoundsExitCode -Expected 1 -Message "Malformed pixel bounds should fail."
} finally {
    if (Test-Path -LiteralPath $root) {
        Remove-Item -LiteralPath $root -Recurse -Force
    }
}

Write-Output "ROI final capture command self-test passed."
