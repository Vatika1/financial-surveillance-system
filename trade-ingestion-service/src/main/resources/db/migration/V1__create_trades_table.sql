CREATE TABLE IF NOT EXISTS trade_ingestion.trades
(
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trade_id            VARCHAR(50)    UNIQUE NOT NULL,
    advisor_id          VARCHAR(50)    NOT NULL,
    account_id          VARCHAR(50)    NOT NULL,
    client_id           VARCHAR(50)    NOT NULL,
    symbol              VARCHAR(20)    NOT NULL,
    trade_type          VARCHAR(10)    NOT NULL,
    quantity            DECIMAL(15, 2) NOT NULL,
    price               DECIMAL(15, 4) NOT NULL,
    total_value         DECIMAL(18, 2) NOT NULL,
    currency            VARCHAR(3)              DEFAULT 'USD',
    exchange            VARCHAR(20),
    trade_timestamp     TIMESTAMPTZ    NOT NULL,
    source_system        VARCHAR(50),
    source_system_id     VARCHAR(100),
    status              VARCHAR(20)             DEFAULT 'RECEIVED',
    created_at          TIMESTAMPTZ             DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_trades_advisor_id
    ON trade_ingestion.trades (advisor_id);

CREATE INDEX IF NOT EXISTS idx_trades_symbol
    ON trade_ingestion.trades (symbol);

CREATE INDEX IF NOT EXISTS idx_trades_timestamp
    ON trade_ingestion.trades (trade_timestamp);

CREATE INDEX IF NOT EXISTS idx_trades_account_id
    ON trade_ingestion.trades (account_id);

CREATE INDEX IF NOT EXISTS idx_trades_status
    ON trade_ingestion.trades (status);

CREATE INDEX IF NOT EXISTS idx_trades_source_system
     ON trade_ingestion.trades (source_system);