# kubectl Commands

[← index](commands.md)

## Is everything running?

| Command | When |
|---|---|
| `kubectl get pods` | App pods. `1/1 Running` is healthy. `0/1 Running` for the first ~60s is normal (Spring Boot + Kafka connect). |
| `kubectl get pods -A` | Every pod in every namespace, in one list. |
| `kubectl get pods -n monitoring` | Monitoring pods (Prometheus, Grafana, Alertmanager, operator, kube-state-metrics, node-exporters, Loki, Fluent Bit, Tempo). |
| `kubectl get pods -n kube-system` | System pods: cluster autoscaler, DNS, networking. |
| `kubectl get pods -n monitoring -l app.kubernetes.io/name=tempo` | Just Tempo. Swap `tempo` for `loki` or `fluent-bit` to check those. A `RESTARTS` count above 0 is worth a look. |
| `kubectl get deployments -o custom-columns="DEPLOYMENT:.metadata.name,IMAGE:.spec.template.spec.containers[0].image"` | Image tag each deployment points at. First check for `InvalidImageName` / `ImagePullBackOff`. |
| `kubectl get pods -o custom-columns=NAME:.metadata.name,IMAGE:.spec.containers[0].image` | Image tag each pod is actually running. First thing to check when behaviour doesn't match the code. |
| `kubectl get deployments` | Desired vs available replicas. |
| `kubectl get services` | ClusterIP / LoadBalancer addresses. trade-ingestion is the only LoadBalancer; its EXTERNAL-IP is the NLB hostname. |
| `kubectl describe pod -l app=<service>` | Events, probe failures, image pull errors, OOM kills. Read this before logs when a pod won't start. |
| `kubectl describe pod <pod> -n <namespace> \| Select-String -Context 0,6 "Last State"` | Why a pod last restarted. `OOMKilled` / exit code 137 means it hit its memory limit (Tempo did this at 512Mi). |
| `kubectl get pods -n monitoring -o custom-columns=POD:.metadata.name,CONTAINERS:.spec.containers[*].name` | Which containers live in each monitoring pod (Grafana has three: grafana, grafana-sc-dashboard, grafana-sc-datasources). |

**Namespaces:** your four services run in `default`, the monitoring stack in `monitoring`, cluster components in `kube-system`. `kubectl` only shows what is deployed in the cluster right now, not local files or commits the pipeline has not deployed yet.

## Logs

| Command | When |
|---|---|
| `kubectl logs deploy/<service> --tail=100` | Quick look at recent output. |
| `kubectl logs deploy/<service> -f` | Live stream. Ctrl+C to stop. |
| `kubectl logs deploy/<service> --since=1h \| Select-String "ERROR\|Exception"` | Only the errors from the last hour. This is how the bot-traffic / 404-as-500 bug was found. |
| `kubectl logs -l app=<service> --since=5m --tail=-1 \| Select-String "<text>"` | Search the logs of every pod of a service at once. `deploy/<service>` only reads one pod. |
| `kubectl logs <pod> -n <namespace> --previous --tail=40` | Last lines from a container before it crashed and restarted. |
| `kubectl logs deploy/<service> \| Select-String "opentelemetry-javaagent"` | Did the tracing agent start? No `--tail`: the line is at the very top of the log. |
| `kubectl logs tempo-0 -n monitoring --tail=30` | Tempo's own log. `Tempo started` means it is up. |
| `kubectl logs deploy/<service> > logs.txt` | Dump to file for offline grepping. |
| `kubectl logs -n monitoring deploy/monitoring-grafana -c grafana-sc-dashboard \| Select-String "trade-surveillance"` | Did the sidecar load the dashboard ConfigMap? Want `Writing /tmp/dashboards/...` and `Dashboards config reloaded`. |
| `kubectl logs -n monitoring prometheus-monitoring-kube-prometheus-prometheus-0 -c prometheus --tail=50` | Prometheus itself — scrape errors, config reload failures. |

## Rollouts / break things on purpose

| Command | When |
|---|---|
| `kubectl scale deployment/activity-monitor-service --replicas=0` | Stop a consumer. Its `trades_raw` lag line disappears — the lag metric is published by the consumer pod, so no pod means no number, not a climbing one. |
| `kubectl scale deployment/activity-monitor-service --replicas=1` | Restore. New pod reports the inherited backlog on first scrape, then drains. Send a few hundred trades to catch the spike. |
| `kubectl rollout restart deployment/<service>` | Restart pods without changing the image. |
| `kubectl rollout undo deployment/<service>` | Roll back to the previous revision. First command in an incident — roll back, then diagnose. |
| `kubectl rollout status deployment/<service> --timeout=5m` | Block until a rollout finishes. CI uses this. |

## Deploying by hand

| Command | When |
|---|---|
| `kubectl apply -f <file>.yaml` | Apply one manifest. Service YAML has `image: ...:IMAGE_TAG` — apply raw and you get `ImagePullBackOff`. Use start-dev Step 5 or substitute first. |
| `kubectl apply --dry-run=server -f k8s\<folder>\<file>.yaml` | Validate a manifest against the cluster without changing anything. Catches indentation and field errors before pushing. Keep the flag: a real apply deploys the literal `IMAGE_TAG`. |
| `kubectl set image deployment/<service> "*=674326380423.dkr.ecr.us-east-1.amazonaws.com/surveillance-prod/<repo>:<sha>"` | Point a deployment at a specific image. `*` covers all containers, so no container name needed. |
| `helm upgrade --install monitoring prometheus-community/kube-prometheus-stack --namespace monitoring --create-namespace --wait --timeout 10m` | (Re)install the monitoring stack. Idempotent. start-dev runs this. |
| `helm upgrade --install tempo grafana/tempo --namespace monitoring -f k8s\monitoring\tempo-values.yaml --wait --timeout 5m` | (Re)install Tempo, or apply a change to `tempo-values.yaml`. start-dev step 4f runs this. |
| `helm list -n monitoring` | What Helm releases exist. Releases live in the cluster and die with it. |

## Secrets, RBAC, cluster plumbing

| Command | When |
|---|---|
| `kubectl get configmap aws-auth -n kube-system -o yaml` | IAM → Kubernetes user mapping. Adding an IAM role to the cluster means editing this. |
| `kubectl get role github-actions-deploy -n default -o yaml` | What CI is allowed to do. |
| `kubectl get rolebinding github-actions-deploy-binding -n default -o yaml` | Who the CI role is bound to. |
| `kubectl get secret db-secret -o jsonpath="{.data.password}" \| % { [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($_)) }` | Decode a secret. Same pattern for `msk-secret` / `brokers`. |
| `kubectl exec deploy/<service> -- env \| Select-String "JAVA_TOOL_OPTIONS\|OTEL"` | Confirm a running pod has the tracing agent settings. |
| `kubectl exec deploy/<service> -- env \| Select-String SPRING_KAFKA` | Config a running pod actually has. Secrets are snapshotted at pod start — changing a secret doesn't reach running pods; restart them. |

## Database access from the laptop

```powershell
kubectl run rds-proxy --image=alpine/socat --restart=Never --port=5432 -- tcp-listen:5432,fork,reuseaddr tcp-connect:<rds-host>:5432
kubectl wait --for=condition=Ready pod/rds-proxy --timeout=60s
kubectl port-forward pod/rds-proxy 5432:5432      # then DBeaver → localhost:5432
kubectl delete pod rds-proxy                      # when done
```

Get `<rds-host>` from the RDS command in [aws.md](aws.md). Stop the local Compose Postgres first — it also binds 5432.
