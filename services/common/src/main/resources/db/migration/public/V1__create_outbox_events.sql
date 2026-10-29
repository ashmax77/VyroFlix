-- Shared transactional outbox for the combined platform. OutboxEvent maps to an unqualified outbox_events table,
-- and one relay publishes events from every domain, so the table lives in public. Domain writes and their outbox
-- rows still commit in the same transaction, because both use the same datasource.

CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    producer VARCHAR(100) NOT NULL,
    correlation_id UUID NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_outbox_status ON outbox_events(status, created_at);
