# Observability Commands — Grafana, Prometheus, Loki

[← index](commands.md)

## Grafana

| Command | When |
|---|---|
| `kubectl port-forward -n monitoring svc/monitoring-grafana 3000:80` | Open http://localhost:3000. Holds the terminal. |
| Login | `admin` / `prom-operator` (chart default). |
| `kubectl get secret -n monitoring monitoring-grafana -o jsonpath="{.data.admin-password}" \| % { [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($_)) }` | Only if the default password was overridden. |
| `kubectl get configmap -n monitoring trade-surveillance-dashboard --show-labels` | Is the dashboard ConfigMap there with `grafana_dashboard=1`? If not, the sidecar has nothing to load. |

Manually (re)load the dashboard after editing the JSON (start-dev does this on a fresh cluster; CI does not):

```powershell
$dashboardFile = Join-Path $k8sPath "monitoring\trade-surveillance-dashboard.json"
kubectl create configmap trade-surveillance-dashboard --from-file=$dashboardFile --namespace monitoring --dry-run=client -o yaml | kubectl apply -f -
kubectl label configmap trade-surveillance-dashboard grafana_dashboard=1 --namespace monitoring --overwrite
```

Never write `--from-file=(Join-Path ...)` — PowerShell splits it into two arguments.

Re-exporting after editing panels in the UI: dashboard → download icon (right sidebar) → Export as code → toggle "Export the dashboard to use in another instance" → Download file → overwrite `k8s\monitoring\trade-surveillance-dashboard.json` → commit.

Dashboard: "Trade Surveillance System", uid `adgbp2w`, 18 panels.

Reading panels: legend colours repeat past ~8 series — hover the tooltip before attributing a line to a pod.

## Prometheus

| Command | When |
|---|---|
| `kubectl get servicemonitors` | All four should exist with label `release: monitoring`, or Prometheus won't scrape them. |
| `kubectl port-forward -n monitoring svc/monitoring-kube-prometheus-prometheus 9090:9090` | Raw Prometheus UI at http://localhost:9090. Status → Targets shows whether each service is scraped. |

Finding a metric name: Grafana → Explore (compass icon) → Prometheus datasource → PromQL box → `{__name__=~"kafka_consumer.*lag.*"}` → Shift+Enter.

## Actuator metrics (app-level source of truth)

Cluster pod:

```powershell
kubectl port-forward deploy/trade-ingestion-service 8081:8081
(Invoke-WebRequest http://localhost:8081/actuator/prometheus -UseBasicParsing).Content | Select-String trades_ingested
```

Other services: activity-monitor 8082, alert-service 8083, case-management 8084.

All four locally (running in IntelliJ against Compose):

```powershell
8081,8082,8083,8084 | ForEach-Object {
    "--- port $_ ---"
    (Invoke-WebRequest "http://localhost:$_/actuator/prometheus" -UseBasicParsing).Content -split "`n" | Select-String kafka_producer_record_send_total
}
```

Producer metrics appear only after the first `send()` — an idle service legitimately shows nothing.

## Loki

Grafana → Explore → Loki datasource.

```logql
{app="case-management-service"} |= "<correlation-id>"
```

Fluent Bit also ships `kube-system` and `monitoring` logs — filter by `namespace`. Retention is 24 h on `emptyDir`; logs are gone on pod restart or teardown.

## Panel PromQL

```promql
sum by (service) (rate(http_server_requests_seconds_count[1m]))                                  # request rate
sum by (service) (rate(http_server_requests_seconds_count{status=~"5.."}[1m]))                   # 5xx rate
histogram_quantile(0.95, sum by (service, le) (rate(http_server_requests_seconds_bucket[5m])))   # p95
sum by (consumergroup, topic) (kafka_consumer_fetch_manager_records_lag)                          # consumer lag
sum by (service) (jvm_memory_used_bytes{area="heap"})                                             # heap
hikaricp_connections_active                                                                       # DB pool active
hikaricp_connections_pending                                                                      # DB pool pending
sum(rate(trades_ingested_total[1m]))                                                              # custom counter
```

The newer panels (produced/consumed/sec, CPU, memory, restarts, GC, RDS) — copy their queries from the dashboard JSON rather than retyping.
