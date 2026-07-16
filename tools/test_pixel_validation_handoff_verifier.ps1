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

function Invoke-VerifierExitCode {
    param(
        [string]$ManifestPath,
        [string]$SourceRoot,
        [string]$AdbPath,
        [switch]$FailOnArtifactMismatch,
        [switch]$FailOnSourceMismatch,
        [switch]$FailOnDeviceUnavailable
    )

    $script = Join-Path $PSScriptRoot "verify_pixel_validation_handoff.ps1"
    $powerShellExe = (Get-Process -Id $PID).Path
    $arguments = @(
        "-NoProfile",
        "-File",
        $script,
        "-ManifestPath",
        $ManifestPath,
        "-SourceRoot",
        $SourceRoot,
        "-AdbPath",
        $AdbPath
    )
    if ($FailOnArtifactMismatch) {
        $arguments += "-FailOnArtifactMismatch"
    }
    if ($FailOnSourceMismatch) {
        $arguments += "-FailOnSourceMismatch"
    }
    if ($FailOnDeviceUnavailable) {
        $arguments += "-FailOnDeviceUnavailable"
    }

    & $powerShellExe @arguments *> $null
    return $LASTEXITCODE
}

function New-ArtifactRecord {
    param(
        [string]$Name,
        [string]$Path
    )

    return [ordered]@{
        name = $Name
        path = $Path
        sha256 = (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
    }
}

$root = Join-Path ([System.IO.Path]::GetTempPath()) "eulerian-handoff-verifier-$([guid]::NewGuid().ToString('N'))"
$repoRoot = Join-Path $root "repo"
$bundleRoot = Join-Path $root "bundle"
New-Item -ItemType Directory -Path $repoRoot -Force | Out-Null
New-Item -ItemType Directory -Path $bundleRoot -Force | Out-Null

try {
    Push-Location -LiteralPath $repoRoot
    try {
        & git init *> $null
        & git config user.email "test@example.invalid"
        & git config user.name "Eulerian Test"
        "source" | Set-Content -LiteralPath (Join-Path $repoRoot "source.txt") -Encoding utf8
        & git add source.txt
        & git commit -m "Verifier source fixture" *> $null
        $sourceCommit = (& git rev-parse HEAD).Trim()
        & git update-ref refs/remotes/origin/main $sourceCommit
    } finally {
        Pop-Location
    }

    $artifactA = Join-Path $bundleRoot "pixel_validation_runbook.txt"
    $artifactB = Join-Path $bundleRoot "pixel_validation_handoff.md"
    "runbook artifact" | Set-Content -LiteralPath $artifactA -Encoding utf8
    "handoff artifact" | Set-Content -LiteralPath $artifactB -Encoding utf8

    $fakeAdb = Join-Path $root "fake-adb.cmd"
    Set-Content -LiteralPath $fakeAdb -Encoding ascii -Value @(
        "@echo off",
        "echo List of devices attached",
        "echo PIXEL-VERIFY-TEST device product:pixel model:Pixel_8a"
    )
    $fakeNoDeviceAdb = Join-Path $root "fake-no-device-adb.cmd"
    Set-Content -LiteralPath $fakeNoDeviceAdb -Encoding ascii -Value @(
        "@echo off",
        "echo List of devices attached"
    )

    $manifestPath = Join-Path $bundleRoot "pixel_validation_handoff_manifest.json"
    $manifest = [ordered]@{
        deviceSerial = "PIXEL-VERIFY-TEST"
        source = [ordered]@{
            branch = "main"
            commit = $sourceCommit
            clean = $true
            commitReachableFromOriginMain = $true
        }
        artifacts = @(
            New-ArtifactRecord -Name "runbook" -Path $artifactA
            New-ArtifactRecord -Name "handoff" -Path $artifactB
        )
    }
    $manifest | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $manifestPath -Encoding utf8

    $result = & (Join-Path $PSScriptRoot "verify_pixel_validation_handoff.ps1") `
        -ManifestPath $manifestPath `
        -SourceRoot $repoRoot `
        -AdbPath $fakeAdb `
        -Json | ConvertFrom-Json

    Assert-Equal -Actual $result.artifactMismatchCount -Expected 0 -Message "Valid handoff should have no artifact mismatches."
    Assert-Equal -Actual $result.sourceMismatchCount -Expected 0 -Message "Valid handoff should have no source mismatches."
    Assert-Equal -Actual $result.expectedDeviceConnected -Expected $true -Message "Valid handoff should find the expected device serial."
    Assert-Equal -Actual @($result.artifactChecks).Count -Expected 2 -Message "Verifier should report every manifest artifact."
    Assert-Equal -Actual @($result.source.checks).Count -Expected 3 -Message "Verifier should report source checks."
    Assert-True -Condition (($result.artifactChecks | Where-Object { $_.name -eq "runbook" }).passed -eq $true) -Message "Runbook artifact should pass hash verification."

    $validExitCode = Invoke-VerifierExitCode -ManifestPath $manifestPath -SourceRoot $repoRoot -AdbPath $fakeAdb -FailOnArtifactMismatch -FailOnSourceMismatch -FailOnDeviceUnavailable
    Assert-Equal -Actual $validExitCode -Expected 0 -Message "Verifier gates should pass for a valid handoff."

    "edited after manifest" | Set-Content -LiteralPath $artifactA -Encoding utf8
    $artifactMismatch = & (Join-Path $PSScriptRoot "verify_pixel_validation_handoff.ps1") `
        -ManifestPath $manifestPath `
        -SourceRoot $repoRoot `
        -AdbPath $fakeAdb `
        -Json | ConvertFrom-Json
    Assert-Equal -Actual $artifactMismatch.artifactMismatchCount -Expected 1 -Message "Edited artifact should produce one mismatch."
    $artifactMismatchExitCode = Invoke-VerifierExitCode -ManifestPath $manifestPath -SourceRoot $repoRoot -AdbPath $fakeAdb -FailOnArtifactMismatch
    Assert-Equal -Actual $artifactMismatchExitCode -Expected 31 -Message "Artifact mismatch gate should exit 31."

    "runbook artifact" | Set-Content -LiteralPath $artifactA -Encoding utf8
    Push-Location -LiteralPath $repoRoot
    try {
        "dirty" | Set-Content -LiteralPath (Join-Path $repoRoot "dirty.txt") -Encoding utf8
    } finally {
        Pop-Location
    }
    $sourceMismatch = & (Join-Path $PSScriptRoot "verify_pixel_validation_handoff.ps1") `
        -ManifestPath $manifestPath `
        -SourceRoot $repoRoot `
        -AdbPath $fakeAdb `
        -Json | ConvertFrom-Json
    Assert-True -Condition ($sourceMismatch.sourceMismatchCount -gt 0) -Message "Dirty source should produce a source mismatch."
    $sourceMismatchExitCode = Invoke-VerifierExitCode -ManifestPath $manifestPath -SourceRoot $repoRoot -AdbPath $fakeAdb -FailOnSourceMismatch
    Assert-Equal -Actual $sourceMismatchExitCode -Expected 32 -Message "Source mismatch gate should exit 32."
    Remove-Item -LiteralPath (Join-Path $repoRoot "dirty.txt") -Force

    $deviceUnavailableExitCode = Invoke-VerifierExitCode -ManifestPath $manifestPath -SourceRoot $repoRoot -AdbPath $fakeNoDeviceAdb -FailOnDeviceUnavailable
    Assert-Equal -Actual $deviceUnavailableExitCode -Expected 33 -Message "Device availability gate should exit 33."

    $text = & (Join-Path $PSScriptRoot "verify_pixel_validation_handoff.ps1") `
        -ManifestPath $manifestPath `
        -SourceRoot $repoRoot `
        -AdbPath $fakeAdb
    Assert-True -Condition (($text -join "`n").Contains("Pixel validation handoff verification")) -Message "Text output should include a heading."
    Assert-True -Condition (($text -join "`n").Contains("Artifact mismatches: 0")) -Message "Text output should include artifact mismatch count."
    Assert-True -Condition (($text -join "`n").Contains("Expected device connected: True")) -Message "Text output should include device availability."
} finally {
    if (Test-Path -LiteralPath $root) {
        Remove-Item -LiteralPath $root -Recurse -Force
    }
}

Write-Output "Pixel validation handoff verifier self-test passed."
