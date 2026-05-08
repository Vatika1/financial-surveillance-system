# start-dev.ps1
# Spins up the ephemeral stack (EKS + MSK), recreates K8s Secrets, deploys services.
# Works around the fact that K8s Secrets die with the cluster and need recreating from AWS Secrets Manager.

$ErrorActionPreference = "Stop"

# Resolve repo paths
$repoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$ephemeralPath = Join-Path $repoRoot "infra\environments\prod\ephemeral"
$setupScriptsPath = Join-Path $repoRoot "setup-scripts"
$k8sPath = Join-Path $repoRoot "k8s"

# ===== STEP 1: Apply ephemeral Terraform =====
Write-Host "`n[1/5] Applying ephemeral Terraform (EKS + MSK)..." -ForegroundColor Cyan
Set-Location $ephemeralPath
terraform apply -auto-approve
if ($LASTEXITCODE -ne 0) { Write-Host "Terraform apply failed" -ForegroundColor Red; exit 1 }

# ===== STEP 2: Update kubeconfig =====
Write-Host "`n[2/5] Updating kubeconfig..." -ForegroundColor Cyan
aws eks update-kubeconfig --name surveillance-prod --region us-east-1
if ($LASTEXITCODE -ne 0) { Write-Host "kubeconfig update failed" -ForegroundColor Red; exit 1 }

# ===== STEP 3: Apply cluster-level setup (aws-auth, RBAC) =====
Write-Host "`n[3/5] Applying cluster permissions (aws-auth, RBAC)..." -ForegroundColor Cyan
kubectl apply -f (Join-Path $setupScriptsPath "aws-auth.yaml")
kubectl apply -f (Join-Path $setupScriptsPath "github-actions-role.yaml")
kubectl apply -f (Join-Path $setupScriptsPath "github-actions-binding.yaml")

# ===== STEP 4: Recreate K8s Secrets from AWS Secrets Manager =====
Write-Host "`n[4/5] Recreating K8s Secret 'db-secret' from AWS Secrets Manager..." -ForegroundColor Cyan

# Find the RDS-managed password secret ARN
$rdsSecretArn = aws rds describe-db-instances `
  --db-instance-identifier surveillance-prod-db `
  --region us-east-1 `
  --query "DBInstances[0].MasterUserSecret.SecretArn" `
  --output text

if (-not $rdsSecretArn) { Write-Host "Could not find RDS master user secret" -ForegroundColor Red; exit 1 }

# Fetch the password
$secretJson = aws secretsmanager get-secret-value `
  --secret-id $rdsSecretArn `
  --region us-east-1 `
  --query SecretString `
  --output text

$dbPassword = ($secretJson | ConvertFrom-Json).password

# Create or update the K8s Secret (idempotent — works whether it exists or not)
kubectl create secret generic db-secret `
  --from-literal=password=$dbPassword `
  --dry-run=client -o yaml | kubectl apply -f -

if ($LASTEXITCODE -ne 0) { Write-Host "Failed to create db-secret" -ForegroundColor Red; exit 1 }

# ===== STEP 5: Deploy services =====
Write-Host "`n[5/5] Deploying services..." -ForegroundColor Cyan
kubectl apply -f (Join-Path $k8sPath "trade-ingestion")
kubectl apply -f (Join-Path $k8sPath "activity-monitor")
kubectl apply -f (Join-Path $k8sPath "alert-service")
kubectl apply -f (Join-Path $k8sPath "case-management")

# Wait for pods to become ready (polling)
Write-Host "`nWaiting for pods to become ready..." -ForegroundColor Cyan
$timeout = 180
$elapsed = 0
$allReady = $false
while ($elapsed -lt $timeout) {
    $notRunning = kubectl get pods --field-selector=status.phase!=Running -o name
    if ([string]::IsNullOrWhiteSpace($notRunning)) {
        Write-Host "All pods running." -ForegroundColor Green
        $allReady = $true
        break
    }
    Write-Host "  Still waiting... ($elapsed sec elapsed)" -ForegroundColor DarkGray
    Start-Sleep -Seconds 10
    $elapsed += 10
}

if (-not $allReady) {
    Write-Host "Timed out waiting for all pods. Check status manually." -ForegroundColor Yellow
}

Write-Host "`n=== START-DEV COMPLETE ===" -ForegroundColor Green
Write-Host "Cluster ready. Pod status:" -ForegroundColor Yellow
kubectl get pods