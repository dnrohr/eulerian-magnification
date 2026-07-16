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

$root = Join-Path ([System.IO.Path]::GetTempPath()) "eulerian-install-helper-$([guid]::NewGuid().ToString('N'))"
New-Item -ItemType Directory -Path $root | Out-Null
$fakeAdb = Join-Path $root "adb.exe"
$fakeApk = Join-Path $root "app-debug.apk"
Set-Content -LiteralPath $fakeAdb -Value "fake adb" -Encoding utf8
Set-Content -LiteralPath $fakeApk -Value "fake apk" -Encoding utf8

$result = & (Join-Path $PSScriptRoot "install_debug_on_pixel.ps1") `
    -AdbPath $fakeAdb `
    -ApkPath $fakeApk `
    -DeviceSerial "PIXEL123" `
    -Build `
    -Launch `
    -DryRun `
    -Json | ConvertFrom-Json

Assert-Equal -Actual $result.dryRun -Expected $true -Message "Dry run flag mismatch."
Assert-Equal -Actual $result.adb -Expected $fakeAdb -Message "Dry run should preserve explicit adb path."
Assert-Equal -Actual $result.apkPath -Expected $fakeApk -Message "Dry run should preserve explicit APK path."
Assert-Equal -Actual $result.package -Expected "com.dnrohr.eulerianmagnification" -Message "Default package mismatch."
Assert-Equal -Actual $result.deviceSerial -Expected "PIXEL123" -Message "Dry run should preserve requested serial."
Assert-True -Condition ((".\gradlew.bat assembleDebug") -in @($result.commands)) -Message "Dry run should include build command when -Build is used."
Assert-True -Condition ((@($result.commands) -join "`n").Contains("install -r")) -Message "Dry run should include adb install command."
Assert-True -Condition ((@($result.commands) -join "`n").Contains("monkey -p com.dnrohr.eulerianmagnification 1")) -Message "Dry run should include launch command."

$text = & (Join-Path $PSScriptRoot "install_debug_on_pixel.ps1") `
    -AdbPath $fakeAdb `
    -ApkPath $fakeApk `
    -DryRun

Assert-True -Condition (($text -join "`n").Contains("Debug install dry run")) -Message "Text dry run should include a heading."
Assert-True -Condition (($text -join "`n").Contains("Command:")) -Message "Text dry run should list commands."

Write-Output "Debug install helper self-test passed."
