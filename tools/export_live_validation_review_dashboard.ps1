param(
    [string]$EvidenceRoot = "sample-videos\exports\live-validation",
    [string]$OutputPath = "",
    [switch]$PendingOnly,
    [switch]$Open
)

$ErrorActionPreference = "Stop"

function ConvertTo-HtmlText {
    param([object]$Value)

    if ($null -eq $Value) {
        return ""
    }

    return [System.Net.WebUtility]::HtmlEncode([string]$Value)
}

function ConvertTo-FileUri {
    param([string]$Path)

    return ([System.Uri](Resolve-Path -LiteralPath $Path).Path).AbsoluteUri
}

$rootPath = Resolve-Path -LiteralPath $EvidenceRoot -ErrorAction SilentlyContinue
if (-not $rootPath) {
    Write-Output "Evidence root was not found: $EvidenceRoot"
    exit 3
}

$queue = & (Join-Path $PSScriptRoot "show_live_validation_review_queue.ps1") -EvidenceRoot $rootPath.Path -Json | ConvertFrom-Json
$entries = @($queue.bundles | Where-Object { $_.screenrecordPresent })
if ($PendingOnly) {
    $entries = @($entries | Where-Object { $_.pendingReviewSheet })
}

$destination = if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    Join-Path $rootPath.Path "live_validation_review_dashboard.html"
} else {
    $OutputPath
}
$destinationDirectory = Split-Path -Parent $destination
if (-not [string]::IsNullOrWhiteSpace($destinationDirectory)) {
    New-Item -ItemType Directory -Path $destinationDirectory -Force | Out-Null
}

$generatedAt = (Get-Date).ToString("o")
$cards = foreach ($entry in $entries) {
    $screenrecordPath = Join-Path $entry.bundle "screenrecord.mp4"
    $sheetPath = Join-Path $entry.bundle "review_contact_sheet.jpg"
    $summaryPath = Join-Path $entry.bundle "evidence_summary.json"
    $videoUri = ConvertTo-FileUri -Path $screenrecordPath
    $sheetHtml = if ($entry.reviewContactSheetPresent -and (Test-Path -LiteralPath $sheetPath)) {
        '<img src="' + (ConvertTo-FileUri -Path $sheetPath) + '" alt="Review contact sheet for ' + (ConvertTo-HtmlText $entry.label) + '" />'
    } else {
        '<div class="missing-sheet">No review contact sheet</div>'
    }
    $summaryLink = if (Test-Path -LiteralPath $summaryPath) {
        '<a href="' + (ConvertTo-FileUri -Path $summaryPath) + '">evidence_summary.json</a>'
    } else {
        '<span>no evidence_summary.json</span>'
    }
    $issue = if ([string]::IsNullOrWhiteSpace($entry.reviewSheetIssue)) { "ready" } else { $entry.reviewSheetIssue }
    @"
<section class="bundle">
  <header>
    <h2>$(ConvertTo-HtmlText $entry.label)</h2>
    <span class="issue">$(ConvertTo-HtmlText $issue)</span>
  </header>
  <video controls preload="metadata" src="$videoUri"></video>
  $sheetHtml
  <dl>
    <dt>Bundle</dt><dd>$(ConvertTo-HtmlText $entry.bundle)</dd>
    <dt>Screenrecord SHA-256</dt><dd>$(ConvertTo-HtmlText $entry.screenrecordSha256)</dd>
    <dt>Review sheet SHA-256</dt><dd>$(ConvertTo-HtmlText $entry.reviewContactSheetSha256)</dd>
    <dt>Manifest screenrecord match</dt><dd>$(ConvertTo-HtmlText $entry.screenrecordSha256Matches)</dd>
    <dt>Summary</dt><dd>$summaryLink</dd>
  </dl>
  <pre>$(ConvertTo-HtmlText $entry.command)</pre>
</section>
"@
}

$html = @"
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Live Validation Review Dashboard</title>
  <style>
    body { font-family: Segoe UI, Arial, sans-serif; margin: 24px; color: #1b1f24; background: #f7f7f4; }
    h1 { margin: 0 0 8px; font-size: 28px; }
    .meta { margin: 0 0 20px; color: #586069; }
    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(360px, 1fr)); gap: 16px; }
    .bundle { background: white; border: 1px solid #d8dee4; border-radius: 8px; padding: 14px; }
    header { display: flex; gap: 12px; justify-content: space-between; align-items: baseline; }
    h2 { font-size: 18px; margin: 0 0 10px; }
    .issue { font-size: 12px; padding: 3px 8px; border-radius: 999px; background: #fff4ce; color: #6f4e00; white-space: nowrap; }
    video, img { width: 100%; max-height: 360px; background: #111; border-radius: 4px; object-fit: contain; }
    .missing-sheet { display: grid; place-items: center; min-height: 120px; background: #f0f2f4; color: #57606a; border-radius: 4px; }
    dl { display: grid; grid-template-columns: max-content 1fr; gap: 6px 10px; font-size: 12px; overflow-wrap: anywhere; }
    dt { font-weight: 600; color: #57606a; }
    dd { margin: 0; }
    pre { white-space: pre-wrap; overflow-wrap: anywhere; background: #f6f8fa; padding: 8px; border-radius: 4px; font-size: 12px; }
  </style>
</head>
<body>
  <h1>Live Validation Review Dashboard</h1>
  <p class="meta">Generated $generatedAt from $(ConvertTo-HtmlText $queue.evidenceRoot). Showing $($entries.Count) of $($queue.screenrecordBundleCount) screenrecord bundle(s); pending review sheets: $($queue.pendingReviewSheetCount).</p>
  <main class="grid">
$($cards -join "`n")
  </main>
</body>
</html>
"@

$html | Out-File -LiteralPath $destination -Encoding utf8
Write-Output "Live validation review dashboard written: $destination"

if ($Open) {
    Start-Process -FilePath $destination
}

exit 0
