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

function Invoke-ReviewSheet {
    param(
        [string]$BundlePath,
        [string]$FfmpegPath = "",
        [string]$OutputPath = "",
        [switch]$Force
    )

    $script = Join-Path $PSScriptRoot "export_live_validation_review_sheet.ps1"
    $stdout = Join-Path $BundlePath "review_sheet_stdout.txt"
    $args = @{
        BundlePath = $BundlePath
    }
    if (-not [string]::IsNullOrWhiteSpace($FfmpegPath)) {
        $args.FfmpegPath = $FfmpegPath
    }
    if (-not [string]::IsNullOrWhiteSpace($OutputPath)) {
        $args.OutputPath = $OutputPath
    }
    if ($Force) {
        $args.Force = $true
    }

    & $script @args *> $stdout
    return $LASTEXITCODE
}

$root = Join-Path ([System.IO.Path]::GetTempPath()) "eulerian-review-sheet-test-$([guid]::NewGuid().ToString('N'))"
New-Item -ItemType Directory -Path $root -Force | Out-Null

try {
    $bundle = Join-Path $root "bundle"
    New-Item -ItemType Directory -Path $bundle -Force | Out-Null
    [System.IO.File]::WriteAllBytes((Join-Path $bundle "screenrecord.mp4"), [byte[]](0, 0, 0, 24, 102, 116, 121, 112, 109, 112, 52, 50))

    $fakeFfmpeg = Join-Path $root "fake-ffmpeg.ps1"
    @'
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Arguments
)

$destination = $Arguments[$Arguments.Count - 1]
[System.IO.File]::WriteAllBytes($destination, [byte[]](255, 216, 255, 217))
$Arguments | Out-File -LiteralPath (Join-Path (Split-Path -Parent $destination) "fake_ffmpeg_args.txt") -Encoding utf8
exit 0
'@ | Out-File -LiteralPath $fakeFfmpeg -Encoding utf8

    $exitCode = Invoke-ReviewSheet -BundlePath $bundle -FfmpegPath $fakeFfmpeg
    Assert-Equal -Actual $exitCode -Expected 0 -Message "Review sheet export exit code mismatch."
    Assert-True -Condition (Test-Path -LiteralPath (Join-Path $bundle "review_contact_sheet.jpg")) -Message "Review sheet should be created."
    Assert-True -Condition (Test-Path -LiteralPath (Join-Path $bundle "review_contact_sheet_manifest.json")) -Message "Review manifest should be created."

    $manifest = Get-Content -LiteralPath (Join-Path $bundle "review_contact_sheet_manifest.json") -Raw | ConvertFrom-Json
    Assert-Equal -Actual $manifest.columns -Expected 3 -Message "Manifest columns mismatch."
    Assert-Equal -Actual $manifest.rows -Expected 3 -Message "Manifest rows mismatch."
    Assert-Equal -Actual $manifest.frameWidth -Expected 360 -Message "Manifest frame width mismatch."
    Assert-Equal -Actual $manifest.filter -Expected "fps=1,scale=360:-1,tile=3x3" -Message "Manifest ffmpeg filter mismatch."
    Assert-True -Condition ($manifest.contactSheetBytes -gt 0) -Message "Manifest should record output bytes."
    Assert-True -Condition (-not [string]::IsNullOrWhiteSpace($manifest.screenrecordSha256)) -Message "Manifest should record screenrecord hash."
    Assert-True -Condition (-not [string]::IsNullOrWhiteSpace($manifest.contactSheetSha256)) -Message "Manifest should record contact sheet hash."

    $fakeArgs = Get-Content -LiteralPath (Join-Path $bundle "fake_ffmpeg_args.txt") -Raw
    Assert-True -Condition ($fakeArgs.Contains("screenrecord.mp4")) -Message "ffmpeg args should include screenrecord input."
    Assert-True -Condition ($fakeArgs.Contains("fps=1,scale=360:-1,tile=3x3")) -Message "ffmpeg args should include contact-sheet filter."

    $overwriteExitCode = Invoke-ReviewSheet -BundlePath $bundle -FfmpegPath $fakeFfmpeg
    Assert-Equal -Actual $overwriteExitCode -Expected 5 -Message "Existing output should require -Force."

    $forceExitCode = Invoke-ReviewSheet -BundlePath $bundle -FfmpegPath $fakeFfmpeg -Force
    Assert-Equal -Actual $forceExitCode -Expected 0 -Message "Force should allow overwriting an existing review sheet."

    $missingBundle = Join-Path $root "missing-screenrecord"
    New-Item -ItemType Directory -Path $missingBundle -Force | Out-Null
    $missingExitCode = Invoke-ReviewSheet -BundlePath $missingBundle -FfmpegPath $fakeFfmpeg
    Assert-Equal -Actual $missingExitCode -Expected 3 -Message "Missing screenrecord should exit 3."
} finally {
    Remove-Item -LiteralPath $root -Recurse -Force
}

Write-Output "Live validation review sheet self-test passed."
