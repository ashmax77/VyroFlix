# ==============================================================================
# VyroFlix — Local / Remote Transcoding Worker Runner (PowerShell)
# Runs services/encoding-service connecting to Upstash Kafka and Cloudflare R2.
# ==============================================================================

param (
    [string]$Profile = "worker",
    [int]$Port = 8084
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "         VyroFlix Transcoding Worker Launcher             " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Load root .env file if present
$EnvFile = Join-Path $RootDir ".env"
if (Test-Path $EnvFile) {
    Write-Host "[INFO] Loading environment variables from .env" -ForegroundColor Green
    Get-Content $EnvFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $key, $val = $line.Split("=", 2)
            $key = $key.Trim()
            $val = $val.Trim().Trim('"').Trim("'")
            if (-not [System.Environment]::GetEnvironmentVariable($key)) {
                [System.Environment]::SetEnvironmentVariable($key, $val, "Process")
            }
        }
    }
}

# 2. Check FFmpeg installation
$FfmpegCmd = Get-Command "ffmpeg" -ErrorAction SilentlyContinue
if ($FfmpegCmd) {
    $ffmpegVer = (& ffmpeg -version | Select-Object -First 1)
    Write-Host "[INFO] FFmpeg CLI detected: $ffmpegVer" -ForegroundColor Green
} else {
    Write-Host "[WARN] FFmpeg not found on PATH. Falling back to synthetic HLS generator." -ForegroundColor Yellow
    Write-Host "[INFO] To enable real multi-bitrate HLS transcoding, install FFmpeg or winget install Gyan.FFmpeg" -ForegroundColor Gray
}

# 3. Configure environment defaults for worker profile
[System.Environment]::SetEnvironmentVariable("SPRING_PROFILES_ACTIVE", $Profile, "Process")
[System.Environment]::SetEnvironmentVariable("PORT", "$Port", "Process")

Write-Host "[INFO] Starting encoding-service on port $Port with profile [$Profile]..." -ForegroundColor Cyan

$ServicesDir = Join-Path $RootDir "services"
Push-Location $ServicesDir
try {
    mvn spring-boot:run -pl encoding-service -Dspring-boot.run.profiles=$Profile
} finally {
    Pop-Location
}
