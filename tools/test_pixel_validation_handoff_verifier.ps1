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
        [switch]$FailOnDeviceUnavailable,
        [switch]$FailOnHandoffConsistencyMismatch
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
    if ($FailOnHandoffConsistencyMismatch) {
        $arguments += "-FailOnHandoffConsistencyMismatch"
    }

    & $powerShellExe @arguments *> $null
    return $LASTEXITCODE
}

function New-ArtifactRecord {
    param(
        [string]$Name,
        [string]$Path,
        [string]$ManifestPath = ""
    )

    return [ordered]@{
        name = $Name
        path = $(if ([string]::IsNullOrWhiteSpace($ManifestPath)) { $Path } else { $ManifestPath })
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
    $commandsBaseArtifact = Join-Path $bundleRoot "pixel_validation_commands_base.txt"
    "# 3. Guarded Commands: requested validation evidence." | Set-Content -LiteralPath $artifactA -Encoding utf8
    "## Guarded Commands" | Set-Content -LiteralPath $artifactB -Encoding utf8
    "# OPERATOR REQUIRED: .\tools\capture_live_validation_evidence.ps1 -Label ""live-linear-pulse-final"" -RequireFinalVisualEvidence" | Set-Content -LiteralPath $commandsBaseArtifact -Encoding utf8
    $sessionReadinessCommand = '.\tools\export_pixel_session_readiness.ps1 -DeviceSerial "PIXEL-VERIFY-TEST" -OutputPath "pixel_session_readiness_preflight.json" -FailOnNotReady'
    @(
        "# 2. Snapshot session readiness before watched capture.",
        $sessionReadinessCommand,
        "# 3. Guarded Commands: requested validation evidence."
    ) | Set-Content -LiteralPath $artifactA -Encoding utf8
    @(
        "## Thermal Preflight",
        ('- Session readiness command: `{0}`' -f $sessionReadinessCommand),
        $sessionReadinessCommand,
        "## Guarded Commands"
    ) | Set-Content -LiteralPath $artifactB -Encoding utf8

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
            New-ArtifactRecord -Name "commands" -Path $commandsBaseArtifact
            New-ArtifactRecord -Name "runbook" -Path $artifactA
            New-ArtifactRecord -Name "handoff" -Path $artifactB
        )
        allowOperatorCommands = $false
        allowFinalCommands = $false
        sessionReadiness = [ordered]@{
            command = $sessionReadinessCommand
            outputPath = "pixel_session_readiness_preflight.json"
        }
    }
    $manifest | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $manifestPath -Encoding utf8

    $result = & (Join-Path $PSScriptRoot "verify_pixel_validation_handoff.ps1") `
        -ManifestPath $manifestPath `
        -SourceRoot $repoRoot `
        -AdbPath $fakeAdb `
        -Json | ConvertFrom-Json

    Assert-Equal -Actual $result.artifactMismatchCount -Expected 0 -Message "Valid handoff should have no artifact mismatches."
    Assert-Equal -Actual $result.sourceMismatchCount -Expected 0 -Message "Valid handoff should have no source mismatches."
    Assert-Equal -Actual $result.handoffConsistencyMismatchCount -Expected 0 -Message "Valid handoff should have no consistency mismatches."
    Assert-Equal -Actual $result.expectedDeviceConnected -Expected $true -Message "Valid handoff should find the expected device serial."
    Assert-Equal -Actual @($result.artifactChecks).Count -Expected 3 -Message "Verifier should report every manifest artifact."
    Assert-Equal -Actual @($result.source.checks).Count -Expected 3 -Message "Verifier should report source checks."
    Assert-Equal -Actual @($result.handoffConsistencyChecks).Count -Expected 8 -Message "Verifier should report ROI helper, command guard, section label, and session readiness consistency checks."
    Assert-True -Condition (($result.artifactChecks | Where-Object { $_.name -eq "runbook" }).passed -eq $true) -Message "Runbook artifact should pass hash verification."

    $validExitCode = Invoke-VerifierExitCode -ManifestPath $manifestPath -SourceRoot $repoRoot -AdbPath $fakeAdb -FailOnArtifactMismatch -FailOnSourceMismatch -FailOnDeviceUnavailable -FailOnHandoffConsistencyMismatch
    Assert-Equal -Actual $validExitCode -Expected 0 -Message "Verifier gates should pass for a valid handoff."

    $sourceRelativeBundleRoot = Join-Path $repoRoot "sample-videos\exports\live-validation"
    New-Item -ItemType Directory -Path $sourceRelativeBundleRoot -Force | Out-Null
    $sourceRelativeCommands = Join-Path $sourceRelativeBundleRoot "pixel_validation_commands.txt"
    $sourceRelativeRunbook = Join-Path $sourceRelativeBundleRoot "pixel_validation_runbook.txt"
    $sourceRelativeHandoff = Join-Path $sourceRelativeBundleRoot "pixel_validation_handoff.md"
    "# OPERATOR REQUIRED: .\tools\capture_live_validation_evidence.ps1 -Label ""live-linear-pulse-final"" -RequireFinalVisualEvidence" | Set-Content -LiteralPath $sourceRelativeCommands -Encoding utf8
    @(
        "# 2. Snapshot session readiness before watched capture.",
        $sessionReadinessCommand,
        "# 3. Guarded Commands: requested validation evidence."
    ) | Set-Content -LiteralPath $sourceRelativeRunbook -Encoding utf8
    @(
        ('- Session readiness command: `{0}`' -f $sessionReadinessCommand),
        $sessionReadinessCommand,
        "## Guarded Commands"
    ) | Set-Content -LiteralPath $sourceRelativeHandoff -Encoding utf8
    $sourceRelativeManifestPath = Join-Path $sourceRelativeBundleRoot "pixel_validation_handoff_manifest.json"
    $sourceRelativeManifest = [ordered]@{
        deviceSerial = "PIXEL-VERIFY-TEST"
        source = [ordered]@{
            branch = "main"
            commit = $sourceCommit
            clean = $false
            commitReachableFromOriginMain = $true
        }
        artifacts = @(
            New-ArtifactRecord -Name "commands" -Path $sourceRelativeCommands -ManifestPath "sample-videos\exports\live-validation\pixel_validation_commands.txt"
            New-ArtifactRecord -Name "runbook" -Path $sourceRelativeRunbook -ManifestPath "sample-videos\exports\live-validation\pixel_validation_runbook.txt"
            New-ArtifactRecord -Name "handoff" -Path $sourceRelativeHandoff -ManifestPath "sample-videos\exports\live-validation\pixel_validation_handoff.md"
        )
        allowOperatorCommands = $false
        allowFinalCommands = $false
        sessionReadiness = [ordered]@{
            command = $sessionReadinessCommand
            outputPath = "pixel_session_readiness_preflight.json"
        }
    }
    $sourceRelativeManifest | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $sourceRelativeManifestPath -Encoding utf8
    $sourceRelativeResult = & (Join-Path $PSScriptRoot "verify_pixel_validation_handoff.ps1") `
        -ManifestPath $sourceRelativeManifestPath `
        -SourceRoot $repoRoot `
        -AdbPath $fakeAdb `
        -Json | ConvertFrom-Json
    Assert-Equal -Actual $sourceRelativeResult.artifactMismatchCount -Expected 0 -Message "Verifier should resolve generated source-root-relative artifact paths."
    Assert-Equal -Actual $sourceRelativeResult.handoffConsistencyMismatchCount -Expected 0 -Message "Source-root-relative guarded handoff should pass consistency checks."

    "edited after manifest" | Set-Content -LiteralPath $artifactA -Encoding utf8
    $artifactMismatch = & (Join-Path $PSScriptRoot "verify_pixel_validation_handoff.ps1") `
        -ManifestPath $manifestPath `
        -SourceRoot $repoRoot `
        -AdbPath $fakeAdb `
        -Json | ConvertFrom-Json
    Assert-Equal -Actual $artifactMismatch.artifactMismatchCount -Expected 1 -Message "Edited artifact should produce one mismatch."
    $artifactMismatchExitCode = Invoke-VerifierExitCode -ManifestPath $manifestPath -SourceRoot $repoRoot -AdbPath $fakeAdb -FailOnArtifactMismatch
    Assert-Equal -Actual $artifactMismatchExitCode -Expected 31 -Message "Artifact mismatch gate should exit 31."

    @(
        "# 2. Snapshot session readiness before watched capture.",
        $sessionReadinessCommand,
        "# 3. Guarded Commands: requested validation evidence."
    ) | Set-Content -LiteralPath $artifactA -Encoding utf8
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

    $unguardedCommandsArtifact = Join-Path $bundleRoot "unguarded_commands.txt"
    '.\tools\capture_live_validation_evidence.ps1 -Label "live-linear-pulse-final" -RequireFinalVisualEvidence' | Set-Content -LiteralPath $unguardedCommandsArtifact -Encoding utf8
    $unguardedManifestPath = Join-Path $bundleRoot "unguarded_commands_manifest.json"
    $unguardedManifest = [ordered]@{
        deviceSerial = "PIXEL-VERIFY-TEST"
        source = [ordered]@{
            branch = "main"
            commit = $sourceCommit
            clean = $true
            commitReachableFromOriginMain = $true
        }
        artifacts = @(
            New-ArtifactRecord -Name "commands" -Path $unguardedCommandsArtifact
            New-ArtifactRecord -Name "runbook" -Path $artifactA
            New-ArtifactRecord -Name "handoff" -Path $artifactB
        )
        allowOperatorCommands = $false
        allowFinalCommands = $false
    }
    $unguardedManifest | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $unguardedManifestPath -Encoding utf8
    $unguardedResult = & (Join-Path $PSScriptRoot "verify_pixel_validation_handoff.ps1") `
        -ManifestPath $unguardedManifestPath `
        -SourceRoot $repoRoot `
        -AdbPath $fakeAdb `
        -Json | ConvertFrom-Json
    Assert-Equal -Actual $unguardedResult.handoffConsistencyMismatchCount -Expected 2 -Message "Unguarded capture commands should fail operator and final guard checks when manifest disallows them."
    Assert-True -Condition (($unguardedResult.handoffConsistencyChecks | Where-Object { $_.name -eq "operatorCommandGuardMatchesManifest" }).passed -eq $false) -Message "Operator guard check should fail for unguarded commands."
    Assert-True -Condition (($unguardedResult.handoffConsistencyChecks | Where-Object { $_.name -eq "finalCommandGuardMatchesManifest" }).passed -eq $false) -Message "Final guard check should fail for unguarded final commands."
    $unguardedExitCode = Invoke-VerifierExitCode -ManifestPath $unguardedManifestPath -SourceRoot $repoRoot -AdbPath $fakeAdb -FailOnHandoffConsistencyMismatch
    Assert-Equal -Actual $unguardedExitCode -Expected 34 -Message "Unguarded command mismatch should exit 34."

    "# 3. Runnable Commands: requested validation evidence." | Set-Content -LiteralPath $artifactA -Encoding utf8
    "## Runnable Commands" | Set-Content -LiteralPath $artifactB -Encoding utf8
    $allowedManifestPath = Join-Path $bundleRoot "allowed_commands_manifest.json"
    $allowedManifest = [ordered]@{
        deviceSerial = "PIXEL-VERIFY-TEST"
        source = [ordered]@{
            branch = "main"
            commit = $sourceCommit
            clean = $true
            commitReachableFromOriginMain = $true
        }
        artifacts = @(
            New-ArtifactRecord -Name "commands" -Path $unguardedCommandsArtifact
            New-ArtifactRecord -Name "runbook" -Path $artifactA
            New-ArtifactRecord -Name "handoff" -Path $artifactB
        )
        allowOperatorCommands = $true
        allowFinalCommands = $true
    }
    $allowedManifest | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $allowedManifestPath -Encoding utf8
    $allowedResult = & (Join-Path $PSScriptRoot "verify_pixel_validation_handoff.ps1") `
        -ManifestPath $allowedManifestPath `
        -SourceRoot $repoRoot `
        -AdbPath $fakeAdb `
        -Json | ConvertFrom-Json
    Assert-Equal -Actual $allowedResult.handoffConsistencyMismatchCount -Expected 0 -Message "Allowed runnable commands should pass guard consistency checks."

    "# 3. Guarded Commands: requested validation evidence." | Set-Content -LiteralPath $artifactA -Encoding utf8
    "## Guarded Commands" | Set-Content -LiteralPath $artifactB -Encoding utf8
    $staleReadinessManifestPath = Join-Path $bundleRoot "stale_session_readiness_manifest.json"
    $staleReadinessManifest = [ordered]@{
        deviceSerial = "PIXEL-VERIFY-TEST"
        source = [ordered]@{
            branch = "main"
            commit = $sourceCommit
            clean = $true
            commitReachableFromOriginMain = $true
        }
        artifacts = @(
            New-ArtifactRecord -Name "commands" -Path $commandsBaseArtifact
            New-ArtifactRecord -Name "runbook" -Path $artifactA
            New-ArtifactRecord -Name "handoff" -Path $artifactB
        )
        allowOperatorCommands = $false
        allowFinalCommands = $false
        sessionReadiness = [ordered]@{
            command = $sessionReadinessCommand
            outputPath = "pixel_session_readiness_preflight.json"
        }
    }
    $staleReadinessManifest | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $staleReadinessManifestPath -Encoding utf8
    $staleReadinessResult = & (Join-Path $PSScriptRoot "verify_pixel_validation_handoff.ps1") `
        -ManifestPath $staleReadinessManifestPath `
        -SourceRoot $repoRoot `
        -AdbPath $fakeAdb `
        -Json | ConvertFrom-Json
    Assert-Equal -Actual $staleReadinessResult.handoffConsistencyMismatchCount -Expected 1 -Message "Missing session readiness guidance should produce one consistency mismatch."
    Assert-True -Condition (($staleReadinessResult.handoffConsistencyChecks | Where-Object { $_.name -eq "sessionReadinessCommandMatchesManifest" }).passed -eq $false) -Message "Session readiness check should fail for stale handoffs."
    $staleReadinessExitCode = Invoke-VerifierExitCode -ManifestPath $staleReadinessManifestPath -SourceRoot $repoRoot -AdbPath $fakeAdb -FailOnHandoffConsistencyMismatch
    Assert-Equal -Actual $staleReadinessExitCode -Expected 34 -Message "Stale session readiness guidance should exit 34."

    "# 3. Guarded Commands: requested validation evidence." | Set-Content -LiteralPath $artifactA -Encoding utf8
    "## Guarded Commands" | Set-Content -LiteralPath $artifactB -Encoding utf8

    $commandsArtifact = Join-Path $bundleRoot "pixel_validation_commands.txt"
    '.\tools\capture_live_validation_evidence.ps1 -Label "manual-roi-known-target-final" -MeasureRoiExpected "<visible-target-bounds-in-screenshot-space>"' | Set-Content -LiteralPath $commandsArtifact -Encoding utf8
    "# 3. Runnable Commands: requested validation evidence." | Set-Content -LiteralPath $artifactA -Encoding utf8
    "## Runnable Commands" | Set-Content -LiteralPath $artifactB -Encoding utf8
    $staleRoiManifestPath = Join-Path $bundleRoot "stale_roi_placeholder_manifest.json"
    $staleRoiManifest = [ordered]@{
        deviceSerial = "PIXEL-VERIFY-TEST"
        source = [ordered]@{
            branch = "main"
            commit = $sourceCommit
            clean = $true
            commitReachableFromOriginMain = $true
        }
        artifacts = @(
            New-ArtifactRecord -Name "commands" -Path $commandsArtifact
            New-ArtifactRecord -Name "runbook" -Path $artifactA
            New-ArtifactRecord -Name "handoff" -Path $artifactB
        )
        allowOperatorCommands = $true
        allowFinalCommands = $true
        roiFinalHelperCommands = @()
    }
    $staleRoiManifest | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $staleRoiManifestPath -Encoding utf8
    $staleRoiResult = & (Join-Path $PSScriptRoot "verify_pixel_validation_handoff.ps1") `
        -ManifestPath $staleRoiManifestPath `
        -SourceRoot $repoRoot `
        -AdbPath $fakeAdb `
        -Json | ConvertFrom-Json
    Assert-Equal -Actual $staleRoiResult.handoffConsistencyMismatchCount -Expected 1 -Message "ROI placeholder without helper should produce one consistency mismatch."
    Assert-True -Condition (($staleRoiResult.handoffConsistencyChecks | Where-Object { $_.name -eq "manualRoiFinalHelper" }).passed -eq $false) -Message "Manual ROI helper check should fail for stale handoff."
    $staleRoiExitCode = Invoke-VerifierExitCode -ManifestPath $staleRoiManifestPath -SourceRoot $repoRoot -AdbPath $fakeAdb -FailOnHandoffConsistencyMismatch
    Assert-Equal -Actual $staleRoiExitCode -Expected 34 -Message "Handoff consistency gate should exit 34."

    @(
        "# 3. Runnable Commands: requested validation evidence.",
        ".\tools\prepare_roi_final_capture_command.ps1 -Slot manualRoi -SetupBundle ""<manual-roi-setup-bundle>"" -PixelBounds ""<left,top,right,bottom-from-setup-screenshot>"""
    ) | Set-Content -LiteralPath $artifactA -Encoding utf8
    @(
        "## Runnable Commands",
        ".\tools\prepare_roi_final_capture_command.ps1 -Slot manualRoi -SetupBundle ""<manual-roi-setup-bundle>"" -PixelBounds ""<left,top,right,bottom-from-setup-screenshot>"""
    ) | Set-Content -LiteralPath $artifactB -Encoding utf8
    $pairedRoiManifestPath = Join-Path $bundleRoot "paired_roi_placeholder_manifest.json"
    $pairedRoiManifest = [ordered]@{
        deviceSerial = "PIXEL-VERIFY-TEST"
        source = [ordered]@{
            branch = "main"
            commit = $sourceCommit
            clean = $true
            commitReachableFromOriginMain = $true
        }
        artifacts = @(
            New-ArtifactRecord -Name "commands" -Path $commandsArtifact
            New-ArtifactRecord -Name "runbook" -Path $artifactA
            New-ArtifactRecord -Name "handoff" -Path $artifactB
        )
        allowOperatorCommands = $true
        allowFinalCommands = $true
        roiFinalHelperCommands = @(
            '.\tools\prepare_roi_final_capture_command.ps1 -Slot manualRoi -SetupBundle "<manual-roi-setup-bundle>" -PixelBounds "<left,top,right,bottom-from-setup-screenshot>"'
        )
    }
    $pairedRoiManifest | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $pairedRoiManifestPath -Encoding utf8
    $pairedRoiResult = & (Join-Path $PSScriptRoot "verify_pixel_validation_handoff.ps1") `
        -ManifestPath $pairedRoiManifestPath `
        -SourceRoot $repoRoot `
        -AdbPath $fakeAdb `
        -Json | ConvertFrom-Json
    Assert-Equal -Actual $pairedRoiResult.handoffConsistencyMismatchCount -Expected 0 -Message "ROI placeholder paired with helper guidance should pass consistency checks."

    $rawTemplateRunbook = @(
        "# 3. Runnable Commands: requested validation evidence.",
        ".\tools\prepare_roi_final_capture_command.ps1 -Slot manualRoi -SetupBundle ""<manual-roi-setup-bundle>"" -PixelBounds ""<left,top,right,bottom-from-setup-screenshot>""",
        ".\tools\capture_live_validation_evidence.ps1 -Label ""manual-roi-known-target-final"" -MeasureRoiExpected ""<visible-target-bounds-in-screenshot-space>"""
    )
    $rawTemplateHandoff = @(
        "## Runnable Commands",
        ".\tools\prepare_roi_final_capture_command.ps1 -Slot manualRoi -SetupBundle ""<manual-roi-setup-bundle>"" -PixelBounds ""<left,top,right,bottom-from-setup-screenshot>""",
        ".\tools\capture_live_validation_evidence.ps1 -Label ""manual-roi-known-target-final"" -MeasureRoiExpected ""<visible-target-bounds-in-screenshot-space>"""
    )
    $rawTemplateRunbook | Set-Content -LiteralPath $artifactA -Encoding utf8
    $rawTemplateHandoff | Set-Content -LiteralPath $artifactB -Encoding utf8
    $rawTemplateManifestPath = Join-Path $bundleRoot "raw_roi_template_manifest.json"
    $rawTemplateManifest = [ordered]@{
        deviceSerial = "PIXEL-VERIFY-TEST"
        source = [ordered]@{
            branch = "main"
            commit = $sourceCommit
            clean = $true
            commitReachableFromOriginMain = $true
        }
        artifacts = @(
            New-ArtifactRecord -Name "commands" -Path $commandsArtifact
            New-ArtifactRecord -Name "runbook" -Path $artifactA
            New-ArtifactRecord -Name "handoff" -Path $artifactB
        )
        allowOperatorCommands = $true
        allowFinalCommands = $true
        roiFinalHelperCommands = @(
            '.\tools\prepare_roi_final_capture_command.ps1 -Slot manualRoi -SetupBundle "<manual-roi-setup-bundle>" -PixelBounds "<left,top,right,bottom-from-setup-screenshot>"'
        )
    }
    $rawTemplateManifest | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $rawTemplateManifestPath -Encoding utf8
    $rawTemplateResult = & (Join-Path $PSScriptRoot "verify_pixel_validation_handoff.ps1") `
        -ManifestPath $rawTemplateManifestPath `
        -SourceRoot $repoRoot `
        -AdbPath $fakeAdb `
        -Json | ConvertFrom-Json
    Assert-Equal -Actual $rawTemplateResult.handoffConsistencyMismatchCount -Expected 2 -Message "Runnable ROI final templates should fail runbook and Markdown consistency checks."
    Assert-True -Condition (($rawTemplateResult.handoffConsistencyChecks | Where-Object { $_.name -eq "runbookRoiFinalTemplatesReferenceOnly" }).passed -eq $false) -Message "Runbook reference-only check should fail for a raw ROI final placeholder."
    Assert-True -Condition (($rawTemplateResult.handoffConsistencyChecks | Where-Object { $_.name -eq "handoffRoiFinalTemplatesReferenceOnly" }).passed -eq $false) -Message "Markdown handoff reference-only check should fail for a raw ROI final placeholder."
    $rawTemplateExitCode = Invoke-VerifierExitCode -ManifestPath $rawTemplateManifestPath -SourceRoot $repoRoot -AdbPath $fakeAdb -FailOnHandoffConsistencyMismatch
    Assert-Equal -Actual $rawTemplateExitCode -Expected 34 -Message "Runnable ROI final template gate should exit 34."

    @(
        "# 2. Snapshot session readiness before watched capture.",
        $sessionReadinessCommand,
        "# 3. Guarded Commands: requested validation evidence."
    ) | Set-Content -LiteralPath $artifactA -Encoding utf8
    @(
        "## Thermal Preflight",
        ('- Session readiness command: `{0}`' -f $sessionReadinessCommand),
        $sessionReadinessCommand,
        "## Guarded Commands"
    ) | Set-Content -LiteralPath $artifactB -Encoding utf8

    $text = & (Join-Path $PSScriptRoot "verify_pixel_validation_handoff.ps1") `
        -ManifestPath $manifestPath `
        -SourceRoot $repoRoot `
        -AdbPath $fakeAdb
    Assert-True -Condition (($text -join "`n").Contains("Pixel validation handoff verification")) -Message "Text output should include a heading."
    Assert-True -Condition (($text -join "`n").Contains("Artifact mismatches: 0")) -Message "Text output should include artifact mismatch count."
    Assert-True -Condition (($text -join "`n").Contains("Handoff consistency mismatches: 0")) -Message "Text output should include handoff consistency mismatch count."
    Assert-True -Condition (($text -join "`n").Contains("Expected device connected: True")) -Message "Text output should include device availability."
} finally {
    if (Test-Path -LiteralPath $root) {
        Remove-Item -LiteralPath $root -Recurse -Force
    }
}

Write-Output "Pixel validation handoff verifier self-test passed."
