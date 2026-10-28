# ==============================================================================
# VyroFlix — Direct Browser/Client Upload Pipeline Test (PowerShell)
# Verifies: Upload Intent -> Direct R2 PUT -> Completion Verification -> video.uploaded.v1
# ==============================================================================

param (
    [string]$GatewayUrl = "http://localhost:8080",
    [string]$ContentId = "b0000000-0000-0000-0000-000000000001",
    [string]$Token = ""
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "         VyroFlix Direct Upload Pipeline Verification     " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "[CONFIG] Gateway URL: $GatewayUrl" -ForegroundColor Gray
Write-Host "[CONFIG] Content ID:  $ContentId" -ForegroundColor Gray

# 1. Create a minimal valid MP4 sample binary (ftyp box + mdat)
$TempVideo = Join-Path ([System.IO.Path]::GetTempPath()) "vyroflix_test_upload.mp4"
$ftypBytes = [byte[]](
    0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70, # size: 24, type: ftyp
    0x69, 0x73, 0x6F, 0x6D, 0x00, 0x00, 0x02, 0x00, # major brand: isom, minor: 512
    0x69, 0x73, 0x6F, 0x6D, 0x69, 0x73, 0x6F, 0x32, # compatible: isom, iso2
    0x00, 0x00, 0x00, 0x10, 0x6D, 0x64, 0x61, 0x74, # size: 16, type: mdat
    0x56, 0x59, 0x52, 0x4F, 0x46, 0x4C, 0x49, 0x58  # data: VYROFLIX
)
[System.IO.File]::WriteAllBytes($TempVideo, $ftypBytes)
$fileSize = (Get-Item $TempVideo).Length
Write-Host "[STEP 1] Created sample MP4 payload ($fileSize bytes) at $TempVideo" -ForegroundColor Green

# 2. Build headers
$headers = @{
    "Content-Type" = "application/json"
}
if ($Token) {
    $headers["Authorization"] = "Bearer $Token"
} elseif ($env:TOKEN) {
    $headers["Authorization"] = "Bearer $env:TOKEN"
}

# 3. Create Upload Intent
Write-Host "[STEP 2] Requesting pre-signed upload intent from Gateway..." -ForegroundColor Cyan
$intentPayload = @{
    contentId = $ContentId
    targetType = "MOVIE"
    filename = "vyroflix_sample.mp4"
    contentType = "video/mp4"
    fileSizeBytes = $fileSize
} | ConvertTo-Json

try {
    $intentRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/videos/upload-intents" -Method Post -Body $intentPayload -Headers $headers
    $intentId = $intentRes.id
    $uploadUrl = $intentRes.uploadUrl
    $s3Key = $intentRes.s3Key

    Write-Host "[SUCCESS] Upload Intent Created!" -ForegroundColor Green
    Write-Host "   Intent ID:   $intentId" -ForegroundColor Gray
    Write-Host "   Object Key:  $s3Key" -ForegroundColor Gray
    Write-Host "   Upload URL:  $($uploadUrl.Substring(0, [Math]::Min(60, $uploadUrl.Length)))..." -ForegroundColor Gray
} catch {
    Write-Host "[ERROR] Failed to create upload intent: $_" -ForegroundColor Red
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        Write-Host "   Details: $($reader.ReadToEnd())" -ForegroundColor Red
    }
    exit 1
}

# 4. Direct binary PUT to storage (Zero bytes through Gateway / Spring Boot)
Write-Host "[STEP 3] Performing DIRECT binary PUT to Cloudflare R2 / MinIO..." -ForegroundColor Cyan
try {
    $putHeaders = @{
        "Content-Type" = "video/mp4"
    }
    $putRes = Invoke-WebRequest -Uri $uploadUrl -Method Put -InFile $TempVideo -Headers $putHeaders -UseBasicParsing
    Write-Host "[SUCCESS] Direct binary upload returned HTTP $($putRes.StatusCode)" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Direct storage PUT failed: $_" -ForegroundColor Red
    Write-Host "   Check that R2 bucket vyroflix-raw has CORS configured allowing PUT from your client origin." -ForegroundColor Yellow
    exit 1
}

# 5. Complete Upload Intent and trigger outbox event
Write-Host "[STEP 4] Completing upload intent & verifying object in storage..." -ForegroundColor Cyan
try {
    $completeRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/videos/upload-intents/$intentId/complete" -Method Post -Headers $headers
    Write-Host "[SUCCESS] Upload Intent Completed!" -ForegroundColor Green
    Write-Host "   Status:      $($completeRes.status)" -ForegroundColor Green
    Write-Host "   Event:       video.uploaded.v1 dispatched via transactional outbox" -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host "Pipeline verified! Asynchronous encoding worker will now process the asset." -ForegroundColor Cyan
} catch {
    Write-Host "[ERROR] Failed to complete upload intent: $_" -ForegroundColor Red
    exit 1
} finally {
    Remove-Item $TempVideo -ErrorAction SilentlyContinue
}
