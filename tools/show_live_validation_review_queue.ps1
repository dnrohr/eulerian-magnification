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

function Read-JsonIfExists {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return $null
    }

    return Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json
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
    $summary = Read-JsonIfExists -Path $summaryPath
    $contactSheetManifest = Read-JsonIfExists -Path $contactSheetManifestPath
    $screenrecordSha256 = Get-FileSha256IfExists -Path $screenrecordPath
    $contactSheetSha256 = Get-FileSha256IfExists -Path $contactSheetPath
    $manifestScreenrecordSha256 = if ($contactSheetManifest -and $contactSheetManifest.PSObject.Properties.Name -contains "screenrecordSha256") { $contactSheetManifest.screenrecordSha256 } else { $null }
    $manifestContactSheetSha256 = if ($contactSheetManifest -and $contactSheetManifest.PSObject.Properties.Name -contains "contactSheetSha256") { $contactSheetManifest.contactSheetSha256 } else { $null }
    $screenrecordSha256Matches = if ($screenrecordPresent -and $contactSheetManifestPresent -and -not [string]::IsNullOrWhiteSpace($manifestScreenrecordSha256) -and -not [string]::IsNullOrWhiteSpace($screenrecordSha256)) {
        $manifestScreenrecordSha256 -eq $screenrecordSha256
    } else {
        $null
    }
    $contactSheetSha256Matches = if ($contactSheetPresent -and $contactSheetManifestPresent -and -not [string]::IsNullOrWhiteSpace($manifestContactSheetSha256) -and -not [string]::IsNullOrWhiteSpace($contactSheetSha256)) {
        $manifestContactSheetSha256 -eq $contactSheetSha256
    } else {
        $null
    }
    $reviewSheetIssues = @()
    if ($screenrecordPresent -and -not $contactSheetPresent) {
        $reviewSheetIssues += "missingContactSheet"
    }
    if ($screenrecordPresent -and $contactSheetPresent -and -not $contactSheetManifestPresent) {
        $reviewSheetIssues += "missingManifest"
    }
    if ($screenrecordPresent -and $contactSheetPresent -and $contactSheetManifestPresent -and $screenrecordSha256Matches -ne $true) {
        $reviewSheetIssues += "screenrecordHashMismatch"
    }
    if ($screenrecordPresent -and $contactSheetPresent -and $contactSheetManifestPresent -and $contactSheetSha256Matches -ne $true) {
        $reviewSheetIssues += "contactSheetHashMismatch"
    }

    $label = if ($summary -and $summary.PSObject.Properties.Name -contains "label") { $summary.label } else { $directory.Name }
    $command = ".\tools\export_live_validation_review_sheet.ps1 -BundlePath $(Format-QuotedArgument $directory.FullName) -RefreshSummary"
    if (-not [string]::IsNullOrWhiteSpace($FfmpegPath)) {
        $command += " -FfmpegPath $(Format-QuotedArgument $FfmpegPath)"
    }

    $entry = [pscustomobject][ordered]@{
        bundle = $directory.FullName
        label = $label
        screenrecordPresent = $screenrecordPresent
        screenrecordBytes = $screenrecordBytes
        screenrecordSha256 = $screenrecordSha256
        reviewContactSheetPresent = $contactSheetPresent
        reviewContactSheetManifestPresent = $contactSheetManifestPresent
        reviewContactSheetSha256 = $contactSheetSha256
        manifestScreenrecordSha256 = $manifestScreenrecordSha256
        manifestContactSheetSha256 = $manifestContactSheetSha256
        screenrecordSha256Matches = $screenrecordSha256Matches
        contactSheetSha256Matches = $contactSheetSha256Matches
        reviewSheetIssues = $reviewSheetIssues
        reviewSheetIssue = if ($reviewSheetIssues.Count -gt 0) { $reviewSheetIssues -join "," } else { "" }
        pendingReviewSheet = $screenrecordPresent -and $reviewSheetIssues.Count -gt 0
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
        Write-Output "- $($entry.label): $($entry.reviewSheetIssue)"
        Write-Output "    Bundle: $($entry.bundle)"
        Write-Output "    $($entry.command)"
    }
}

if ($FailOnPending -and $pending.Count -gt 0) {
    exit 2
}

exit 0
