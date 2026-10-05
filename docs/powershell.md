# PowerShell Gotchas

[← index](commands.md)

- `curl` is `Invoke-WebRequest` and prompts for input — always use `Invoke-WebRequest ... -UseBasicParsing`.
- `kubectl get ... -o yaml | Select-String` mangles output. Use `-o jsonpath` for one field.
- `--flag=(expression)` breaks: PowerShell splits it into two arguments. Assign to a `$variable` first, then `--flag=$variable`.
- Backtick at line end = continuation. No trailing space after it.
- AWS CLI `--output text` + a paginated call = one result per page, joined by a tab. Use `--output json | ConvertFrom-Json` when you need a single value.
- `gh` is not installed — check CI at https://github.com/Vatika1/financial-surveillance-system/actions.

## HTTP requests (curl equivalents)

Send one trade — local (Compose / IntelliJ):

```powershell
$cid = [guid]::NewGuid().ToString()
$body = @{
    tradeId = "TRD-LOCAL-" + (Get-Date -Format "HHmmss")
    advisorId = "ADV-001"; accountId = "ACC-001"; clientId = "CLI-001"
    symbol = "AAPL"; tradeType = "BUY"; quantity = 100; price = 150.00
    currency = "USD"; exchange = "NYSE"
    tradeTimestamp = (Get-Date).ToUniversalTime().AddMinutes(-5).ToString("yyyy-MM-ddTHH:mm:ssZ")
    sourceSystem = "ETRADE"; sourceSystemId = "SRC-001"
} | ConvertTo-Json
(Invoke-WebRequest -Uri "http://localhost:8081/api/trades" -Method POST -Body $body -ContentType "application/json" -Headers @{ "X-Correlation-Id" = $cid } -UseBasicParsing).StatusCode
```

Send one trade — cluster (NLB, port 80):

```powershell
$nlb = kubectl get svc trade-ingestion-service -o jsonpath="{.status.loadBalancer.ingress[0].hostname}"
$cid = [guid]::NewGuid().ToString()
$body = @{
    tradeId = "TRD-CID-" + (Get-Date -Format "HHmmss")
    advisorId = "ADV-001"; accountId = "ACC-001"; clientId = "CLI-001"
    symbol = "AAPL"; tradeType = "BUY"; quantity = 100; price = 150.00
    currency = "USD"; exchange = "NYSE"
    tradeTimestamp = (Get-Date).ToUniversalTime().AddMinutes(-5).ToString("yyyy-MM-ddTHH:mm:ssZ")
    sourceSystem = "ETRADE"; sourceSystemId = "SRC-001"
} | ConvertTo-Json
(Invoke-WebRequest -Uri "http://$nlb/api/trades" -Method POST -Body $body -ContentType "application/json" -Headers @{ "X-Correlation-Id" = $cid } -UseBasicParsing).StatusCode
$cid   # search this in Loki
```

Health check (local; for the cluster, port-forward first — see [kubectl.md](kubectl.md)):

```powershell
(Invoke-WebRequest http://localhost:8081/actuator/health -UseBasicParsing).Content
```

One metric from the actuator:

```powershell
(Invoke-WebRequest http://localhost:8081/actuator/metrics/hikaricp.connections.max -UseBasicParsing).Content
```

Prometheus text endpoint, filtered:

```powershell
(Invoke-WebRequest http://localhost:8081/actuator/prometheus -UseBasicParsing).Content -split "`n" | Select-String kafka_producer_record_send_total
```

Ports: trade-ingestion 8081, activity-monitor 8082, alert-service 8083, case-management 8084.

Real curl (skips PowerShell's alias):

```powershell
curl.exe http://localhost:8081/actuator/health
```

## Search files

```powershell
# Find a string in every .tf file under infra
Get-ChildItem -Path C:\Users\vatik\dev\Backend\financial-surveillance-system\infra -Recurse -Filter *.tf | Select-String "aws_ecr_repository"
```

`Get-ChildItem -Recurse -Filter` lists the files; `Select-String` prints matching lines with file and line number. Change the path, filter (`*.yaml`, `*.java`) and string as needed.
