param(
    [Parameter(Mandatory = $true)]
    [string]$BundlePath,

    [string]$OutputPath = "",
    [string]$FfmpegPath = "",
    [int]$Columns = 3,
    [int]$Rows = 3,
    [int]$FrameWidth = 360,
    [string]$NpxPath = "",
    [string]$BrowserChannel = "chrome",
    [switch]$NoBrowserFallback,
    [switch]$RefreshSummary,
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

function Find-Npx {
    param([string]$ExplicitPath)

    if (-not [string]::IsNullOrWhiteSpace($ExplicitPath)) {
        if (Test-Path -LiteralPath $ExplicitPath) {
            return (Resolve-Path -LiteralPath $ExplicitPath).Path
        }
        throw "Requested npx path does not exist: $ExplicitPath"
    }

    $command = Get-Command npx -ErrorAction SilentlyContinue
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

function ConvertTo-FileUri {
    param([string]$Path)

    return ([System.Uri](Resolve-Path -LiteralPath $Path).Path).AbsoluteUri
}

function Add-BooleanGateArg {
    param(
        [hashtable]$Arguments,
        $Summary,
        [string]$Gate,
        [string]$ParameterName
    )

    if ($Summary -and
        $Summary.requiredGates -and
        ($Summary.requiredGates.PSObject.Properties.Name -contains $Gate) -and
        $Summary.requiredGates.$Gate.required -eq $true) {
        $Arguments[$ParameterName] = $true
    }
}

function Invoke-SummaryRefresh {
    param([string]$BundlePath)

    $summaryScript = Join-Path $PSScriptRoot "summarize_live_validation_evidence.ps1"
    if (-not (Test-Path -LiteralPath $summaryScript)) {
        Write-Output "Summary refresh requested, but summarize_live_validation_evidence.ps1 was not found."
        return 6
    }

    $summaryPath = Join-Path $BundlePath "evidence_summary.json"
    $existingSummary = if (Test-Path -LiteralPath $summaryPath) {
        Get-Content -LiteralPath $summaryPath -Raw | ConvertFrom-Json
    } else {
        $null
    }

    $summaryArgs = @{
        BundlePath = $BundlePath
        RequireReviewContactSheet = $true
    }
    Add-BooleanGateArg -Arguments $summaryArgs -Summary $existingSummary -Gate "cleanSource" -ParameterName "RequireCleanSource"
    Add-BooleanGateArg -Arguments $summaryArgs -Summary $existingSummary -Gate "visualValidation" -ParameterName "RequireVisualValidation"
    Add-BooleanGateArg -Arguments $summaryArgs -Summary $existingSummary -Gate "roiMeasurement" -ParameterName "RequireRoiMeasurement"
    Add-BooleanGateArg -Arguments $summaryArgs -Summary $existingSummary -Gate "screenrecord" -ParameterName "RequireScreenrecord"
    Add-BooleanGateArg -Arguments $summaryArgs -Summary $existingSummary -Gate "thermalReady" -ParameterName "RequireThermalReady"
    Add-BooleanGateArg -Arguments $summaryArgs -Summary $existingSummary -Gate "cameraFps" -ParameterName "RequireCameraFps"
    Add-BooleanGateArg -Arguments $summaryArgs -Summary $existingSummary -Gate "focusedApp" -ParameterName "RequireFocusedApp"
    Add-BooleanGateArg -Arguments $summaryArgs -Summary $existingSummary -Gate "rendererDiagnostics" -ParameterName "RequireRendererDiagnostics"
    Add-BooleanGateArg -Arguments $summaryArgs -Summary $existingSummary -Gate "phaseDiagnostics" -ParameterName "RequirePhaseDiagnostics"
    Add-BooleanGateArg -Arguments $summaryArgs -Summary $existingSummary -Gate "noWarnings" -ParameterName "RequireNoWarnings"

    if ($existingSummary -and $existingSummary.uiTextAssertions -and @($existingSummary.uiTextAssertions.required).Count -gt 0) {
        $summaryArgs.RequireUiText = @($existingSummary.uiTextAssertions.required)
    }
    if ($existingSummary -and
        $existingSummary.requiredGates -and
        ($existingSummary.requiredGates.PSObject.Properties.Name -contains "deviceSerial") -and
        $existingSummary.requiredGates.deviceSerial.required -eq $true -and
        -not [string]::IsNullOrWhiteSpace($existingSummary.requiredGates.deviceSerial.expected)) {
        $summaryArgs.RequireDeviceSerial = [string]$existingSummary.requiredGates.deviceSerial.expected
    }
    if ($existingSummary -and
        $existingSummary.requiredGates -and
        ($existingSummary.requiredGates.PSObject.Properties.Name -contains "evidenceVerdict") -and
        $existingSummary.requiredGates.evidenceVerdict.required -eq $true -and
        -not [string]::IsNullOrWhiteSpace($existingSummary.requiredGates.evidenceVerdict.expected)) {
        $summaryArgs.RequireEvidenceVerdict = [string]$existingSummary.requiredGates.evidenceVerdict.expected
    }

    & $summaryScript @summaryArgs
    return $LASTEXITCODE
}

function Write-BrowserContactSheet {
    param(
        [string]$ScreenrecordPath,
        [string]$Destination,
        [string]$Npx,
        [int]$Columns,
        [int]$Rows,
        [int]$FrameWidth,
        [string]$BrowserChannel
    )

    $tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) "eulerian-review-sheet-browser-$([guid]::NewGuid().ToString('N'))"
    New-Item -ItemType Directory -Path $tempRoot -Force | Out-Null
    try {
        $pagePath = Join-Path $tempRoot "contact-sheet.html"
        $videoUri = ConvertTo-FileUri -Path $ScreenrecordPath
        $safeVideoUri = [System.Net.WebUtility]::HtmlEncode($videoUri)
        $html = @"
<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <style>
    html, body { margin: 0; padding: 0; background: #111; }
    canvas { display: block; background: #111; }
    video { display: none; }
    #ready { position: absolute; left: 0; top: 0; width: 1px; height: 1px; background: #111; }
  </style>
</head>
<body>
  <canvas id="sheet"></canvas>
  <video id="source" src="$safeVideoUri" muted playsinline preload="auto"></video>
  <script>
    const columns = $Columns;
    const rows = $Rows;
    const frameWidth = $FrameWidth;
    const totalFrames = columns * rows;
    const video = document.getElementById('source');
    const canvas = document.getElementById('sheet');
    const ctx = canvas.getContext('2d');

    function once(target, event) {
      return new Promise((resolve, reject) => {
        const timer = setTimeout(() => reject(new Error('Timed out waiting for ' + event)), 10000);
        target.addEventListener(event, () => {
          clearTimeout(timer);
          resolve();
        }, { once: true });
      });
    }

    async function seekVideo(timeSeconds) {
      const clamped = Math.max(0, Math.min(timeSeconds, Math.max(0, video.duration - 0.05)));
      const wait = once(video, 'seeked');
      video.currentTime = clamped;
      await wait;
    }

    async function render() {
      await once(video, 'loadedmetadata');
      const aspect = video.videoWidth > 0 ? video.videoHeight / video.videoWidth : 16 / 9;
      const frameHeight = Math.max(1, Math.round(frameWidth * aspect));
      canvas.width = columns * frameWidth;
      canvas.height = rows * frameHeight;
      ctx.fillStyle = '#111';
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      ctx.font = '16px sans-serif';
      ctx.textBaseline = 'top';

      for (let index = 0; index < totalFrames; index += 1) {
        const duration = Number.isFinite(video.duration) && video.duration > 0 ? video.duration : totalFrames;
        const timestamp = ((index + 0.5) / totalFrames) * duration;
        await seekVideo(timestamp);
        const x = (index % columns) * frameWidth;
        const y = Math.floor(index / columns) * frameHeight;
        ctx.drawImage(video, x, y, frameWidth, frameHeight);
        ctx.fillStyle = 'rgba(0, 0, 0, 0.58)';
        ctx.fillRect(x, y, 74, 24);
        ctx.fillStyle = '#fff';
        ctx.fillText(timestamp.toFixed(1) + 's', x + 8, y + 4);
      }

      const ready = document.createElement('div');
      ready.id = 'ready';
      document.body.appendChild(ready);
    }

    render().catch((error) => {
      document.body.textContent = error.stack || String(error);
      document.body.style.color = 'white';
      document.body.style.font = '16px sans-serif';
    });
  </script>
</body>
</html>
"@
        Set-Content -LiteralPath $pagePath -Value $html -Encoding utf8
        $pageUri = ConvertTo-FileUri -Path $pagePath
        $playwrightArgs = @(
            "playwright",
            "screenshot",
            "--channel", $BrowserChannel,
            "--wait-for-selector", "#ready",
            "--timeout", "120000",
            "--full-page",
            $pageUri,
            $Destination
        )
        & $Npx @playwrightArgs *> $null
        return $LASTEXITCODE
    } finally {
        if (Test-Path -LiteralPath $tempRoot) {
            Remove-Item -LiteralPath $tempRoot -Recurse -Force
        }
    }
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
$npx = $null
if ([string]::IsNullOrWhiteSpace($ffmpeg) -and -not $NoBrowserFallback) {
    $npx = Find-Npx -ExplicitPath $NpxPath
}
if ([string]::IsNullOrWhiteSpace($ffmpeg) -and [string]::IsNullOrWhiteSpace($npx)) {
    Write-Output "ffmpeg was not found, and browser fallback is unavailable. Install ffmpeg, pass -FfmpegPath, or install Node/npx with Playwright."
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

$generator = "ffmpeg"
$filter = "fps=1,scale=$($FrameWidth):-1,tile=$($Columns)x$($Rows)"
if (-not [string]::IsNullOrWhiteSpace($ffmpeg)) {
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
} else {
    $generator = "browser"
    $browserExitCode = Write-BrowserContactSheet -ScreenrecordPath $screenrecordPath -Destination $destination -Npx $npx -Columns $Columns -Rows $Rows -FrameWidth $FrameWidth -BrowserChannel $BrowserChannel
    if ($browserExitCode -ne 0) {
        Write-Output "Browser contact-sheet export failed with exit code $browserExitCode."
        exit 4
    }
}
if (-not (Test-Path -LiteralPath $destination)) {
    Write-Output "Contact-sheet export completed but did not create the review sheet: $destination"
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
    generator = $generator
    ffmpeg = $ffmpeg
    npx = $npx
    browserChannel = if ($generator -eq "browser") { $BrowserChannel } else { $null }
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

if ($RefreshSummary) {
    $summaryExitCode = Invoke-SummaryRefresh -BundlePath $bundle
    if ($summaryExitCode -ne 0) {
        Write-Output "Review sheet was written, but summary refresh failed with exit code $summaryExitCode."
        exit $summaryExitCode
    }
}

Write-Output "Live validation review sheet written: $($outputItem.FullName)"
Write-Output "Manifest: $manifestPath"
if ($RefreshSummary) {
    Write-Output "Summary refreshed: $(Join-Path $bundle "evidence_summary.json")"
}
exit 0
