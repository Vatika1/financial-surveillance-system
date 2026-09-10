# Command Reference — Financial Surveillance System

Grouped by task. `<name>` means substitute your value. Everything is PowerShell unless noted.
Port-forwards hold the terminal — open a second one for anything else.

---

## 1. Bring the stack up / tear it down

Run from the repo root. RDS and ECR are persistent and survive `stop-dev`; everything else is rebuilt each time.

| Command | When |
|---|---|
| `.\scripts\start-dev.ps1` | Start of a session. Applies ephemeral Terraform (EKS + MSK), recreates secrets, installs kube-prometheus-stack, provisions the Grafana dashboard, deploys the four services at the current commit SHA. ~25 min. |
| `.\scripts\stop-dev.ps1` | End of a session. Destroys EKS/MSK/NAT, checks for leftover load balancers. **Always run this — EKS + MSK + NAT bill while idle.** |
| `git -C . rev-parse --short HEAD` | The image tag `start-dev` will deploy. CI must have already built and pushed this SHA to ECR or pods go `ImagePullBackOff`. |

**Constraint:** `start-dev.ps1` only works from a commit CI has already built. Push, wait for green, then start.

---

## 2. Is everything running?

| Command | When |
|---|---|
| `kubectl get pods` | App pods. `1/1 Running` is healthy. `0/1 Running` for the first ~60s is normal (Spring Boot + Kafka connect). |
| `kubectl get pods -n monitoring` | The 7 monitoring pods (Prometheus, Grafana, Alertmanager, operator, kube-state-metrics, 2× node-exporter). |
| `kubectl get pods -o custom-columns=NAME:.metadata.name,IMAGE:.spec.containers[0].image` | Which image tag each pod is actually running. First thing to check when behaviour doesn't match the code. |
| `kubectl get deployments` | Desired vs available replicas. |
| `kubectl get services` | ClusterIP / LoadBalancer addresses. trade-ingestion is the only `LoadBalancer`; its `EXTERNAL-IP` is the NLB hostname. |
| `kubectl describe pod -l app=<service>` | Events, probe failures, image pull errors, OOM kills. Read this before reading logs when a pod won't start. |
| `kubectl get pods -n monitoring -o custom-columns=POD:.metadata.name,CONTAINERS:.spec.containers[*].name` | Which containers live in each monitoring pod (Grafana has three: `grafana`, `grafana-sc-dashboard`, `grafana-sc-datasources`). |

---

## 3. Logs

| Command | When |
|---|---|
| `kubectl logs deploy/<service> --tail=100` | Quick look at recent output. |
| `kubectl logs deploy/<service> -f` | Live stream. Ctrl+C to stop. |
| `kubectl logs deploy/<service> --since=1h \| Select-String "ERROR\|Exception"` | Only the errors from the last hour. This is how the bot-traffic / 404-as-500 bug was found. |
| `kubectl logs deploy/<service> > logs.txt` | Dump to file for offline grepping. |
| `kubectl logs -n monitoring deploy/monitoring-grafana -c grafana-sc-dashboard \| Select-String "trade-surveillance"` | Did the sidecar load the dashboard ConfigMap? Want `Writing /tmp/dashboards/...` and `Dashboards config reloaded`. |
| `kubectl logs -n monitoring prometheus-monitoring-kube-prometheus-prometheus-0 -c prometheus --tail=50` | Prometheus itself — scrape errors, config reload failures. |

---

## 4. Grafana

| Command | When |
|---|---|
| `kubectl port-forward -n monitoring svc/monitoring-grafana 3000:80` | Open http://localhost:3000. Holds the terminal. |
| Login | `admin` / `prom-operator` (chart default). |
| `kubectl get secret -n monitoring monitoring-grafana -o jsonpath="{.data.admin-password}" \| % { [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($_)) }` | Only if the default password was overridden. |
| `kubectl get configmap -n monitoring trade-surveillance-dashboard --show-labels` | Is the dashboard ConfigMap there with `grafana_dashboard=1`? If not, the sidecar has nothing to load. |
| `$dashboardFile = Join-Path $k8sPath "monitoring\trade-surveillance-dashboard.json"` <br> `kubectl create configmap trade-surveillance-dashboard --from-file=$dashboardFile --namespace monitoring --dry-run=client -o yaml \| kubectl apply -f -` <br> `kubectl label configmap trade-surveillance-dashboard grafana_dashboard=1 --namespace monitoring --overwrite` | Manually (re)load the dashboard. `start-dev` does this; run by hand after editing the JSON without a full restart. **Never** write `--from-file=(Join-Path ...)` — PowerShell splits it into two arguments. |

**Re-exporting the dashboard after editing panels in the UI:** dashboard → download icon (right sidebar) → Export as code → toggle "Export the dashboard to use in another instance" → Download file → overwrite `k8s\monitoring\trade-surveillance-dashboard.json` → commit. The JSON in the repo is the asset; Prometheus data is disposable.

**Dashboard:** "Trade Surveillance System", uid `adgbp2w`. Seven panels — request rate, 5xx rate, p95 latency, Kafka consumer lag, JVM heap, HikariCP active, trades ingested/sec.

---

## 5. Prometheus / metrics

| Command | When |
|---|---|
| `kubectl port-forward deploy/trade-ingestion-service 8081:8081` | Then hit the actuator locally. Other services: activity-monitor 8082, alert-service 8083, case-management 8084. |
| `(Invoke-WebRequest http://localhost:8081/actuator/prometheus -UseBasicParsing).Content \| Select-String trades_ingested` | Is the app exposing a metric? PowerShell aliases `curl` to `Invoke-WebRequest` — use this form. |
| `kubectl get servicemonitors` | All four should exist with label `release: monitoring`, or Prometheus won't scrape them. |
| `kubectl port-forward -n monitoring svc/monitoring-kube-prometheus-prometheus 9090:9090` | Raw Prometheus UI at http://localhost:9090. Status → Targets shows whether each service is being scraped. |

**Finding a metric name** — Grafana → Explore (compass icon) → Prometheus datasource → PromQL box → `{__name__=~"kafka_consumer.*lag.*"}` → Shift+Enter. Use this for any "what is this metric actually called" question.

**Panel PromQL:**

```promql
sum by (service) (rate(http_server_requests_seconds_count[1m]))                                  # request rate
sum by (service) (rate(http_server_requests_seconds_count{status=~"5.."}[1m]))                   # 5xx rate
histogram_quantile(0.95, sum by (service, le) (rate(http_server_requests_seconds_bucket[5m])))   # p95
sum by (consumergroup, topic) (kafka_consumer_fetch_manager_records_lag)                          # consumer lag (name unverified)
sum by (service) (jvm_memory_used_bytes{area="heap"})                                             # heap
hikaricp_connections_active                                                                       # DB pool
sum(rate(trades_ingested_total[1m]))                                                              # custom counter
```

---

## 6. Generate load

```powershell
$nlb = kubectl get svc trade-ingestion-service -o jsonpath="{.status.loadBalancer.ingress[0].hostname}"
$ts  = (Get-Date).ToUniversalTime().AddMinutes(-5).ToString("yyyy-MM-ddTHH:mm:ssZ")
0..19 | ForEach-Object {
    $body = @{ tradeId = "TRD-LOAD-{0:D3}" -f $_; tradeTimestamp = $ts; <# other required fields #> } | ConvertTo-Json
    Invoke-WebRequest "http://$nlb/api/trades" -Method POST -Body $body -ContentType "application/json" -UseBasicParsing | Out-Null
}
```

`tradeTimestamp` must not be older than 3 days or the trade is rejected. Sequential loop over internet RTT ≈ 0.6 req/s — it measures the loop, not capacity. Real load testing is k6 (Phase 5).

---

## 7. Break things on purpose (demo / resilience)

| Command | When |
|---|---|
| `kubectl scale deployment/activity-monitor-service --replicas=0` | Stop a consumer. Watch Kafka consumer lag climb on the dashboard. |
| `kubectl scale deployment/activity-monitor-service --replicas=1` | Restore. Watch lag drain. |
| `kubectl rollout restart deployment/<service>` | Restart pods without changing the image. |
| `kubectl rollout undo deployment/<service>` | Roll back to the previous revision. **First command in an incident — roll back, then diagnose.** |
| `kubectl rollout status deployment/<service> --timeout=5m` | Block until a rollout finishes. CI uses this. |

---

## 8. Deploying by hand

| Command | When |
|---|---|
| `kubectl apply -f <file>.yaml` | Apply one manifest. Service YAML has `image: ...:IMAGE_TAG` — apply raw and you get `ImagePullBackOff`. Use `start-dev` Step 5 or substitute first. |
| `kubectl set image deployment/<service> <service>=<ECR>/<repo>:<sha>` | Point a deployment at a specific image. |
| `helm upgrade --install monitoring prometheus-community/kube-prometheus-stack --namespace monitoring --create-namespace --wait --timeout 10m` | (Re)install the monitoring stack. Idempotent. `start-dev` runs this. |
| `helm list -n monitoring` | What Helm releases exist. Releases live in the cluster and die with it. |

---

## 9. AWS

| Command | When |
|---|---|
| `aws eks update-kubeconfig --name surveillance-prod --region us-east-1` | Point kubectl at the cluster. `start-dev` does this; run manually after a new cluster if kubectl says "connection refused". |
| `aws ecr describe-images --repository-name surveillance-prod/<service> --region us-east-1 --query "sort_by(imageDetails,&imagePushedAt)[-5:].[imageTags[0],imagePushedAt]" --output table` | Last five images pushed. Is the SHA you want to deploy actually there? |
| `aws rds describe-db-instances --db-instance-identifier surveillance-prod-db --region us-east-1 --query "DBInstances[0].[DBInstanceStatus,Endpoint.Address]"` | RDS status and host. |
| `aws elb describe-load-balancers --region us-east-1 --query "LoadBalancerDescriptions[].LoadBalancerName"` | Classic LB leak check (`stop-dev` does this). |
| `aws elbv2 describe-load-balancers --region us-east-1 --query "LoadBalancers[].[LoadBalancerName,Type]" --output table` | NLB/ALB leak check. |

---

## 10. Secrets, RBAC, cluster plumbing

| Command | When |
|---|---|
| `kubectl get configmap aws-auth -n kube-system -o yaml` | IAM → Kubernetes user mapping. Adding an IAM role to the cluster means editing this. |
| `kubectl get role github-actions-deploy -n default -o yaml` <br> `kubectl get rolebinding github-actions-deploy-binding -n default -o yaml` | What CI is allowed to do. |
| `kubectl get secret db-secret -o jsonpath="{.data.password}" \| % { [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($_)) }` | Decode a secret. Same pattern for `msk-secret` / `brokers`. |
| `kubectl exec deploy/<service> -- env \| Select-String SPRING_KAFKA` | What config a running pod actually has. Secrets are snapshotted at pod start — changing a secret does not reach running pods; restart them. |

---

## 11. Database access from the laptop

```powershell
kubectl run rds-proxy --image=alpine/socat --restart=Never --port=5432 -- tcp-listen:5432,fork,reuseaddr tcp-connect:<rds-host>:5432
kubectl wait --for=condition=Ready pod/rds-proxy --timeout=60s
kubectl port-forward pod/rds-proxy 5432:5432      # then DBeaver → localhost:5432
kubectl delete pod rds-proxy                      # when done
```

---

## PowerShell gotchas

- `curl` is `Invoke-WebRequest` and prompts for input — always use `Invoke-WebRequest ... -UseBasicParsing`.
- `kubectl get ... -o yaml | Select-String` mangles output. Use `-o jsonpath` for one field.
- `--flag=(expression)` breaks: PowerShell splits it into two arguments. Assign to a `$variable` first, then `--flag=$variable`.
- Backtick at line end = continuation. No trailing space after it.
- `gh` is not installed — check CI at https://github.com/Vatika1/financial-surveillance-system/actions.
