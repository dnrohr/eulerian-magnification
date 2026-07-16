param(
    [string]$ManifestPath = "sample-videos\exports\live-validation\pixel_validation_handoff_manifest.json",
    [string]$SourceRoot = "",
    [string]$AdbPath = "",
    [switch]$FailOnArtifactMismatch,
    [switch]$FailOnSourceMismatch,
    [switch]$FailOnDeviceUnavailable,
    [switch]$Json
)

$ErrorActionPreference = "Stop"

function Find-Adb {
    param([string]$ExplicitPath)

    if (-not [string]::IsNullOrWhiteSpace($ExplicitPath)) {
        if (Test-Path -LiteralPath $ExplicitPath) {
            return (Resolve-Path -LiteralPath $ExplicitPath).Path
        }
        throw "Requested adb path does not exist: $ExplicitPath"
    }

    $sdkAdb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
    if (Test-Path -LiteralPath $sdkAdb) {
        return $sdkAdb
    }

    $pathAdb = Get-Command adb -ErrorAction SilentlyContinue
    if ($pathAdb) {
        return $pathAdb.Source
    }

    return $null
}

function Invoke-GitValue {
    param(
        [string]$Root,
        [string[]]$Arguments
    )

    try {
        $output = & git -C $Root @Arguments 2>$null
        if ($LASTEXITCODE -ne 0) {
            return $null
        }
        return ($output -join "`n").Trim()
    } catch {
        return $null
    }
}

function Get-DeviceAvailability {
    param(
        [string]$ExpectedSerial,
        [string]$ExplicitAdbPath
    )

    $adb = Find-Adb -ExplicitPath $ExplicitAdbPath
    if ([string]::IsNullOrWhiteSpace($adb)) {
        return [pscustomobject]@{
            adbPath = $null
            checked = $false
            expectedSerial = $ExpectedSerial
            connected = $false
            connectedSerials = @()
            note = "adb.exe was not found; device availability was not checked."
        }
    }

    try {
        $output = & $adb devices -l 2>&1
        $exitCode = $LASTEXITCODE
    } catch {
        return [pscustomobject]@{
            adbPath = $adb
            checked = $false
            expectedSerial = $ExpectedSerial
            connected = $false
            connectedSerials = @()
            note = "adb devices failed: $($_.Exception.Message)"
        }
    }

    if ($exitCode -ne 0) {
        return [pscustomobject]@{
            adbPath = $adb
            checked = $false
            expectedSerial = $ExpectedSerial
            connected = $false
            connectedSerials = @()
            note = "adb devices failed with exit code ${exitCode}: $($output -join ' ')"
        }
    }

    $connectedSerials = @()
    foreach ($line in @($output)) {
        if ($line -match '^(\S+)\s+device\b') {
            $connectedSerials += $Matches[1]
        }
    }

    $connected = -not [string]::IsNullOrWhiteSpace($ExpectedSerial) -and $ExpectedSerial -in $connectedSerials
    $note = if ($connected) {
        "Expected Pixel serial is connected."
    } elseif ($connectedSerials.Count -gt 0) {
        "ADB is available, but the expected Pixel serial is not connected."
    } else {
        "ADB is available, but no connected devices were reported."
    }

    return [pscustomobject]@{
        adbPath = $adb
        checked = $true
        expectedSerial = $ExpectedSerial
        connected = $connected
        connectedSerials = $connectedSerials
        note = $note
    }
}

function New-Check {
    param(
        [string]$Name,
        [bool]$Passed,
        [string]$Message,
        $Details = $null
    )

    return [pscustomobject]@{
        name = $Name
        passed = $Passed
        message = $Message
        details = $Details
    }
}

$resolvedManifest = Resolve-Path -LiteralPath $ManifestPath -ErrorAction SilentlyContinue
if (-not $resolvedManifest) {
    throw "Handoff manifest not found: $ManifestPath"
}

$manifest = Get-Content -LiteralPath $resolvedManifest.Path -Raw | ConvertFrom-Json
$manifestDirectory = Split-Path -Parent $resolvedManifest.Path
if ([string]::IsNullOrWhiteSpace($SourceRoot)) {
    $SourceRoot = Split-Path -Parent $PSScriptRoot
}

$artifactChecks = @()
foreach ($artifact in @($manifest.artifacts)) {
    $path = [string]$artifact.path
    if (-not [System.IO.Path]::IsPathRooted($path)) {
        $path = Join-Path $manifestDirectory $path
    }
    if (-not (Test-Path -LiteralPath $path)) {
        $artifactChecks += New-Check -Name $artifact.name -Passed $false -Message "artifact missing" -Details ([pscustomobject]@{ path = $path; expectedSha256 = $artifact.sha256; actualSha256 = $null })
        continue
    }
    $actualHash = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
    $expectedHash = ([string]$artifact.sha256).ToLowerInvariant()
    $artifactChecks += New-Check -Name $artifact.name -Passed ($actualHash -eq $expectedHash) -Message $(if ($actualHash -eq $expectedHash) { "hash ok" } else { "hash mismatch" }) -Details ([pscustomobject]@{ path = $path; expectedSha256 = $expectedHash; actualSha256 = $actualHash })
}

$currentBranch = Invoke-GitValue -Root $SourceRoot -Arguments @("rev-parse", "--abbrev-ref", "HEAD")
$currentCommit = Invoke-GitValue -Root $SourceRoot -Arguments @("rev-parse", "HEAD")
$currentStatus = Invoke-GitValue -Root $SourceRoot -Arguments @("status", "--porcelain")
$currentClean = [string]::IsNullOrWhiteSpace($currentStatus)
$commitReachable = $false
if (-not [string]::IsNullOrWhiteSpace($currentCommit)) {
    try {
        & git -C $SourceRoot merge-base --is-ancestor $currentCommit origin/main *> $null
        $commitReachable = ($LASTEXITCODE -eq 0)
    } catch {
        $commitReachable = $false
    }
}

$sourceChecks = @(
    New-Check -Name "commitMatchesManifest" -Passed ($currentCommit -eq $manifest.source.commit) -Message $(if ($currentCommit -eq $manifest.source.commit) { "source commit matches manifest" } else { "source commit does not match manifest" }) -Details ([pscustomobject]@{ expected = $manifest.source.commit; actual = $currentCommit })
    New-Check -Name "sourceClean" -Passed $currentClean -Message $(if ($currentClean) { "source tree is clean" } else { "source tree has uncommitted changes" }) -Details ([pscustomobject]@{ expected = $true; actual = $currentClean })
    New-Check -Name "commitReachableFromOriginMain" -Passed $commitReachable -Message $(if ($commitReachable) { "current commit is reachable from origin/main" } else { "current commit is not reachable from origin/main" }) -Details ([pscustomobject]@{ expected = $true; actual = $commitReachable })
)

$deviceAvailability = Get-DeviceAvailability -ExpectedSerial $manifest.deviceSerial -ExplicitAdbPath $AdbPath
$artifactMismatchCount = @($artifactChecks | Where-Object { -not $_.passed }).Count
$sourceMismatchCount = @($sourceChecks | Where-Object { -not $_.passed }).Count

$result = [pscustomobject]@{
    manifestPath = $resolvedManifest.Path
    sourceRoot = $SourceRoot
    deviceSerial = $manifest.deviceSerial
    artifactMismatchCount = $artifactMismatchCount
    sourceMismatchCount = $sourceMismatchCount
    expectedDeviceConnected = $deviceAvailability.connected
    artifactChecks = $artifactChecks
    source = [pscustomobject]@{
        manifestBranch = $manifest.source.branch
        manifestCommit = $manifest.source.commit
        currentBranch = $currentBranch
        currentCommit = $currentCommit
        currentClean = $currentClean
        currentCommitReachableFromOriginMain = $commitReachable
        checks = $sourceChecks
    }
    deviceAvailability = $deviceAvailability
}

if ($Json) {
    $result | ConvertTo-Json -Depth 6
} else {
    Write-Output "Pixel validation handoff verification"
    Write-Output "Manifest: $($result.manifestPath)"
    Write-Output "Artifact mismatches: $($result.artifactMismatchCount)"
    Write-Output "Source mismatches: $($result.sourceMismatchCount)"
    Write-Output "Expected device connected: $($result.expectedDeviceConnected)"
    foreach ($check in @($artifactChecks)) {
        $mark = if ($check.passed) { "[x]" } else { "[ ]" }
        Write-Output "$mark artifact $($check.name): $($check.message)"
    }
    foreach ($check in @($sourceChecks)) {
        $mark = if ($check.passed) { "[x]" } else { "[ ]" }
        Write-Output "$mark source $($check.name): $($check.message)"
    }
    Write-Output "Device: $($deviceAvailability.note)"
}

if ($FailOnArtifactMismatch -and $artifactMismatchCount -gt 0) {
    exit 31
}
if ($FailOnSourceMismatch -and $sourceMismatchCount -gt 0) {
    exit 32
}
if ($FailOnDeviceUnavailable -and -not $deviceAvailability.connected) {
    exit 33
}

exit 0
