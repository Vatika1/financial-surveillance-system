-- Idempotency: one row per alertId this service has already handled.
-- Consumer inserts here first; a duplicate key means "already processed, skip".
CREATE TABLE case_management.processed_alerts (
                                                  alert_id UUID PRIMARY KEY,
                                                  processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);