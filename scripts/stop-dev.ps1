# stop-dev.ps1
# Tears down the ephemeral stack (EKS + MSK + Kafka secret + standalone rule).
# Persistent (RDS, ECR, VPC, DB secret) stays running.

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$ephemeralPath = Join-Path $repoRoot "infra\environments\prod\ephemeral"

Write-Host "`nStopping ephemeral stack (EKS + MSK)..." -ForegroundColor Cyan
Write-Host "Persistent (RDS, ECR, VPC) will remain running." -ForegroundColor Yellow

Set-Location $ephemeralPath
terraform destroy -auto-approve

if ($LASTEXITCODE -ne 0) { Write-Host "Destroy failed" -ForegroundColor Red; exit 1 }

Write-Host "`n=== STOP-DEV COMPLETE ===" -ForegroundColor Green
Write-Host "RDS data and ECR images preserved." -ForegroundColor Yellow