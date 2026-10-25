param(
    [string]$TradeDate = (Get-Date -Format "yyyy-MM-dd"),
    [int]$Advisors = 15000
)

$last = $Advisors - 1
$adv  = "'ADV-' || CASE WHEN g < 10000 THEN lpad(g::text, 4, '0') ELSE g::text END"

$sql = @"
INSERT INTO trade_ingestion.trades
  (trade_id, advisor_id, account_id, client_id, symbol, trade_type, quantity, price, total_value,
   currency, exchange, trade_timestamp, source_system, source_system_id, status)
SELECT 'T-SEED-$TradeDate-' || g, $adv, 'ACC-SEED', 'CLI-SEED', 'MSFT', 'BUY', 100, 150, 15000,
       'USD', 'NYSE', (DATE '$TradeDate' + TIME '10:00') AT TIME ZONE 'America/New_York',
       'SEED', 'SEED-' || g, 'RECEIVED'
FROM generate_series(0, $last) g
ON CONFLICT (trade_id) DO NOTHING;

INSERT INTO activity_monitor.advisor_daily_activity (advisor_id, trade_date, trade_count, total_value)
SELECT $adv, DATE '$TradeDate', 1, 15000
FROM generate_series(0, $last) g
ON CONFLICT (advisor_id, trade_date) DO NOTHING;
"@
$sql = $sql -replace "\r?\n", " "

$rdsHost = "surveillance-prod-db.ccfkou6gmo5z.us-east-1.rds.amazonaws.com"
$pw = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String(
        (kubectl get secret db-secret -o jsonpath="{.data.password}")))

kubectl delete pod month-end-seed --ignore-not-found | Out-Null
kubectl run month-end-seed --restart=Never --image=postgres:15 `
    --env="PGPASSWORD=$pw" -- psql -h $rdsHost -U surveillance -d surveillance -c $sql | Out-Null
kubectl wait --for=jsonpath='{.status.phase}'=Succeeded pod/month-end-seed --timeout=180s | Out-Null
kubectl logs month-end-seed
kubectl delete pod month-end-seed | Out-Null