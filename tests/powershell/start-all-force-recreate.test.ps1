$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$global:recordedDockerArgs = [System.Collections.Generic.List[string]]::new()
$global:recordedWebRequests = [System.Collections.Generic.List[string]]::new()

function docker {
    param(
        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]]$Args
    )

    $global:recordedDockerArgs.Add(($Args -join " "))
    $global:LASTEXITCODE = 0
}

function mvn {
    param(
        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]]$Args
    )

    $global:LASTEXITCODE = 0
}

function Invoke-WebRequest {
    param(
        [string]$Uri,
        [switch]$UseBasicParsing,
        [int]$TimeoutSec
    )

    $global:recordedWebRequests.Add($Uri)
    return [pscustomobject]@{ StatusCode = 200 }
}

& (Join-Path $repoRoot "start-all.ps1") -SkipBackendBuild -SkipFrontend

$composeUpInvocation = $global:recordedDockerArgs | Where-Object { $_ -like "compose * up *" } | Select-Object -Last 1
if (-not $composeUpInvocation) {
    throw "Expected start-all.ps1 to invoke 'docker compose ... up'."
}

if ($composeUpInvocation -notmatch '(^| )--force-recreate( |$)') {
    throw "Expected docker compose up to include --force-recreate, but got: $composeUpInvocation"
}

$gatewayHealthProbe = $global:recordedWebRequests | Where-Object { $_ -like "http://127.0.0.1:25698/actuator/health*" } | Select-Object -Last 1
if ($gatewayHealthProbe -ne "http://127.0.0.1:25698/actuator/health/liveness") {
    throw "Expected gateway readiness probe to use /actuator/health/liveness, but got: $gatewayHealthProbe"
}
