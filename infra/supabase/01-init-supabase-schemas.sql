-- ============================================================
-- VyroFlix — Supabase PostgreSQL Schema Initialization
-- Run this in the Supabase SQL Editor prior to first deployment
-- ============================================================

-- Enable pgcrypto for UUID generation if not already active
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create microservice domain schemas
CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS content;
CREATE SCHEMA IF NOT EXISTS video;
CREATE SCHEMA IF NOT EXISTS encoding;
CREATE SCHEMA IF NOT EXISTS streaming;
CREATE SCHEMA IF NOT EXISTS history;
CREATE SCHEMA IF NOT EXISTS search;
CREATE SCHEMA IF NOT EXISTS recommendation;

-- Grant full usage and permissions to postgres and authenticated roles
GRANT ALL ON SCHEMA identity TO postgres, authenticated, service_role;
GRANT ALL ON SCHEMA content TO postgres, authenticated, service_role;
GRANT ALL ON SCHEMA video TO postgres, authenticated, service_role;
GRANT ALL ON SCHEMA encoding TO postgres, authenticated, service_role;
GRANT ALL ON SCHEMA streaming TO postgres, authenticated, service_role;
GRANT ALL ON SCHEMA history TO postgres, authenticated, service_role;
GRANT ALL ON SCHEMA search TO postgres, authenticated, service_role;
GRANT ALL ON SCHEMA recommendation TO postgres, authenticated, service_role;

-- Set search path to include all microservice schemas
ALTER DATABASE postgres SET search_path TO public, content, identity, video, encoding, streaming, history, search, recommendation;
