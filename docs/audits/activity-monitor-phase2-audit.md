# Kafka Phase 2 Hardening Audit — activity-monitor-service

_Auditor: kafka-phase2-auditor agent_
_Date: 2026-05-23_
_Sprint snapshot reference: 2026-05-22 (CLAUDE.md)_

## Summary

- Files audited:
  - `activity-monitor-service/src/main/java/.../config/KafkaProducerConfig.java`
  - `activity-monitor-service/src/main/java/.../config/KafkaConsumerConfig.java`
  - `activity-monitor-service/src/main/java/.../config/KafkaTopicConfig.java`
  - `activity-monitor-service/src/main/java/.../producer/AlertEventProducer.java`
  - `activity-monitor-service/src/main/java/.../consumer/TradeEventConsumer.java`
  - `activity-monitor-service/src/main/java/.../consumer/TradeProcessor.java`
  - `activity-monitor-service/src/main/java/.../service/ActivityMonitorService.java`
  - `activity-monitor-service/src/main/java/.../cache/InMemoryTradeWindowStore.java`
  - `activity-monitor-service/src/main/java/.../service/IdempotencyService.java`
  - `activity-monitor-service/src/main/resources/application.yaml`
  - `activity-monitor-service/src/main/resources/application-local.yml`
  - `activity-monitor-service/src/main/resources/application-prod.yml`
  - `k8s/activity-monitor/activity-monitor-deployment.yaml`
  - `k8s/activity-monitor/activity-monitor-service.yaml`
- Producers found: 1 (`AlertEventProducer` → `alerts.created`)
- Consumers found: 1 (`TradeEventConsumer` ← `trades.raw`)
- PASS: 16 GAP: 9 RISK: 4
- Overall status: **NEEDS REMEDIATION** — Chunk 5 manifest hardening is not done, and there are several config gaps beyond what the sprint snapshot lists.

Checklist source note: there is no standalone Phase 2 checklist document in the repo. The audit uses CLAUDE.md "Patterns that must be preserved" + the 2026-05-22 sprint snapshot as the authoritative checklist, supplemented with industry-standard items flagged as such.

---

## Producer audit — `AlertEventProducer` / `KafkaProducerConfig`

| Check | Required | Actual | Status | Notes |
|-------|----------|--------|--------|-------|
| `acks` | `all` | `all` | PASS | `KafkaProducerConfig.java:38` |
| `enable.idempotence` | `true` | `true` | PASS | `KafkaProducerConfig.java:39` |
| `max.in.flight.requests.per.connection` | ≤ 5 | `5` | PASS | `KafkaProducerConfig.java:52` |
| `retries` | high (or `Integer.MAX_VALUE`) | `10` | RISK | `KafkaProducerConfig.java:40` — with idempotence on, this is overly conservative. Industry guidance is `Integer.MAX_VALUE` bounded by `delivery.timeout.ms`. With `delivery.timeout.ms=120000` already in place, `retries=10` is the binding cap during transient broker outages and could surface as an avoidable `AlertPublishException`. Trade-ingestion does not constrain `retries` (left at default), so this is a delta. |
| `delivery.timeout.ms` | explicit | `120000` | PASS | `KafkaProducerConfig.java:48` |
| `request.timeout.ms` | explicit | `30000` | PASS | `KafkaProducerConfig.java:49` |
| `compression.type` | set | `lz4` | PASS | `KafkaProducerConfig.java:45` |
| `batch.size` / `linger.ms` | tuned | `16384` / `5` | PASS | `KafkaProducerConfig.java:43-44` |
| `buffer.memory` | sized | (default 32MB) | GAP | not explicitly set; acceptable but not explicit per Phase 2 "explicit beats implicit." |
| `security.protocol` | SASL_SSL / SSL in prod | `SSL` | PASS | `k8s/activity-monitor/activity-monitor-deployment.yaml:38-41` (SPRING_KAFKA_SECURITY_PROTOCOL and SPRING_KAFKA_PROPERTIES_SECURITY_PROTOCOL). Note: MSK is configured TLS-only without SASL, which matches platform convention. |
| Producer-side SSL props | mirror consumer | NOT SET | GAP | `k8s/activity-monitor/activity-monitor-deployment.yaml:42-51` sets `SPRING_KAFKA_CONSUMER_PROPERTIES_SSL_*` only. There are no `SPRING_KAFKA_PRODUCER_PROPERTIES_SSL_*` envs. The producer relies on the top-level `SPRING_KAFKA_PROPERTIES_*` to pick up `security.protocol`, but no `ssl.endpoint.identification.algorithm`, `ssl.truststore.type`, `ssl.enabled.protocols`, or `ssl.protocol` are set for the producer. Inconsistent with the consumer side. |
| Credentials externalized | yes | yes | PASS | brokers come from `msk-secret`; no hardcoded secrets. |
| `client.id` | descriptive, explicit | not set | GAP | `KafkaProducerConfig.java` does not call `ProducerConfig.CLIENT_ID_CONFIG`. Defaults to `producer-<n>` — bad for observability when multiple services share an MSK cluster. Should be `activity-monitor-alert-producer` or similar. |
| Dual-write inside `@Transactional` | `kafkaTemplate.send(...).get(5s)` re-thrown as domain exception | implemented | PASS | `AlertEventProducer.java:67-100` uses `Duration.ofSeconds(5)` `.get(...)` and throws `AlertPublishException` for `TimeoutException`/`ExecutionException`/`InterruptedException`. Called from `ActivityMonitorService.processTrade` (`ActivityMonitorService.java:62`) which is `@Transactional` (line 38). |
| Topic key = `advisorId` | yes | yes | PASS | `AlertEventProducer.java:52` |
| `transactional.id` | N/A (no Kafka transactions used) | not set | N/A | The dual-write pattern uses DB-transaction-bounded send, not Kafka transactions. Consistent with trade-ingestion. |

---

## Consumer audit — `TradeEventConsumer` / `KafkaConsumerConfig`

| Check | Required | Actual | Status | Notes |
|-------|----------|--------|--------|-------|
| `ErrorHandlingDeserializer` wrapping `JsonDeserializer` | required | yes | PASS | `KafkaConsumerConfig.java:47-48` |
| `DefaultErrorHandler` with `ExponentialBackOff(1s, 2.0, max=10s, maxAttempts=3)` | required | yes | PASS | `KafkaConsumerConfig.java:99-101` |
| Non-retryable: `IllegalArgumentException`, `DeserializationException`, `ClassCastException` | required | yes | PASS | `KafkaConsumerConfig.java:108-112` |
| `DeadLetterPublishingRecoverer` → `<topic>.DLT` | required | yes | PASS | `KafkaConsumerConfig.java:89-96` |
| `enable.auto.commit` | `false` | `false` | PASS | `KafkaConsumerConfig.java:51` and `application.yaml:46` (`ack-mode: manual`) |
| `AckMode.MANUAL` + `ack.acknowledge()` | required | yes | PASS | `KafkaConsumerConfig.java:71-73`; `TradeEventConsumer.java:35` |
| `auto.offset.reset` | explicit | `earliest` | PASS | `KafkaConsumerConfig.java:50` |
| `isolation.level=read_committed` | only if producer uses Kafka tx | not set | N/A | Upstream producer (trade-ingestion) uses dual-write, not Kafka transactions. Default `read_uncommitted` is correct here. |
| `group.id` | explicit, conventional | `activity-monitor-service` | PASS | `KafkaConsumerConfig.java:43` AND `TradeEventConsumer.java:19` AND `application.yaml:37` — set in three places. Consistent but redundant; the `@KafkaListener(groupId=...)` overrides the factory. Should be centralized. |
| `group.instance.id` (static membership) | recommended for stable consumers | not set | GAP | No `GROUP_INSTANCE_ID_CONFIG`. With `concurrency=3` and stable pods, static membership would dampen rebalance storms during pod restarts. Not in the Phase 2 sprint scope, but worth noting. |
| `session.timeout.ms` / `heartbeat.interval.ms` | tuned/explicit | not set | GAP | Defaults (45s/3s) are usable, but not explicit. |
| `max.poll.interval.ms` | ≥ max processing time | not set | RISK | Default 5min. `processTrade` runs 12 rules + JPA save + Kafka send (with 5s timeout) per trade and `max.poll.records` is also default (500). Worst-case batch processing time = 500 × (rule time + 5s send timeout) easily exceeds 5min if MSK has latency. Recommend explicit `max.poll.interval.ms` ≥ 300000 with reduced `max.poll.records`. |
| `max.poll.records` | tuned | not set | RISK | Default 500. Combined with synchronous `.get(5s)` per send and rule evaluation per record, a worst-case poll can stall the consumer past `max.poll.interval.ms` and trigger a rebalance. Recommend ~50–100. |
| `fetch.min.bytes` / `fetch.max.wait.ms` / `max.partition.fetch.bytes` | tuned | not set | GAP | All defaults. Acceptable for current throughput; flag as "explicit beats implicit." |
| `partition.assignment.strategy` (cooperative) | recommended | not set | GAP | Defaults to `RangeAssignor` + `CooperativeStickyAssignor` (Kafka 3.x). Phase 2 doesn't mandate cooperative explicitly; safe to leave but worth setting `CooperativeStickyAssignor` explicitly for predictable rebalance behavior. |
| `security.protocol` (prod) | SSL | `SSL` | PASS | `k8s/activity-monitor/activity-monitor-deployment.yaml:38-51` |
| Consumer SSL props in prod | set | partially set | RISK | `SSL_ENDPOINT_IDENTIFICATION_ALGORITHM` is set to empty string (`""`) at line 42-43. MSK supports hostname verification — disabling it weakens TLS. Trade-ingestion does not disable this. Verify intent. |
| `ErrorHandlingDeserializer` value-deserializer default type | `TradeCreatedEvent` | yes | PASS | `KafkaConsumerConfig.java:53-54` |
| `client.id` | explicit | not set | GAP | Same as producer — no `CLIENT_ID_CONFIG`. |
| Concurrency | reasonable | `3` | PASS | `KafkaConsumerConfig.java:68`; matches partition count (`alerts.created` partitions=3 from `KafkaTopicConfig.java:13`). The upstream `trades.raw` has 3 partitions per trade-ingestion's config — concurrency matches. |
| Manual ack of duplicate `DataIntegrityViolationException` | required | yes | PASS | `TradeEventConsumer.java:32-35` catches duplicate, ack proceeds. |

---

## Idempotency & Transactional integrity

| Check | Required | Actual | Status | Notes |
|-------|----------|--------|--------|-------|
| Insert-and-catch idempotency (no check-then-insert) | required | yes | PASS | `IdempotencyService.java:16-22` does a blind `save(...)` keyed on `tradeId`. Consumer catches `DataIntegrityViolationException` (`TradeEventConsumer.java:32`). |
| `processInTransaction(...)` is `@Transactional` | required | yes | PASS | `TradeProcessor.java:17` |
| Idempotency check inside the same tx as DB writes | required | yes | PASS | `TradeProcessor.java:19-20` |
| `ActivityMonitorService.processTrade` is `@Transactional` | required | yes | PASS | `ActivityMonitorService.java:38` |
| `TransactionSynchronization` rolls back `TradeWindowStore` add on STATUS_ROLLED_BACK | required | yes | PASS | `ActivityMonitorService.java:43-51`; `removeTrade` impl is correct (`InMemoryTradeWindowStore.java:30-38`). |
| Cache write happens BEFORE the registerSynchronization call | required | yes (correct ordering) | PASS | `ActivityMonitorService.java:40` then `:43-51`. The sync is registered after the side effect, which is the correct pattern — registration only needs to happen before the tx completes. |
| Producer send is inside the surrounding `@Transactional` | required | yes | PASS | `ActivityMonitorService.java:62` `alertEventProducer.publishAlert(...)` is called inside `processTrade`. |

---

## Topic configuration audit

| Check | Required | Actual | Status | Notes |
|-------|----------|--------|--------|-------|
| `alerts.created` partitions | ≥ 3 | `3` | PASS | `KafkaTopicConfig.java:13` |
| `alerts.created` replicas | `2` (baseline from trade-ingestion) | `1` | **FAIL / GAP** | `KafkaTopicConfig.java:14`. Trade-ingestion uses `replicas=2`; this regresses durability for the alerts topic. |
| `alerts.created` `min.insync.replicas` | `2` explicit | not set | **FAIL / GAP** | `KafkaTopicConfig.java` does not configure `MIN_IN_SYNC_REPLICAS_CONFIG`. With `acks=all` on the producer but `min.insync.replicas` undefined (broker-default 1), `acks=all` collapses to single-replica durability. This is the exact gap that the sprint snapshot calls out for trade-ingestion; same gap exists here. |
| `alerts.created.DLT` declared | yes (since consumer routes to `<topic>.DLT`) | NOT DECLARED | **FAIL / GAP** | The `DeadLetterPublishingRecoverer` in `alert-service` will produce to `alerts.created.DLT`, which no service declares. Phase 2 sprint snapshot lists this exact item for trade-ingestion (declare `trades.raw.DLT`) — same gap here for the alerts side. |
| `trades.raw.DLT` declared (since this consumer routes there) | yes | NOT DECLARED HERE; declared in trade-ingestion | PASS (by virtue of trade-ingestion's KafkaTopicConfig) | `trade-ingestion-service/.../config/KafkaTopicConfig.java:37-47` declares `trades.raw.DLT` as of commit 5e124f4. Cross-service consistency holds. |

---

## Configuration / profile audit

| Check | Required | Actual | Status | Notes |
|-------|----------|--------|--------|-------|
| `server.shutdown: graceful` | required for in-flight Kafka work | NOT SET | RISK | `application.yaml` for activity-monitor has no `server.shutdown: graceful` or `spring.lifecycle.timeout-per-shutdown-phase`. Trade-ingestion sets both (`application.yaml:2-3, 11-12`). Without graceful shutdown, in-flight `processTrade` invocations (with cache writes, DB inserts, and Kafka sends) can be killed mid-transaction by SIGTERM, leaving partial state. This is a regression vs. the trade-ingestion baseline. |
| `JsonDeserializer` trusted packages | restricted | `com.financialsurveillance.events` | PASS | `application.yaml:42` |
| `spring.json.add.type.headers=false` | required to interop with strict deserializers | yes | PASS | `application.yaml:34` and `KafkaConsumerConfig.java:54` (`USE_TYPE_INFO_HEADERS, false`) |
| Profile split (local + prod) | required | yes | PASS | `application-local.yml` (localhost broker), `application-prod.yml` (env-driven). |
| MSK brokers via Secrets Manager | required | yes | PASS | `deployment.yaml:33-37` reads `msk-secret`. |

---

## Kubernetes manifest audit (the focus of "Chunk 5")

| Check | Required | Actual | Status | Notes |
|-------|----------|--------|--------|-------|
| `replicas` ≥ 2 | required for HA (matches trade-ingestion outstanding item) | `1` | **FAIL** | `k8s/activity-monitor/activity-monitor-deployment.yaml:6` |
| `terminationGracePeriodSeconds` | ≥ 30 to drain Kafka | NOT SET | **FAIL** | Trade-ingestion sets `60` (`trade-ingestion-deployment.yaml:15`). Activity-monitor has nothing — defaults to 30s but undocumented and not aligned with `spring.lifecycle.timeout-per-shutdown-phase` (which is also missing). |
| `startupProbe` | required | NOT SET | **FAIL** | Trade-ingestion has one (`trade-ingestion-deployment.yaml:48-54`). |
| `livenessProbe` | required | NOT SET | **FAIL** | Trade-ingestion has one (`:55-60`). |
| `readinessProbe` | required | NOT SET | **FAIL** | Trade-ingestion has one (`:61-66`). Without readiness, K8s routes traffic / counts the pod as ready before Kafka consumer is actually polling — and during rolling updates, both replicas (once `replicas=2`) could be terminated simultaneously. |
| `JAVA_TOOL_OPTIONS` (heap %, ExitOnOOM) | required | NOT SET | **FAIL** | Trade-ingestion sets `-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError` (`trade-ingestion-deployment.yaml:23-24`). Without `ExitOnOutOfMemoryError`, an OOM in a rule evaluation produces a stuck pod that K8s won't restart (no liveness probe either, see above). |
| Resource requests/limits | reasonable | `512Mi/1Gi`, `250m/500m` | RISK | Same shape as trade-ingestion but memory ceiling is 1Gi vs trade-ingestion's 1.28Gi. Activity-monitor holds `TradeWindowStore` in-memory across all advisors plus runs 12 rules — likely needs more headroom than trade-ingestion. |
| `actuator/health/liveness` and `/readiness` exposed | required | `health,info,metrics` included | PASS (config); FAIL (probe wiring) | `application.yaml:54-61` exposes health. The endpoints exist; the manifest just doesn't probe them. |
| `imagePullPolicy: Always` | OK with SHA tags | yes (`:latest` tag) | RISK | `:latest` with `Always` works in CI but defeats reproducibility. CI says image tag = `${GITHUB_SHA::7}` — manifest should match. Trade-ingestion has the same pattern; flag for the sprint, not blocking for Chunk 5. |
| K8s `Service` definition | required | yes | PASS | `activity-monitor-service.yaml` exposes `:8082`. Note: activity-monitor is internal (not via NLB) so ClusterIP is correct. |
| Producer SSL props in deployment | parity with consumer | not set | GAP | See "Producer audit" above. |

---

## Critical findings

1. **Chunk 5 manifest hardening has not started** — `replicas=1`, no probes, no `terminationGracePeriodSeconds`, no `JAVA_TOOL_OPTIONS`. This matches the sprint snapshot's "Remaining: Chunk 5 manifest hardening" — confirmed BLOCKED. File: `k8s/activity-monitor/activity-monitor-deployment.yaml` entire file.

2. **`alerts.created` topic is single-replica, no `min.insync.replicas`, no DLT declared** — `acks=all` on the producer is effectively `acks=1` here. This is the same Phase 2 durability gap that trade-ingestion just closed; activity-monitor needs the equivalent fix. File: `KafkaTopicConfig.java:11-16`.

3. ~~Topic naming drift between CLAUDE.md and code.~~ **RESOLVED 2026-05-23**: `alerts.created` confirmed canonical; CLAUDE.md updated at lines 33, 34, 79, 132.

4. **No `server.shutdown: graceful` / `spring.lifecycle.timeout-per-shutdown-phase`** — `application.yaml` for activity-monitor is missing both. Risk: SIGTERM during `processTrade` can abort a transaction after `tradeWindowStore.addTrade` and after DB insert but before the Kafka send returns, leaving the rollback `TransactionSynchronization` to fire (good) but Spring may not give the listener container time to finish polling/acking. Combined with no `terminationGracePeriodSeconds`, this is an availability bug. File: `application.yaml` (compare to `trade-ingestion-service/src/main/resources/application.yaml:2-12`).

5. **SSL endpoint identification disabled for consumer** — `SPRING_KAFKA_CONSUMER_PROPERTIES_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM=""` (`k8s/activity-monitor/activity-monitor-deployment.yaml:42-43`). Trade-ingestion does not do this. Confirm whether this is required by MSK config or whether it was added during debugging and never reverted.

6. **Producer-side SSL config not mirrored** — All `SPRING_KAFKA_*_SSL_*` envs in the deployment are scoped to `CONSUMER`. The producer relies only on the top-level `SPRING_KAFKA_PROPERTIES_SECURITY_PROTOCOL=SSL`. If the consumer needs explicit truststore/protocol envs to connect to MSK, the producer likely does too. File: `k8s/activity-monitor/activity-monitor-deployment.yaml:42-51`.

---

## Recommended remediations (ordered)

1. ✅ **DONE 2026-05-23** — Resolved topic naming drift (issue #3). Canonical name is `alerts.created`; CLAUDE.md updated.

2. **Harden `alerts.created` topic in `KafkaTopicConfig.java`** — match the trade-ingestion baseline:
   ```java
   private static final int PARTITIONS = 3;
   private static final short REPLICAS = 2;

   @Bean
   public NewTopic alertsCreatedTopic() {
       return TopicBuilder.name(topics.alertsCreated())
               .partitions(PARTITIONS)
               .replicas(REPLICAS)
               .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
               .build();
   }

   @Bean
   public NewTopic alertsCreatedDltTopic() {
       return TopicBuilder.name(topics.alertsCreatedDlt())
               .partitions(PARTITIONS)
               .replicas(REPLICAS)
               .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
               .build();
   }
   ```
   Add `kafka.topics.alerts-created-dlt: alerts.created.DLT` to `application.yaml`. Mirror trade-ingestion's Javadoc warning about `NewTopic` not altering existing topics.

3. **Close out Chunk 5 manifest hardening** — port the trade-ingestion deployment pattern to activity-monitor:
   - `replicas: 2`
   - `terminationGracePeriodSeconds: 60`
   - `JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"`
   - `startupProbe`, `livenessProbe`, `readinessProbe` against `/actuator/health/liveness` and `/actuator/health/readiness` on port `8082`
   - Resource limits: consider `1280Mi`+ memory limit given `TradeWindowStore` in-memory cache
   - Add `spring.kafka.producer.properties.*` SSL envs that mirror the consumer SSL envs (or refactor consumer envs up to `SPRING_KAFKA_PROPERTIES_*` so both clients inherit)
   - Investigate the empty `SSL_ENDPOINT_IDENTIFICATION_ALGORITHM` and remove if not justified

4. **Add `server.shutdown: graceful` + `spring.lifecycle.timeout-per-shutdown-phase: 30s`** to `application.yaml` (mirror trade-ingestion lines 2-3, 11-12).

5. **Set consumer poll-loop guardrails** in `KafkaConsumerConfig.java`:
   ```java
   props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50);
   props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
   props.put(ConsumerConfig.CLIENT_ID_CONFIG, "activity-monitor-trade-consumer");
   ```
   And on the producer side: `props.put(ProducerConfig.CLIENT_ID_CONFIG, "activity-monitor-alert-producer");`

6. **Reconsider `retries=10` on the producer** (`KafkaProducerConfig.java:40`). With `delivery.timeout.ms=120000` and `enable.idempotence=true`, removing the explicit retries cap (default `Integer.MAX_VALUE`) is safer — `delivery.timeout.ms` will bound the retry behavior.

---

## Re-audit delta vs. trade-ingestion baseline

| Item | Trade-ingestion (post 5e124f4) | Activity-monitor (now) | Delta |
|---|---|---|---|
| Source topic `replicas=2` + `min.insync.replicas=2` | YES | NO (`replicas=1`, MISR unset for `alerts.created`) | REGRESSION on durability for produced topic |
| DLT declared | YES (`trades.raw.DLT`) | NO (`alerts.created.DLT` missing) | REGRESSION |
| `server.shutdown: graceful` + lifecycle timeout | YES | NO | REGRESSION |
| `terminationGracePeriodSeconds` | `60` | unset | REGRESSION |
| K8s probes (startup/liveness/readiness) | YES | NO | REGRESSION |
| `JAVA_TOOL_OPTIONS` (ExitOnOOM, MaxRAMPercentage) | YES | NO | REGRESSION |
| `replicas: 2` | `1` (still open) | `1` | SAME (both still open) |
| Dual-write inside `@Transactional` | YES | YES | PARITY |
| `ErrorHandlingDeserializer` + `DefaultErrorHandler` + DLT recoverer | YES | YES | PARITY |
| MSK via Secrets Manager | YES | YES | PARITY |
| SSL endpoint identification | enabled (not disabled) | DISABLED (`""`) | REGRESSION (security) |

Net: activity-monitor is structurally **behind** the trade-ingestion baseline. Closing Chunk 5 requires more than just manifest changes — it requires reaching parity on application-yaml graceful-shutdown, topic durability, and DLT declaration as well.

---

## Closeout punch list for Chunk 5

To mark activity-monitor's Phase 2 chunk complete (in priority order):

1. [x] Resolve `alerts.raw` vs `alerts.created` naming — done 2026-05-23, canonical = `alerts.created`.
2. [ ] `KafkaTopicConfig`: bump alerts topic to `replicas=2`, add `MIN_IN_SYNC_REPLICAS=2`, declare DLT.
3. [ ] `application.yaml`: add `server.shutdown: graceful`, `spring.lifecycle.timeout-per-shutdown-phase: 30s`, add `kafka.topics.alerts-created-dlt`.
4. [ ] `k8s/activity-monitor/activity-monitor-deployment.yaml`: replicas=2, probes (startup/liveness/readiness), `terminationGracePeriodSeconds: 60`, `JAVA_TOOL_OPTIONS`, producer SSL envs, review the empty `SSL_ENDPOINT_IDENTIFICATION_ALGORITHM`.
5. [ ] `KafkaConsumerConfig` / `KafkaProducerConfig`: explicit `client.id` on both; `max.poll.records`, `max.poll.interval.ms` on consumer.
6. [ ] (Nice-to-have, not blocking) `retries` removal on producer; `CooperativeStickyAssignor`; `group.instance.id`.

Items 2–4 are the minimum to close the chunk. Items 5–6 are hardening polish that the team can defer to a follow-up if needed.
