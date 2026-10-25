param(
    [string]$TradeDate = (Get-Date -Format "yyyy-MM-dd")
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

$csv = docker exec surveillance-postgres psql -U surveillance -d surveillance -c $sql
[System.IO.File]::WriteAllText($outFile, ($csv -join "`n") + "`n")

Write-Host "Booking feed written: $outFile"