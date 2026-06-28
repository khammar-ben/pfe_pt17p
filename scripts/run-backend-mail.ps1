$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root ".env.local"

if (-not (Test-Path $envFile)) {
  Write-Host "Missing .env.local. Create it from .env.example and fill your SMTP/OpenAI values." -ForegroundColor Yellow
  exit 1
}

Get-Content $envFile | ForEach-Object {
  $line = $_.Trim()
  if (-not $line -or $line.StartsWith("#")) {
    return
  }
  $parts = $line.Split("=", 2)
  if ($parts.Count -eq 2) {
    [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), "Process")
  }
}

$port = if ($env:SERVER_PORT) { [int]$env:SERVER_PORT } else { 9090 }
Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | ForEach-Object {
  try {
    Write-Host "Stopping previous backend on port $port (PID $($_.OwningProcess))..." -ForegroundColor Yellow
    Stop-Process -Id $_.OwningProcess -Force -ErrorAction Stop
    Start-Sleep -Seconds 1
  } catch {
    Write-Host "Could not stop PID $($_.OwningProcess). Close the old backend terminal manually if startup fails." -ForegroundColor Yellow
  }
}

Write-Host "Starting PT17 backend on port $port with AI model $env:OPENAI_MODEL..." -ForegroundColor Cyan
mvn.cmd spring-boot:run "-Dspring-boot.run.profiles=mysql"
