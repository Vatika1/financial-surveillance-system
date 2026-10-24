# Local Testing and Debugging (Docker Compose)

[← index](commands.md)

PowerShell, from the repo root, with the stack up (`docker compose up -d`). For starting, stopping and rebuilding the stack, see [docker.md](docker.md). For the same checks on EKS, see [cluster-testing.md](cluster-testing.md).

**Rule:** a trade sent to `localhost:8081` is checked with `docker logs`. Local containers do not ship logs to Loki, so Grafana never shows local trades.

`docker compose up -d --build <service>` builds from the files on disk, so an unpushed change is tested locally. Rebuild only the service you changed.

Compose runs every service with `SPRING_PROFILES_ACTIVE: prod`, so the `TradeSimulator` is off: the only trades are the ones you send.

Container names: `surveillance-trade-ingestion`, `surveillance-activity-monitor`, `surveillance-alert-service`, `surveillance-case-management`, `surveillance-kafka`, `surveillance-postgres`, `surveillance-redis`.

## The usual loop

```powershell
# 1. Rebuild the service you changed
docker compose up -d --build activity-monitor-service

# 2. Wait until it has started (the time must be after the rebuild)
docker logs surveillance-activity-monitor 2>&1 | Select-String "Started ActivityMonitor" | Select-Object -Last 1

# 3. Send trades (next section)
# 4. Check logs, Postgres, Redis and Kafka (sections below)
```

**Gotcha:** if step 2 shows an old time, the container is still running the previous image.

### Per service

Rebuild:

```powershell
docker compose up -d --build trade-ingestion-service
docker compose up -d --build activity-monitor-service
docker compose up -d --build alert-service
docker compose up -d --build case-management-service
```

Has it started? (time must be after the rebuild)

```powershell
docker logs surveillance-trade-ingestion 2>&1 | Select-String "Started TradeIngestion" | Select-Object -Last 1
docker logs surveillance-activity-monitor 2>&1 | Select-String "Started ActivityMonitor" | Select-Object -Last 1
docker logs surveillance-alert-service 2>&1 | Select-String "Started AlertService" | Select-Object -Last 1
docker logs surveillance-case-management 2>&1 | Select-String "Started CaseManagement" | Select-Object -Last 1
```

All four at once:

```powershell
"surveillance-trade-ingestion","surveillance-activity-monitor","surveillance-alert-service","surveillance-case-management" | ForEach-Object {
  "$_ : " + (docker logs $_ 2>&1 | Select-String "Started .*Application" | Select-Object -Last 1)
}
```

Last 5 minutes of logs:

```powershell
docker logs surveillance-trade-ingestion --since 5m 2>&1
docker logs surveillance-activity-monitor --since 5m 2>&1
docker logs surveillance-alert-service --since 5m 2>&1
docker logs surveillance-case-management --since 5m 2>&1
```

Real errors only:

```powershell
docker logs surveillance-trade-ingestion --since 15m 2>&1 | Select-String -CaseSensitive '"level":"ERROR"'
docker logs surveillance-activity-monitor --since 15m 2>&1 | Select-String -CaseSensitive '"level":"ERROR"'
docker logs surveillance-alert-service --since 15m 2>&1 | Select-String -CaseSensitive '"level":"ERROR"'
docker logs surveillance-case-management --since 15m 2>&1 | Select-String -CaseSensitive '"level":"ERROR"'
```

What each service logs for one healthy trade:

```powershell
docker logs surveillance-trade-ingestion --since 5m 2>&1 | Select-String "Processing trade|Trade persisted"
docker logs surveillance-activity-monitor --since 5m 2>&1 | Select-String "Received batch|Published|Successfully processed|Sending to DLT"
docker logs surveillance-alert-service --since 5m 2>&1 | Select-String "Received batch|Successfully processed|Sending to DLT"
docker logs surveillance-case-management --since 5m 2>&1 | Select-String "Received batch|Creating|Published CaseCreatedEvent|Successfully processed"
```

## Send trades

Use a new trade ID every run: trade-ingestion returns `409 Conflict` for an ID it has seen before.

One trade:

```powershell
$ts = (Get-Date).ToUniversalTime().AddSeconds(-5).ToString("yyyy-MM-ddTHH:mm:ssZ")
"{`"tradeId`":`"T-TEST-1`",`"advisorId`":`"ADV-TEST`",`"accountId`":`"ACC-1`",`"clientId`":`"CL-1`",`"symbol`":`"AAPL`",`"tradeType`":`"BUY`",`"quantity`":100,`"price`":150.25,`"currency`":`"USD`",`"exchange`":`"NASDAQ`",`"tradeTimestamp`":`"$ts`",`"sourceSystem`":`"MANUAL`",`"sourceSystemId`":`"SRC-1`"}" | Set-Content -Encoding ascii body.json
curl.exe -s -X POST http://localhost:8081/api/trades -H "Content-Type: application/json" --data-binary "@body.json"
Remove-Item body.json
```

Then check it went through every service (wait ~10 s first; change `T-TEST-1` to your trade ID):

```powershell
# 1. trade-ingestion accepted it — and grab its correlationId
$trade = "T-TEST-1"
docker logs surveillance-trade-ingestion 2>&1 | Select-String $trade
$cid = ((docker logs surveillance-trade-ingestion 2>&1 | Select-String $trade | Select-Object -First 1).Line | ConvertFrom-Json).correlationId
"correlationId = $cid"

# 2. activity-monitor: Received batch (once) → Published N AlertCreatedEvents → Successfully processed
docker logs surveillance-activity-monitor 2>&1 | Select-String $cid

# 3. alert-service: Received batch of N alerts (N values add up to the alert count)
docker logs surveillance-alert-service 2>&1 | Select-String $cid

# 4. case-management: Received batch of N alerts
docker logs surveillance-case-management 2>&1 | Select-String $cid

# 5. nothing failed
docker logs surveillance-activity-monitor --since 5m 2>&1 | Select-String "Sending to DLT"
```

Readable version of steps 2–4 (message text only):

```powershell
"surveillance-activity-monitor","surveillance-alert-service","surveillance-case-management" | ForEach-Object {
  "--- $_"
  docker logs $_ 2>&1 | Select-String $cid | ForEach-Object { ($_.Line | ConvertFrom-Json).message }
}
```

Several trades at once (they arrive in activity-monitor as one batch):

```powershell
foreach ($i in 1..5) {
  $ts = (Get-Date).ToUniversalTime().AddSeconds(-5).ToString("yyyy-MM-ddTHH:mm:ssZ")
  "{`"tradeId`":`"T-BATCH-$i`",`"advisorId`":`"ADV-BATCH`",`"accountId`":`"ACC-1`",`"clientId`":`"CL-1`",`"symbol`":`"AAPL`",`"tradeType`":`"BUY`",`"quantity`":100,`"price`":150.25,`"currency`":`"USD`",`"exchange`":`"NASDAQ`",`"tradeTimestamp`":`"$ts`",`"sourceSystem`":`"MANUAL`",`"sourceSystemId`":`"SRC-B-$i`"}" | Set-Content -Encoding ascii body.json
  curl.exe -s -X POST http://localhost:8081/api/trades -H "Content-Type: application/json" --data-binary "@body.json"
}
Remove-Item body.json
```

- The timestamp is 5 seconds ago: `@PastOrPresent` rejects future times, and the Redis window only counts recent trades.
- `AAPL` is a restricted symbol, so every trade raises at least one alert.
- A `500` with "Required request body is missing" means the body never got sent; check `body.json` was written.
- `curl.exe -s -w "`n%{http_code}`n" ...` also prints the HTTP status on its own line.

## Test scenarios

Change the `tradeId` (and the field under test) in the one-trade command above. Same scenarios work on the cluster: swap `localhost:8081` for the NLB ([cluster-testing.md](cluster-testing.md)).

| # | Scenario | Body change | HTTP | Expected in activity-monitor |
|---|---|---|---|---|
| 1 | Happy path | as above (`exchange: NASDAQ`) | 201 | `Received batch` once → `Published N AlertCreatedEvents` → `Successfully processed 1 trades` |
| 2 | No exchange (Rule013 null case) | remove the `exchange` field | 201 | Same as 1. With the bug: `Received batch` 4 times (1s/2s/4s), then `Sending to DLT` ERROR |
| 3 | OTC trade | `exchange: OTC` | 201 | RULE_013 violation raised (one extra alert) |
| 4 | Duplicate tradeId | resend the same body | 409 `Trade already exists` | Nothing — the request never reaches Kafka |
| 5 | Invalid request | remove a `@NotBlank` field, e.g. `symbol` | 400 | Nothing |
| 6 | Batch | the 5-trade loop above | 201 ×5 | `Received batch of N trades` with N > 1 |
| 7 | Idempotency-Key, first call | add `-H "Idempotency-Key: key-1"` | 201 | Normal processing; Redis `idem:trades:key-1` holds the saved response |
| 8 | Idempotency-Key, replay | same key, same body | 201 (saved body) | Nothing new; only one row in `trade_ingestion.trades` |
| 9 | Idempotency-Key, in progress | `SET idem:trades:key-2 PROCESSING EX 60` in Redis, then send with `key-2` | 409 | Nothing |

Redis checks for 7–9:

```powershell
docker exec -it surveillance-redis redis-cli GET idem:trades:key-1
docker exec -it surveillance-redis redis-cli TTL idem:trades:key-1      # -2 = key does not exist
```

## Follow one trade through every service

Each request gets a `correlationId` in trade-ingestion, carried as a Kafka header. Pull it out of the trade-ingestion log, then search the downstream services with it:

```powershell
$cid = ((docker logs surveillance-trade-ingestion 2>&1 | Select-String "T-TEST-1" | Select-Object -First 1).Line | ConvertFrom-Json).correlationId
docker logs surveillance-activity-monitor 2>&1 | Select-String $cid
docker logs surveillance-alert-service 2>&1 | Select-String $cid
docker logs surveillance-case-management 2>&1 | Select-String $cid
```

What "healthy" looks like:

| Service | Lines |
|---|---|
| trade-ingestion | `Processing trade tradeId=...`, `Trade persisted tradeId=...` |
| activity-monitor | `Received batch of 1 trades`, `Marked processed`, `Processing 1 trades`, `Published N AlertCreatedEvents`, `Successfully processed 1 trades` — each once |
| alert-service | `Received batch of N alerts` — the N values add up to the alert count (alerts can arrive in several polls) |
| case-management | `Received batch of N alerts` only. Its other lines (`Creating N cases`, `Published CaseCreatedEvent`) carry no correlationId yet — search those by text and time |

**Gotcha:** a 409 request gets its own correlationId but never reaches Kafka, so downstream searches for it return nothing. Trace the original 201 request instead.

## Silent consumer failures

A listener exception is retried 3 times (1s, 2s, 4s) and then routed to `<topic>.DLT`. The `loggingRecoverer` in `KafkaConsumerConfig` logs one ERROR at that point; before it existed, the only sign was the same trade received 4 times.

```powershell
# Anything routed to a DLT in the last 15 minutes
docker logs surveillance-activity-monitor --since 15m 2>&1 | Select-String "Sending to DLT"

# The root cause line from that ERROR
docker logs surveillance-activity-monitor --since 15m 2>&1 | Select-String "Sending to DLT" | ForEach-Object { ($_.Line | ConvertFrom-Json).stack_trace -split "`n" } | Select-String "Caused by" -Context 0,1

# Retry pattern for one trade: more than one "Received batch" = it failed at least once
docker logs surveillance-activity-monitor 2>&1 | Select-String $cid | Select-String "Received batch"
```

## Logs

Logs are JSON lines. Always add `2>&1`, otherwise `Select-String` misses lines.

| Command | When |
|---|---|
| `docker logs surveillance-activity-monitor --tail 50` | Last 50 lines |
| `docker logs surveillance-activity-monitor --since 5m 2>&1` | Everything from the last 5 minutes |
| `docker logs surveillance-activity-monitor 2>&1 \| Select-String "T-BATCH-1"` | Every line about one trade |
| `docker logs surveillance-activity-monitor --since 5m 2>&1 \| Select-String -CaseSensitive '"level":"ERROR"'` | Real errors only |
| `docker logs surveillance-activity-monitor --since 5m 2>&1 \| Select-String -NotMatch "Config values\|ConsumerConfig\|ProducerConfig" \| Select-Object -Last 40` | Recent lines without the Kafka config dumps |
| `docker logs -f surveillance-activity-monitor` | Follow live (Ctrl+C to stop) |

**Gotcha:** searching for `ERROR` without `-CaseSensitive '"level":"ERROR"'` also matches `ErrorHandlingDeserializer` in every config dump.

## Postgres

Open a SQL prompt (`\q` to quit):

```powershell
docker exec -it surveillance-postgres psql -U surveillance -d surveillance
```

One-off queries:

```powershell
# Was the batch committed? Rows = committed, none = rolled back
docker exec -it surveillance-postgres psql -U surveillance -d surveillance -c "select trade_id, processed_at from activity_monitor.processed_events where trade_id like 'T-BATCH%';"

# Rule violations saved for an advisor
docker exec -it surveillance-postgres psql -U surveillance -d surveillance -c "select rule_id, trade_id from activity_monitor.rule_violations where advisor_id = 'ADV-BATCH';"

# List the tables in a schema
docker exec -it surveillance-postgres psql -U surveillance -d surveillance -c "\dt activity_monitor.*"
```

Schemas: `activity_monitor`, plus one per service. Use `\dn` at the SQL prompt to list them.

## Redis (trade windows)

One sorted set per advisor: key `window:<advisorId>`, member = trade JSON, score = trade time in epoch millis.

| Command | What it shows |
|---|---|
| `docker exec -it surveillance-redis redis-cli KEYS "window:*"` | All advisor windows |
| `docker exec -it surveillance-redis redis-cli ZCARD window:ADV-BATCH` | How many trades are in the window |
| `docker exec -it surveillance-redis redis-cli ZRANGE window:ADV-BATCH 0 -1 WITHSCORES` | The trades and their timestamps |
| `docker exec -it surveillance-redis redis-cli DEL window:ADV-BATCH` | Delete one test window |
| `docker exec -it surveillance-redis redis-cli` | Interactive prompt (`exit` to quit) |

**Gotcha:** `ZCARD` of 0 after sending trades means they were never added or were removed by a rollback. Check `processed_events` next.

## Kafka

Topics: `trades.raw`, `alerts.created`, `alerts.persisted`, `cases.created`, `cases.closed`, plus a `.DLT` (dead letter topic) for each consumed topic. A DLT must have the same partition count as its source topic: `DeadLetterPublishingRecoverer` writes to the same partition number, and a missing partition makes the batch re-seek forever.

```powershell
# Partition counts side by side — every .DLT should match its source
docker exec surveillance-kafka kafka-topics --bootstrap-server kafka:29092 --describe | Select-String "PartitionCount"

# Raise a DLT that is short (partitions can only go up)
docker exec surveillance-kafka kafka-topics --bootstrap-server kafka:29092 --alter --topic alerts.created.DLT --partitions 6
```

| Command | When |
|---|---|
| `docker exec surveillance-kafka kafka-topics --bootstrap-server localhost:9092 --list` | List topics |
| `docker exec surveillance-kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic trades.raw` | Partitions of a topic |
| `docker exec surveillance-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group activity-monitor-service` | Offsets and lag per partition |
| `docker exec surveillance-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic alerts.created --from-beginning --timeout-ms 10000` | Read a topic (stops after 10 s with no new message) |

Kafdrop shows topics, partitions and messages in the browser: http://localhost:9000

### Why did a record go to the DLT?

The failure reason is stored in the record headers (`kafka_dlt-exception-message`, `Caused by`). The stack trace sits between the headers and the record JSON, so search backwards from the trade ID:

```powershell
docker exec surveillance-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic trades.raw.DLT --from-beginning --property print.headers=true --timeout-ms 10000 2>&1 | Select-String "T-BATCH-1" -Context 120,0 | ForEach-Object { $_.Context.PreContext } | Select-String "exception-message|Caused by"
```

**Gotcha:** `--from-beginning` starts with the oldest record in the topic, which may be from a much older test. Always filter by the trade ID you just sent.

A record that lands in the DLT with no retries threw one of the non-retryable exceptions (`IllegalArgumentException`, `DeserializationException`, `ClassCastException`). Example: a `ClassCastException` from `TradeCreatedEvent` to `ConsumerRecord` means the listener method takes a `List` but `setBatchListener(true)` is missing from the container factory.

## Run tests without Docker

```powershell
mvn -q compile -pl activity-monitor-service -am
mvn test -pl activity-monitor-service -am 2>&1 | Select-String "Tests run:|FAIL|ERROR\]|BUILD"
mvn test -pl activity-monitor-service -Dtest=RedisTradeWindowStoreTest
```

The Docker build skips tests (`-DskipTests`), so failing tests don't stop `docker compose up --build`. CI runs them.
