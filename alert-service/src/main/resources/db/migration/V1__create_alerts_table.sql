CREATE TABLE alert_management.alerts (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alert_id          UUID NOT NULL UNIQUE,
    alert_type_id     VARCHAR(50) NOT NULL,
    trade_id          VARCHAR(50) NOT NULL,
    advisor_id        VARCHAR(50) NOT NULL,
    rule_id           VARCHAR(50) NOT NULL,
    rule_name         VARCHAR(100) NOT NULL,
    severity          VARCHAR(20) NOT NULL,
    status            VARCHAR(20) NOT NULL,
    violation_details JSONB,
    created_at        TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_alerts_advisor_id ON alert_management.alerts(advisor_id);
CREATE INDEX idx_alerts_rule_id ON alert_management.alerts(rule_id);
CREATE INDEX idx_alerts_status ON alert_management.alerts(status);