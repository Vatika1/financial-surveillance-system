-- Creates a separate schema for each microservice
-- Runs automatically when PostgreSQL container starts for the first time
-- Each service owns its schema — they never share tables

CREATE SCHEMA IF NOT EXISTS trade_ingestion;
CREATE SCHEMA IF NOT EXISTS activity_monitor;
CREATE SCHEMA IF NOT EXISTS alert_management;
CREATE SCHEMA IF NOT EXISTS case_management;
CREATE SCHEMA IF NOT EXISTS notification;
CREATE SCHEMA IF NOT EXISTS audit;
CREATE SCHEMA IF NOT EXISTS reporting;
CREATE SCHEMA IF NOT EXISTS user_management;