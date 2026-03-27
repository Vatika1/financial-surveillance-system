#!/bin/bash

KAFKA="docker exec surveillance-kafka kafka-topics --bootstrap-server localhost:9092"

echo "🗑️ Deleting Kafka topics..."
$KAFKA --delete --topic trades.raw
$KAFKA --delete --topic alerts.created
$KAFKA --delete --topic alerts.persisted

echo "🗑️ Clearing database tables..."
docker exec -i surveillance-postgres psql -U surveillance -d surveillance <<EOF
DELETE FROM activity_monitor.rule_violations;
DELETE FROM trade_ingestion.trades;
DELETE FROM alert_management.alerts;
EOF

echo "✅ Reset complete! Now restart your services."