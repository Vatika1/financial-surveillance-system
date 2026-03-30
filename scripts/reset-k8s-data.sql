
-- Reset all surveillance data
TRUNCATE TABLE alert_management.alerts CASCADE;
TRUNCATE TABLE activity_monitor.rule_violations CASCADE;
TRUNCATE TABLE trade_ingestion.trades CASCADE;