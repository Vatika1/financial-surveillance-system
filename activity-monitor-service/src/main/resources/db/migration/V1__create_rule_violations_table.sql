CREATE TABLE IF NOT EXISTS activity_monitor.rule_violations (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trade_id         VARCHAR(50)  NOT NULL,
    advisor_id       VARCHAR(50)  NOT NULL,
    rule_id          VARCHAR(50)  NOT NULL,
    rule_name        VARCHAR(100) NOT NULL,
    severity         VARCHAR(20)  NOT NULL,
    violation_details JSONB,
    detected_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rule_violations_advisor_id  ON activity_monitor.rule_violations(advisor_id);
CREATE INDEX idx_rule_violations_rule_id     ON activity_monitor.rule_violations(rule_id);
CREATE INDEX idx_rule_violations_detected_at ON activity_monitor.rule_violations(detected_at);