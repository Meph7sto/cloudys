[CmdletBinding()]
param(
    [switch]$SkipBackendBuild,
    [switch]$SkipFrontend
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$rootDir = $PSScriptRoot
$runDir = Join-Path $rootDir ".run"
$frontendDir = Join-Path $rootDir "frontend"
$frontendPidFile = Join-Path $runDir "frontend.pid"
$frontendLogFile = Join-Path $runDir "frontend.log"
$frontendErrFile = Join-Path $runDir "frontend.err.log"
$semanticAtlasBackendDir = Join-Path $rootDir "..\Semantic-Atlas\backend_inference"

function Get-EnvInt {
    param(
        [string]$Name,
        [int]$Default
    )

    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) {
        return $Default
    }

    $parsed = 0
    if ([int]::TryParse($value, [ref]$parsed)) {
        return $parsed
    }

    throw "Environment variable '$Name' must be an integer."
}

$gatewayHostPort = Get-EnvInt -Name "HOST_GATEWAY_PORT" -Default 25698
$frontendHostPort = Get-EnvInt -Name "HOST_FRONTEND_PORT" -Default 25699
$eurekaHostPort = Get-EnvInt -Name "HOST_EUREKA_PORT" -Default 25701
$adminerHostPort = Get-EnvInt -Name "HOST_ADMINER_PORT" -Default 25708
$composeFiles = @("-f", "docker-compose.yml", "-f", "docker-compose.dev.yml")

function Write-Step {
    param([string]$Message)

    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Assert-Command {
    param(
        [string]$Name,
        [string]$Hint
    )

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command '$Name' was not found. $Hint"
    }
}

function Invoke-Checked {
    param(
        [scriptblock]$Command,
        [string]$FailureMessage
    )

    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw $FailureMessage
    }
}

function Get-TrackedFrontendProcess {
    if (-not (Test-Path $frontendPidFile)) {
        return $null
    }

    $pidText = (Get-Content $frontendPidFile -Raw).Trim()
    if ([string]::IsNullOrWhiteSpace($pidText)) {
        Remove-Item $frontendPidFile -Force -ErrorAction SilentlyContinue
        return $null
    }

    $frontendPid = 0
    if (-not [int]::TryParse($pidText, [ref]$frontendPid)) {
        Remove-Item $frontendPidFile -Force -ErrorAction SilentlyContinue
        return $null
    }

    try {
        return Get-Process -Id $frontendPid -ErrorAction Stop
    }
    catch {
        Remove-Item $frontendPidFile -Force -ErrorAction SilentlyContinue
        return $null
    }
}

function Wait-HttpReady {
    param(
        [string]$Url,
        [string]$Label,
        [int]$TimeoutSeconds = 180
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
                return $true
            }
        }
        catch {
        }

        Start-Sleep -Seconds 3
    }

    return $false
}

New-Item -ItemType Directory -Path $runDir -Force | Out-Null
Set-Location $rootDir

Write-Step "Check local prerequisites"
Assert-Command -Name "docker" -Hint "Install and start Docker Desktop first."
Assert-Command -Name "mvn" -Hint "Install Maven 3.9+ and add it to PATH first."
if (-not $SkipFrontend) {
    Assert-Command -Name "npm" -Hint "Install Node.js/npm and add it to PATH first."
}

Write-Step "Check local prerequisites"
Write-Step "Check Docker status"
Invoke-Checked -FailureMessage "Docker is not available. Make sure Docker Desktop is running." -Command {
    docker info | Out-Null
}

Write-Step "Check Python sidecar build directory"
if (-not (Test-Path $semanticAtlasBackendDir)) {
    throw "Missing directory '$semanticAtlasBackendDir'. Current docker-compose needs it to build python-sidecar."
}

if (-not $SkipBackendBuild) {
    Write-Step "Build backend JARs"
    Invoke-Checked -FailureMessage "Maven build failed." -Command {
        mvn clean package "-DskipTests"
    }
}
else {
    Write-Step "Skip backend build"
}

if (-not $SkipFrontend) {
    $nodeModulesDir = Join-Path $frontendDir "node_modules"
    $packageLockFile = Join-Path $frontendDir "package-lock.json"

    if (-not (Test-Path $nodeModulesDir)) {
        Write-Step "Install frontend dependencies"
        Push-Location $frontendDir
        try {
            if (Test-Path $packageLockFile) {
                Invoke-Checked -FailureMessage "Frontend dependency install failed." -Command {
                    npm ci
                }
            }
            else {
                Invoke-Checked -FailureMessage "Frontend dependency install failed." -Command {
                    npm install
                }
            }
        }
        finally {
            Pop-Location
        }
    }
}

Write-Step "Start Docker services"
Invoke-Checked -FailureMessage "docker compose up failed." -Command {
    docker compose @composeFiles up -d --build
}

if (-not $SkipFrontend) {
    $frontendProcess = Get-TrackedFrontendProcess
    if ($null -ne $frontendProcess) {
        Write-Step "Frontend is already running, skip duplicate start (PID: $($frontendProcess.Id))"
    }
    else {
        Write-Step "Start frontend Vite in background"
        if (Test-Path $frontendLogFile) {
            Remove-Item $frontendLogFile -Force -ErrorAction SilentlyContinue
        }
        if (Test-Path $frontendErrFile) {
            Remove-Item $frontendErrFile -Force -ErrorAction SilentlyContinue
        }

        $npmCommand = Get-Command "npm.cmd" -ErrorAction SilentlyContinue
        if ($null -eq $npmCommand) {
            $npmCommand = Get-Command "npm" -ErrorAction Stop
        }

        $previousViteDevServerPort = $env:VITE_DEV_SERVER_PORT
        $previousVitePublicPort = $env:VITE_PUBLIC_PORT
        $previousViteGatewayTarget = $env:VITE_GATEWAY_TARGET
        $env:VITE_DEV_SERVER_PORT = "$frontendHostPort"
        $env:VITE_PUBLIC_PORT = "$frontendHostPort"
        $env:VITE_GATEWAY_TARGET = "http://localhost:$gatewayHostPort"

        try {
            $frontendProcess = Start-Process `
                -FilePath $npmCommand.Source `
                -ArgumentList @("run", "dev") `
                -WorkingDirectory $frontendDir `
                -WindowStyle Hidden `
                -RedirectStandardOutput $frontendLogFile `
                -RedirectStandardError $frontendErrFile `
                -PassThru
        }
        finally {
            $env:VITE_DEV_SERVER_PORT = $previousViteDevServerPort
            $env:VITE_PUBLIC_PORT = $previousVitePublicPort
            $env:VITE_GATEWAY_TARGET = $previousViteGatewayTarget
        }

        Set-Content -Path $frontendPidFile -Value $frontendProcess.Id -Encoding ascii
    }
}

Write-Step "Wait for gateway health check"
$gatewayReady = Wait-HttpReady -Url "http://localhost:$gatewayHostPort/actuator/health" -Label "gateway-service"
if (-not $gatewayReady) {
    Write-Warning 'gateway-service did not become healthy in time. Run "docker compose ps" to inspect containers.'
}

if (-not $SkipFrontend) {
    Write-Step "Wait for frontend dev server"
    $frontendReady = Wait-HttpReady -Url "http://localhost:$frontendHostPort" -Label "frontend"
    if (-not $frontendReady) {
        Write-Warning "Frontend did not become ready in time. Check log: $frontendLogFile"
    }
}

Write-Host ""
Write-Host "Startup complete." -ForegroundColor Green
Write-Host "Gateway:   http://localhost:$gatewayHostPort"
Write-Host "Eureka:   http://localhost:$eurekaHostPort"
Write-Host "Adminer:  http://localhost:$adminerHostPort"
if (-not $SkipFrontend) {
    Write-Host "Frontend:  http://localhost:$frontendHostPort"
    Write-Host "FE log:    $frontendLogFile"
}
Write-Host "Stop with: .\stop-all.ps1"

