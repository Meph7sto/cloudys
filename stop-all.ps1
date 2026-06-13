[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$rootDir = $PSScriptRoot
$runDir = Join-Path $rootDir ".run"
$frontendPidFile = Join-Path $runDir "frontend.pid"

function Write-Step {
    param([string]$Message)

    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Stop-ProcessTree {
    param([int]$ProcessId)

    $children = Get-CimInstance Win32_Process -Filter "ParentProcessId = $ProcessId" |
        Select-Object -ExpandProperty ProcessId

    foreach ($childId in $children) {
        Stop-ProcessTree -ProcessId $childId
    }

    try {
        Stop-Process -Id $ProcessId -Force -ErrorAction Stop
    }
    catch {
    }
}

Set-Location $rootDir

if (Test-Path $frontendPidFile) {
    $pidText = (Get-Content $frontendPidFile -Raw).Trim()
    $frontendPid = 0
    if ([int]::TryParse($pidText, [ref]$frontendPid)) {
        Write-Step "Stop frontend process (PID: $frontendPid)"
        Stop-ProcessTree -ProcessId $frontendPid
    }

    Remove-Item $frontendPidFile -Force -ErrorAction SilentlyContinue
}
else {
    Write-Step "No frontend PID file found, skip frontend stop"
}

if (Get-Command "docker" -ErrorAction SilentlyContinue) {
    Write-Step "Stop Docker services"
    docker compose -f docker-compose.yml -f docker-compose.dev.yml down
}
else {
    Write-Warning "Docker command not found, skip docker compose down."
}

Write-Host ""
Write-Host "cloudys services stopped." -ForegroundColor Green
