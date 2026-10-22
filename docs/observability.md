# Observability Commands — Grafana, Prometheus, Loki, Tempo

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

Grafana → Explore → Loki datasource → **Code** mode. Cluster logs only — local Docker trades never appear here.

Fluent Bit also ships `kube-system` and `monitoring` logs — filter by `namespace`. Retention is 24 h on `emptyDir`; logs are gone on pod restart or teardown.

Labels you can select on: `app`, `namespace`, `pod`, `job`. Everything else (`level`, `message`, `correlationId`, `logger_name`, `trace_id`, `stack_trace`) is inside the JSON line — add `| json` before filtering or formatting on it.

### Syntax in one minute

| Piece | Meaning |
|---|---|
| `{namespace="default"}` | Stream selector — every query starts with one. `default` = the four services |
| `{app=~"alert-service\|case-management-service"}` | Regex selector: either service |
| `\|= "text"` / `!= "text"` | Line contains / does not contain |
| `\|~ "a\|b"` | Line matches a regex |
| `\| json` | Parse the JSON line into fields |
| `\| level="ERROR"` | Filter on a parsed field (needs `\| json` first) |
| `\| line_format "{{.app}} {{.message}}"` | Show only those fields (needs `\| json` first) |

### The incident walkthrough — five queries, in order

```logql
# 1. Which service is logging errors? (switch the panel to Time series)
sum by (app) (count_over_time({namespace="default"} | json | level="ERROR" [5m]))

# 2. That service's errors, readable
{app="activity-monitor-service"} | json | level="ERROR" | line_format "{{.message}}"

# 3. Find the trade a user reported — copy the correlationId
{namespace="default"} |= "T-EKS-1"

# 4. Follow it through every service — where does it stop?
{namespace="default"} | json | correlationId="<id>" | line_format "{{.app}}  {{.message}}"

# 5. Anything routed to a DLT, with the stack trace
{namespace="default"} |= "Sending to DLT"
```

### Stack traces

The `Sending to DLT` ERROR keeps the whole trace in the `stack_trace` field. The useful line (`Caused by: ...` + the first `at` under it) is near the bottom. In-panel search highlights the match but does not scroll to it — scroll inside the entry, or extract just the cause:

```logql
{app="activity-monitor-service"} |= "Sending to DLT" | json | line_format "{{.stack_trace}}" | regexp "(?s)(?P<cause>Caused by: [^\n]+\n[^\n]+)" | line_format "{{.cause}}"
```

### Per service

```logql
# trade-ingestion: accepted trades and failures
{app="trade-ingestion-service"} |~ "Processing trade|Trade persisted"
{app="trade-ingestion-service"} |= "Trade publish failed"
{app="trade-ingestion-service"} |~ "(?i)idempot"

# activity-monitor: batches, alerts produced, success
{app="activity-monitor-service"} |~ "Received batch|Published|Successfully processed" | json | line_format "{{.pod}}  {{.message}}"

# alert-service: batches for one trade (sizes add up to the alert count)
{app="alert-service"} |= "<id>" |= "Received batch" | json | line_format "{{.pod}}  {{.message}}"

# case-management: case creation (no correlationId on these lines yet — search by text + time)
{app="case-management-service"} |~ "Creating|Published CaseCreatedEvent|Successfully processed" | json | line_format "{{.message}}"
```

`OutboxPoller` only logs when its 5-minute cleanup deletes rows; the 100 ms publish loop logs nothing on success, so `|= "Outbox"` is usually empty.

### Metric queries over logs

Switch the panel from Logs to Time series.

```logql
count_over_time({namespace="default"} |= "Sending to DLT" [5m])                         # DLT sends — alert on > 0
sum by (app) (count_over_time({namespace="default"} | json | level="ERROR" [5m]))      # errors per service
sum(count_over_time({app="trade-ingestion-service"} |= "Trade persisted" [1m]))        # trades accepted per minute
count_over_time({app="activity-monitor-service"} |= "Received batch" |= "<id>" [10m])  # > 1 = retried
```

### Copying results

Explore has no "copy all". Use the Logs panel download icon (right toolbar) → .txt, or Query inspector → Data → Download CSV / JSON → Copy to clipboard. Or skip Grafana: `kubectl logs -l app=<service> --since=1h --tail=-1 | Select-String "<id>" > out.txt`.

## Tempo

Grafana → Explore → Tempo datasource.

| Goal | How |
|---|---|
| Open one trace | **TraceQL** tab → paste the `trace_id` from a Loki line → Run query |
| Browse recent traces | **Search** tab → Service Name → time range → Run query → click a trace ID |
| Jump from a log | Click the `trace_id` field on a Loki line |

Each bar is a span (Kafka poll, SQL statement, Kafka send); width = duration. Click a span for its attributes.

### TraceQL queries

Paste into the **TraceQL** tab. TraceQL is not LogQL — a Loki query pasted here gives `invalid TraceQL query: parse error`. Log text ("Creating case", "Sending to DLT") is in Loki only; Tempo has spans (HTTP request, Kafka poll/send, SQL statements).

```traceql
# Your trades arriving at trade-ingestion, and the outbox publishing them
{ name = "POST /api/trades" || name = "outbox.publishBatch" }

# All traces for one service (swap the name: trade-ingestion-service, activity-monitor-service, alert-service, case-management-service)
{ resource.service.name = "case-management-service" }

# case-management doing real work — publishing cases.created
{ resource.service.name = "case-management-service" && name =~ "cases.created.*" }

# Any span that failed
{ status = error }

# Errors in one service
{ resource.service.name = "activity-monitor-service" && status = error }

# Slow spans (where is the time going?)
{ resource.service.name = "case-management-service" && duration > 100ms }

# Slowest database statements across all services
{ span.db.system = "postgresql" && duration > 50ms }
```

Kafka spans — the Tempo side of the "Published" / "Successfully processed" log lines:

```traceql
# Publish / process spans per topic
{ name =~ "trades.raw.*" }
{ name =~ "alerts.created.*" }
{ name =~ "alerts.persisted.*" }
{ name =~ "cases.created.*" }

# Only publishes (producer side)
{ span.messaging.operation = "publish" }

# Only consumes (consumer side)
{ span.messaging.operation = "process" || span.messaging.operation = "receive" }

# One service publishing to one topic
{ resource.service.name = "activity-monitor-service" && span.messaging.destination.name = "alerts.created" }

# Anything sent to a DLT
{ span.messaging.destination.name =~ ".*\\.DLT" }
```

Span names depend on the agent version, hence the regex. If one returns nothing, open any trade trace and check what the Kafka span is called.

### Noise check (after the OTel Collector)

Services send traces to the OTel Collector (`otel-collector.monitoring.svc.cluster.local:4318`); its `filter/noise` processor drops noisy **root** spans before Tempo. Values: `k8s\monitoring\otel-collector-values.yaml`.

Noise is cut in two places:

| Where | What | Covers |
|---|---|---|
| Deployment env vars | `OTEL_INSTRUMENTATION_SPRING_SCHEDULING_ENABLED`, `_SPRING_DATA_ENABLED`, `_HIBERNATE_ENABLED` = `"false"` | Each service whose deployment YAML has them — must be in all four |
| Collector `filter/noise` | Drops root spans: outbox SELECT, `GET /actuator/*`, `surveillance` | All four services |

```traceql
# Should return nothing recent — outbox poll and health-probe roots are dropped
{ name = "SELECT trade_ingestion.outbox" || name =~ "GET /actuator.*" }

# activity-monitor background noise — should return nothing once filtered
{ resource.service.name = "activity-monitor-service" && (name = "surveillance" || name = "OutcomeTrackingRunnable.run") }
```

`<root span not yet received>` rows are child spans whose root was dropped by the filter — harmless. Removing them needs `tail_sampling` (drops whole traces), not worth the memory.

Batch Kafka listeners start a new trace per poll and only *link* to the producer span, so one trade shows as one trace per service, not one connected timeline. Logs (by correlationId) are the way to follow a trade end to end; traces answer "where is the time going inside this service".

`connection refused` on `tempo...:3200` or `4318` = Tempo is down. `trace not found` for an older trace = Tempo restarted (persistence is off). Check `kubectl get pods -n monitoring | Select-String tempo` for restarts / `OOMKilled`.

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

## Incident metrics (Prometheus)

The `Error rate (5xx)` panel only counts HTTP responses. A Kafka consumer failure returns no HTTP status, so it never shows there — use these instead.

```promql
# Kafka listener failures per service (consumer-side errors). If empty, find the name with {__name__=~"spring_kafka_listener.*"}
sum by (service) (increase(spring_kafka_listener_seconds_count{result="failure"}[5m]))

# Memory leak: heap after GC keeps rising (sawtooth with a climbing floor)
sum by (pod) (jvm_memory_used_bytes{service="activity-monitor-service", area="heap"})
jvm_memory_used_bytes{service="activity-monitor-service", id=~".*Old Gen.*"}

# GC working harder as the heap fills
rate(jvm_gc_pause_seconds_sum[5m])

# Container memory vs its limit (approaching the limit → OOMKilled)
container_memory_working_set_bytes{pod=~"activity-monitor.*"}

# Consumer lag per partition (one partition climbing = poison message or a stuck partition)
sum by (consumergroup, topic, partition) (kafka_consumer_fetch_manager_records_lag)
```
