param(
    [string]$ManifestPath = "sample-videos\exports\live-validation\pixel_validation_handoff_manifest.json",
    [string]$SourceRoot = "",
    [string]$AdbPath = "",
    [switch]$FailOnArtifactMismatch,
    [switch]$FailOnSourceMismatch,
    [switch]$FailOnDeviceUnavailable,
    [switch]$FailOnHandoffConsistencyMismatch,
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

function Resolve-ManifestArtifactPath {
    param(
        $Manifest,
        [string]$ManifestDirectory,
        [string]$SourceRoot,
        [string]$Name
    )

    $artifact = @($Manifest.artifacts | Where-Object { $_.name -eq $Name } | Select-Object -First 1)
    if (@($artifact).Count -eq 0) {
        return $null
    }

    $path = [string]$artifact[0].path
    if ([string]::IsNullOrWhiteSpace($path)) {
        return $null
    }
    if ([System.IO.Path]::IsPathRooted($path)) {
        return $path
    }

    $manifestRelativePath = Join-Path $ManifestDirectory $path
    if (Test-Path -LiteralPath $manifestRelativePath) {
        return $manifestRelativePath
    }

    if (-not [string]::IsNullOrWhiteSpace($SourceRoot)) {
        $sourceRelativePath = Join-Path $SourceRoot $path
        if (Test-Path -LiteralPath $sourceRelativePath) {
            return $sourceRelativePath
        }
    }

    return $manifestRelativePath
}

function Get-ArtifactText {
    param(
        $Manifest,
        [string]$ManifestDirectory,
        [string]$Name
    )

    $path = Resolve-ManifestArtifactPath -Manifest $Manifest -ManifestDirectory $ManifestDirectory -SourceRoot $SourceRoot -Name $Name
    if ([string]::IsNullOrWhiteSpace($path) -or -not (Test-Path -LiteralPath $path)) {
        return ""
    }
    return Get-Content -LiteralPath $path -Raw
}

function Get-UncommentedRoiFinalTemplateLines {
    param([string]$Text)

    if ([string]::IsNullOrWhiteSpace($Text)) {
        return @()
    }

    $lines = $Text -split "`r?`n"
    $uncommented = @()
    for ($index = 0; $index -lt $lines.Count; $index++) {
        $line = $lines[$index]
        $hasFinalPlaceholder = $line.Contains("<visible-target-bounds-in-screenshot-space>") -or
            $line.Contains("<visible-face-or-skin-target-bounds-in-screenshot-space>")
        if (-not $hasFinalPlaceholder) {
            continue
        }

        if ($line.TrimStart().StartsWith("# TEMPLATE ONLY:")) {
            continue
        }

        $uncommented += [pscustomobject]@{
            lineNumber = $index + 1
            line = $line
        }
    }

    return $uncommented
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
$SourceRoot = (Resolve-Path -LiteralPath $SourceRoot).Path

$artifactChecks = @()
foreach ($artifact in @($manifest.artifacts)) {
    $path = [string]$artifact.path
    if (-not [System.IO.Path]::IsPathRooted($path)) {
        $manifestRelativePath = Join-Path $manifestDirectory $path
        $sourceRelativePath = Join-Path $SourceRoot $path
        $path = if (Test-Path -LiteralPath $manifestRelativePath) {
            $manifestRelativePath
        } elseif (Test-Path -LiteralPath $sourceRelativePath) {
            $sourceRelativePath
        } else {
            $manifestRelativePath
        }
    }
    if (-not (Test-Path -LiteralPath $path)) {
        $artifactChecks += New-Check -Name $artifact.name -Passed $false -Message "artifact missing" -Details ([pscustomobject]@{ path = $path; expectedSha256 = $artifact.sha256; actualSha256 = $null })
        continue
    }
    $actualHash = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
    $expectedHash = ([string]$artifact.sha256).ToLowerInvariant()
    $artifactChecks += New-Check -Name $artifact.name -Passed ($actualHash -eq $expectedHash) -Message $(if ($actualHash -eq $expectedHash) { "hash ok" } else { "hash mismatch" }) -Details ([pscustomobject]@{ path = $path; expectedSha256 = $expectedHash; actualSha256 = $actualHash })
}

$commandText = Get-ArtifactText -Manifest $manifest -ManifestDirectory $manifestDirectory -Name "commands"
$runbookText = Get-ArtifactText -Manifest $manifest -ManifestDirectory $manifestDirectory -Name "runbook"
$handoffText = Get-ArtifactText -Manifest $manifest -ManifestDirectory $manifestDirectory -Name "handoff"
$roiHelperText = @($manifest.roiFinalHelperCommands) -join "`n"
$manualRoiPlaceholderPresent = $commandText.Contains("<visible-target-bounds-in-screenshot-space>")
$autoRoiPlaceholderPresent = $commandText.Contains("<visible-face-or-skin-target-bounds-in-screenshot-space>")
$helperTextPresent = $runbookText.Contains("prepare_roi_final_capture_command.ps1") -and $handoffText.Contains("prepare_roi_final_capture_command.ps1")
$runbookUncommentedRoiFinalTemplateLines = @(Get-UncommentedRoiFinalTemplateLines -Text $runbookText)
$handoffUncommentedRoiFinalTemplateLines = @(Get-UncommentedRoiFinalTemplateLines -Text $handoffText)
$allowOperatorCommands = [bool]$manifest.allowOperatorCommands
$allowFinalCommands = [bool]$manifest.allowFinalCommands
$commandLines = @($commandText -split "`r?`n" | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
$captureCommandLines = @($commandLines | Where-Object { $_.Contains("capture_live_validation_evidence.ps1") })
$guardedCommandLines = @($captureCommandLines | Where-Object { $_.TrimStart().StartsWith("# OPERATOR REQUIRED") })
$unguardedCommandLines = @($captureCommandLines | Where-Object { -not $_.TrimStart().StartsWith("#") })
$finalCommandLines = @($captureCommandLines | Where-Object { $_.Contains("-RequireFinalVisualEvidence") })
$guardedFinalCommandLines = @($finalCommandLines | Where-Object { $_.TrimStart().StartsWith("# OPERATOR REQUIRED") })
$unguardedFinalCommandLines = @($finalCommandLines | Where-Object { -not $_.TrimStart().StartsWith("#") })
$operatorCommandGuardPassed = if ($allowOperatorCommands) {
    @($guardedCommandLines).Count -eq 0
} else {
    @($captureCommandLines).Count -eq 0 -or @($unguardedCommandLines).Count -eq 0
}
$finalCommandGuardPassed = if ($allowFinalCommands) {
    $allowOperatorCommands -and @($guardedFinalCommandLines).Count -eq 0
} else {
    @($finalCommandLines).Count -eq 0 -or @($unguardedFinalCommandLines).Count -eq 0
}
$guardedLabelPresent = $runbookText.Contains("Guarded Commands") -and $handoffText.Contains("Guarded Commands")
$runnableLabelPresent = $runbookText.Contains("Runnable Commands") -and $handoffText.Contains("Runnable Commands")
$sessionReadinessCommand = if ($manifest.PSObject.Properties.Name -contains "sessionReadiness" -and
    $manifest.sessionReadiness -and
    $manifest.sessionReadiness.PSObject.Properties.Name -contains "command") {
    [string]$manifest.sessionReadiness.command
} else {
    ""
}
$sessionReadinessOutputPath = if ($manifest.PSObject.Properties.Name -contains "sessionReadiness" -and
    $manifest.sessionReadiness -and
    $manifest.sessionReadiness.PSObject.Properties.Name -contains "outputPath") {
    [string]$manifest.sessionReadiness.outputPath
} else {
    ""
}
$sessionReadinessExpected = -not [string]::IsNullOrWhiteSpace($sessionReadinessCommand)
$sessionReadinessRunbookPresent = (-not $sessionReadinessExpected) -or (
    $runbookText.Contains($sessionReadinessCommand) -and
    $runbookText.Contains("Snapshot session readiness")
)
$sessionReadinessHandoffPresent = (-not $sessionReadinessExpected) -or (
    $handoffText.Contains($sessionReadinessCommand) -and
    $handoffText.Contains("Session readiness command")
)
$sessionReadinessOutputPresent = (-not $sessionReadinessExpected) -or
    [string]::IsNullOrWhiteSpace($sessionReadinessOutputPath) -or
    ($runbookText.Contains($sessionReadinessOutputPath) -and $handoffText.Contains($sessionReadinessOutputPath))
$handoffConsistencyChecks = @(
    New-Check -Name "manualRoiFinalHelper" -Passed (-not $manualRoiPlaceholderPresent -or ($roiHelperText.Contains("-Slot manualRoi") -and $helperTextPresent)) -Message $(if (-not $manualRoiPlaceholderPresent) { "manual ROI final command has no placeholder" } elseif ($roiHelperText.Contains("-Slot manualRoi") -and $helperTextPresent) { "manual ROI placeholder is paired with final-command helper guidance" } else { "manual ROI placeholder is missing final-command helper guidance" }) -Details ([pscustomobject]@{ placeholderPresent = $manualRoiPlaceholderPresent; helperCommandPresent = $roiHelperText.Contains("-Slot manualRoi"); helperTextPresent = $helperTextPresent })
    New-Check -Name "autoRoiFinalHelper" -Passed (-not $autoRoiPlaceholderPresent -or ($roiHelperText.Contains("-Slot autoRoi") -and $helperTextPresent)) -Message $(if (-not $autoRoiPlaceholderPresent) { "automatic ROI final command has no placeholder" } elseif ($roiHelperText.Contains("-Slot autoRoi") -and $helperTextPresent) { "automatic ROI placeholder is paired with final-command helper guidance" } else { "automatic ROI placeholder is missing final-command helper guidance" }) -Details ([pscustomobject]@{ placeholderPresent = $autoRoiPlaceholderPresent; helperCommandPresent = $roiHelperText.Contains("-Slot autoRoi"); helperTextPresent = $helperTextPresent })
    New-Check -Name "runbookRoiFinalTemplatesReferenceOnly" -Passed (@($runbookUncommentedRoiFinalTemplateLines).Count -eq 0) -Message $(if (@($runbookUncommentedRoiFinalTemplateLines).Count -eq 0) { "runbook ROI final templates are reference-only" } else { "runbook has pasteable ROI final placeholder commands" }) -Details ([pscustomobject]@{ uncommentedLineCount = @($runbookUncommentedRoiFinalTemplateLines).Count; uncommentedLines = $runbookUncommentedRoiFinalTemplateLines })
    New-Check -Name "handoffRoiFinalTemplatesReferenceOnly" -Passed (@($handoffUncommentedRoiFinalTemplateLines).Count -eq 0) -Message $(if (@($handoffUncommentedRoiFinalTemplateLines).Count -eq 0) { "Markdown handoff ROI final templates are reference-only" } else { "Markdown handoff has pasteable ROI final placeholder commands" }) -Details ([pscustomobject]@{ uncommentedLineCount = @($handoffUncommentedRoiFinalTemplateLines).Count; uncommentedLines = $handoffUncommentedRoiFinalTemplateLines })
    New-Check -Name "operatorCommandGuardMatchesManifest" -Passed $operatorCommandGuardPassed -Message $(if ($operatorCommandGuardPassed) { "operator command guard matches manifest" } else { "operator command guard does not match manifest" }) -Details ([pscustomobject]@{ allowOperatorCommands = $allowOperatorCommands; captureCommandCount = @($captureCommandLines).Count; guardedCommandCount = @($guardedCommandLines).Count; unguardedCommandCount = @($unguardedCommandLines).Count })
    New-Check -Name "finalCommandGuardMatchesManifest" -Passed $finalCommandGuardPassed -Message $(if ($finalCommandGuardPassed) { "final command guard matches manifest" } else { "final command guard does not match manifest" }) -Details ([pscustomobject]@{ allowOperatorCommands = $allowOperatorCommands; allowFinalCommands = $allowFinalCommands; finalCommandCount = @($finalCommandLines).Count; guardedFinalCommandCount = @($guardedFinalCommandLines).Count; unguardedFinalCommandCount = @($unguardedFinalCommandLines).Count })
    New-Check -Name "commandSectionLabelMatchesManifest" -Passed $(if ($allowOperatorCommands) { $runnableLabelPresent } else { $guardedLabelPresent }) -Message $(if ($allowOperatorCommands) { if ($runnableLabelPresent) { "runbook and handoff label commands runnable" } else { "runbook or handoff missing runnable command label" } } else { if ($guardedLabelPresent) { "runbook and handoff label commands guarded" } else { "runbook or handoff missing guarded command label" } }) -Details ([pscustomobject]@{ allowOperatorCommands = $allowOperatorCommands; guardedLabelPresent = $guardedLabelPresent; runnableLabelPresent = $runnableLabelPresent })
    New-Check -Name "sessionReadinessCommandMatchesManifest" -Passed ($sessionReadinessRunbookPresent -and $sessionReadinessHandoffPresent -and $sessionReadinessOutputPresent) -Message $(if (-not $sessionReadinessExpected) { "session readiness command is not recorded in manifest" } elseif ($sessionReadinessRunbookPresent -and $sessionReadinessHandoffPresent -and $sessionReadinessOutputPresent) { "session readiness command matches manifest" } else { "session readiness command does not match manifest" }) -Details ([pscustomobject]@{ expected = $sessionReadinessExpected; command = $sessionReadinessCommand; outputPath = $sessionReadinessOutputPath; runbookPresent = $sessionReadinessRunbookPresent; handoffPresent = $sessionReadinessHandoffPresent; outputPathPresent = $sessionReadinessOutputPresent })
)

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
$handoffConsistencyMismatchCount = @($handoffConsistencyChecks | Where-Object { -not $_.passed }).Count

$result = [pscustomobject]@{
    manifestPath = $resolvedManifest.Path
    sourceRoot = $SourceRoot
    deviceSerial = $manifest.deviceSerial
    artifactMismatchCount = $artifactMismatchCount
    sourceMismatchCount = $sourceMismatchCount
    handoffConsistencyMismatchCount = $handoffConsistencyMismatchCount
    expectedDeviceConnected = $deviceAvailability.connected
    artifactChecks = $artifactChecks
    handoffConsistencyChecks = $handoffConsistencyChecks
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
    Write-Output "Handoff consistency mismatches: $($result.handoffConsistencyMismatchCount)"
    Write-Output "Expected device connected: $($result.expectedDeviceConnected)"
    foreach ($check in @($artifactChecks)) {
        $mark = if ($check.passed) { "[x]" } else { "[ ]" }
        Write-Output "$mark artifact $($check.name): $($check.message)"
    }
    foreach ($check in @($sourceChecks)) {
        $mark = if ($check.passed) { "[x]" } else { "[ ]" }
        Write-Output "$mark source $($check.name): $($check.message)"
    }
    foreach ($check in @($handoffConsistencyChecks)) {
        $mark = if ($check.passed) { "[x]" } else { "[ ]" }
        Write-Output "$mark handoff $($check.name): $($check.message)"
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
if ($FailOnHandoffConsistencyMismatch -and $handoffConsistencyMismatchCount -gt 0) {
    exit 34
}

exit 0
