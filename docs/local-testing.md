# Local Testing and Debugging (Docker Compose)

[← index](commands.md)

PowerShell, from the repo root, with the stack up (`docker compose up -d`). For starting, stopping and rebuilding the stack, see [docker.md](docker.md).

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

## Send trades

Use a new trade ID every run: trade-ingestion returns `409 Conflict` for an ID it has seen before.

One trade:

```powershell
$ts = (Get-Date).ToUniversalTime().AddSeconds(-5).ToString("yyyy-MM-ddTHH:mm:ssZ")
"{`"tradeId`":`"T-TEST-1`",`"advisorId`":`"ADV-TEST`",`"accountId`":`"ACC-1`",`"clientId`":`"CL-1`",`"symbol`":`"AAPL`",`"tradeType`":`"BUY`",`"quantity`":100,`"price`":150.25,`"currency`":`"USD`",`"exchange`":`"NASDAQ`",`"tradeTimestamp`":`"$ts`",`"sourceSystem`":`"MANUAL`",`"sourceSystemId`":`"SRC-1`"}" | Set-Content -Encoding ascii body.json
curl.exe -s -X POST http://localhost:8081/api/trades -H "Content-Type: application/json" --data-binary "@body.json"
Remove-Item body.json
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

Topics: `trades.raw`, `alerts.created`, `alerts.persisted`, plus a `.DLT` (dead letter topic) for each.

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
