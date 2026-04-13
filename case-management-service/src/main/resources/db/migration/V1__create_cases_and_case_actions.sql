CREATE TABLE cases (
    id              UUID         PRIMARY KEY,
    alert_id        UUID         NOT NULL,
    advisor_id      VARCHAR(64)  NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    assigned_to     VARCHAR(64),
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    closed_at       TIMESTAMPTZ
);

CREATE TABLE case_actions (
    id              UUID         PRIMARY KEY,
    case_id         UUID         NOT NULL REFERENCES cases(id),
    action_type     VARCHAR(32)  NOT NULL,
    performed_by    VARCHAR(64)  NOT NULL,
    performed_at    TIMESTAMPTZ  NOT NULL,
    from_value      VARCHAR(128),
    to_value        VARCHAR(128)
);

CREATE INDEX idx_cases_status          ON cases(status);
CREATE INDEX idx_cases_assigned_to     ON cases(assigned_to);
CREATE INDEX idx_cases_advisor_id      ON cases(advisor_id);
CREATE INDEX idx_case_actions_case_id  ON case_actions(case_id);