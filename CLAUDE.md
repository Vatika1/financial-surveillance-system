# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Stack

Java 21 (Temurin), Spring Boot 3.5.x microservices, **Maven** (not Gradle). Deployed to AWS EKS, with MSK (managed Kafka), RDS Postgres, and Secrets Manager. Infra is Terraform, split into `persistent` (RDS, VPC, ECR, secrets) and `ephemeral` (EKS, MSK) for cost control.

## Working conventions

- **PowerShell only**, never bash. Use `$env:VAR`, not `$VAR`. Backtick line continuation, not `\`.
- **Show diffs and explain changes before committing. Never auto-commit.** Wait for explicit user approval per commit.
- **Direct and concise.** No essay-style explanations, no walls of text, no excessive bullet points. Yes/no questions get a yes/no first, then reasoning if needed.
- Don't restate what the user already knows or just told you.

## Git conventions

- Use Conventional Commits prefixes: `feat`, `fix`, `chore`, `harden`, `refactor`, `docs`, `test`.
- Imperative mood in subject line, under 72 chars.
- Blank line between subject and body.
- Body bullets explain *what* changed; trailing paragraph explains *why*.
- Always show the commit message before running `git commit`.
- Never auto-push — always wait for explicit approval.

## Repository shape

Multi-module Maven project (parent `pom.xml` at root). Four runtime services plus one shared library:

| Module | Role | Port |
|---|---|---|
| `surveillance-events-lib` | Shared Kafka event DTOs — every service depends on this | — |
| `trade-ingestion-service` | REST entry (`POST /api/trades`), publishes to `trades.raw`, exposed via public NLB | 8081 |
| `activity-monitor-service` | Consumes `trades.raw`, runs rule engine, publishes to `alerts.created` | 8082 |
| `alert-service` | Consumes `alerts.created`, persists alerts, publishes to the cases topic | 8083 |
| `case-management-service` | Terminal consumer (case lifecycle) | — |

Older modules (`notification-service`, `audit-service`, `reporting-service`, `user-service`) exist as directories but are commented out in the parent `pom.xml` — they don't build.

## Build & test

```powershell
mvn clean install                                  # full reactor (tests + JaCoCo)
mvn test                                            # unit tests only
mvn test -pl trade-ingestion-service               # one module
mvn test -pl activity-monitor-service -Dtest=ActivityMonitorServiceTest          # single class
mvn test -pl activity-monitor-service "-Dtest=ActivityMonitorServiceTest#processTrade_*"   # single method
mvn verify                                          # also enforces JaCoCo 50% gate
mvn package -DskipTests                            # build jars without testing
```

**JaCoCo gate**: `jacoco.minimum.coverage = 0.50` at `verify`. Excluded from coverage (and Sonar): `dto/`, `domain/`, `entity/`, `model/`, `mapper/`, `*Application.java`. Add coverage to `service/`, `consumer/`, `producer/`, `rules/`, `engine/`, `controller/`.

## Local development

```powershell
docker compose up -d                               # postgres + zookeeper + kafka + kafdrop (:9000)
mvn -pl trade-ingestion-service spring-boot:run   # local profile is default
```

`trade-ingestion-service` has a `TradeSimulator` (`trade.simulator.enabled=true` in local) that generates synthetic load.

## Cloud / ephemeral environment

Prod runs on EKS + MSK + RDS in `us-east-1`. **K8s Secrets die with the EKS cluster**, so `scripts/start-dev.ps1` is the canonical bring-up: it re-fetches the DB password and MSK bootstrap brokers from AWS Secrets Manager (`surveillance-prod-msk-bootstrap-brokers`) and recreates `db-secret` / `msk-secret` before applying `k8s/` manifests. Deployments reference these via `secretKeyRef`. If you spin up the cluster without this script, pods will not start.

`scripts/stop-dev.ps1` tears down ephemeral; `scripts/reset-local.sh` resets local Postgres.

## CI/CD (`.github/workflows/ci.yml`)

Push to `master`/`main`: tests → build → SonarCloud → push 4 images to ECR → `kubectl set image` rollout on EKS. AWS auth is OIDC, no static creds. Image tag = `${GITHUB_SHA::7}`. `workflow_dispatch` allows targeting a single service.

## Architectural conventions

### Event flow

```
POST /api/trades
  → trade-ingestion (DB insert + publish → trades.raw)
    → activity-monitor (rule engine + publish → alerts.created)
      → alert-service (persist alert + publish → cases topic)
        → case-management (terminal)
```

Every message is keyed by `advisorId` so per-advisor events stay on one partition. Sliding-window rule evaluation in `activity-monitor` depends on this ordering.

### One Postgres, schema per service

`init-db.sql` creates `trade_ingestion`, `activity_monitor`, `alert_management`, `case_management`. Each service owns its schema and **never reads tables from another service's schema**. Cross-service communication is Kafka, not SQL. Flyway is enabled per service with `spring.flyway.schemas=<owning-schema>`; migrations in `src/main/resources/db/migration/V*__*.sql`.

### Per-service package layout

```
config/      Spring @Configuration (Kafka factories, topic property beans)
controller/  @RestController (services with REST APIs only)
consumer/    @KafkaListener + thin processors
producer/    Kafka send wrappers that translate KafkaException → domain exceptions
service/     @Service orchestration with @Transactional boundaries
domain/      JPA entities
dto/         Request/response/internal DTOs
mapper/      Pure entity ↔ DTO ↔ event conversion
repository/  Spring Data interfaces
exception/   Domain exceptions + @RestControllerAdvice
```

`activity-monitor-service` adds `engine/` (`SurveillanceEngine`), `rules/<name>/` (one package per rule), and `cache/` (`TradeWindowStore`).

### Patterns that must be preserved

**Dual-write rollback.** Producers call `kafkaTemplate.send(...).get(5, TimeUnit.SECONDS)` inside a `@Transactional` method. Failures are caught and re-thrown as an unchecked domain exception (`TradePublishException`, `AlertPublishException`) so Spring rolls back the DB insert. Do not switch to fire-and-forget — it breaks the "DB row exists ⇔ event published" invariant. No producer-side DLQ: the upstream caller (HTTP client or Kafka consumer with retry) is the retry mechanism.

**Cache rollback via `TransactionSynchronization`.** When a service mutates a non-transactional resource (e.g. `TradeWindowStore` in-memory cache) *before* the DB write, register a `TransactionSynchronization` whose `afterCompletion(STATUS_ROLLED_BACK)` undoes the cache write. See `ActivityMonitorService.processTrade`.

**Idempotency: insert-and-catch, not check-then-insert.** `IdempotencyService.markProcessed(eventId)` does a blind `INSERT` into `processed_events` (keyed on `tradeId` in `activity-monitor`, `alertId` in `alert-service`). A duplicate raises `DataIntegrityViolationException`, which the listener catches and silently acks. Do not introduce a `SELECT ... WHERE id = ?` check first — that's a TOCTOU race under at-least-once delivery.

**Consumer error handling.** Listener container factory wires:
- `ErrorHandlingDeserializer` wrapping `JsonDeserializer` (poison-pill safe — deserialization failures become records with a marker value rather than killing the consumer).
- `DefaultErrorHandler` with `ExponentialBackOff(initialInterval=1s, multiplier=2.0, maxInterval=10s, maxAttempts=3)`.
- Non-retryable exception list: `IllegalArgumentException`, `DeserializationException`, `ClassCastException` (fail-fast to DLT, no retries).
- `DeadLetterPublishingRecoverer` routes terminal failures to `<topic>.DLT`.

**Rule plugin pattern (`activity-monitor`).** Rules implement `SurveillanceRule.evaluate(event, context) -> Optional<RuleViolationDTO>`. `SurveillanceEngine` collects every `@Component` implementation and runs them. To add a rule, drop a new package under `rules/<name>/` — no engine changes.

**MSK bootstrap brokers via Secrets Manager.** Brokers live in AWS Secrets Manager (`surveillance-prod-msk-bootstrap-brokers`), are synced to the K8s `msk-secret` by `start-dev.ps1`, and referenced from deployments via `secretKeyRef`. Don't hardcode broker addresses in manifests or YAML.

**Profile split.** Each service has `application.yaml` (defaults) + `application-local.yml` (localhost wiring + simulator enabled) + `application-prod.yml` (env-var-driven + SSL Kafka + actuator probes). Match this when adding config.

### Kafka topics

| Topic | Producer | Consumer | DLT |
|---|---|---|---|
| `trades.raw` | trade-ingestion | activity-monitor | `trades.raw.DLT` |
| `alerts.created` | activity-monitor | alert-service | `alerts.created.DLT` |
| (cases topic) | alert-service | case-management | — |

Topic names live in `application.yaml` under `kafka.topics.*` and are read via `@ConfigurationProperties` beans. Never hardcode topic strings in producer/consumer classes.

## Current state — sprint snapshot (2026-05-23)

**Phase 1 — Terraform foundation: DONE.**

**Phase 2 — hardening + DLQ + Testcontainers: IN PROGRESS.**
- `trade-ingestion`: **DONE** — dual-write, manifest hardening, NLB, graceful shutdown, topic durability (`replicas=2`, `min.insync.replicas=2`), `trades.raw.DLT` declared, K8s `replicas=2`, aggregate startup probe. Baseline for the other services.
- `activity-monitor`: dual-write shipped (with `TransactionSynchronization` rollback for `TradeWindowStore`), Chunks 2/3/4 shipped. Remaining: Chunk 5 manifest hardening.
- `alert-service`: not started.
- `case-management`: not started.
- Closeout: end-to-end smoke test + Testcontainers sweep.

**Phase 4 — DB performance: partially started, paused.** Composite index `idx_trades_advisor_timestamp` created. To resume, set `max_parallel_workers_per_gather = 0` before reproducing the query plan.

**Out of scope for this sprint:** Phase 8 (AI/LLM) and Phase 4 sessions B/C — dropped.
