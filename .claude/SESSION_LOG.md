# Session Log

## Current TODO

### Phase 2 — Hardening + DLQ + Testcontainers

**Trade-ingestion** ✅ DONE
- Dual-write fix shipped
- Manifest hardening shipped (NLB, probes, memory, graceful shutdown)
- Topic replicas 2, min.insync.replicas, trades.raw.DLT declared

**Activity-monitor** ✅ DONE
- Dual-write fix with TransactionSynchronization rollback callback for TradeWindowStore
- ErrorHandlingDeserializer + ExponentialBackOff + DLT routing
- IdempotencyService (insert-and-catch pattern, keyed on tradeId)
- Manifest hardening (probes, memory, graceful shutdown)
- Removed redundant @Value bootstrapServers from Kafka configs

**Alert-service** ❌ NOT STARTED — next major work
- [ ] Dual-write fix on its producer (publishes to case-management's topic)
- [ ] ErrorHandlingDeserializer wrapping JsonDeserializer
- [ ] ExponentialBackOff + non-retryable exceptions + DLT routing
- [ ] IdempotencyService keyed on alertId
- [ ] Manifest hardening (probes, memory, graceful shutdown)
- [ ] Declare alerts.raw.DLT topic

**Case-management** ❌ NOT STARTED
- [ ] Same 5-chunk pattern as alert-service (terminal consumer, no downstream producer)

**Phase 2 closeout**
- [ ] End-to-end smoke test (trade via NLB → 4-service chain → case in DB)
- [ ] Testcontainers integration tests across all 4 services

### Phase 3 — CI/CD Maturity (after Phase 2)
- [ ] Post-deploy smoke test (curl /actuator/health, fail pipeline if not 200)
- [ ] Automatic rollback on smoke-test failure (kubectl rollout undo on failure)
- [ ] Image security scanning (Trivy, fail on CRITICAL/HIGH CVEs)
- [ ] Environment separation with manual approval gate (dev → prod via GitHub Environments)
- [ ] Integration tests in CI (mvn verify with Testcontainers *IT.java)
- [ ] Matrix strategy for DRY (collapse 8 per-service steps into one matrix loop)

### Phase 4 — DB Performance (paused, resume after CI/CD)
- [x] Session A partial: composite index idx_trades_advisor_timestamp created
- [ ] Session A finish: re-test with SET max_parallel_workers_per_gather=0
- [ ] Sessions B + C dropped from sprint scope

### Dropped from sprint scope
- Phase 4 Sessions B (N+1 with JOIN FETCH) and C (HikariCP pool exhaustion)
- Phase 8 (AI/LLM via Bedrock)

---

## Active patterns / conventions

- **Dual-write fix**: kafkaTemplate.send(...).get(5s, MILLISECONDS) blocking + try/catch + throw unchecked exception → @Transactional rolls back DB
- **IdempotencyService**: insert-and-catch DataIntegrityViolationException (NOT check-then-insert)
- **MSK bootstrap brokers**: stored in AWS Secrets Manager, synced to K8s Secret 'msk-secret' by start-dev.ps1, referenced via secretKeyRef in deployments
- **Topic ownership**: trade-ingestion declares all trades.raw.* topics including DLT (centralized)
- **Image tags**: git SHA via GitHub Actions, never :latest (ECR immutability enforces)
- **Deploy**: kubectl apply -f (full manifest), not kubectl set image (image-only)
- **Communication style with Claude**: concise, direct, diffs over full files, no essays

---

## Sessions

### 2026-05-24 (today)
**Shipped:**
- Removed redundant @Value bootstrapServers from activity-monitor's KafkaConsumerConfig and KafkaProducerConfig
- Activity-monitor fully complete

**Discussed:**
- ECR immutability vs :latest tag — why SHA tagging is correct pattern
- kubectl set image vs kubectl apply — pipeline now uses apply
- Started Claude Code hooks setup (.claude/settings.json)

**Blockers:** None

**Next:** Start alert-service (5-chunk pattern)

### 2026-05-23
**Shipped:**
- Activity-monitor manifest hardening (probes, memory limits, graceful shutdown, terminationGracePeriodSeconds)
- Fixed CI/CD pipeline: kubectl set image → kubectl apply -f (sync full manifest, not just image)
- Confirmed bootstrap automation works across cluster recreation

**Discussed:**
- Difference between kubectl set image (image only) and kubectl apply (full manifest)
- Why probes weren't applied initially (pipeline only synced image tag)
- ECR immutability cause of yesterday's :latest push failure

**Blockers:** Pipeline divergence between git YAML and cluster state — fixed.

### 2026-05-22 (cluster setup + MSK debugging)
**Shipped:**
- MSK bootstrap broker automation (Terraform → Secrets Manager → K8s Secret → pod env var via secretKeyRef)
- Verified end-to-end across stop-dev/start-dev cycle
- Trade-ingestion graceful shutdown + LoadBalancer NLB exposed publicly

**Blockers:** stale MSK hostnames in deployment YAMLs after cluster recreation — fixed via automation.

### 2026-05-21 (activity-monitor dual-write fix)
**Shipped:**
- Dual-write fix on processTrade with AlertEventProducer blocking + AlertPublishException
- TransactionSynchronization rollback callback to undo TradeWindowStore on rollback
- Tests fixed (AlertEventProducerTest assertThrows, ActivityMonitorServiceTest uses TransactionSynchronizationManager.initSynchronization)

**Blockers:** IntelliJ 2022.2 couldn't index JDK 21 (TimeUnit red squiggle). Upgraded to IntelliJ 2026.1.1.