param(
    [string]$TradeDate = (Get-Date -Format "yyyy-MM-dd"),
    [ValidateSet("local", "eks")]
    [string]$Target = "local"
)

$outDir  = Join-Path $PSScriptRoot "out"
$outFile = Join-Path $outDir "booking_totals_$TradeDate.csv"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$sql = @"
COPY (
  SELECT advisor_id,
         COUNT(*)              AS trade_count,
         SUM(quantity * price) AS total_value
  FROM trade_ingestion.trades
  WHERE (trade_timestamp AT TIME ZONE 'America/New_York')::date = DATE '$TradeDate'
  GROUP BY advisor_id
  ORDER BY advisor_id
) TO STDOUT WITH CSV HEADER
"@
$sql = $sql -replace "\r?\n", " "

if ($Target -eq "local") {
    $csv = docker exec surveillance-postgres psql -U surveillance -d surveillance -c $sql
} else {
    $rdsHost = "surveillance-prod-db.ccfkou6gmo5z.us-east-1.rds.amazonaws.com"
    $pwB64   = kubectl get secret db-secret -o jsonpath="{.data.password}"
    $pw      = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($pwB64))
    kubectl delete pod booking-feed-gen --ignore-not-found | Out-Null
    kubectl run booking-feed-gen --restart=Never --image=postgres:15 `
        --env="PGPASSWORD=$pw" -- psql -h $rdsHost -U surveillance -d surveillance -c $sql | Out-Null
    kubectl wait --for=jsonpath='{.status.phase}'=Succeeded pod/booking-feed-gen --timeout=120s | Out-Null
    $csv = kubectl logs booking-feed-gen
    kubectl delete pod booking-feed-gen | Out-Null
}

[System.IO.File]::WriteAllText($outFile, ($csv -join "`n") + "`n")
Write-Host "Booking feed written: $outFile"

if ($Target -eq "eks") {
    kubectl create configmap booking-feed --from-file=$outFile --dry-run=client -o yaml | kubectl apply -f -
    Write-Host "Booking feed delivered to EKS as ConfigMap 'booking-feed'"
}