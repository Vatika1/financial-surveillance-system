# Financial Surveillance System

A cloud-native, event-driven trade surveillance platform built on AWS — designed and developed as a portfolio project to demonstrate production-grade patterns in distributed systems engineering.

> **Status:** Phase 2 in progress · Phased build · 4 of 8 planned services implemented

---

## What this is

A multi-service Java / Spring Boot platform that ingests trade events, evaluates them against surveillance rules in near real-time, and generates alerts for compliance analyst review. Modeled after the kind of trade surveillance systems used in regulated financial environments.

The goal is not to build a toy app — it's to build something I'd be willing to defend in a senior engineering interview. Every architectural choice (exactly-once semantics, DLQ handling, idempotency, observability-from-day-one) is deliberate.

---

## Architecture

The system is composed of independent Spring Boot services communicating via Apache Kafka, deployed on AWS EKS.

### Implemented services

| Service | Responsibility |
|---------|----------------|
| **trade-ingestion-service** | Ingests trade events from external sources with transactional dual-write integrity (DB + Kafka) |
| **activity-monitor-service** | Consumes events, applies surveillance rules, enforces idempotency on duplicate events |
| **alert-service** | Publishes surveillance alerts to downstream Kafka topics for analyst consumption |
| **case-management-service** | Workflow service tracking analyst case lifecycle and resolution |

### Planned services

| Service | Responsibility |
|---------|----------------|
| audit-service | System-wide audit trail for regulatory compliance |
| notification-service | Alert delivery via email, Slack, and downstream systems |
| reporting-service | Compliance reports and analyst dashboards |
| user-service | Analyst identity, roles, and permissions |

---

## Tech stack

**Language & framework:** Java 21, Spring Boot 3.5

**Messaging:** Apache Kafka (AWS MSK)

**Persistence:** PostgreSQL (AWS RDS)

**Infrastructure:** AWS EKS, MSK, RDS, Secrets Manager, ECR, IAM (managed via Terraform)

**Build & deploy:** Maven, Docker, GitHub Actions (CI), OIDC-based AWS authentication

**Testing:** JUnit, Mockito, Testcontainers (integration tests against real Kafka + Postgres in CI)

**Observability (planned, Phase 3):** Grafana, Loki, OpenTelemetry

---

## Design decisions worth highlighting

**Dual-write consistency.** Originally hit a classic bug where trade events were persisted to Postgres but Kafka publishing failed silently, leaving the system in an inconsistent state. Fixed by switching from fire-and-forget `KafkaTemplate.send()` to synchronous `.get(5s)` with a transactional rollback on publish failure. Consumers handle resulting duplicates via idempotency checks on the consumer side.

**Secrets management.** MSK bootstrap brokers and DB credentials live in AWS Secrets Manager, surfaced into Kubernetes via Secret references. Avoids hardcoded values in manifests and supports rotation. Future work (Phase 6): migrate to External Secrets Operator with IRSA for auto-sync.

**Cost-conscious infrastructure.** The full stack targets under $100/month using a Terraform split between persistent resources (VPC, RDS, MSK) and ephemeral resources (EKS node groups, ECR images) so I can spin the cluster down when not actively developing.

**Phased build.** Rather than half-building eight services in parallel, the roadmap is sequential: get four services to production quality (Phase 2 hardening + DLQ + integration tests), then add observability (Phase 3), then performance (Phase 4), and so on. Each phase makes a real interview story.

---

## Roadmap

- [x] **Phase 1** — Terraform foundation, EKS cluster, MSK, RDS, base service skeletons
- [ ] **Phase 2** — Hardening (DLQ, idempotency, ErrorHandlingDeserializer), Testcontainers integration tests *(in progress)*
- [ ] **Phase 3** — Observability (Grafana, Loki, OpenTelemetry)
- [ ] **Phase 4** — DB performance tuning (indexing, JOIN FETCH, HikariCP)
- [ ] **Phase 5** — Resilience (Resilience4j, retry/backoff policies)
- [ ] **Phase 6** — Security (IRSA, External Secrets Operator, Spring Security, network policies)
- [ ] **Phase 7** — CI/CD maturity (auto-deploy on merge, image tagging by SHA, rollback)
- [ ] **Phase 8** — AI/LLM integration via AWS Bedrock (capstone)

---

## Local development

Each service is independently runnable. The repo also includes scripts to spin up a local development environment using Docker Compose for Kafka + Postgres.

```bash
# Bring up local Kafka + Postgres
./scripts/start-local.sh

# Run a service against local infrastructure
cd trade-ingestion-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Production deployment uses GitHub Actions to build, push images to ECR, and deploy to EKS.

---

## About

Built by [Vatika Prasad](https://vatika1.github.io) — senior software engineer based in Montreal, focused on cloud-native backend systems.

- 🌐 [Portfolio](https://vatika1.github.io)
- 💼 [LinkedIn](https://www.linkedin.com/in/vatikaprasad)
- 👤 [GitHub](https://github.com/Vatika1)
