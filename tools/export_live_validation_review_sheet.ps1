param(
    [Parameter(Mandatory = $true)]
    [string]$BundlePath,

    [string]$OutputPath = "",
    [string]$FfmpegPath = "",
    [int]$Columns = 3,
    [int]$Rows = 3,
    [int]$FrameWidth = 360,
    [switch]$Force
)

$ErrorActionPreference = "Stop"

function Find-Ffmpeg {
    param([string]$ExplicitPath)

    if (-not [string]::IsNullOrWhiteSpace($ExplicitPath)) {
        if (Test-Path -LiteralPath $ExplicitPath) {
            return (Resolve-Path -LiteralPath $ExplicitPath).Path
        }
        throw "Requested ffmpeg path does not exist: $ExplicitPath"
    }

    $command = Get-Command ffmpeg -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    return $null
}

function Get-FileSha256IfExists {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return $null
    }

    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash
}

if ($Columns -lt 1 -or $Columns -gt 8) {
    throw "-Columns must be between 1 and 8."
}
if ($Rows -lt 1 -or $Rows -gt 8) {
    throw "-Rows must be between 1 and 8."
}
if ($FrameWidth -lt 120 -or $FrameWidth -gt 1080) {
    throw "-FrameWidth must be between 120 and 1080."
}

$bundle = (Resolve-Path -LiteralPath $BundlePath).Path
$screenrecordPath = Join-Path $bundle "screenrecord.mp4"
if (-not (Test-Path -LiteralPath $screenrecordPath)) {
    Write-Output "screenrecord.mp4 was not found in bundle: $bundle"
    exit 3
}

$ffmpeg = Find-Ffmpeg -ExplicitPath $FfmpegPath
if ([string]::IsNullOrWhiteSpace($ffmpeg)) {
    Write-Output "ffmpeg was not found. Install ffmpeg or pass -FfmpegPath to generate a review sheet."
    exit 2
}

$destination = if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    Join-Path $bundle "review_contact_sheet.jpg"
} else {
    $OutputPath
}
$destinationDirectory = Split-Path -Parent $destination
if (-not [string]::IsNullOrWhiteSpace($destinationDirectory)) {
    New-Item -ItemType Directory -Path $destinationDirectory -Force | Out-Null
}
if ((Test-Path -LiteralPath $destination) -and -not $Force) {
    Write-Output "Output already exists: $destination. Pass -Force to overwrite."
    exit 5
}

$filter = "fps=1,scale=$($FrameWidth):-1,tile=$($Columns)x$($Rows)"
$ffmpegArgs = @(
    "-y",
    "-i", $screenrecordPath,
    "-vf", $filter,
    "-frames:v", "1",
    $destination
)

& $ffmpeg @ffmpegArgs
$ffmpegExitCode = $LASTEXITCODE
if ($ffmpegExitCode -ne 0) {
    Write-Output "ffmpeg failed with exit code $ffmpegExitCode."
    exit 4
}
if (-not (Test-Path -LiteralPath $destination)) {
    Write-Output "ffmpeg completed but did not create the review sheet: $destination"
    exit 4
}

$outputItem = Get-Item -LiteralPath $destination
if ($outputItem.Length -le 0) {
    Write-Output "Review sheet is empty: $destination"
    exit 4
}

$manifest = [ordered]@{
    createdAt = (Get-Date).ToString("o")
    bundle = $bundle
    screenrecord = $screenrecordPath
    screenrecordSha256 = Get-FileSha256IfExists -Path $screenrecordPath
    contactSheet = $outputItem.FullName
    contactSheetSha256 = Get-FileSha256IfExists -Path $outputItem.FullName
    contactSheetBytes = $outputItem.Length
    ffmpeg = $ffmpeg
    columns = $Columns
    rows = $Rows
    frameWidth = $FrameWidth
    filter = $filter
    notes = @(
        "This review sheet samples screenrecord.mp4 for visual inspection.",
        "It is a review aid only; final roadmap closeout still requires accepted visual evidence and the strict summary gates."
    )
}
$manifestPath = Join-Path $bundle "review_contact_sheet_manifest.json"
$manifest | ConvertTo-Json -Depth 6 | Out-File -LiteralPath $manifestPath -Encoding utf8

Write-Output "Live validation review sheet written: $($outputItem.FullName)"
Write-Output "Manifest: $manifestPath"
exit 0
