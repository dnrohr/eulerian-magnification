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

function Invoke-HandoffExitCode {
    param(
        [string]$EvidenceRoot,
        [string]$OutputRoot,
        [string[]]$Slot = @(),
        [switch]$FailOnInvalidSlot,
        [switch]$FailOnEmptyQueue
    )

    $script = Join-Path $PSScriptRoot "prepare_pixel_validation_handoff.ps1"
    $powerShellExe = (Get-Process -Id $PID).Path
    $arguments = @("-NoProfile", "-File", $script, "-EvidenceRoot", $EvidenceRoot, "-OutputRoot", $OutputRoot)
    foreach ($slotId in $Slot) {
        $arguments += @("-Slot", $slotId)
    }
    if ($FailOnInvalidSlot) {
        $arguments += "-FailOnInvalidSlot"
    }
    if ($FailOnEmptyQueue) {
        $arguments += "-FailOnEmptyQueue"
    }

    & $powerShellExe @arguments *> $null
    return $LASTEXITCODE
}

$evidenceRoot = Join-Path ([System.IO.Path]::GetTempPath()) "eulerian-handoff-evidence-$([guid]::NewGuid().ToString('N'))"
$outputRoot = Join-Path ([System.IO.Path]::GetTempPath()) "eulerian-handoff-output-$([guid]::NewGuid().ToString('N'))"

$result = & (Join-Path $PSScriptRoot "prepare_pixel_validation_handoff.ps1") `
    -EvidenceRoot $evidenceRoot `
    -OutputRoot $outputRoot `
    -Slot pulseLinear `
    -CaptureStage Final `
    -Json | ConvertFrom-Json

Assert-Equal -Actual $result.evidenceRoot -Expected $evidenceRoot -Message "Handoff should preserve evidence root."
Assert-Equal -Actual $result.outputRoot -Expected $outputRoot -Message "Handoff should preserve output root."
Assert-Equal -Actual $result.captureStage -Expected "Final" -Message "Handoff should preserve capture stage."
Assert-Equal -Actual @($result.requestedSlots).Count -Expected 1 -Message "Handoff should preserve requested slot count."
Assert-Equal -Actual $result.requestedSlots[0] -Expected "pulseLinear" -Message "Handoff should preserve requested slot id."
Assert-Equal -Actual @($result.invalidRequestedSlots).Count -Expected 0 -Message "Known handoff slot should not be invalid."
Assert-Equal -Actual $result.recommendedCaptureCount -Expected 1 -Message "Filtered handoff should include one recommended capture."
Assert-Equal -Actual $result.commandCount -Expected 1 -Message "Filtered handoff should include one command template."
Assert-True -Condition (Test-Path -LiteralPath $result.planPath) -Message "Handoff should write plan JSON."
Assert-True -Condition (Test-Path -LiteralPath $result.closeoutPath) -Message "Handoff should write closeout JSON."
Assert-True -Condition (Test-Path -LiteralPath $result.commandsPath) -Message "Handoff should write command templates."
Assert-True -Condition (Test-Path -LiteralPath $result.handoffPath) -Message "Handoff should write a readable Markdown summary."

$plan = Get-Content -LiteralPath $result.planPath -Raw | ConvertFrom-Json
$closeout = Get-Content -LiteralPath $result.closeoutPath -Raw | ConvertFrom-Json
$commands = Get-Content -LiteralPath $result.commandsPath -Raw
$handoff = Get-Content -LiteralPath $result.handoffPath -Raw

Assert-Equal -Actual @($plan.recommendedCaptures).Count -Expected 1 -Message "Written plan should preserve filtered recommended captures."
Assert-Equal -Actual $plan.recommendedCaptures[0].slot -Expected "pulseLinear" -Message "Written plan should preserve filtered slot."
Assert-True -Condition ($commands.Contains("live-linear-pulse-final")) -Message "Command handoff should include the filtered final command."
Assert-True -Condition (-not $commands.Contains("live-linear-pulse-setup")) -Message "Final-only command handoff should omit setup command."
Assert-Equal -Actual @($closeout.closeoutBlockers).Count -Expected 1 -Message "Missing evidence handoff should preserve closeout blockers."
Assert-True -Condition ($handoff.Contains("# Pixel Validation Handoff")) -Message "Markdown handoff should include a title."
Assert-True -Condition ($handoff.Contains('Requested slots: `pulseLinear`')) -Message "Markdown handoff should include requested slots."
Assert-True -Condition ($handoff.Contains('Expected final label: `live-linear-pulse-final`')) -Message "Markdown handoff should include expected final labels."
Assert-True -Condition ($handoff.Contains("```powershell")) -Message "Markdown handoff should include a PowerShell command block."
Assert-True -Condition ($handoff.Contains("live-linear-pulse-final")) -Message "Markdown handoff should include the filtered command."
Assert-True -Condition ($handoff.Contains("missingSlots")) -Message "Markdown handoff should summarize closeout blockers."

$textOutput = & (Join-Path $PSScriptRoot "prepare_pixel_validation_handoff.ps1") `
    -EvidenceRoot $evidenceRoot `
    -OutputRoot $outputRoot `
    -Slot notARealSlot

Assert-True -Condition (($textOutput -join "`n").Contains("Pixel validation handoff prepared")) -Message "Text handoff should print a heading."
Assert-True -Condition (($textOutput -join "`n").Contains("Handoff:")) -Message "Text handoff should print the Markdown handoff path."
Assert-True -Condition (($textOutput -join "`n").Contains("Warning: requested slot(s) not currently missing or unknown: notARealSlot")) -Message "Text handoff should warn about invalid slots."
Assert-True -Condition (($textOutput -join "`n").Contains("Warning: no recommended captures match the current filters.")) -Message "Text handoff should warn about empty queues."

$validExitCode = Invoke-HandoffExitCode -EvidenceRoot $evidenceRoot -OutputRoot $outputRoot -Slot pulseLinear -FailOnInvalidSlot -FailOnEmptyQueue
$invalidExitCode = Invoke-HandoffExitCode -EvidenceRoot $evidenceRoot -OutputRoot $outputRoot -Slot notARealSlot -FailOnInvalidSlot
$emptyExitCode = Invoke-HandoffExitCode -EvidenceRoot $evidenceRoot -OutputRoot $outputRoot -Slot notARealSlot -FailOnEmptyQueue

Assert-Equal -Actual $validExitCode -Expected 0 -Message "Handoff gates should allow valid non-empty filters."
Assert-Equal -Actual $invalidExitCode -Expected 21 -Message "Handoff should fail invalid slot filters with exit code 21."
Assert-Equal -Actual $emptyExitCode -Expected 22 -Message "Handoff should fail empty command queues with exit code 22."

Write-Output "Pixel validation handoff self-test passed."
