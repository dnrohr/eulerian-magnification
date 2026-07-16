param(
    [string]$EvidenceRoot = "sample-videos\exports\live-validation",
    [string]$FfmpegPath = "",
    [string]$OutputPath = "",
    [switch]$Json,
    [switch]$CommandsOnly,
    [switch]$FailOnPending
)

$ErrorActionPreference = "Stop"

function Get-FileSha256IfExists {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return $null
    }

    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash
}

function Format-QuotedArgument {
    param([string]$Value)

    return '"' + ($Value -replace '"', '\"') + '"'
}

$rootPath = Resolve-Path -LiteralPath $EvidenceRoot -ErrorAction SilentlyContinue
$bundleDirectories = @()
if ($rootPath) {
    $bundleDirectories = @(Get-ChildItem -LiteralPath $rootPath.Path -Directory)
}

$bundles = @()
$pending = @()
foreach ($directory in $bundleDirectories) {
    $screenrecordPath = Join-Path $directory.FullName "screenrecord.mp4"
    $contactSheetPath = Join-Path $directory.FullName "review_contact_sheet.jpg"
    $contactSheetManifestPath = Join-Path $directory.FullName "review_contact_sheet_manifest.json"
    $summaryPath = Join-Path $directory.FullName "evidence_summary.json"

    $screenrecordPresent = Test-Path -LiteralPath $screenrecordPath
    $screenrecordBytes = if ($screenrecordPresent) { (Get-Item -LiteralPath $screenrecordPath).Length } else { $null }
    $contactSheetPresent = Test-Path -LiteralPath $contactSheetPath
    $contactSheetManifestPresent = Test-Path -LiteralPath $contactSheetManifestPath
    $summary = if (Test-Path -LiteralPath $summaryPath) {
        Get-Content -LiteralPath $summaryPath -Raw | ConvertFrom-Json
    } else {
        $null
    }

    $label = if ($summary -and $summary.PSObject.Properties.Name -contains "label") { $summary.label } else { $directory.Name }
    $command = ".\tools\export_live_validation_review_sheet.ps1 -BundlePath $(Format-QuotedArgument $directory.FullName)"
    if (-not [string]::IsNullOrWhiteSpace($FfmpegPath)) {
        $command += " -FfmpegPath $(Format-QuotedArgument $FfmpegPath)"
    }

    $entry = [pscustomobject][ordered]@{
        bundle = $directory.FullName
        label = $label
        screenrecordPresent = $screenrecordPresent
        screenrecordBytes = $screenrecordBytes
        screenrecordSha256 = Get-FileSha256IfExists -Path $screenrecordPath
        reviewContactSheetPresent = $contactSheetPresent
        reviewContactSheetManifestPresent = $contactSheetManifestPresent
        reviewContactSheetSha256 = Get-FileSha256IfExists -Path $contactSheetPath
        pendingReviewSheet = $screenrecordPresent -and -not $contactSheetPresent
        command = $command
    }
    $bundles += $entry
    if ($entry.pendingReviewSheet) {
        $pending += $entry
    }
}

$result = [pscustomobject][ordered]@{
    evidenceRoot = if ($rootPath) { $rootPath.Path } else { $EvidenceRoot }
    bundleCount = $bundles.Count
    screenrecordBundleCount = @($bundles | Where-Object { $_.screenrecordPresent }).Count
    pendingReviewSheetCount = $pending.Count
    pendingReviewSheets = $pending
    bundles = $bundles
}

$resultJson = $result | ConvertTo-Json -Depth 8
if (-not [string]::IsNullOrWhiteSpace($OutputPath)) {
    $outputDirectory = Split-Path -Parent $OutputPath
    if (-not [string]::IsNullOrWhiteSpace($outputDirectory)) {
        New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
    }
    $resultJson | Out-File -LiteralPath $OutputPath -Encoding utf8
}

if ($CommandsOnly) {
    foreach ($entry in @($pending)) {
        Write-Output $entry.command
    }
} elseif ($Json) {
    $resultJson
} else {
    Write-Output "Live validation review queue"
    if (-not [string]::IsNullOrWhiteSpace($OutputPath)) {
        Write-Output "Queue output: $OutputPath"
    }
    Write-Output "Evidence root: $($result.evidenceRoot)"
    Write-Output "Bundles: $($result.bundleCount)"
    Write-Output "Screenrecord bundles: $($result.screenrecordBundleCount)"
    Write-Output "Pending review sheets: $($result.pendingReviewSheetCount)"
    foreach ($entry in @($pending)) {
        Write-Output "- $($entry.label): $($entry.bundle)"
        Write-Output "    $($entry.command)"
    }
}

if ($FailOnPending -and $pending.Count -gt 0) {
    exit 2
}

exit 0
