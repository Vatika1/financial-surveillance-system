# Command Reference — Financial Surveillance System

Everything is PowerShell unless noted. `<name>` means substitute your value. Port-forwards hold the terminal — open a second one for anything else.

| File | What's in it |
|---|---|
| [kubectl.md](kubectl.md) | Pod/deployment status, logs, rollouts, deploying by hand, secrets, DB access |
| [aws.md](aws.md) | ECR, RDS, EKS kubeconfig, load balancer checks, teardown verification |
| [terraform.md](terraform.md) | Persistent infra (RDS, ECR) changes |
| [docker.md](docker.md) | Local Docker Compose stack, port conflicts |
| [local-testing.md](local-testing.md) | Testing locally: test scenarios, sending trades, following a trade by correlationId, silent failures, Postgres, Redis, Kafka and DLT checks |
| [cluster-testing.md](cluster-testing.md) | Testing on EKS: deploying a branch, confirming the image, sending trades to the NLB, checking in Grafana, Kafka CLI against MSK |
| [k6.md](k6.md) | Load tests and manual trade generation |
| [observability.md](observability.md) | Grafana, Prometheus, Loki (incident queries, stack traces), Tempo, actuator metrics, PromQL incident metrics |
| [incident-drills.md](incident-drills.md) | Drill process, drill list, template, and write-ups with postmortems and interview answers |
| [powershell.md](powershell.md) | PowerShell gotchas |

## Bring the stack up / tear it down

Run from the repo root. RDS and ECR are persistent and survive stop-dev; everything else is rebuilt each time.

| Command | When |
|---|---|
| `.\scripts\start-dev.ps1` | Start of a session. Applies ephemeral Terraform (EKS + MSK), recreates secrets, installs kube-prometheus-stack, provisions the Grafana dashboard, deploys each service at its newest image in ECR. ~25 min. |
| `.\scripts\stop-dev.ps1` | End of a session. Destroys EKS/MSK/NAT, checks for leftover load balancers. Always run this — EKS + MSK + NAT bill while idle. Then run the teardown checks in [aws.md](aws.md). |

**Constraint:** start-dev deploys whatever CI last pushed to ECR. Push, wait for green, then start.
