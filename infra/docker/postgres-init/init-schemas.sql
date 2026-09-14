-- ============================================================
-- VyroFlix — PostgreSQL Schema Initialization
-- Runs automatically on first startup via /docker-entrypoint-initdb.d
-- ============================================================

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Dedicated service schemas preserving microservice data ownership
CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS content;
CREATE SCHEMA IF NOT EXISTS video;
CREATE SCHEMA IF NOT EXISTS encoding;
CREATE SCHEMA IF NOT EXISTS streaming;
CREATE SCHEMA IF NOT EXISTS history;
CREATE SCHEMA IF NOT EXISTS search;
CREATE SCHEMA IF NOT EXISTS recommendation;

-- Grant permissions to default user
GRANT ALL ON SCHEMA identity TO vyroflix;
GRANT ALL ON SCHEMA content TO vyroflix;
GRANT ALL ON SCHEMA video TO vyroflix;
GRANT ALL ON SCHEMA encoding TO vyroflix;
GRANT ALL ON SCHEMA streaming TO vyroflix;
GRANT ALL ON SCHEMA history TO vyroflix;
GRANT ALL ON SCHEMA search TO vyroflix;
GRANT ALL ON SCHEMA recommendation TO vyroflix;
