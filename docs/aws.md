# AWS CLI Commands

[← index](commands.md)

All in `us-east-1`.

## Cluster and resources

| Command | When |
|---|---|
| `aws eks update-kubeconfig --name surveillance-prod --region us-east-1` | Point kubectl at the cluster. start-dev does this; run manually after a new cluster if kubectl says "connection refused". |
| `aws rds describe-db-instances --db-instance-identifier surveillance-prod-db --region us-east-1 --query "DBInstances[0].[DBInstanceStatus,Endpoint.Address]"` | RDS status and host. |

## ECR

| Command | When |
|---|---|
| `aws ecr describe-images --repository-name surveillance-prod/<repo> --region us-east-1 --query "sort_by(imageDetails,&imagePushedAt)[-5:].[imageTags[0],imagePushedAt]" --output json` | Last five images pushed. Is the SHA you want to deploy actually there? |
| `aws ecr describe-images --repository-name surveillance-prod/<repo> --region us-east-1 --query "length(imageDetails)" --output text` | Image count. Two numbers printed = results span two pages. |
| `aws ecr get-lifecycle-policy --repository-name surveillance-prod/<repo> --region us-east-1` | Confirm the keep-last-20 lifecycle policy is applied. |

**Gotcha:** with `--output text`, the CLI runs `--query` on each page of results separately (100 images per page) and prints one answer per page. Any query that picks "the newest" must use `--output json`. This caused the Sept 27–28 `InvalidImageName` on trade-ingestion.

Repo names: `trade-ingestion-service`, `activity-monitor-service`, `alert-service`, `case-management-service`.

## Load balancer leak checks

| Command | When |
|---|---|
| `aws elb describe-load-balancers --region us-east-1 --query "LoadBalancerDescriptions[].LoadBalancerName"` | Classic LB leak check (stop-dev does this). |
| `aws elbv2 describe-load-balancers --region us-east-1 --query "LoadBalancers[].[LoadBalancerName,Type]" --output table` | NLB/ALB leak check. |

## Teardown verification

Run after `stop-dev.ps1`. Expected output, in order: `[]`, `[]`, `"deleted"`, `[]`.

| Command | What it proves |
|---|---|
| `aws eks list-clusters --region us-east-1` | No EKS control plane billing |
| `aws kafka list-clusters --region us-east-1 --query "ClusterInfoList[].State"` | No MSK brokers |
| `aws ec2 describe-nat-gateways --region us-east-1 --query "NatGateways[].State"` | NAT gateway gone (shows deleted for a while, then disappears) |
| `aws elbv2 describe-load-balancers --region us-east-1 --query "LoadBalancers[].LoadBalancerName"` | No stranded NLB — Kubernetes creates these outside Terraform, so `terraform destroy` never sees them |

If the cluster is up when you expected it down, list worker nodes:

```powershell
aws ec2 describe-instances --region us-east-1 --query "Reservations[].Instances[].[InstanceId,State.Name]"
```
