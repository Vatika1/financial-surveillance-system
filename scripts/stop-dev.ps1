
# stop-dev.ps1
# Tears down the ephemeral stack (EKS + MSK). Persistent (RDS, ECR, VPC, DB secret) stays running.
# IMPORTANT: deletes K8s services BEFORE terraform destroy so the LoadBalancer controller
# cleans up its cloud LB. Skipping this orphans the trade-ingestion LB every cycle.

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$ephemeralPath = Join-Path $repoRoot "infra\environments\prod\ephemeral"
$k8sPath = Join-Path $repoRoot "k8s"

# ===== STEP 1: Delete K8s services FIRST (while the cluster is still alive) =====
# The LoadBalancer service has a cleanup finalizer, so 'kubectl delete' blocks until the
# controller has actually deleted the cloud load balancer. That's what prevents orphans.
Write-Host "`n[1/2] Deleting K8s services so the load balancer is cleaned up..." -ForegroundColor Cyan

aws eks update-kubeconfig --name surveillance-prod --region us-east-1 2>$null
if ($LASTEXITCODE -eq 0) {
    foreach ($svc in @("case-management", "alert-service", "activity-monitor", "trade-ingestion")) {
        kubectl delete -f (Join-Path $k8sPath $svc) --ignore-not-found --timeout=180s
        if ($LASTEXITCODE -ne 0) {
            Write-Host "  Warning: '$svc' did not delete cleanly. Check for a leftover LB after destroy." -ForegroundColor Yellow
        }
    }
    Write-Host "  Services deleted." -ForegroundColor Green
} else {
    Write-Host "  Cluster not reachable (already destroyed?). Skipping kubectl delete." -ForegroundColor Yellow
}

# ===== STEP 2: Destroy the ephemeral Terraform stack =====
Write-Host "`n[2/2] Destroying ephemeral stack (EKS + MSK)..." -ForegroundColor Cyan
Write-Host "Persistent (RDS, ECR, VPC) will remain running." -ForegroundColor Yellow
Set-Location $ephemeralPath
terraform destroy -auto-approve
if ($LASTEXITCODE -ne 0) { Write-Host "Destroy failed" -ForegroundColor Red; exit 1 }

# ===== Sanity check: confirm no orphaned Classic LBs remain =====
Write-Host "`nChecking for leftover load balancers..." -ForegroundColor Cyan
$leftover = aws elb describe-load-balancers --query "LoadBalancerDescriptions[].LoadBalancerName" --output text 2>$null
if ([string]::IsNullOrWhiteSpace($leftover)) {
    Write-Host "  No Classic load balancers remaining. Clean." -ForegroundColor Green
} else {
    Write-Host "  WARNING: load balancer(s) still present: $leftover" -ForegroundColor Red
    Write-Host "  Delete with: aws elb delete-load-balancer --load-balancer-name <name>" -ForegroundColor Yellow
}

Write-Host "`n=== STOP-DEV COMPLETE ===" -ForegroundColor Green
Write-Host "RDS data and ECR images preserved." -ForegroundColor Yellow