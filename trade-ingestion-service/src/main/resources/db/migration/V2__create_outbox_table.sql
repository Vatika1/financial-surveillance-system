CREATE TABLE IF NOT EXISTS trade_ingestion.outbox
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id    VARCHAR(50)  NOT NULL,
    topic           VARCHAR(100) NOT NULL,
    payload         TEXT         NOT NULL,
    headers         TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    sent_at         TIMESTAMPTZ
    );

CREATE INDEX IF NOT EXISTS idx_outbox_unsent
    ON trade_ingestion.outbox (created_at)
    WHERE sent_at IS NULL;