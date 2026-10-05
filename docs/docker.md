# Docker Compose Commands (local stack)

[← index](commands.md)

Run from the repo root.

## Services and ports

```powershell
docker compose config --services
# trade-ingestion-service, activity-monitor-service, alert-service,
# case-management-service, kafka, zookeeper, postgres, kafdrop
```

Ports: trade-ingestion 8081, activity-monitor 8082, alert-service 8083, case-management 8084, kafka 9092, postgres 5432. Kafdrop (topic/partition viewer): http://localhost:9000

## Whole stack

```powershell
docker compose up -d --build
docker compose down
docker compose ps
```

## Rebuild + restart one service from source

```powershell
docker compose up -d --build trade-ingestion-service
docker compose up -d --build activity-monitor-service
docker compose up -d --build alert-service
docker compose up -d --build case-management-service
```

## Follow logs (Ctrl+C to stop)

```powershell
docker compose logs -f trade-ingestion-service
docker compose logs -f activity-monitor-service
docker compose logs -f alert-service
docker compose logs -f case-management-service
docker compose logs postgres --tail 30
```

## Port conflicts / stale containers

```powershell
# Who owns port 5432?
netstat -ano | Select-String ":5432"
Get-Process -Id <pid> | Select-Object Name, Path

# Container exists but port not published (ports: added after creation)
docker compose up -d --force-recreate postgres
```

- The client SaaS project's Postgres also binds 5432 — stop that stack before starting this one. Symptom: `28P01 password authentication failed` with the right password.
- `docker compose ps` showing `5432/tcp` with no `0.0.0.0:5432->` means the port isn't published. `--force-recreate` fixes it; the data volume survives.
- Kafka `NodeExistsException` on restart → Zookeeper still holds broker-1; wait ~20s and rerun.
