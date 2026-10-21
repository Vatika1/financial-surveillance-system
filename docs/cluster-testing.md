# Cluster Testing (EKS + MSK)

[← index](commands.md)

Testing a change on the cluster: deploy it, confirm it is running, send trades to the NLB, check the result in Grafana. For the queries themselves see [observability.md](observability.md); for the same scenarios locally see [local-testing.md](local-testing.md#test-scenarios).

**Rule:** a trade sent to the NLB is checked in Grafana (Loki) or with `kubectl logs`. Local Docker logs never reach Loki.

## 1. Deploy a branch

Pushes to `master` deploy all services automatically. Any other branch (e.g. `incident-drills`) is deployed by hand:

GitHub → **Actions → CI/CD Pipeline → Run workflow** → branch → service → **Run workflow**.

Or from the terminal:

```powershell
gh workflow run ci.yml --ref incident-drills -f service=activity-monitor-service
gh run watch
```

- Use **Run workflow**, not **Re-run** on an old run: Re-run builds the old commit.
- Pick only the service you changed. Maven still builds and tests every module, so a coverage failure in any module blocks the deploy.
- Wait for any running `master` deploy to finish first. The last deploy wins — a master run finishing after yours replaces your service with master's version.
- After a drill, run the pipeline from `master` for the same service to put the clean version back.

## 2. Confirm what is running

```powershell
git rev-parse --short incident-drills
kubectl get deployment activity-monitor-service -o jsonpath="{.spec.template.spec.containers[0].image}"
kubectl rollout status deployment/activity-monitor-service
```

The image tag is the first 7 characters of the commit SHA. It must match before you send anything. Deployment names end in `-service` (`activity-monitor-service`, not `activity-monitor`).

## 3. Send a trade to the cluster

```powershell
$nlb = kubectl get svc trade-ingestion-service -o jsonpath="{.status.loadBalancer.ingress[0].hostname}"
$ts = (Get-Date).ToUniversalTime().AddSeconds(-5).ToString("yyyy-MM-ddTHH:mm:ssZ")
"{`"tradeId`":`"T-EKS-1`",`"advisorId`":`"ADV-TEST`",`"accountId`":`"ACC-1`",`"clientId`":`"CL-1`",`"symbol`":`"AAPL`",`"tradeType`":`"BUY`",`"quantity`":100,`"price`":150.25,`"currency`":`"USD`",`"exchange`":`"NASDAQ`",`"tradeTimestamp`":`"$ts`",`"sourceSystem`":`"MANUAL`",`"sourceSystemId`":`"SRC-1`"}" | Set-Content -Encoding ascii body.json
curl.exe -s -w "`n%{http_code}`n" -X POST "http://$nlb/api/trades" -H "Content-Type: application/json" --data-binary "@body.json"
```

Use a new `tradeId` every time (409 otherwise). The scenario table in [local-testing.md](local-testing.md#test-scenarios) applies unchanged — only the URL differs.

The simulator is off in the `prod` profile, so the cluster only sees your trades. An empty Loki result for the last 15 minutes usually just means no traffic; widen the time range before suspecting Loki.

## 4. Check the result

Grafana: `kubectl port-forward -n monitoring svc/monitoring-grafana 3000:80` → http://localhost:3000 → Explore → Loki. Time range must cover the trade.

| Step | Query | Expect |
|---|---|---|
| Find the trade | `{namespace="default"} \|= "T-EKS-1"` | 2 trade-ingestion lines; copy `correlationId` |
| Follow it | `{namespace="default"} \| json \| correlationId="<id>" \| line_format "{{.app}}  {{.message}}"` | Lines from trade-ingestion, activity-monitor, alert-service (case-management: batch line only) |
| Alert count | `{app="activity-monitor-service"} \|= "<id>" \|= "AlertCreatedEvents" \| json \| line_format "{{.message}}"` | `Published N AlertCreatedEvents` |
| Alerts arrived | `{app="alert-service"} \|= "<id>" \|= "Received batch" \| json \| line_format "{{.pod}}  {{.message}}"` | Batch sizes add up to N |
| Nothing failed | `{namespace="default"} \|= "Sending to DLT"` | No line after your trade's time |

Without Grafana:

```powershell
kubectl logs -l app=activity-monitor-service --since=30m --tail=-1 | Select-String "<correlationId>"
```

`-l app=...` reads every pod of the service; `deploy/<name>` reads only one.

## 5. Kafka on MSK

MSK is not a pod and is only reachable inside the VPC, so run the Kafka CLI from a throwaway pod. Window 1:

```powershell
$b = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String((kubectl get secret msk-secret -o jsonpath="{.data.brokers}")))
kubectl run kafka-client --rm -it --restart=Never --image=apache/kafka:3.7.0 --env="B=$b" -- bash
```

Inside the pod (bash, not PowerShell). Run `kubectl` commands from a second PowerShell window meanwhile; `exit` deletes the pod.

```bash
echo "security.protocol=SSL" > /tmp/c.properties
K=/opt/kafka/bin

# Partition counts — every .DLT must match its source topic
$K/kafka-topics.sh --bootstrap-server $B --command-config /tmp/c.properties --describe | grep PartitionCount

# Raise a short DLT (only increases are allowed; NewTopic beans never alter an existing topic)
$K/kafka-topics.sh --bootstrap-server $B --command-config /tmp/c.properties --alter --topic alerts.created.DLT --partitions 6

# Lag per partition for a consumer group
$K/kafka-consumer-groups.sh --bootstrap-server $B --command-config /tmp/c.properties --describe --group activity-monitor-service

# Read the DLT with headers (exception message + stack trace are in the headers)
$K/kafka-console-consumer.sh --bootstrap-server $B --consumer.config /tmp/c.properties --topic trades.raw.DLT --from-beginning --timeout-ms 15000 --property print.headers=true | grep T-EKS-1
```

Consumer groups: `activity-monitor-service`, `alert-service`, `case-management-service`.

## 6. Health of the observability stack

| Symptom | Check |
|---|---|
| No logs in Loki at all | `kubectl get pods -n monitoring \| Select-String "loki\|fluent"`, then `kubectl logs -l app=<service> --since=15m --tail=3` — empty means no traffic, not a Loki problem |
| Tempo `connection refused` / `trace not found` | `kubectl get pods -n monitoring \| Select-String tempo` — rising RESTARTS = OOMKilled. Persistence is off, so each restart wipes stored traces |
| Why a pod restarted | `kubectl describe pod <pod> -n <ns> \| Select-String "Last State\|Reason\|Exit Code" -Context 0,1` and `kubectl logs <pod> -n <ns> --previous --tail=30` |
| CI failed with `Rule violated ... covered ratio` | JaCoCo gate. Nothing was deployed. Fix coverage (or the module's `jacoco.minimum.coverage` override) and start a new run |
