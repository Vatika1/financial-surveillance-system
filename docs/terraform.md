# Terraform Commands

[← index](commands.md)

Two environments:
- `infra\environments\prod\ephemeral` — EKS, MSK, NAT. Created by start-dev, destroyed by stop-dev. Don't apply by hand unless start-dev failed partway.
- `infra\environments\prod\persistent` — RDS, ECR. Survives stop-dev. Changes here are applied by hand.

Modules live in `infra\modules\` (e.g. `modules\ecr\main.tf` defines the repos and their lifecycle policy).

## Applying a persistent change

```powershell
cd C:\Users\vatik\dev\Backend\financial-surveillance-system\infra\environments\prod\persistent
terraform init
terraform plan
terraform apply
```

Read the `plan` output before `apply`. A persistent change should show only what you edited — anything saying **destroy** on RDS or an ECR repo means stop.

## Finding where something is defined

```powershell
Get-ChildItem -Path C:\Users\vatik\dev\Backend\financial-surveillance-system\infra -Recurse -Filter *.tf | Select-String "aws_ecr_repository"
```

Swap the string for any resource type (`aws_db_instance`, `aws_msk_cluster`, ...).
