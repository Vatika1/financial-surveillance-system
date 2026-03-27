ALTER TABLE alert_management.alerts DROP COLUMN id;
ALTER TABLE alert_management.alerts ADD PRIMARY KEY (alert_id);