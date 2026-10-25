CREATE TABLE activity_monitor.advisor_daily_activity (
                                                         advisor_id   VARCHAR(50)    NOT NULL,
                                                         trade_date   DATE           NOT NULL,
                                                         trade_count  INTEGER        NOT NULL DEFAULT 0,
                                                         total_value  NUMERIC(19,4)  NOT NULL DEFAULT 0,
                                                         reconciled   BOOLEAN        NOT NULL DEFAULT FALSE,
                                                         updated_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
                                                         PRIMARY KEY (advisor_id, trade_date)
);

COMMENT ON TABLE activity_monitor.advisor_daily_activity IS
    'Running daily trade totals per advisor. Updated by activity-monitor per trade; reconciled at end of day by eod-reconciliation-job.';