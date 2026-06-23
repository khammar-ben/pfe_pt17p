$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root ".env.local"

if (-not (Test-Path $envFile)) {
  Write-Host "Missing .env.local. Create it from .env.example and fill your SMTP values." -ForegroundColor Yellow
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

mvn.cmd spring-boot:run "-Dspring-boot.run.profiles=mysql"
