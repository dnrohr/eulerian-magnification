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

$root = Join-Path ([System.IO.Path]::GetTempPath()) "eulerian-preview-recovery-$([guid]::NewGuid().ToString('N'))"
New-Item -ItemType Directory -Path $root | Out-Null
$fakeAdb = Join-Path $root "adb.exe"
Set-Content -LiteralPath $fakeAdb -Value "fake adb" -Encoding utf8

$result = & (Join-Path $PSScriptRoot "recover_pixel_preview_session.ps1") `
    -AdbPath $fakeAdb `
    -DeviceSerial "PIXEL123" `
    -Attempts 2 `
    -SettleSeconds 0 `
    -OutputRoot $root `
    -RequireFinalReady `
    -DryRun `
    -Json | ConvertFrom-Json

Assert-Equal -Actual $result.dryRun -Expected $true -Message "Dry run flag mismatch."
Assert-Equal -Actual $result.adb -Expected $fakeAdb -Message "Dry run should preserve explicit adb path."
Assert-Equal -Actual $result.deviceSerial -Expected "PIXEL123" -Message "Dry run should preserve requested serial."
Assert-Equal -Actual $result.attempts -Expected 2 -Message "Dry run should expose attempt count."
Assert-Equal -Actual $result.settleSeconds -Expected 0 -Message "Dry run should expose settle seconds."
Assert-Equal -Actual $result.requireFinalReady -Expected $true -Message "Dry run should expose final-readiness mode."

$commands = @($result.commands) -join "`n"
Assert-True -Condition ($commands.Contains("am force-stop com.dnrohr.eulerianmagnification")) -Message "Recovery should force-stop the app."
Assert-True -Condition ($commands.Contains("logcat -c")) -Message "Recovery should clear logcat before launch."
Assert-True -Condition ($commands.Contains("validation.cameraSession recovery-")) -Message "Recovery should launch with a fresh camera session token."
Assert-True -Condition ($commands.Contains("validation.glPreview true")) -Message "Recovery should request GL preview."
Assert-True -Condition ($commands.Contains("validation.clean true")) -Message "Recovery should request clean preview."
Assert-True -Condition ($commands.Contains("export_pixel_session_readiness.ps1")) -Message "Recovery should probe readiness after launch."
Assert-True -Condition ($commands.Contains("-RequireFreshCameraFrames")) -Message "Recovery readiness probe should require fresh camera-frame logs."

$text = & (Join-Path $PSScriptRoot "recover_pixel_preview_session.ps1") `
    -AdbPath $fakeAdb `
    -OutputRoot $root `
    -DryRun

Assert-True -Condition (($text -join "`n").Contains("Pixel preview recovery dry run")) -Message "Text dry run should include a heading."
Assert-True -Condition (($text -join "`n").Contains("Command:")) -Message "Text dry run should list commands."

Write-Output "Pixel preview recovery self-test passed."
