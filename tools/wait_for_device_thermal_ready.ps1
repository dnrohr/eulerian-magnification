param(
    [int]$ReadyBelowThermalStatus = 4,
    [int]$RequiredReadySamples = 2,
    [int]$TimeoutSeconds = 900,
    [int]$PollSeconds = 30,
    [string]$OutputPath = ""
)

$ErrorActionPreference = "Stop"

function Find-Adb {
    $sdkAdb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
    if (Test-Path -LiteralPath $sdkAdb) {
        return $sdkAdb
    }

    $pathAdb = Get-Command adb -ErrorAction SilentlyContinue
    if ($pathAdb) {
        return $pathAdb.Source
    }

    throw "adb.exe was not found in LOCALAPPDATA Android SDK or PATH."
}

function Thermal-StatusLabel {
    param([Nullable[int]]$Status)
    switch ($Status) {
        0 { "none" }
        1 { "light" }
        2 { "moderate" }
        3 { "severe" }
        4 { "critical" }
        5 { "emergency" }
        6 { "shutdown" }
        default { $null }
    }
}

function Parse-ThermalSummary {
    param([string]$Text)

    $thermalStatus = $null
    $statusMatch = [regex]::Match($Text, 'Thermal Status:\s+(\d+)')
    if ($statusMatch.Success) {
        $thermalStatus = [int]::Parse($statusMatch.Groups[1].Value, [Globalization.CultureInfo]::InvariantCulture)
    }

    $maxSensorStatus = $null
    $maxTemperatureC = $null
    $hottestSensorName = $null
    $temperaturePattern = 'Temperature\{mValue=([-+]?[0-9]+(?:\.[0-9]+)?),\s*mType=[^,]+,\s*mName=([^,}]+),\s*mStatus=(\d+)\}'
    foreach ($match in [regex]::Matches($Text, $temperaturePattern)) {
        $temperature = [double]::Parse($match.Groups[1].Value, [Globalization.CultureInfo]::InvariantCulture)
        $name = $match.Groups[2].Value
        $sensorStatus = [int]::Parse($match.Groups[3].Value, [Globalization.CultureInfo]::InvariantCulture)
        if ($null -eq $maxSensorStatus -or $sensorStatus -gt $maxSensorStatus) {
            $maxSensorStatus = $sensorStatus
        }
        if ($null -eq $maxTemperatureC -or $temperature -gt $maxTemperatureC) {
            $maxTemperatureC = $temperature
            $hottestSensorName = $name
        }
    }

    return [ordered]@{
        status = $thermalStatus
        statusLabel = Thermal-StatusLabel $thermalStatus
        maxSensorStatus = $maxSensorStatus
        maxSensorStatusLabel = Thermal-StatusLabel $maxSensorStatus
        maxTemperatureC = $maxTemperatureC
        hottestSensorName = $hottestSensorName
    }
}

function Get-MaxThermalStatus {
    param([hashtable]$Summary)

    $values = @($Summary.status, $Summary.maxSensorStatus) | Where-Object { $null -ne $_ }
    if ($values.Count -eq 0) {
        return $null
    }
    return ($values | Measure-Object -Maximum).Maximum
}

function Write-Record {
    param(
        [System.Collections.IList]$Records,
        [hashtable]$Summary,
        [int]$ElapsedSeconds,
        [int]$ConsecutiveReadySamples
    )

    $record = [ordered]@{
        observedAt = (Get-Date).ToString("o")
        elapsedSeconds = $ElapsedSeconds
        thermal = $Summary
        consecutiveReadySamples = $ConsecutiveReadySamples
    }
    [void]$Records.Add($record)
    $statusText = if ($null -ne $Summary.status) { "$($Summary.status) ($($Summary.statusLabel))" } else { "unknown" }
    $sensorText = if ($null -ne $Summary.maxSensorStatus) { "$($Summary.maxSensorStatus) ($($Summary.maxSensorStatusLabel))" } else { "unknown" }
    $temperatureText = if ($null -ne $Summary.maxTemperatureC) { "$($Summary.maxTemperatureC) C $($Summary.hottestSensorName)" } else { "unknown" }
    Write-Output "thermal status=$statusText maxSensor=$sensorText hottest=$temperatureText readySamples=$ConsecutiveReadySamples/$RequiredReadySamples elapsed=${ElapsedSeconds}s"
}

if ($RequiredReadySamples -lt 1) {
    throw "RequiredReadySamples must be at least 1."
}
if ($ReadyBelowThermalStatus -lt 0) {
    throw "ReadyBelowThermalStatus must be non-negative."
}
if ($TimeoutSeconds -lt 0) {
    throw "TimeoutSeconds must be non-negative."
}

$adb = Find-Adb
$records = [System.Collections.ArrayList]::new()
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$lastSummary = $null
$exitCode = 2
$consecutiveReadySamples = 0

do {
    $elapsed = [Math]::Max(0, [int]($TimeoutSeconds - [Math]::Ceiling(($deadline - (Get-Date)).TotalSeconds)))
    $thermalText = & $adb shell dumpsys thermalservice
    if ($LASTEXITCODE -ne 0) {
        throw "adb thermalservice query failed with exit code $LASTEXITCODE."
    }
    $lastSummary = Parse-ThermalSummary (($thermalText | Out-String).Trim())

    $maxStatus = Get-MaxThermalStatus $lastSummary
    if ($null -ne $maxStatus -and $maxStatus -lt $ReadyBelowThermalStatus) {
        $consecutiveReadySamples += 1
    } else {
        $consecutiveReadySamples = 0
    }
    Write-Record -Records $records -Summary $lastSummary -ElapsedSeconds $elapsed -ConsecutiveReadySamples $consecutiveReadySamples

    if ($consecutiveReadySamples -ge $RequiredReadySamples) {
        $exitCode = 0
        break
    }

    if ((Get-Date) -ge $deadline) {
        $exitCode = 2
        break
    }
    Start-Sleep -Seconds ([Math]::Max(1, $PollSeconds))
} while ($true)

$result = [ordered]@{
    createdAt = (Get-Date).ToString("o")
    ready = ($exitCode -eq 0)
    readyBelowThermalStatus = $ReadyBelowThermalStatus
    requiredReadySamples = $RequiredReadySamples
    timeoutSeconds = $TimeoutSeconds
    pollSeconds = $PollSeconds
    consecutiveReadySamples = $consecutiveReadySamples
    lastThermal = $lastSummary
    records = $records
}

if (-not [string]::IsNullOrWhiteSpace($OutputPath)) {
    $parent = Split-Path -Parent $OutputPath
    if (-not [string]::IsNullOrWhiteSpace($parent)) {
        New-Item -ItemType Directory -Force -Path $parent | Out-Null
    }
    $result | ConvertTo-Json -Depth 8 | Out-File -LiteralPath $OutputPath -Encoding utf8
}

if ($exitCode -eq 0) {
    Write-Output "Device thermal state is ready below $ReadyBelowThermalStatus for $RequiredReadySamples consecutive sample(s)."
} else {
    Write-Output "Device thermal state did not become ready below $ReadyBelowThermalStatus for $RequiredReadySamples consecutive sample(s) within $TimeoutSeconds seconds."
}
exit $exitCode
