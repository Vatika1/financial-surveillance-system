CREATE TABLE activity_monitor.processed_events (
    trade_id VARCHAR(50) PRIMARY KEY,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE activity_monitor.processed_events IS
    'Idempotency tracking — records every tradeId successfully processed by activity-monitor. PK on trade_id enforces deduplication atomically.';