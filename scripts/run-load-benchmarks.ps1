# ==============================================================================
# VyroFlix — Automated k6 Load Benchmark Runner (PowerShell)
# Runs browse.js, search.js, and playback.js via native k6 CLI or Docker.
# ==============================================================================

param (
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Token = "",
    [string]$Suite = "all" # "all", "browse", "search", "playback"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir
$LoadDir = Join-Path $RootDir "tests" | Join-Path -ChildPath "load"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "         VyroFlix k6 Load Benchmark Suite                 " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "[CONFIG] Target Base URL: $BaseUrl" -ForegroundColor Gray
Write-Host "[CONFIG] Suite:           $Suite" -ForegroundColor Gray

# 1. Determine runner: native k6 or Docker
$NativeK6 = Get-Command "k6" -ErrorAction SilentlyContinue
$UseDocker = $false

if ($NativeK6) {
    Write-Host "[INFO] Using native k6 CLI ($NativeK6)" -ForegroundColor Green
} else {
    $DockerCmd = Get-Command "docker" -ErrorAction SilentlyContinue
    if ($DockerCmd) {
        Write-Host "[INFO] Native k6 CLI not found. Using Docker image [grafana/k6:latest]" -ForegroundColor Yellow
        $UseDocker = $true
    } else {
        Write-Host "[ERROR] Neither 'k6' nor 'docker' is installed on PATH." -ForegroundColor Red
        Write-Host "   Install k6 via winget install k6 or start Docker Desktop." -ForegroundColor Red
        exit 1
    }
}

# 2. Identify test files to run
$scriptsToRun = @()
if ($Suite -eq "all" -or $Suite -eq "browse") { $scriptsToRun += "browse.js" }
if ($Suite -eq "all" -or $Suite -eq "search") { $scriptsToRun += "search.js" }
if ($Suite -eq "all" -or $Suite -eq "playback") { $scriptsToRun += "playback.js" }

# 3. Execute each test script
foreach ($script in $scriptsToRun) {
    Write-Host "`n----------------------------------------------------------" -ForegroundColor Cyan
    Write-Host "[RUNNING BENCHMARK] tests/load/$script against $BaseUrl" -ForegroundColor Cyan
    Write-Host "----------------------------------------------------------" -ForegroundColor Cyan

    if ($UseDocker) {
        # Mount tests/load to /tests inside container
        $dockerMount = "${LoadDir}:/tests"
        docker run --rm -i -v $dockerMount `
            -e BASE_URL="$BaseUrl" `
            -e TOKEN="$Token" `
            grafana/k6 run "/tests/$script"
    } else {
        $env:BASE_URL = $BaseUrl
        if ($Token) { $env:TOKEN = $Token }
        Push-Location $LoadDir
        try {
            & k6 run $script
        } finally {
            Pop-Location
        }
    }
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "All specified load benchmark suites completed!" -ForegroundColor Green
Write-Host "Architectural targets: P95 cached read < 300ms, Playback session auth < 500ms." -ForegroundColor Gray
Write-Host "==========================================================" -ForegroundColor Cyan
